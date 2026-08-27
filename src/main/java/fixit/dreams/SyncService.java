package fixit.dreams;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fixit.dreams.sync.AuthResult;
import fixit.dreams.sync.DreamFingerprint;
import fixit.dreams.sync.FirebaseAuthClient;
import fixit.dreams.sync.FirebaseAuthException;
import fixit.dreams.sync.FirestoreClient;
import fixit.dreams.sync.FirestoreException;
import fixit.dreams.sync.MetaMerge;
import fixit.dreams.sync.SyncDTO;
import fixit.dreams.sync.SyncMerge;
import fixit.dreams.sync.SyncObjectMapper;
import fixit.dreams.sync.Tombstone;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

// Orkestrerer den valgfri cloud-synkronisering. Ligger bevidst i fixit.dreams (ikke
// fixit.dreams.sync) fordi den er tæt koblet til User/Dream/DreamData/IOutils - selve
// netværks-/protokollaget (auth, Firestore, JSON-oversættelse) ligger i fixit.dreams.sync
// og er det eneste denne klasse importerer derfra.
//
// Synkroniseringen dækker to slags data, med hver sin flettemodel:
//
//   drømmene            users/{uid}/dreams/{id}   - ét dokument pr. drøm, flettet stykke for
//                                                   stykke, med gravsten for sletninger
//   kategori-definitioner,
//   temaer, indstillinger
//                       users/{uid}/meta/{navn}   - ét dokument hver, hvor hele dokumentet
//                                                   vinder eller taber samlet (se MetaMerge)
//
// De to modeller er forskellige med vilje: drømme skrives der hele tiden nye af, og to
// maskiner må kunne bidrage hver sine uden at overskrive hinanden. De tre meta-lister ændres
// sjældent, og til gengæld følger sletninger gratis med når hele listen er enheden - en
// fjernet kategori er bare en kortere liste, og der er ingen gravsten at føre.
public class SyncService {
    private static final String META_CATEGORIES = "categories";
    private static final String META_TEMAER = "temaer";
    private static final String META_SETTINGS = "settings";

    // Ejerskabsdokumentet: hvem skrev her sidst, og med hvilke meta-tidsstempler. Det er det
    // eneste dokument en almindelig sync behøver læse (se syncNow).
    private static final String META_STATE = "state";

    private final User user;
    private final FirebaseAuthClient authClient;
    private final FirestoreClient firestoreClient;

    public SyncService(User user) {
        this(user, new FirebaseAuthClient(), new FirestoreClient());
    }

    // Klienterne kan skiftes ud, så hele orkestreringen kan køres mod en fake sky i unittests -
    // uden netværk, uden et rigtigt Firebase-projekt og uden at nogen risikerer at teste mod
    // brugerens egne data. Selve netværkskoden dækkes ikke af det, og skal stadig røgtestes.
    SyncService(User user, FirebaseAuthClient authClient, FirestoreClient firestoreClient) {
        this.user = user;
        this.authClient = authClient;
        this.firestoreClient = firestoreClient;
    }

    public SyncDTO getStatus() {
        return IOutils.loadSync();
    }

    public void signUp(String email, String password) throws FirebaseAuthException {
        AuthResult result = authClient.signUp(email, password);
        persistLogin(email, result);
    }

    public void signIn(String email, String password) throws FirebaseAuthException {
        AuthResult result = authClient.signIn(email, password);
        persistLogin(email, result);
    }

    private void persistLogin(String email, AuthResult result) {
        SyncDTO dto = new SyncDTO();
        dto.email = email;
        dto.refreshToken = result.refreshToken();
        dto.uid = result.uid();
        dto.syncEnabled = true;
        dto.lastSyncedAt = null;
        IOutils.saveSync(dto);
    }

    // Sletter kun sync.json - rører aldrig dreams.json/cats.json/user.json.
    public void logout() {
        IOutils.deleteSync();
    }

    public void setSyncEnabled(boolean enabled) {
        SyncDTO dto = IOutils.loadSync();
        if (dto == null) return;
        dto.syncEnabled = enabled;
        IOutils.saveSync(dto);
    }

    public void syncNow() throws SyncException {
        syncNow(false);
    }

