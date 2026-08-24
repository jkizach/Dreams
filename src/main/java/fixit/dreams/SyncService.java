package fixit.dreams;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import fixit.dreams.sync.AuthResult;
import fixit.dreams.sync.FirebaseAuthClient;
import fixit.dreams.sync.FirebaseAuthException;
import fixit.dreams.sync.FirestoreClient;
import fixit.dreams.sync.FirestoreException;
import fixit.dreams.sync.SyncDTO;
import fixit.dreams.sync.SyncMerge;
import fixit.dreams.sync.SyncObjectMapper;

import java.time.Instant;
import java.util.Map;

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
    // Faresignalet er IKKE "er det her den første sync" (det udløses jo også helt normalt af
    // fx log ud/log ind igen på samme maskine med samme, allerede-synkede data) - det er om
    // lokale og sky-drømme slet IKKE deler nogen ID'er, selvom begge sider har drømme. Det er
    // fingeraftrykket for to uafhængigt migrerede kopier (se SyncConflictException). Findes der
    // bare ét overlappende ID, er det tydeligvis samme datasæt med inkrementelle ændringer, og
    // der sammenkøres uden at spørge - uanset hvor mange drømme der er tilføjet på hver side.
    public void syncNow(boolean confirmedMerge) throws SyncException {
        SyncDTO dto = IOutils.loadSync();
        if (dto == null || !dto.syncEnabled) {
            return;
        }

        String idToken = obtainIdToken(dto);
        String dreamsPath = "users/" + dto.uid + "/dreams";

        try {
            Map<String, JsonNode> cloudDreams = firestoreClient.listDocuments(idToken, dreamsPath);

            if (!confirmedMerge && looksLikeIndependentCopies(cloudDreams)) {
                throw new SyncConflictException(user.getDreams().size(), cloudDreams.size());
            }

            pullNewerDreams(cloudDreams);
            pushNewerDreams(idToken, dreamsPath, cloudDreams);

            dto.lastSyncedAt = Instant.now();
            IOutils.saveSync(dto);
        } catch (FirestoreException e) {
            throw new SyncException("Kunne ikke synkronisere: " + e.getMessage(), e);
        }
    }

    private boolean looksLikeIndependentCopies(Map<String, JsonNode> cloudDreams) {
        if (user.getDreams().isEmpty() || cloudDreams.isEmpty()) {
            return false;
        }
        for (String cloudId : cloudDreams.keySet()) {
            if (user.getDreams().containsKey(cloudId)) {
                return false; // mindst ét overlap - samme datasæt, ikke to uafhængige kopier
            }
        }
        return true;
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

            boolean cloudIsCurrent = cloudFields != null && SyncMerge.cloudWins(d.getUpdatedAt(), cloudUpdatedAt);
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
