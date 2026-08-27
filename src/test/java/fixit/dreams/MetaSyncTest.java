package fixit.dreams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fixit.dreams.sync.AuthResult;
import fixit.dreams.sync.FirebaseAuthClient;
import fixit.dreams.sync.FirestoreClient;
import fixit.dreams.sync.SyncDTO;
import fixit.dreams.sync.SyncObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// Kører hele meta-orkestreringen mod en fake sky: intet netværk, intet rigtigt Firebase-projekt.
// Det er præcis dét test-sømmet i SyncService findes for. Selve HTTP-laget dækkes ikke her og
// skal stadig røgtestes mod det rigtige projekt.
class MetaSyncTest {

    private static final String UID = "uid-test";
    private static final String CAT_PATH = "users/" + UID + "/meta/categories";
    private static final String TEMA_PATH = "users/" + UID + "/meta/temaer";
    private static final String SETTINGS_PATH = "users/" + UID + "/meta/settings";

    private static final Instant GAMMEL = Instant.parse("2026-01-01T12:00:00Z");
    private static final Instant NY = Instant.parse("2026-06-01T12:00:00Z");

    private static final List<String> FILNAVNE = List.of(
            "user.json", "temaer.json", "cats.json", "dreams.json", "sync.json", "deleted.json", "meta.json",
            "cloudindex.json");

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
    }

    private void synkroniser() throws SyncException {
        new SyncService(user, new FakeAuth(), sky).syncNow();
    }

    // Svarer til det SchemaMigrator gør ved opgradering: et tidsstempel, endnu ingen hash.
    private void lokaleStempler(Instant updatedAt) {
        MetaDTO meta = new MetaDTO();
        meta.categories.updatedAt = updatedAt;
        meta.temaer.updatedAt = updatedAt;
        meta.settings.updatedAt = updatedAt;
        IOutils.saveMeta(meta);
    }

    private ObjectNode skyKategorier(Instant updatedAt, String... navne) {
        ObjectNode doc = SyncObjectMapper.INSTANCE.createObjectNode();
        ArrayNode cats = doc.putArray("categories");
        for (String navn : navne) {
            ObjectNode cat = cats.addObject();
            cat.put("name", navn);
            cat.putArray("symbols");
            cat.putArray("customOrder");
        }
        if (updatedAt != null) {
            doc.put("updatedAt", updatedAt.toString());
        }
        return doc;
    }

    private List<String> kategoriNavnePaaDisken() {
        List<String> navne = new ArrayList<>();
        for (Category c : IOutils.loadCategories()) {
            navne.add(c.getName());
        }
        return navne;
    }

    @Test
    void tom_sky_faar_alle_tre_dokumenter_sendt_op() throws SyncException {
        synkroniser();

        assertTrue(sky.patched.containsKey(CAT_PATH));
        assertTrue(sky.patched.containsKey(TEMA_PATH));
        assertTrue(sky.patched.containsKey(SETTINGS_PATH));
    }

    // En frisk installations standarddata har intet tidsstempel, og må heller ikke få et med op
    // i skyen - ellers ville de se nyere ud end den anden maskines rigtige kategorier.
    @Test
    void urorte_standarddata_sendes_uden_tidsstempel() throws SyncException {
        synkroniser();

        assertFalse(sky.patched.get(CAT_PATH).has("updatedAt"));
    }

    @Test
    void redigerede_kategorier_sendes_med_deres_tidsstempel() throws SyncException {
        lokaleStempler(NY);

        synkroniser();

        assertEquals(NY.toString(), sky.patched.get(CAT_PATH).path("updatedAt").asText());
        assertTrue(sky.patched.get(CAT_PATH).path("categories").size() > 0);
    }

    @Test
    void nyere_kategorier_i_skyen_skrives_til_disken() throws SyncException {
        lokaleStempler(GAMMEL);
        sky.documents.put(CAT_PATH, skyKategorier(NY, "Farver", "Steder"));

        synkroniser();

        assertEquals(List.of("Farver", "Steder"), kategoriNavnePaaDisken());
        assertFalse(sky.patched.containsKey(CAT_PATH), "vores egen udgave må ikke sendes retur");
    }

    @Test
    void hentede_kategorier_overtager_skyens_tidsstempel() throws SyncException {
        lokaleStempler(GAMMEL);
        sky.documents.put(CAT_PATH, skyKategorier(NY, "Farver"));

        synkroniser();

        assertEquals(NY, IOutils.loadMeta().categories.updatedAt);
    }

    // Flaget er det der forhindrer appluk i at skrive den forældede RAM-udgave hen over det vi
    // lige har hentet (se DreamApp.handleWindowClose).
    @Test
    void hentede_kategorier_saetter_flaget_men_roerer_ikke_den_koerende_user() throws SyncException {
        lokaleStempler(GAMMEL);
        int antalFoer = user.getCategories().size();
        sky.documents.put(CAT_PATH, skyKategorier(NY, "Farver"));

        synkroniser();

        assertTrue(user.harHentetKategorierFraSkyen());
        assertEquals(antalFoer, user.getCategories().size());
    }

    @Test
    void aeldre_kategorier_i_skyen_overskrives_af_vores() throws SyncException {
        lokaleStempler(NY);
        sky.documents.put(CAT_PATH, skyKategorier(GAMMEL, "Gammel"));

        synkroniser();

        assertTrue(sky.patched.containsKey(CAT_PATH));
        assertNotEquals(List.of("Gammel"), kategoriNavnePaaDisken());
    }

    @Test
    void urorte_lokale_kategorier_overskriver_ikke_skyen() throws SyncException {
        sky.documents.put(CAT_PATH, skyKategorier(GAMMEL, "Farver"));

        synkroniser();

        assertFalse(sky.patched.containsKey(CAT_PATH));
        assertEquals(List.of("Farver"), kategoriNavnePaaDisken());
    }

    @Test
    void nyere_indstillinger_i_skyen_skrives_til_disken() throws SyncException {
        lokaleStempler(GAMMEL);
        ObjectNode doc = SyncObjectMapper.INSTANCE.createObjectNode();
        doc.put("foretrukneTema", "lyst gult");
        doc.put("visAdvarsel", true);
        doc.put("visKollektiv", false);
        doc.put("visHolografisk", true);
        doc.put("startFromThisDate", "2026-01-05");
        doc.put("updatedAt", NY.toString());
        sky.documents.put(SETTINGS_PATH, doc);

        synkroniser();

        UserDTO paaDisken = IOutils.loadUser();
        assertEquals("lyst gult", paaDisken.foretrukneTema);
        assertTrue(paaDisken.visAdvarsel);
        assertTrue(paaDisken.visHolografisk);
        assertTrue(user.harHentetIndstillingerFraSkyen());
    }

    // Indstillinger er den ene af de tre der også kan lægges direkte ind i den kørende app.
    // Uden det ville RAM og disk stå med hver sin udgave resten af sessionen.
    @Test
    void hentede_indstillinger_laegges_ind_i_den_koerende_user() throws SyncException {
        lokaleStempler(GAMMEL);
        ObjectNode doc = SyncObjectMapper.INSTANCE.createObjectNode();
        doc.put("foretrukneTema", "mørkt grønt"); // et tema denne installation allerede kender
        doc.put("visAdvarsel", true);
        doc.put("visHolografisk", true);
        doc.put("updatedAt", NY.toString());
        sky.documents.put(SETTINGS_PATH, doc);

        synkroniser();

        assertTrue(user.isVisAdvarsel());
        assertTrue(user.isVisHolografisk());
        assertEquals("mørkt grønt", user.getForetrukneTemaNavn());
        assertFalse(user.harHentetIndstillingerFraSkyen(),
                "RAM og disk er enige, så appluk må gerne gemme som normalt");
    }

    // Kommer det foretrukne tema fra en maskine hvis tema vi ikke kender endnu, kan navnet ikke
    // sættes i RAM (opslaget ville give NPE). Så skal filen i stedet fredes indtil genstart.
    @Test
    void ukendt_foretrukket_tema_freder_filen_i_stedet() throws SyncException {
        lokaleStempler(GAMMEL);
        ObjectNode doc = SyncObjectMapper.INSTANCE.createObjectNode();
        doc.put("foretrukneTema", "et tema vi ikke har");
        doc.put("updatedAt", NY.toString());
        sky.documents.put(SETTINGS_PATH, doc);

        synkroniser();

        assertNotEquals("et tema vi ikke har", user.getForetrukneTemaNavn());
        assertTrue(user.harHentetIndstillingerFraSkyen());
        assertEquals("et tema vi ikke har", IOutils.loadUser().foretrukneTema);
    }

    // Skyen bærer ikke schemaVersion. Skrives der 0 i user.json, tror SchemaMigrator at filerne
    // er ældgamle og kører alle migrationer forfra ved næste opstart - oven på nutidige data.
    @Test
    void hentede_indstillinger_bevarer_skemaversionen() throws SyncException {
        lokaleStempler(GAMMEL);
        ObjectNode doc = SyncObjectMapper.INSTANCE.createObjectNode();
        doc.put("foretrukneTema", "lyst gult");
        doc.put("updatedAt", NY.toString());
        sky.documents.put(SETTINGS_PATH, doc);

        synkroniser();

        assertEquals(SchemaMigrator.CURRENT_SCHEMA_VERSION, IOutils.loadUser().schemaVersion);
    }

    @Test
    void indstillinger_sendes_uden_skemaversion() throws SyncException {
        lokaleStempler(NY);

        synkroniser();

        assertFalse(sky.patched.get(SETTINGS_PATH).has("schemaVersion"));
        assertTrue(sky.patched.get(SETTINGS_PATH).has("foretrukneTema"));
    }

    @Test
    void temaer_sendes_i_samme_form_som_de_ligger_i_filen() throws SyncException {
        lokaleStempler(NY);

        synkroniser();

        JsonNode sendt = sky.patched.get(TEMA_PATH).path("temaer");
        assertTrue(sendt.size() > 0);
        assertTrue(sendt.get(0).has("temaName"));
        assertTrue(sendt.get(0).has("baggrundA"));
    }

    @Test
    void nyere_temaer_i_skyen_skrives_til_disken() throws SyncException {
        lokaleStempler(GAMMEL);
        ObjectNode doc = SyncObjectMapper.INSTANCE.createObjectNode();
        ObjectNode tema = doc.putArray("temaer").addObject();
        for (String key : new String[]{"baggrundA", "baggrundB", "baggrundC", "baggrundD", "tekstA", "tekstB", "tekstC", "kant"}) {
            tema.put(key, "#123456");
        }
        tema.put("temaName", "SkyTema");
        tema.put("font", "Courier New");
        doc.put("updatedAt", NY.toString());
        sky.documents.put(TEMA_PATH, doc);

        synkroniser();

        assertEquals(1, IOutils.loadTemaer().size());
        assertTrue(IOutils.loadTemaer().containsKey("SkyTema"));
        assertTrue(user.harHentetTemaerFraSkyen());
    }

    @Test
    void sync_slaaet_fra_roerer_ingenting() throws SyncException {
        SyncDTO dto = IOutils.loadSync();
        dto.syncEnabled = false;
        IOutils.saveSync(dto);

        synkroniser();

        assertTrue(sky.patched.isEmpty());
    }

    private static class FakeFirestore extends FirestoreClient {
        final Map<String, JsonNode> documents = new LinkedHashMap<>();
        final Map<String, JsonNode> patched = new LinkedHashMap<>();

        @Override
        public Optional<JsonNode> getDocument(String idToken, String docPath) {
            return Optional.ofNullable(documents.get(docPath));
        }

        @Override
        public void patchDocument(String idToken, String docPath, JsonNode plainFields) {
            patched.put(docPath, plainFields);
            documents.put(docPath, plainFields);
        }

        @Override
        public Map<String, JsonNode> listDocuments(String idToken, String collectionPath) {
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
