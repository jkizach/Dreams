package fixit.dreams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fixit.dreams.sync.AuthResult;
import fixit.dreams.sync.FirebaseAuthClient;
import fixit.dreams.sync.FirestoreClient;
import fixit.dreams.sync.FirestoreException;
import fixit.dreams.sync.SyncDTO;
import fixit.dreams.sync.SyncObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stopskiltet: en app maa aldrig roere en sky der er skrevet i et dataformat den ikke kender.
 *
 * Skyen baerer ingen versionsmarkoer paa drommene selv, og SchemaMigrator koerer kun paa de
 * lokale filer ved opstart - hentede dokumenter kommer aldrig forbi den. En aeldre udgave ville
 * derfor bare laese nyt data forkert, uden en fejl: felter den ikke forstaar bliver til null.
 * Netop v4 er beviset - en v3-laeser ville se {"id":"dyr"} og saette name = null, hvorefter
 * taggen matcher ingen kategori og symbolerne stille holder op med at taelle med.
 *
 * Vaernet hjaelper kun den maskine der MODTAGER noget uventet, saa det skal ligge i den udgave
 * der en dag bliver den gamle. Testene her holder fast i baade at det stopper, og - lige saa
 * vigtigt - at det ikke staar i vejen i alle de tilfaelde hvor der intet er galt.
 */
class SyncSkemaVersionTest {

    private static final String UID = "uid-version";
    private static final String STATE_PATH = "users/" + UID + "/meta/state";
    private static final String DREAMS_PATH = "users/" + UID + "/dreams";

    private static final List<String> FILNAVNE = List.of(
            "user.json", "temaer.json", "cats.json", "dreams.json", "sync.json", "deleted.json",
            "meta.json", "machine.json");

    private FakeFirestore sky;
    private User user;

    @BeforeEach
    void setUp() throws IOException {
        User.resetForTests();
        ryd();

        SyncDTO dto = new SyncDTO();
        dto.email = "test@example.com";
        dto.refreshToken = "refresh-token";
        dto.uid = UID;
        dto.syncEnabled = true;
        IOutils.saveSync(dto);

        sky = new FakeFirestore();
        user = User.getInstance();
    }

    @AfterEach
    void tearDown() throws IOException {
        ryd();
        User.resetForTests();
    }

    private void ryd() throws IOException {
        for (String navn : FILNAVNE) {
            Files.deleteIfExists(AppPaths.APP_DATA_PATH.resolve(navn));
        }
        try (var stier = Files.list(AppPaths.APP_DATA_PATH)) {
            for (Path sti : stier.filter(p -> p.getFileName().toString().startsWith("cloudindex")).toList()) {
                Files.deleteIfExists(sti);
            }
        }
    }

    private SyncService tjeneste() {
        return new SyncService(user, new FakeAuth(), sky);
    }

    /** Et ejerskabsdokument fra en fremmed maskine, med den angivne formatversion. */
    private void skyMedVersion(Integer version) {
        ObjectNode doc = SyncObjectMapper.INSTANCE.createObjectNode();
        doc.put("machineId", "en-anden-maskine");
        doc.put("updatedAt", Instant.now().toString());
        if (version != null) {
            doc.put("schemaVersion", version);
        }
        sky.documents.put(STATE_PATH, doc);
    }

    // ---------- Det stopper ----------

    @Test
    void en_nyere_sky_standser_synkroniseringen() {
        skyMedVersion(SchemaMigrator.CURRENT_SCHEMA_VERSION + 1);

        SyncVersionException e = assertThrows(SyncVersionException.class, () -> tjeneste().syncNow());

        assertEquals(SchemaMigrator.CURRENT_SCHEMA_VERSION + 1, e.getSkyensVersion());
        assertEquals(SchemaMigrator.CURRENT_SCHEMA_VERSION, e.getVoresVersion());
    }

    @Test
    void en_nyere_sky_bliver_hverken_laest_eller_skrevet() {
        skyMedVersion(SchemaMigrator.CURRENT_SCHEMA_VERSION + 1);

        assertThrows(SyncVersionException.class, () -> tjeneste().syncNow());

        // Intet hentet ud over selve ejerskabsdokumentet, intet listet, intet skrevet.
        // Et push ville vaere lige saa slemt som et pull: det ville skrive vores aeldre
        // format hen over det nyere der allerede ligger deroppe.
        assertEquals(List.of(STATE_PATH), sky.hentede, "der blev hentet mere end ejerskabet");
        assertEquals(0, sky.listKald, "drommene blev listet");
        assertTrue(sky.patched.isEmpty(), "der blev skrevet til skyen: " + sky.patched.keySet());
    }