    // Fuld synkronisering: henter et gyldigt idToken, pull'er sky-drømme der er nyere end de
    // lokale (eller helt mangler lokalt), og push'er lokale drømme der er nyere end skyen
    // (eller helt mangler i skyen). Stille no-op hvis sync ikke er sat op/slået fra.
    //
    // Faresignalet er IKKE "er det her den første sync", og heller ikke bare "ID'erne overlapper
    // ikke" - begge dele sker jo helt normalt og ufarligt (fx log ud/log ind igen, eller en frisk
    // installation hvor man har skrevet et par nye drømme inden man synkroniserer). Signalet er,
    // at lokale drømme INDHOLDSMÆSSIGT allerede ligger i skyen under et andet ID: kun dét kan
    // blive til dubletter ved en sammenkøring (se countLikelyDuplicates/SyncConflictException).
    //
    // Der findes TO veje igennem her, og hvilken der tages afgøres af meta/state-dokumentet:
    //
    //   den billige   vi skrev selv sidst  -> 1 læsning. Skyen er vores eget spejl, og
    //                                        cloudindex.json ved allerede hvad der ligger der.
    //   den dyre      alle andre tilfælde  -> hele samlingen listes og køres sammen som før.
    //
    // Genvejen tages KUN på et positivt svar. Alt andet - intet ejerskabsdokument, et fremmed
    // maskin-id, et manglende eller ødelagt indeks - falder tilbage til den dyre vej. En
    // afbrudt sync når aldrig at skrive ejerskabet, og gør os derfor for forsigtige frem for
    // for dristige. Det er med vilje den eneste retning fejlen kan gå i.
    //
    // Ét hjørne er værd at kende: kører to maskiner samtidig, kan A nå at læse ejerskabet
    // (som stadig siger A) i det sekund B er ved at skrive, og dermed springe B's netop
    // uploadede drømme over. B skriver ejerskabet til sidst, så A opdager dem ved NÆSTE sync.
    // Det er en forsinkelse, ikke et tab - og appen er bygget til én maskine ad gangen.
    public void syncNow(boolean confirmedMerge) throws SyncException {
        SyncDTO dto = IOutils.loadSync();
        if (dto == null || !dto.syncEnabled) {
            return;
        }

        String idToken = obtainIdToken(dto);
        String dreamsPath = "users/" + dto.uid + "/dreams";

        // Kategorier, temaer og indstillinger gemmes normalt først ved appluk - og først dér
        // opdateres deres tidsstempler (se MetaDTO). Uden dette gem ville et tryk på
        // "Synkronisér nu" midt i en session sende en forældet udgave, eller slet ingenting.
        gemLokaleMetaÆndringer();

        String maskinId = sikrMaskinId(dto);

        try {
            // Ét enkelt dokument fortæller hvem der skrev her sidst. Er det os selv, kan skyen
            // ikke indeholde noget vi ikke allerede kender, og hele den dyre udforskning kan
            // springes over. Det koster én læsning at spørge, mod 800+ ved at liste alt.
            JsonNode ejerskab = firestoreClient.getDocument(idToken, metaSti(dto.uid, META_STATE)).orElse(null);
            LinkedHashMap<String, Instant> indeks = IOutils.loadCloudIndex();
            boolean viSkrevSidst = erVoresEget(ejerskab, maskinId) && indeks != null;

            Map<String, JsonNode> cloudDreams;
            if (viSkrevSidst) {
                cloudDreams = indeksSomSkyudgave(indeks);
            } else {
                cloudDreams = firestoreClient.listDocuments(idToken, dreamsPath);

                if (!confirmedMerge) {
                    int duplicates = countLikelyDuplicates(cloudDreams);
                    if (duplicates > 0) {
                        throw new SyncConflictException(
                                user.getDreams().size(), countLivingCloudDreams(cloudDreams), duplicates);
                    }
                }
            }

            // Rækkefølgen er ikke tilfældig. Gravstenene skal skrives FØR pull, fordi pushTombstones
            // også opdaterer vores snapshot af skyen: ellers ville pullNewerDreams stadig se den
            // netop slettede drøm som et levende dokument, ikke finde den lokalt, og hente den ned igen.
            int gravsten = pushTombstones(idToken, dreamsPath, cloudDreams);

            // Der er intet at hente når vi selv var den sidste der skrev - og vigtigere: der er
            // heller intet at hente FRA. cloudDreams er her vores eget indeks, ikke skyen.
            if (!viSkrevSidst && pullNewerDreams(cloudDreams) > 0) {
                // Skal ske FØR indekset skrives: indekset må aldrig kende en drøm som disken
                // ikke gør. Se kommentaren over pullNewerDreams for hvad det ellers koster.
                IOutils.saveDreams(user.getDreams());
            }

            Push drømme = pushNewerDreams(idToken, dreamsPath, cloudDreams);
            boolean metaÆndret = viSkrevSidst
                    ? pushMetaSomEjer(idToken, dto.uid, ejerskab)
                    : syncMeta(idToken, dto.uid);

            // Indekset skrives først her, efter at alle push er lykkedes. Fejler et undervejs,
            // kastes der, og filen står stadig med det den sagde før - næste sync sender så det
            // manglende igen. Aldrig et indeks der lover mere end der er kommet afsted.
            IOutils.saveCloudIndex(drømme.indeks());

            // Ejerskabet skrives kun når vi faktisk har ændret noget deroppe. Ellers ville en
            // app der bare åbnes og lukkes koste to skrivninger om dagen for ingenting.
            if (!viSkrevSidst || gravsten > 0 || drømme.sendt() > 0 || metaÆndret) {
                skrivEjerskab(idToken, dto.uid, maskinId);
            }

            dto.lastSyncedAt = Instant.now();
            IOutils.saveSync(dto);
        } catch (FirestoreException e) {
            throw new SyncException("Kunne ikke synkronisere: " + e.getMessage(), e);
        }
    }

