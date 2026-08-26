package fixit.dreams;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fixit.dreams.sync.AuthResult;
import fixit.dreams.sync.DreamFingerprint;
import fixit.dreams.sync.FirebaseAuthClient;
import fixit.dreams.sync.FirebaseAuthException;
import fixit.dreams.sync.FirestoreClient;
import fixit.dreams.sync.FirestoreException;
import fixit.dreams.sync.SyncDTO;
import fixit.dreams.sync.SyncMerge;
import fixit.dreams.sync.SyncObjectMapper;
import fixit.dreams.sync.Tombstone;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

// Orkestrerer valgfri cloud-synkronisering af drømme. Ligger bevidst i fixit.dreams (ikke
// fixit.dreams.sync) fordi den er tæt koblet til User/Dream/DreamData/IOutils - selve
// netværks-/protokollaget (auth, Firestore, JSON-oversættelse) ligger i fixit.dreams.sync
// og er det eneste denne klasse importerer derfra.
//
// Fase 1 af sync dækker selve drømmene (indhold + deres per-drøm kategori-tags, som allerede
// rejser med hver drøm som en del af DreamData.categories). Kategori-DEFINITIONER/temaer/
// generelle indstillinger synkroniseres endnu ikke - en frisk installation får sine egne
// standardkategorier/-temaer via User.addDefaultCategories()/addPredefinedThemes().
public class SyncService {
    private final User user;
    private final FirebaseAuthClient authClient = new FirebaseAuthClient();
    private final FirestoreClient firestoreClient = new FirestoreClient();

    public SyncService(User user) {
        this.user = user;
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