    @Test
    void beskeden_siger_hvad_brugeren_skal_goere() {
        skyMedVersion(SchemaMigrator.CURRENT_SCHEMA_VERSION + 1);

        SyncVersionException e = assertThrows(SyncVersionException.class, () -> tjeneste().syncNow());

        assertTrue(e.getMessage().contains("Opdatér"), "beskeden skal sige hvad man skal gore: " + e.getMessage());
    }

    @Test
    void et_versionsstop_tier_ikke_stille_ved_opstart() {
        // Netvaerksfejl maa gerne synke ned i en stakspor - de retter sig selv. Et versionsstop
        // gor ikke: det blokerer indtil maskinen opdateres, saa brugeren SKAL faa det at vide.
        skyMedVersion(SchemaMigrator.CURRENT_SCHEMA_VERSION + 1);

        String besked = tjeneste().pullOnStartIfEnabled();

        assertNotNull(besked, "opstarts-pullet slugte versionsstoppet");
        assertTrue(besked.contains("nyere udgave"));
    }

    // ---------- Og det staar ikke i vejen ----------

    @Test
    void samme_version_synkroniserer_normalt() {
        skyMedVersion(SchemaMigrator.CURRENT_SCHEMA_VERSION);

        assertDoesNotThrow(() -> tjeneste().syncNow());
    }

    @Test
    void en_aeldre_sky_synkroniserer_normalt() {
        // Den anden retning er ikke farlig paa samme maade: en nyere udgave kender per definition
        // det format den selv er vokset ud af. Den forste maskine der opgraderer tager skyen med
        // sig, hvorefter modparten selv standser - indtil ogsaa den er opdateret.
        skyMedVersion(SchemaMigrator.CURRENT_SCHEMA_VERSION - 1);

        assertDoesNotThrow(() -> tjeneste().syncNow());
    }

    @Test
    void et_ejerskab_uden_versionsfelt_blokerer_ikke() {
        // Skrevet for dette vaern fandtes. Der er intet ukendt format at beskytte sig mod.
        skyMedVersion(null);

        assertDoesNotThrow(() -> tjeneste().syncNow());
    }

    @Test
    void en_tom_sky_blokerer_ikke() {
        assertDoesNotThrow(() -> tjeneste().syncNow());
    }

    @Test
    void et_uforstaaeligt_versionsfelt_blokerer_ikke() {
        ObjectNode doc = SyncObjectMapper.INSTANCE.createObjectNode();
        doc.put("machineId", "en-anden-maskine");
        doc.put("schemaVersion", "noget-vroevl");
        sky.documents.put(STATE_PATH, doc);

        assertDoesNotThrow(() -> tjeneste().syncNow());
    }

    @Test
    void opstarts_pullet_giver_null_naar_alt_er_i_orden() {
        skyMedVersion(SchemaMigrator.CURRENT_SCHEMA_VERSION);

        assertNull(tjeneste().pullOnStartIfEnabled());
    }

    // ---------- Og vi efterlader selv en markor ----------

    @Test
    void syncen_skriver_sin_egen_version_i_skyen() throws SyncException {
        tjeneste().syncNow();

        JsonNode ejerskab = sky.patched.get(STATE_PATH);
        assertNotNull(ejerskab, "ejerskabet blev ikke skrevet");
        assertEquals(SchemaMigrator.CURRENT_SCHEMA_VERSION, ejerskab.path("schemaVersion").asInt(),
                "uden dette felt kan en aeldre udgave ikke opdage at den skal holde sig vaek");
    }

    // ---------- Falsk sky ----------

    private static class FakeFirestore extends FirestoreClient {
        final Map<String, JsonNode> documents = new LinkedHashMap<>();
        final Map<String, JsonNode> patched = new LinkedHashMap<>();
        final List<String> hentede = new ArrayList<>();
        int listKald = 0;

        @Override
        public Optional<JsonNode> getDocument(String idToken, String docPath) {
            hentede.add(docPath);
            return Optional.ofNullable(documents.get(docPath));
        }

        @Override
        public void patchDocument(String idToken, String docPath, JsonNode plainFields) throws FirestoreException {
            patched.put(docPath, plainFields);
            documents.put(docPath, plainFields);
        }

        @Override
        public Map<String, JsonNode> listDocuments(String idToken, String collectionPath) {
            listKald++;
            return new LinkedHashMap<>();
        }
    }

    private static class FakeAuth extends FirebaseAuthClient {
        @Override
        public AuthResult refreshToken(String refreshToken) {
            return new AuthResult("id-token", refreshToken, UID, 3600);
        }
    }
}