    // Maskin-id'et laves første gang der synkroniseres og bliver liggende i sync.json.
    private String sikrMaskinId(SyncDTO dto) {
        if (dto.machineId == null || dto.machineId.isBlank()) {
            dto.machineId = UUID.randomUUID().toString();
            IOutils.saveSync(dto);
        }
        return dto.machineId;
    }

    private boolean erVoresEget(JsonNode ejerskab, String maskinId) {
        if (ejerskab == null) {
            return false; // ingen har skrevet her endnu - eller i hvert fald ikke med denne udgave
        }
        JsonNode id = ejerskab.get("machineId");
        return id != null && !id.isNull() && maskinId.equals(id.asText());
    }

    // Klæder det lokale indeks på som var det svaret fra en listDocuments, så resten af
    // syncen ikke behøver vide hvor billedet af skyen kom fra. Kun updatedAt er med - det er
    // det eneste felt push-siden overhovedet kigger på.
    private Map<String, JsonNode> indeksSomSkyudgave(LinkedHashMap<String, Instant> indeks) {
        Map<String, JsonNode> som = new LinkedHashMap<>();
        for (Map.Entry<String, Instant> entry : indeks.entrySet()) {
            ObjectNode felter = SyncObjectMapper.INSTANCE.createObjectNode();
            if (entry.getValue() != null) {
                felter.put("updatedAt", entry.getValue().toString());
            }
            som.put(entry.getKey(), felter);
        }
        return som;
    }

    private void skrivEjerskab(String idToken, String uid, String maskinId) throws FirestoreException {
        MetaDTO meta = IOutils.loadMeta();
        ObjectNode doc = SyncObjectMapper.INSTANCE.createObjectNode();
        doc.put("machineId", maskinId);
        doc.put("updatedAt", Instant.now().toString());

        // Meta-stemplerne kommer med, så den billige vej også kan afgøre om kategorier, temaer
        // og indstillinger er ajour - uden at hente de tre dokumenter ned og kigge.
        sætStempel(doc, "categoriesUpdatedAt", meta.categories.updatedAt);
        sætStempel(doc, "temaerUpdatedAt", meta.temaer.updatedAt);
        sætStempel(doc, "settingsUpdatedAt", meta.settings.updatedAt);

        firestoreClient.patchDocument(idToken, metaSti(uid, META_STATE), doc);
    }

    private void sætStempel(ObjectNode doc, String felt, Instant tidspunkt) {
        if (tidspunkt != null) {
            doc.put(felt, tidspunkt.toString());
        } else {
            doc.putNull(felt);
        }
    }

    // Meta-siden af den billige vej: skyens tre dokumenter er vores egne, og ejerskabsdokumentet
    // husker hvilke tidsstempler de havde. Er de uændrede, er der intet at gøre og intet at hente.
    private boolean pushMetaSomEjer(String idToken, String uid, JsonNode ejerskab) throws SyncException {
        MetaDTO meta = IOutils.loadMeta();
        boolean ændret = false;
        ændret |= pushMetaHvisAnderledes(idToken, uid, META_CATEGORIES, meta.categories.updatedAt,
                læsTidsstempel(ejerskab, "categoriesUpdatedAt"), this::byggKategoriDokument);
        ændret |= pushMetaHvisAnderledes(idToken, uid, META_TEMAER, meta.temaer.updatedAt,
                læsTidsstempel(ejerskab, "temaerUpdatedAt"), this::byggTemaDokument);
        ændret |= pushMetaHvisAnderledes(idToken, uid, META_SETTINGS, meta.settings.updatedAt,
                læsTidsstempel(ejerskab, "settingsUpdatedAt"), this::byggIndstillingsDokument);
        return ændret;
    }

