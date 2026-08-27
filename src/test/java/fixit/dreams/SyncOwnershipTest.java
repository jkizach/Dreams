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
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// Den billige sync-vej: når skyens meta/state-dokument siger at DENNE maskine også var den
// sidste der skrev, kan hele listningen af alle drømme springes over. Testene her måler
// nøjagtig dét - hvor mange kald der bliver gjort, og hvilke - for det er hele pointen.
//
// Sikkerheden i modellen ligger i at den kun springer over på et positivt svar. Derfor
// handler halvdelen af testene om at den falder tilbage til den dyre vej: fremmed maskine,
// mistet indeks, log ud.
class SyncOwnershipTest {

    private static final String UID = "uid-test";
    private static final String STATE_PATH = "users/" + UID + "/meta/state";
    private static final String DREAMS_PATH = "users/" + UID + "/dreams";
    private static final String CAT_PATH = "users/" + UID + "/meta/categories";

    private static final List<String> FILNAVNE = List.of(
            "user.json", "temaer.json", "cats.json", "dreams.json", "sync.json", "deleted.json",
            "meta.json", "cloudindex.json");

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

    // ---------- Første sync: den dyre vej, og et ejerskab efterlades ----------

    @Test
    void foerste_sync_gaar_den_dyre_vej_og_saetter_et_ejerskab() throws SyncException {
        synkroniser();

        assertEquals(1, sky.listKald, "uden et ejerskabsdokument SKAL skyen listes");
        assertTrue(sky.patched.containsKey(STATE_PATH), "ejerskabet blev ikke skrevet");
        assertFalse(sky.patched.get(STATE_PATH).path("machineId").asText().isBlank());
        assertNotNull(IOutils.loadCloudIndex(), "indekset skal ligge klar til næste gang");
    }

    @Test
    void maskinId_laves_en_gang_og_bliver_liggende() throws SyncException {
        synkroniser();
        String foerste = IOutils.loadSync().machineId;

        sky.nulstilTællere();
        synkroniser();

        assertEquals(foerste, IOutils.loadSync().machineId,
                "et nyt id ved hver sync ville sende os ad den dyre vej hver gang");
    }

    // ---------- Anden sync: intet nyt, og næsten intet at betale for ----------

    @Test
    void anden_sync_lister_ikke_skyen_igen() throws SyncException {
        synkroniser();
        sky.nulstilTællere();

        synkroniser();

        assertEquals(0, sky.listKald, "skyen blev listet selvom vi selv skrev sidst");
    }

    @Test
    void anden_sync_henter_kun_ejerskabsdokumentet() throws SyncException {
        synkroniser();
        sky.nulstilTællere();

        synkroniser();

        assertEquals(List.of(STATE_PATH), sky.hentede,
                "en almindelig sync må koste præcis én læsning - ellers er meta-genvejen utæt");
    }

    @Test
    void en_sync_uden_aendringer_skriver_overhovedet_ingenting() throws SyncException {
        synkroniser();
        sky.nulstilTællere();

        synkroniser();

        assertTrue(sky.patched.isEmpty(),
                "der blev skrevet " + sky.patched.keySet() + " selvom intet var ændret");
    }

    // ---------- Ændringer finder stadig vej op ----------

    @Test
    void en_ny_droem_sendes_op_uden_at_skyen_listes() throws SyncException {
        synkroniser();
        sky.nulstilTællere();

        user.addDream(droem("ny-droem", "Jeg fløj over byen", Instant.parse("2026-08-27T10:00:00Z")));
        synkroniser();

        assertEquals(0, sky.listKald);
        assertTrue(sky.patched.containsKey(DREAMS_PATH + "/ny-droem"), "den nye drøm kom aldrig afsted");
        assertTrue(sky.patched.containsKey(STATE_PATH), "ejerskabet skal opdateres når vi har skrevet");
    }

