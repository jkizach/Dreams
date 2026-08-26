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
import java.util.Optional;
import java.util.Set;
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

        try {
            Map<String, JsonNode> cloudDreams = firestoreClient.listDocuments(idToken, dreamsPath);

            if (!confirmedMerge) {
                int duplicates = countLikelyDuplicates(cloudDreams);
                if (duplicates > 0) {
                    throw new SyncConflictException(
                            user.getDreams().size(), countLivingCloudDreams(cloudDreams), duplicates);
                }
            }

            // Rækkefølgen er ikke tilfældig. Gravstenene skal skrives FØR pull, fordi pushTombstones
            // også opdaterer vores snapshot af skyen: ellers ville pullNewerDreams stadig se den
            // netop slettede drøm som et levende dokument, ikke finde den lokalt, og hente den ned igen.
            pushTombstones(idToken, dreamsPath, cloudDreams);
            pullNewerDreams(cloudDreams);
            pushNewerDreams(idToken, dreamsPath, cloudDreams);

            syncMeta(idToken, dto.uid);

            dto.lastSyncedAt = Instant.now();
            IOutils.saveSync(dto);
        } catch (FirestoreException e) {
            throw new SyncException("Kunne ikke synkronisere: " + e.getMessage(), e);
        }
    }

    // Skriver en gravsten for hver drøm der er slettet lokalt, og tømmer køen efterhånden.
    //
    // En kø-post springes over og kasseres hvis drømmen findes lokalt igen (genoprettet siden
    // sletningen) - ellers ville vi slette en drøm brugeren lige har skrevet.
    //
    // Fejler et enkelt kald undervejs, gemmes køen ikke, og hele køen forsøges igen ved næste
    // sync. Det er harmløst: at skrive den samme gravsten to gange giver samme resultat.
    private void pushTombstones(String idToken, String dreamsPath, Map<String, JsonNode> cloudDreams) throws SyncException {
        LinkedHashMap<String, Instant> pending = IOutils.loadDeletedDreams();
        if (pending.isEmpty()) {
            return;
        }

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
        }
        IOutils.saveDeletedDreams(tilbage);
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

    // Best-effort variant til brug ved vindueslukning - må aldrig forsinke/blokere lukning.
    public void pushOnCloseIfEnabled() {
        try {
            syncNow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pullNewerDreams(Map<String, JsonNode> cloudDreams) {
        for (Map.Entry<String, JsonNode> entry : cloudDreams.entrySet()) {
            String id = entry.getKey();

            if (Tombstone.isTombstone(entry.getValue())) {
                applyRemoteDeletion(id, entry.getValue());
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
            }
        }
    }

    private void pushNewerDreams(String idToken, String dreamsPath, Map<String, JsonNode> cloudDreams) throws SyncException {
        for (Dream d : user.getDreams().values()) {
            JsonNode cloudFields = cloudDreams.get(d.getId());
            Instant cloudUpdatedAt = extractUpdatedAt(cloudFields);

            boolean cloudIsCurrent = SyncMerge.cloudIsUpToDate(d.getUpdatedAt(), cloudUpdatedAt);
            if (cloudIsCurrent) {
                continue; // skyen har allerede den nyeste/samme version - intet at sende
            }

            try {
                firestoreClient.patchDocument(idToken, dreamsPath + "/" + d.getId(), toPlainJson(d));
            } catch (FirestoreException e) {
                throw new SyncException("Kunne ikke gemme drøm i skyen: " + e.getMessage(), e);
            }
        }
    }

    // En gravsten i skyen betyder "denne drøm er slettet på en anden maskine". Sletningen
    // gentages derfor lokalt - men kun hvis gravstenen er nyere end vores egen udgave, så en
    // redigering foretaget EFTER sletningen ikke går tabt. Det er nøjagtig samme
    // last-write-wins-regel som for indhold; gravstenen er bare "det nyeste er ingenting".
    private void applyRemoteDeletion(String id, JsonNode fields) {
        Dream local = user.getDreams().get(id);
        if (local == null) {
            return; // allerede væk her - intet at gøre
        }
        if (SyncMerge.cloudWins(local.getUpdatedAt(), extractUpdatedAt(fields))) {
            // Bevidst User.deleteDream og ikke UserService.deleteDream: sletningen kommer FRA
            // skyen, og må ikke lægges i vores egen kø som var den en ny lokal sletning.
            user.deleteDream(id);
        }
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

    private void syncMeta(String idToken, String uid) throws SyncException {
        MetaDTO meta = IOutils.loadMeta();

        syncMetaDocument(idToken, uid, META_CATEGORIES, meta.categories.updatedAt,
                this::byggKategoriDokument, this::overtagKategorierFraSkyen);
        syncMetaDocument(idToken, uid, META_TEMAER, meta.temaer.updatedAt,
                this::byggTemaDokument, this::overtagTemaerFraSkyen);
        syncMetaDocument(idToken, uid, META_SETTINGS, meta.settings.updatedAt,
                this::byggIndstillingsDokument, this::overtagIndstillingerFraSkyen);
    }

    // Fælles forløb for alle tre meta-dokumenter: hent skyens udgave, afgør hvem der vinder
    // (MetaMerge), og enten overtag skyens eller send vores egen.
    //
    // Bemærk at et push aldrig sætter et tidsstempel som vi ikke selv har: er det lokale null
    // ("aldrig redigeret"), sendes dokumentet UDEN updatedAt. Ellers ville en frisk
    // installations standardkategorier - stemplet med afsendelsestidspunktet - se nyere ud end
    // den anden maskines rigtige kategorier, og overskrive dem ved næste sync.
    private void syncMetaDocument(String idToken, String uid, String navn, Instant localUpdatedAt,
                                  Supplier<ObjectNode> lokalUdgave,
                                  BiConsumer<JsonNode, Instant> overtagSkyensUdgave) throws SyncException {
        String path = "users/" + uid + "/meta/" + navn;

        Optional<JsonNode> cloud;
        try {
            cloud = firestoreClient.getDocument(idToken, path);
        } catch (FirestoreException e) {
            throw new SyncException("Kunne ikke hente " + navn + " fra skyen: " + e.getMessage(), e);
        }

        Instant cloudUpdatedAt = extractUpdatedAt(cloud.orElse(null));

        if (MetaMerge.cloudWins(localUpdatedAt, cloudUpdatedAt, cloud.isPresent())) {
            overtagSkyensUdgave.accept(cloud.get(), cloudUpdatedAt);
        } else if (MetaMerge.shouldPush(localUpdatedAt, cloudUpdatedAt, cloud.isPresent())) {
            ObjectNode udgaaende = lokalUdgave.get();
            if (localUpdatedAt != null) {
                udgaaende.put("updatedAt", localUpdatedAt.toString());
            }
            try {
                firestoreClient.patchDocument(idToken, path, udgaaende);
            } catch (FirestoreException e) {
                throw new SyncException("Kunne ikke gemme " + navn + " i skyen: " + e.getMessage(), e);
            }
        }
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
        if (fields == null) return null;
        JsonNode ua = fields.get("updatedAt");
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