    private boolean pushMetaHvisAnderledes(String idToken, String uid, String navn, Instant lokal,
                                           Instant iSkyen, Supplier<ObjectNode> lokalUdgave) throws SyncException {
        if (Objects.equals(lokal, iSkyen)) {
            return false;
        }
        ObjectNode udgaaende = lokalUdgave.get();
        if (lokal != null) {
            udgaaende.put("updatedAt", lokal.toString());
        }
        try {
            firestoreClient.patchDocument(idToken, metaSti(uid, navn), udgaaende);
        } catch (FirestoreException e) {
            throw new SyncException("Kunne ikke gemme " + navn + " i skyen: " + e.getMessage(), e);
        }
        return true;
    }

    private String metaSti(String uid, String navn) {
        return "users/" + uid + "/meta/" + navn;
    }

    // Skriver en gravsten for hver drøm der er slettet lokalt, og tømmer køen efterhånden.
    //
    // En kø-post springes over og kasseres hvis drømmen findes lokalt igen (genoprettet siden
    // sletningen) - ellers ville vi slette en drøm brugeren lige har skrevet.
    //
    // Fejler et enkelt kald undervejs, gemmes køen ikke, og hele køen forsøges igen ved næste
    // sync. Det er harmløst: at skrive den samme gravsten to gange giver samme resultat.
    private int pushTombstones(String idToken, String dreamsPath, Map<String, JsonNode> cloudDreams) throws SyncException {
        LinkedHashMap<String, Instant> pending = IOutils.loadDeletedDreams();
        if (pending.isEmpty()) {
            return 0;
        }

        int skrevet = 0;
        LinkedHashMap<String, Instant> tilbage = new LinkedHashMap<>(pending);
        for (Map.Entry<String, Instant> entry : pending.entrySet()) {
            String id = entry.getKey();
            if (user.getDreams().containsKey(id)) {
                tilbage.remove(id);
                continue;
            }

            ObjectNode tombstone = Tombstone.of(id, entry.getValue());
            try {
                firestoreClient.patchDocument(idToken, dreamsPath + "/" + id, tombstone);
            } catch (FirestoreException e) {
                throw new SyncException("Kunne ikke slette drøm i skyen: " + e.getMessage(), e);
            }
            cloudDreams.put(id, tombstone);
            tilbage.remove(id);
            skrevet++;
        }
        IOutils.saveDeletedDreams(tilbage);
        return skrevet;
    }

    private int countLivingCloudDreams(Map<String, JsonNode> cloudDreams) {
        int levende = 0;
        for (JsonNode fields : cloudDreams.values()) {
            if (!Tombstone.isTombstone(fields)) {
                levende++;
            }
        }
        return levende;
    }

    // Tæller lokale drømme der ville blive til dubletter ved en sammenkøring: samme indhold som
    // en sky-drøm, men under et andet ID. 0 betyder at det er sikkert at køre sammen uden at
    // spørge brugeren - enten fordi siderne deler lineage (ID-overlap), eller fordi de lokale
    // drømme simpelthen er nye og ukendte for skyen.
    private int countLikelyDuplicates(Map<String, JsonNode> cloudDreams) {
        if (user.getDreams().isEmpty() || cloudDreams.isEmpty()) {
            return 0;
        }
        for (String cloudId : cloudDreams.keySet()) {
            if (user.getDreams().containsKey(cloudId)) {
                return 0; // mindst ét fælles ID - tydeligvis samme datasæt med inkrementelle ændringer
            }
        }

        Set<String> cloudFingerprints = new HashSet<>();
        for (JsonNode fields : cloudDreams.values()) {
            if (Tombstone.isTombstone(fields)) {
                continue; // en gravsten er ikke en drøm og kan ikke være nogens dublet
            }
            String fp = DreamFingerprint.of(textOrNull(fields, "dato"), textOrNull(fields, "indhold"));
            if (fp != null) {
                cloudFingerprints.add(fp);
            }
        }

        int duplicates = 0;
        for (Dream d : user.getDreams().values()) {
            String fp = DreamFingerprint.of(
                    d.getDato() != null ? d.getDato().toString() : null, d.getIndhold());
            if (fp != null && cloudFingerprints.contains(fp)) {
                duplicates++;
            }
        }
        return duplicates;
    }

    private String textOrNull(JsonNode fields, String felt) {
        JsonNode node = fields.get(felt);
        return (node == null || node.isNull()) ? null : node.asText();
    }