    @Test
    void en_redigeret_droem_sendes_op_igen_men_en_uroert_gor_ikke() throws SyncException {
        user.addDream(droem("a", "Første drøm", Instant.parse("2026-08-27T10:00:00Z")));
        user.addDream(droem("b", "Anden drøm", Instant.parse("2026-08-27T10:00:00Z")));
        synkroniser();
        sky.nulstilTællere();

        // Samme id, nyere tidsstempel - præcis det en redigering giver.
        user.addDream(droem("a", "Første drøm, nu med tolkning", Instant.parse("2026-08-27T12:00:00Z")));
        synkroniser();

        assertTrue(sky.patched.containsKey(DREAMS_PATH + "/a"), "den redigerede drøm blev ikke sendt");
        assertFalse(sky.patched.containsKey(DREAMS_PATH + "/b"),
                "den urørte drøm blev sendt igen - det var præcis kvotefejlen fra 7011e74");
    }

    @Test
    void indekset_foelger_med_saa_naeste_sync_ved_besked() throws SyncException {
        synkroniser();
        user.addDream(droem("ny-droem", "Jeg fløj over byen", Instant.parse("2026-08-27T10:00:00Z")));
        synkroniser();

        LinkedHashMap<String, Instant> indeks = IOutils.loadCloudIndex();
        assertEquals(Instant.parse("2026-08-27T10:00:00Z"), indeks.get("ny-droem"),
                "uden indeksopdatering ville drømmen blive sendt op igen ved hver eneste sync");
    }

    @Test
    void en_slettet_droem_bliver_til_en_gravsten_uden_listning() throws SyncException {
        user.addDream(droem("a", "Første drøm", Instant.parse("2026-08-27T10:00:00Z")));
        synkroniser();
        sky.nulstilTællere();

        user.deleteDream("a");
        LinkedHashMap<String, Instant> koe = new LinkedHashMap<>();
        koe.put("a", Instant.parse("2026-08-27T13:00:00Z"));
        IOutils.saveDeletedDreams(koe);

        synkroniser();

        assertEquals(0, sky.listKald);
        JsonNode gravsten = sky.patched.get(DREAMS_PATH + "/a");
        assertNotNull(gravsten, "gravstenen kom aldrig afsted");
        assertEquals("2026-08-27T13:00:00Z", gravsten.path("deletedAt").asText());
    }

    @Test
    void aendrede_kategorier_sendes_op_uden_at_meta_dokumentet_hentes() throws SyncException {
        synkroniser();
        sky.nulstilTællere();

        // Svarer til at brugeren har redigeret sine kategorier: meta.json får et nyere stempel.
        MetaDTO meta = IOutils.loadMeta();
        meta.categories.updatedAt = Instant.parse("2026-08-27T14:00:00Z");
        IOutils.saveMeta(meta);

        synkroniser();

        assertEquals(List.of(STATE_PATH), sky.hentede,
                "meta-dokumentet blev hentet - ejerskabets stempler skulle have svaret på det");
        assertTrue(sky.patched.containsKey(CAT_PATH), "de ændrede kategorier kom aldrig afsted");
        assertEquals("2026-08-27T14:00:00Z",
                sky.patched.get(STATE_PATH).path("categoriesUpdatedAt").asText(),
                "ejerskabet skal huske det nye stempel, ellers sendes kategorierne igen næste gang");
    }

    // ---------- Fallback: genvejen tages kun på et positivt svar ----------

    @Test
    void et_fremmed_maskinId_tvinger_den_dyre_vej() throws SyncException {
        synkroniser();
        sky.nulstilTællere();

        // Som om en anden maskine havde synkroniseret imens.
        ObjectNode fremmed = SyncObjectMapper.INSTANCE.createObjectNode();
        fremmed.put("machineId", "en-helt-anden-maskine");
        sky.documents.put(STATE_PATH, fremmed);

        synkroniser();

        assertEquals(1, sky.listKald, "en fremmed maskine skal sende os hele vejen rundt igen");
    }

    @Test
    void en_droem_fra_en_anden_maskine_hentes_ned_ad_den_dyre_vej() throws SyncException {
        synkroniser();
        sky.nulstilTællere();

        ObjectNode fremmed = SyncObjectMapper.INSTANCE.createObjectNode();
        fremmed.put("machineId", "en-helt-anden-maskine");
        sky.documents.put(STATE_PATH, fremmed);
        sky.samling.put("fra-den-anden", skyDroem("fra-den-anden", "Skrevet på den nye pc"));

        synkroniser();

        assertTrue(user.getDreams().containsKey("fra-den-anden"),
                "drømmen fra den anden maskine blev aldrig hentet ned");
    }

