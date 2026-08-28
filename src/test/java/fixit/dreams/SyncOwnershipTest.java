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
            "meta.json", "machine.json", "cloudindex.json");

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

    private static Path cloudIndexFil(String uid) {
        return AppPaths.APP_DATA_PATH.resolve("cloudindex-" + uid + ".json");
    }

    private void ryd() throws IOException {
        for (String navn : FILNAVNE) {
            Files.deleteIfExists(AppPaths.APP_DATA_PATH.resolve(navn));
        }
        // Indeksfilerne hedder noget forskelligt pr. konto og kan derfor ikke stå på listen.
        try (var stier = Files.list(AppPaths.APP_DATA_PATH)) {
            for (Path sti : stier.filter(p -> p.getFileName().toString().startsWith("cloudindex-")).toList()) {
                Files.deleteIfExists(sti);
            }
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
        assertNotNull(IOutils.loadCloudIndex(UID), "indekset skal ligge klar til næste gang");
    }

    @Test
    void maskinId_laves_en_gang_og_bliver_liggende() throws SyncException {
        synkroniser();
        String foerste = IOutils.loadMachineId();

        sky.nulstilTællere();
        synkroniser();

        assertEquals(foerste, IOutils.loadMachineId(),
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

        LinkedHashMap<String, Instant> indeks = IOutils.loadCloudIndex(UID);
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
        Files.deleteIfExists(cloudIndexFil(UID));

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

        assertFalse(IOutils.loadCloudIndex(UID).containsKey("ny-droem"),
                "indekset påstod at drømmen lå i skyen, selvom skrivningen fejlede");
    }

    // Log ud rev tidligere indekset med sig, fordi der kun fandtes ét. Nu hører hvert indeks til
    // ét uid, og så er det både ufarligt og en pointe at lade det ligge: logger man ind på samme
    // konto igen, koster første sync fire læsninger i stedet for at liste 800+ dokumenter.
    @Test
    void log_ud_beholder_indekset_for_kontoen() throws SyncException {
        synkroniser();
        assertNotNull(IOutils.loadCloudIndex(UID));

        new SyncService(user, new FakeAuth(), sky).logout();

        assertNotNull(IOutils.loadCloudIndex(UID),
                "indekset beskriver stadig den konto det blev skrevet for");
        assertNull(IOutils.loadSync(), "men selve login'et skal være væk");
    }

    // Sikkerheden ligger nu i filens navn frem for i at huske at slette den: en anden konto har
    // ganske enkelt ikke noget indeks, og syncen falder af sig selv tilbage på den dyre vej.
    // Holder det ikke, ville den nye kontos drømme aldrig blive sendt op.
    @Test
    void en_anden_konto_arver_ikke_det_gamle_indeks() throws SyncException {
        synkroniser();
        assertNotNull(IOutils.loadCloudIndex(UID));

        new SyncService(user, new FakeAuth(), sky).logout();

        assertNull(IOutils.loadCloudIndex("et-helt-andet-uid"));
    }

    // Maskin-id'et er hardware, ikke konto. Overlevede det ikke et log ud, ville ejerskabs-
    // dokumentet aldrig genkende maskinen igen, og indekset ville ligge der til ingen nytte -
    // genvejen kræver begge dele.
    @Test
    void log_ud_beholder_maskinId() throws SyncException {
        synkroniser();
        String foer = IOutils.loadMachineId();
        assertNotNull(foer);

        new SyncService(user, new FakeAuth(), sky).logout();

        assertEquals(foer, IOutils.loadMachineId());
    }

    // Hele pointen med de to foregående, målt hvor det gør ondt.
    @Test
    void log_ind_paa_samme_konto_igen_gaar_den_billige_vej() throws SyncException {
        synkroniser();
        new SyncService(user, new FakeAuth(), sky).logout();

        SyncDTO igen = new SyncDTO();
        igen.email = "test@example.com";
        igen.refreshToken = "refresh-token";
        igen.uid = UID;
        igen.syncEnabled = true;
        IOutils.saveSync(igen);

        sky.nulstilTællere();
        synkroniser();

        assertEquals(0, sky.listKald, "indeks og maskin-id lå der jo - skyen skulle ikke listes igen");
    }

    // Den gamle udgave havde ét fælles cloudindex.json, som stadig ligger der efter en
    // opdatering. Navngives den ikke om, lister den første sync efter opgraderingen hele skyen
    // igen - og så koster omlægningen præcis dét den skulle spare.
    @Test
    void et_gammelt_faelles_indeks_overtages_af_kontoen() throws SyncException, IOException {
        synkroniser();
        Files.move(cloudIndexFil(UID), AppPaths.APP_DATA_PATH.resolve("cloudindex.json"));
        sky.nulstilTællere();

        synkroniser();

        assertEquals(0, sky.listKald, "det gamle indeks skulle være taget i brug, ikke ignoreret");
        assertTrue(Files.notExists(AppPaths.APP_DATA_PATH.resolve("cloudindex.json")),
                "den gamle fil skal flyttes, ikke kopieres - ellers lever to indeks side om side");
    }

    // ---------- Hentede drømme skal på DISKEN, ikke kun i hukommelsen ----------

    // Appen skriver ellers kun dreams.json ved appluk. Uden dette gem ville en drøm hentet af
    // luk-syncen forsvinde med processen - og indekset ville bagefter kende den, så den
    // billige vej aldrig listede skyen igen. Drømmen ville findes i skyen og være væk her.
    @Test
    void en_hentet_droem_gemmes_med_det_samme_paa_disken() throws SyncException {
        sky.samling.put("fra-den-anden", skyDroem("fra-den-anden", "Skrevet på den nye pc"));

        synkroniser();

        assertTrue(IOutils.loadDreams().containsKey("fra-den-anden"),
                "drømmen lå kun i hukommelsen - den ville være tabt når appen lukkede");
    }

    @Test
    void indekset_kender_aldrig_en_droem_som_disken_ikke_kender() throws SyncException {
        sky.samling.put("fra-den-anden", skyDroem("fra-den-anden", "Skrevet på den nye pc"));

        synkroniser();

        for (String id : IOutils.loadCloudIndex(UID).keySet()) {
            assertTrue(IOutils.loadDreams().containsKey(id),
                    "indekset påstod at kende " + id + ", som ikke findes på disken");
        }
    }

    // ---------- Lukning uden ændringer må slet ikke røre skyen ----------

    @Test
    void efter_en_sync_er_der_intet_usendt() throws SyncException {
        user.addDream(droem("a", "Første drøm", Instant.parse("2026-08-27T10:00:00Z")));
        synkroniser();

        assertFalse(tjeneste().harUsendteÆndringer(),
                "en lukning uden ændringer ville kontakte Firebase helt unødigt");
    }

    @Test
    void en_ny_droem_taeller_som_usendt() throws SyncException {
        synkroniser();

        user.addDream(droem("ny", "Skrevet lige før lukning", Instant.parse("2026-08-27T18:00:00Z")));

        assertTrue(tjeneste().harUsendteÆndringer());
    }

    @Test
    void en_redigeret_droem_taeller_som_usendt() throws SyncException {
        user.addDream(droem("a", "Første drøm", Instant.parse("2026-08-27T10:00:00Z")));
        synkroniser();

        user.addDream(droem("a", "Første drøm, rettet", Instant.parse("2026-08-27T18:00:00Z")));

        assertTrue(tjeneste().harUsendteÆndringer());
    }

    @Test
    void en_ventende_sletning_taeller_som_usendt() throws SyncException {
        synkroniser();

        LinkedHashMap<String, Instant> koe = new LinkedHashMap<>();
        koe.put("a", Instant.parse("2026-08-27T18:00:00Z"));
        IOutils.saveDeletedDreams(koe);

        assertTrue(tjeneste().harUsendteÆndringer());
    }

    @Test
    void aendrede_kategorier_taeller_som_usendt() throws SyncException {
        synkroniser();

        MetaDTO meta = IOutils.loadMeta();
        meta.categories.updatedAt = Instant.now().plusSeconds(60);
        IOutils.saveMeta(meta);

        assertTrue(tjeneste().harUsendteÆndringer(),
                "kategorier har intet indeks - de måles mod lastSyncedAt");
    }

    @Test
    void et_mistet_indeks_taeller_som_usendt() throws SyncException, IOException {
        synkroniser();
        Files.deleteIfExists(cloudIndexFil(UID));

        assertTrue(tjeneste().harUsendteÆndringer(),
                "i tvivl skal den svare ja - ellers kan en drøm blive hængende her");
    }

    @Test
    void uden_sync_slaaet_til_er_der_aldrig_noget_usendt() {
        SyncDTO dto = IOutils.loadSync();
        dto.syncEnabled = false;
        IOutils.saveSync(dto);

        user.addDream(droem("a", "Første drøm", Instant.parse("2026-08-27T10:00:00Z")));

        assertFalse(tjeneste().harUsendteÆndringer());
    }

    // ---------- Hjælpere ----------

    private SyncService tjeneste() {
        return new SyncService(user, new FakeAuth(), sky);
    }

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