    // Best-effort variant til brug ved appstart - fejl logges kun, blokerer aldrig UI'et.
    public void pullOnStartIfEnabled() {
        try {
            syncNow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Kan besvares HELT lokalt: er der noget der endnu ikke er kommet i skyen? Ingen netværk,
    // ingen læsning, ingen kvote - kun disken.
    //
    // Bruges ved appluk til at afgøre om der overhovedet er grund til at kontakte Firebase.
    // Svarer den nej, lukker appen øjeblikkeligt uden at røre skyen; svarer den ja, er det
    // værd at vente et øjeblik på at pushet bliver færdigt (se DreamApp.handleWindowClose).
    //
    // Den fejler bevidst mod JA: mangler indekset, eller er vi i tvivl, siger den ja og lader
    // den rigtige sync afgøre sagen. At sige nej for meget ville betyde tabte drømme; at sige
    // ja for meget koster ét enkelt overflødigt opslag.
    public boolean harUsendteÆndringer() {
        SyncDTO dto = IOutils.loadSync();
        if (dto == null || !dto.syncEnabled) {
            return false; // sync er slet ikke i brug
        }

        if (!IOutils.loadDeletedDreams().isEmpty()) {
            return true; // sletninger der endnu ikke er blevet til gravsten
        }

        LinkedHashMap<String, Instant> indeks = IOutils.loadCloudIndex();
        if (indeks == null) {
            return true; // vi ved ikke hvad skyen indeholder - lad syncen finde ud af det
        }

        Map<String, Dream> drømme = user.getDreams();
        if (drømme.size() != indeks.size()) {
            return true;
        }
        for (Dream d : drømme.values()) {
            if (!Objects.equals(indeks.get(d.getId()), d.getUpdatedAt())) {
                return true; // ny eller redigeret siden sidste sync
            }
        }

        // Kategorier, temaer og indstillinger har ikke deres eget indeks, men deres stempler
        // sættes af DENNE maskines ur, ligesom lastSyncedAt. Er et stempel nyere end den sidste
        // gennemførte sync, er ændringen altså sket bagefter og mangler at komme afsted.
        MetaDTO meta = IOutils.loadMeta();
        return ændretEfter(meta.categories.updatedAt, dto.lastSyncedAt)
                || ændretEfter(meta.temaer.updatedAt, dto.lastSyncedAt)
                || ændretEfter(meta.settings.updatedAt, dto.lastSyncedAt);
    }

    private boolean ændretEfter(Instant stempel, Instant sidsteSync) {
        if (stempel == null) return false;        // aldrig redigeret
        if (sidsteSync == null) return true;      // aldrig synkroniseret
        return stempel.isAfter(sidsteSync);
    }

    // Best-effort variant til brug ved vindueslukning - må aldrig forsinke/blokere lukning.
    public void pushOnCloseIfEnabled() {
        try {
            syncNow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Returnerer hvor mange lokale drømme der blev ændret. Tallet er ikke pynt: kalderen SKAL
    // gemme dreams.json når det er over nul.
    //
    // Appen skriver ellers kun drømmene til disk ved appluk (DreamApp.handleWindowClose), og
    // alt hentet ligger indtil da kun i hukommelsen. Det gik an dengang hver sync listede hele
    // skyen igen - en tabt hentning blev bare hentet igen. Med skyindekset gør den ikke: den
    // hentede drøm ville stå i indekset som kendt, ejerskabet ville sende os ad den billige
    // vej, og drømmen ville aldrig blive listet igen. Den ville altså findes i skyen og være
    // væk her, permanent og lydløst.
    private int pullNewerDreams(Map<String, JsonNode> cloudDreams) {
        int ændrede = 0;
        for (Map.Entry<String, JsonNode> entry : cloudDreams.entrySet()) {
            String id = entry.getKey();

            if (Tombstone.isTombstone(entry.getValue())) {
                if (applyRemoteDeletion(id, entry.getValue())) {
                    ændrede++;
                }
                continue; // en gravsten må ALDRIG læses som en drøm - den har hverken indhold eller kategorier
            }

            DreamData cloudData;
            try {
                cloudData = SyncObjectMapper.INSTANCE.treeToValue(entry.getValue(), DreamData.class);
            } catch (JsonProcessingException e) {
                continue; // spring uventede/ugyldige sky-dokumenter over frem for at fejle hele syncen
            }
            if (cloudData.id == null || cloudData.id.isBlank()) {
                cloudData.id = id;
            }

            Dream local = user.getDreams().get(id);
            Instant localUpdatedAt = (local != null) ? local.getUpdatedAt() : null;

            if (SyncMerge.cloudWins(localUpdatedAt, cloudData.updatedAt)) {
                user.addDream(new Dream(cloudData));
                ændrede++;
            }
        }
        return ændrede;
    }

    // Hvor mange drømme der blev sendt, og hvad skyen derefter indeholder pr. drøm.
    private record Push(int sendt, LinkedHashMap<String, Instant> indeks) {}

    // Indekset bygges HER, undervejs, af nøjagtig de tidsstempler vi enten har sendt afsted
    // eller bekræftet allerede lå deroppe - ikke bagefter ud fra user.getDreams().
    //
    // Forskellen er ikke teoretisk: syncen kører på en baggrundstråd, så brugeren kan nå at
    // redigere en drøm midt i forløbet. Et indeks bygget til sidst ville notere den redigering
    // som "ligger i skyen", selvom den blev lavet efter at drømmen var sendt - og så ville den
    // aldrig komme derop. Sådan et hul er værre end en dyr sync.
    private Push pushNewerDreams(String idToken, String dreamsPath, Map<String, JsonNode> cloudDreams) throws SyncException {
        LinkedHashMap<String, Instant> indeks = new LinkedHashMap<>();
        int sendt = 0;

        for (Dream d : new ArrayList<>(user.getDreams().values())) {
            JsonNode cloudFields = cloudDreams.get(d.getId());
            Instant cloudUpdatedAt = extractUpdatedAt(cloudFields);

            boolean cloudIsCurrent = SyncMerge.cloudIsUpToDate(d.getUpdatedAt(), cloudUpdatedAt);
            if (cloudIsCurrent) {
                indeks.put(d.getId(), cloudUpdatedAt);
                continue; // skyen har allerede den nyeste/samme version - intet at sende
            }

            JsonNode udgaaende = toPlainJson(d);
            try {
                firestoreClient.patchDocument(idToken, dreamsPath + "/" + d.getId(), udgaaende);
            } catch (FirestoreException e) {
                throw new SyncException("Kunne ikke gemme drøm i skyen: " + e.getMessage(), e);
            }
            indeks.put(d.getId(), extractUpdatedAt(udgaaende));
            sendt++;
        }
        return new Push(sendt, indeks);
    }

    // En gravsten i skyen betyder "denne drøm er slettet på en anden maskine". Sletningen
    // gentages derfor lokalt - men kun hvis gravstenen er nyere end vores egen udgave, så en
    // redigering foretaget EFTER sletningen ikke går tabt. Det er nøjagtig samme
    // last-write-wins-regel som for indhold; gravstenen er bare "det nyeste er ingenting".
    private boolean applyRemoteDeletion(String id, JsonNode fields) {
        Dream local = user.getDreams().get(id);
        if (local == null) {
            return false; // allerede væk her - intet at gøre
        }
        if (SyncMerge.cloudWins(local.getUpdatedAt(), extractUpdatedAt(fields))) {
            // Bevidst User.deleteDream og ikke UserService.deleteDream: sletningen kommer FRA
            // skyen, og må ikke lægges i vores egen kø som var den en ny lokal sletning.
            user.deleteDream(id);
            return true;
        }
        return false;
    }

    // Skriver den kørende Users kategorier/temaer/indstillinger til disk, så tidsstemplerne er
    // ajour inden vi sammenligner med skyen. Filer der ER hentet fra skyen i denne session
    // springes over: så er det RAM'en der er forældet, ikke disken (se markerXHentetFraSkyen).
    private void gemLokaleMetaÆndringer() {
        if (!user.harHentetKategorierFraSkyen()) {
            IOutils.saveCategories(user.getCategories());
        }
        if (!user.harHentetTemaerFraSkyen()) {
            IOutils.saveTemaer(user.getTemaer());
        }
        if (!user.harHentetIndstillingerFraSkyen()) {
            IOutils.saveUser(user);
        }
    }

    // Returnerer true hvis mindst ét dokument blev sendt op - så ved syncNow at skyen har
    // ændret sig, og at ejerskabsdokumentet skal opdateres.
    private boolean syncMeta(String idToken, String uid) throws SyncException {
        MetaDTO meta = IOutils.loadMeta();

        boolean ændret = syncMetaDocument(idToken, uid, META_CATEGORIES, meta.categories.updatedAt,
                this::byggKategoriDokument, this::overtagKategorierFraSkyen);
        ændret |= syncMetaDocument(idToken, uid, META_TEMAER, meta.temaer.updatedAt,
                this::byggTemaDokument, this::overtagTemaerFraSkyen);
        ændret |= syncMetaDocument(idToken, uid, META_SETTINGS, meta.settings.updatedAt,
                this::byggIndstillingsDokument, this::overtagIndstillingerFraSkyen);
        return ændret;
    }

    // Fælles forløb for alle tre meta-dokumenter: hent skyens udgave, afgør hvem der vinder
    // (MetaMerge), og enten overtag skyens eller send vores egen.
    //
    // Bemærk at et push aldrig sætter et tidsstempel som vi ikke selv har: er det lokale null
    // ("aldrig redigeret"), sendes dokumentet UDEN updatedAt. Ellers ville en frisk
    // installations standardkategorier - stemplet med afsendelsestidspunktet - se nyere ud end
    // den anden maskines rigtige kategorier, og overskrive dem ved næste sync.
    private boolean syncMetaDocument(String idToken, String uid, String navn, Instant localUpdatedAt,
                                     Supplier<ObjectNode> lokalUdgave,
                                     BiConsumer<JsonNode, Instant> overtagSkyensUdgave) throws SyncException {
        String path = metaSti(uid, navn);

        Optional<JsonNode> cloud;
        try {
            cloud = firestoreClient.getDocument(idToken, path);
        } catch (FirestoreException e) {
            throw new SyncException("Kunne ikke hente " + navn + " fra skyen: " + e.getMessage(), e);
        }

        Instant cloudUpdatedAt = extractUpdatedAt(cloud.orElse(null));

        if (MetaMerge.cloudWins(localUpdatedAt, cloudUpdatedAt, cloud.isPresent())) {
            overtagSkyensUdgave.accept(cloud.get(), cloudUpdatedAt);
            return false;
        }
        if (MetaMerge.shouldPush(localUpdatedAt, cloudUpdatedAt, cloud.isPresent())) {
            ObjectNode udgaaende = lokalUdgave.get();
            if (localUpdatedAt != null) {
                udgaaende.put("updatedAt", localUpdatedAt.toString());
            }
            try {
                firestoreClient.patchDocument(idToken, path, udgaaende);
            } catch (FirestoreException e) {
                throw new SyncException("Kunne ikke gemme " + navn + " i skyen: " + e.getMessage(), e);
            }
            return true;
        }
        return false;
    }

    // De udgående dokumenter bygges fra DISKEN, ikke fra den kørende User. Tidsstemplet i
    // meta.json hører til filens indhold - det er præcis dét indhold der er blevet hashet - så
    // det er også filens indhold der skal afsted, hvis de to skulle nå at være forskellige.
    private ObjectNode byggKategoriDokument() {
        ArrayList<Category> cats = IOutils.loadCategories();
        ObjectNode doc = SyncObjectMapper.INSTANCE.createObjectNode();
        doc.set("categories", SyncObjectMapper.INSTANCE.valueToTree(
                IOutils.toCategoryDTOs(cats != null ? cats : new ArrayList<>())));
        return doc;
    }

    private ObjectNode byggTemaDokument() {
        HashMap<String, Tema> temaer = IOutils.loadTemaer();
        ArrayList<HashMap<String, String>> liste = new ArrayList<>();
        if (temaer != null) {
            for (Tema tema : temaer.values()) {
                liste.add(tema.getTemaForSaving());
            }
        }
        ObjectNode doc = SyncObjectMapper.INSTANCE.createObjectNode();
        doc.set("temaer", SyncObjectMapper.INSTANCE.valueToTree(liste));
        return doc;
    }

    private ObjectNode byggIndstillingsDokument() {
        UserDTO dto = IOutils.loadUser();
        ObjectNode doc = (dto != null)
                ? SyncObjectMapper.INSTANCE.valueToTree(dto)
                : SyncObjectMapper.INSTANCE.createObjectNode();
        doc.remove("schemaVersion"); // appens eget bogholderi - hører ikke hjemme i skyen
        return doc;
    }

    // Et hentet meta-dokument skrives DIREKTE til disk og rører aldrig den kørende User.
    //
    // Det er med vilje. Kategorierne er vævet ind i UI'ets CheckComboBox'e (Category holder selv
    // styr på sine bokse), og at bytte listen ud under en kørende app ville efterlade UI'et
    // koblet til objekter der ikke længere er dem der bliver gemt. Hentede kategorier/temaer/
    // indstillinger træder derfor i kraft ved næste opstart - og indtil da sørger flaget for,
    // at appluk ikke skriver den forældede RAM-udgave hen over det vi lige har hentet.
    private void overtagKategorierFraSkyen(JsonNode doc, Instant cloudUpdatedAt) {
        List<CategoryDTO> dtoer = SyncObjectMapper.INSTANCE.convertValue(
                doc.path("categories"), new TypeReference<List<CategoryDTO>>() {});
        IOutils.saveCategories(IOutils.toCategories(dtoer));
        stemplFraSkyen(meta -> meta.categories, cloudUpdatedAt);
        user.markerKategorierHentetFraSkyen();
    }

    private void overtagTemaerFraSkyen(JsonNode doc, Instant cloudUpdatedAt) {
        List<HashMap<String, String>> liste = SyncObjectMapper.INSTANCE.convertValue(
                doc.path("temaer"), new TypeReference<List<HashMap<String, String>>>() {});
        IOutils.saveTemaer(IOutils.toTemaer(liste));
        stemplFraSkyen(meta -> meta.temaer, cloudUpdatedAt);
        user.markerTemaerHentetFraSkyen();
    }

    // Indstillingerne er den ene af de tre der OGSÅ kan lægges direkte ind i den kørende User:
    // det er almindelige felter, ikke et objektgraf-net UI'et hænger fast i. Det er værd at
    // gøre, for ellers ville RAM og disk stå med hver sin udgave, og enhver indstilling
    // brugeren ændrede senere i sessionen ville enten gå tabt eller skrive det hentede væk.
    private void overtagIndstillingerFraSkyen(JsonNode doc, Instant cloudUpdatedAt) {
        UserDTO dto = SyncObjectMapper.INSTANCE.convertValue(doc, UserDTO.class);

        // Skyen bærer ikke schemaVersion, og en nul her ville få SchemaMigrator til at tro at
        // filerne var gamle og køre alle migrationer forfra ved næste opstart.
        dto.schemaVersion = SchemaMigrator.CURRENT_SCHEMA_VERSION;

        IOutils.saveUserDTO(dto);
        stemplFraSkyen(meta -> meta.settings, cloudUpdatedAt);

        user.setVisAdvarsel(dto.visAdvarsel);
        user.setVisKollektiv(dto.visKollektiv);
        user.setVisHolografisk(dto.visHolografisk);
        user.setStartFromThisDate(dto.startFromThisDate);

        // Det foretrukne tema er undtagelsen: kommer det fra en maskine med et tema vi ikke
        // kender endnu, findes det kun i den temaer.json vi lige har hentet - ikke i denne
        // Users temaer. At sætte navnet alligevel ville give en NPE ved næste opslag. Så lader
        // vi RAM beholde sit gamle tema, og markerer i stedet filen som "hentet", så appluk
        // ikke skriver det gamle navn hen over det nye. Begge dele falder på plads ved genstart.
        if (user.getTemaer().containsKey(dto.foretrukneTema)) {
            user.setForetrukneTema(dto.foretrukneTema);
        } else {
            user.markerIndstillingerHentetFraSkyen();
        }
    }

    // Efter en overtagelse er skyens tidspunkt også vores. Gemmet ovenfor har allerede
    // registreret hash'en af det nye indhold og sat updatedAt til nu; her rettes tidspunktet
    // til skyens, så de to sider er enige og næste sync ikke sender det samme retur.
    private void stemplFraSkyen(Function<MetaDTO, MetaDTO.Stamp> vælgStempel, Instant cloudUpdatedAt) {
        MetaDTO meta = IOutils.loadMeta();
        vælgStempel.apply(meta).updatedAt = cloudUpdatedAt;
        IOutils.saveMeta(meta);
    }

    private Instant extractUpdatedAt(JsonNode fields) {
        return læsTidsstempel(fields, "updatedAt");
    }

    // Et tidsstempel fra et sky-dokument. Alt der ikke er en parsebar ISO-streng bliver til
    // null - "det ved vi ikke" - og null fører hos samtlige kaldere ad den forsigtige vej.
    private Instant læsTidsstempel(JsonNode fields, String felt) {
        if (fields == null) return null;
        JsonNode ua = fields.get(felt);
        if (ua == null || ua.isNull() || ua.asText().isBlank()) return null;
        try {
            return Instant.parse(ua.asText());
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode toPlainJson(Dream d) {
        DreamData data = new DreamData();
        data.id = d.getId();
        data.categories = d.getCategories();
        data.indhold = d.getIndhold();
        data.dagrest = d.getDagrest();
        data.tolkning = d.getTolkning();
        data.dato = d.getDato();
        data.updatedAt = d.getUpdatedAt();
        return SyncObjectMapper.INSTANCE.valueToTree(data);
    }

    // Fornyer idToken ud fra det gemte refresh-token. Hvis selve refresh fejler (token
    // tilbagekaldt/udløbet), behandles det som logget ud: sync.json ryddes, ingen retry-loop.
    private String obtainIdToken(SyncDTO dto) throws SyncException {
        try {
            AuthResult result = authClient.refreshToken(dto.refreshToken);
            if (!result.refreshToken().equals(dto.refreshToken)) {
                dto.refreshToken = result.refreshToken();
                IOutils.saveSync(dto);
            }
            return result.idToken();
        } catch (FirebaseAuthException e) {
            IOutils.deleteSync();
            throw new SyncException("Login er udløbet - log ind igen.", e);
        }
    }
}