    @Test
    void et_mistet_indeks_tvinger_den_dyre_vej() throws SyncException, IOException {
        synkroniser();
        sky.nulstilTællere();

        // Ejerskabet siger stadig at det var os - men uden indekset aner vi ikke hvad der
        // allerede ligger deroppe, og så må vi ikke tro noget som helst.
        Files.deleteIfExists(AppPaths.APP_DATA_PATH.resolve("cloudindex.json"));

        synkroniser();

        assertEquals(1, sky.listKald, "uden indeks må genvejen ikke tages");
    }

    // Indekset er kun værd at have, hvis det aldrig lover mere end der faktisk er kommet
    // afsted. Sker det, holder syncen op med at sende drømmen - permanent, og uden en lyd.
    @Test
    void en_fejlet_push_efterlader_ikke_et_indeks_der_lover_for_meget() throws SyncException {
        synkroniser();

        user.addDream(droem("ny-droem", "Jeg fløj over byen", Instant.parse("2026-08-27T10:00:00Z")));
        sky.fejlPaaSti = DREAMS_PATH + "/ny-droem";

        assertThrows(SyncException.class, this::synkroniser);

        assertFalse(IOutils.loadCloudIndex().containsKey("ny-droem"),
                "indekset påstod at drømmen lå i skyen, selvom skrivningen fejlede");
    }

    @Test
    void log_ud_sletter_indekset() throws SyncException {
        synkroniser();
        assertNotNull(IOutils.loadCloudIndex());

        new SyncService(user, new FakeAuth(), sky).logout();

        assertNull(IOutils.loadCloudIndex(),
                "et indeks fra den gamle konto ville få drømmene til aldrig at blive sendt til den nye");
    }

    // ---------- Hjælpere ----------

    private Dream droem(String id, String indhold, Instant updatedAt) {
        DreamData data = new DreamData();
        data.id = id;
        data.categories = new ArrayList<>();
        data.indhold = indhold;
        data.dagrest = "";
        data.tolkning = "";
        data.dato = LocalDate.of(2026, 8, 27);
        data.updatedAt = updatedAt;
        return new Dream(data);
    }

    private ObjectNode skyDroem(String id, String indhold) {
        ObjectNode doc = SyncObjectMapper.INSTANCE.createObjectNode();
        doc.put("id", id);
        doc.putArray("categories");
        doc.put("indhold", indhold);
        doc.put("dagrest", "");
        doc.put("tolkning", "");
        doc.put("dato", "2026-08-27");
        doc.put("updatedAt", "2026-08-27T16:00:00Z");
        return doc;
    }

    private static class FakeFirestore extends FirestoreClient {
        final Map<String, JsonNode> documents = new LinkedHashMap<>();
        final Map<String, JsonNode> samling = new LinkedHashMap<>();
        final Map<String, JsonNode> patched = new LinkedHashMap<>();
        final List<String> hentede = new ArrayList<>();
        int listKald = 0;
        String fejlPaaSti = null;

        void nulstilTællere() {
            patched.clear();
            hentede.clear();
            listKald = 0;
        }

        @Override
        public Optional<JsonNode> getDocument(String idToken, String docPath) {
            hentede.add(docPath);
            return Optional.ofNullable(documents.get(docPath));
        }

        @Override
        public void patchDocument(String idToken, String docPath, JsonNode plainFields) throws FirestoreException {
            if (docPath.equals(fejlPaaSti)) {
                throw new FirestoreException("nægtet af den falske sky", 503);
            }
            patched.put(docPath, plainFields);
            documents.put(docPath, plainFields);
        }

        @Override
        public Map<String, JsonNode> listDocuments(String idToken, String collectionPath) {
            listKald++;
            return new LinkedHashMap<>(samling);
        }
    }

    private static class FakeAuth extends FirebaseAuthClient {
        @Override
        public AuthResult refreshToken(String refreshToken) {
            return new AuthResult("id-token", refreshToken, UID, 3600);
        }
    }
}
