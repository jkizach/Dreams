package fixit.dreams.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fixit.dreams.CategoryDTO;
import fixit.dreams.DreamData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

// RØGTEST mod det RIGTIGE Firebase-projekt (dreams-b7dac) - den eneste test i suiten der rører
// netværket. Alt andet her i huset er ren logik og kører mod en fake sky (se MetaSyncTest);
// denne dækker det ene lag de ikke kan nå: HTTP-kaldene og Firestore-reglerne, som kun det
// levende projekt kan svare på.
//
// Springes over medmindre DREAMS_ROEGTEST=1 er sat i miljøet, så en almindelig `mvn test`
// stadig er 100% netværksfri og kan køre offline:
//
//     DREAMS_ROEGTEST=1 mvn test -Dtest=FirebaseSmokeTest
//
// Testen medbringer ingen hemmeligheder og rører aldrig din egen konto: den opretter to helt
// friske engangskonti, arbejder udelukkende i deres eget rum, og sletter både dokumenterne og
// kontiene igen bagefter (se rydOp).
@EnabledIfEnvironmentVariable(named = "DREAMS_ROEGTEST", matches = "1")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FirebaseSmokeTest {

    private static final String DOKUMENT_URL = "https://firestore.googleapis.com/v1/projects/"
            + FirebaseConfig.PROJECT_ID + "/databases/(default)/documents/";
    private static final String SLET_KONTO_URL =
            "https://identitytoolkit.googleapis.com/v1/accounts:delete?key=" + FirebaseConfig.WEB_API_KEY;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    private static final FirebaseAuthClient auth = new FirebaseAuthClient();
    private static final FirestoreClient firestore = new FirestoreClient();

    private static final String DROEM_ID = "roegtest-" + UUID.randomUUID();
    private static final String GRAVSTEN_ID = "roegtest-gravsten-" + UUID.randomUUID();

    private static AuthResult mig;      // "min maskine"
    private static AuthResult fremmed;  // en anden bruger af appen

    private static String droemmeSti;
    private static String droemSti;
    private static String gravstenSti;
    private static String kategoriSti;

    @BeforeAll
    static void opretEngangskonti() throws FirebaseAuthException {
        mig = opretKonto();
        fremmed = opretKonto();

        droemmeSti = "users/" + mig.uid() + "/dreams";
        droemSti = droemmeSti + "/" + DROEM_ID;
        gravstenSti = droemmeSti + "/" + GRAVSTEN_ID;
        kategoriSti = "users/" + mig.uid() + "/meta/categories";
    }

    // Rydder altid op, også når en test undervejs er fejlet - ellers ville et fejlet løb
    // efterlade skrald i det rigtige projekt.
    @AfterAll
    static void rydOp() {
        if (mig != null) {
            sletDokument(mig.idToken(), droemSti);
            sletDokument(mig.idToken(), gravstenSti);
            sletDokument(mig.idToken(), kategoriSti);
            sletKonto(mig.idToken());
        }
        if (fremmed != null) {
            sletKonto(fremmed.idToken());
        }
    }

    // ---------- Adgang: virker nøglen, og lukker Auth os overhovedet ind? ----------

    @Test
    @Order(1)
    void oprettelse_af_konto_giver_uid_og_tokens() {
        assertNotNull(mig.uid());
        assertFalse(mig.uid().isBlank(), "uid mangler - så kan ingen sti bygges");
        assertFalse(mig.idToken().isBlank());
        assertFalse(mig.refreshToken().isBlank());
        assertTrue(mig.expiresInSeconds() > 0);
        assertNotEquals(mig.uid(), fremmed.uid(), "de to engangskonti skal være forskellige brugere");
    }

    // Det er refresh-tokenet der ligger i sync.json og holder brugeren logget ind mellem
    // sessioner - selve idToken'et er dødt efter en time.
    @Test
    @Order(2)
    void refresh_token_giver_et_nyt_gyldigt_login() throws FirebaseAuthException {
        AuthResult fornyet = auth.refreshToken(mig.refreshToken());

        assertEquals(mig.uid(), fornyet.uid(), "fornyelsen skal give SAMME bruger");
        assertFalse(fornyet.idToken().isBlank());
        assertTrue(fornyet.expiresInSeconds() > 0);
    }

    @Test
    @Order(3)
    void forkert_adgangskode_giver_en_kode_vi_kan_oversaette() {
        FirebaseAuthException e = assertThrows(FirebaseAuthException.class,
                () -> auth.signIn("roegtest-findes-ikke@dreams-roegtest.invalid", "forkert-kode-123"));

        assertFalse(e.toDanishMessage().contains("("),
                "fejlkoden " + e.getErrorCode() + " har ingen dansk oversættelse i FirebaseAuthException");
    }

    // ---------- Trådformatet: overlever en rigtig drøm turen op og ned? ----------

    @Test
    @Order(4)
    void en_droem_kan_skrives_og_hentes_igen_uden_at_tabe_noget() throws FirestoreException {
        DreamData afsendt = byggDroem();

        firestore.patchDocument(mig.idToken(), droemSti, SyncObjectMapper.INSTANCE.valueToTree(afsendt));

        Optional<JsonNode> hentet = firestore.getDocument(mig.idToken(), droemSti);
        assertTrue(hentet.isPresent(), "dokumentet blev skrevet, men kunne ikke hentes igen");

        DreamData retur = assertDoesNotThrow(
                () -> SyncObjectMapper.INSTANCE.treeToValue(hentet.get(), DreamData.class));

        assertEquals(afsendt.id, retur.id);
        assertEquals(afsendt.indhold, retur.indhold, "æøå/linjeskift overlevede ikke turen");
        assertEquals(afsendt.dagrest, retur.dagrest);
        assertEquals(afsendt.tolkning, retur.tolkning);
        assertEquals(afsendt.dato, retur.dato, "LocalDate skal komme retur som samme dag");
        assertEquals(afsendt.updatedAt, retur.updatedAt, "updatedAt er hele last-write-wins-logikken");

        // Kategorierne er det eneste indlejrede objekt-i-array vi sender, og dermed det eneste
        // sted FirestoreJson's mapValue/arrayValue reelt bliver prøvet af mod Firestore.
        assertEquals(1, retur.categories.size());
        assertEquals("Personer", retur.categories.get(0).name);
        assertEquals(new TreeSet<>(List.of("Mormor", "Ukendt kvinde")), retur.categories.get(0).symbols);
    }

    @Test
    @Order(5)
    void en_tom_kategoriliste_kommer_retur_som_tom_og_ikke_som_null() throws FirestoreException {
        DreamData uden = byggDroem();
        uden.categories = new ArrayList<>();
        uden.tolkning = null;

        firestore.patchDocument(mig.idToken(), droemSti, SyncObjectMapper.INSTANCE.valueToTree(uden));
        JsonNode hentet = firestore.getDocument(mig.idToken(), droemSti).orElseThrow();

        assertTrue(hentet.path("categories").isArray());
        assertEquals(0, hentet.path("categories").size());
        assertTrue(hentet.path("tolkning").isNull(), "et tomt felt må ikke forsvinde helt fra dokumentet");

        // Læg drømmen tilbage som den var, så listDocuments-testen ser den rigtige udgave.
        firestore.patchDocument(mig.idToken(), droemSti, SyncObjectMapper.INSTANCE.valueToTree(byggDroem()));
    }

    @Test
    @Order(6)
    void en_gravsten_genkendes_stadig_efter_turen_gennem_skyen() throws FirestoreException {
        Instant slettet = Instant.parse("2026-08-27T10:15:30Z");

        firestore.patchDocument(mig.idToken(), gravstenSti, Tombstone.of(GRAVSTEN_ID, slettet));
        JsonNode hentet = firestore.getDocument(mig.idToken(), gravstenSti).orElseThrow();

        assertTrue(Tombstone.isTombstone(hentet),
                "gravstenen blev ikke genkendt efter turen - så ville en slettet drøm genopstå");
        assertEquals(slettet.toString(), hentet.path("updatedAt").asText());
    }

    @Test
    @Order(7)
    void listDocuments_finder_baade_droemmen_og_gravstenen() throws FirestoreException {
        Map<String, JsonNode> alle = firestore.listDocuments(mig.idToken(), droemmeSti);

        assertTrue(alle.containsKey(DROEM_ID), "drømmen manglede i samlingen");
        assertTrue(alle.containsKey(GRAVSTEN_ID), "gravstenen manglede i samlingen");
        assertEquals(DROEM_ID, alle.get(DROEM_ID).path("id").asText(),
                "dokument-id'et skal svare til drømmens eget id");
    }

    @Test
    @Order(8)
    void et_dokument_der_ikke_findes_er_tomt_og_ikke_en_fejl() throws FirestoreException {
        assertEquals(Optional.empty(),
                firestore.getDocument(mig.idToken(), droemmeSti + "/findes-slet-ikke-" + UUID.randomUUID()));
    }

    @Test
    @Order(9)
    void en_tom_samling_giver_et_tomt_kort_og_ikke_en_fejl() throws FirestoreException {
        assertTrue(firestore.listDocuments(fremmed.idToken(), "users/" + fremmed.uid() + "/dreams").isEmpty());
    }

    // ---------- Reglerne: er users/{uid}/{document=**} rent faktisk udgivet? ----------

    @Test
    @Order(10)
    void meta_dokumenter_er_ogsaa_daekket_af_reglen() throws FirestoreException {
        ObjectNode doc = SyncObjectMapper.INSTANCE.createObjectNode();
        doc.putArray("categories").addObject().put("name", "Steder");
        doc.put("updatedAt", "2026-08-27T09:00:00Z");

        firestore.patchDocument(mig.idToken(), kategoriSti, doc);

        JsonNode hentet = firestore.getDocument(mig.idToken(), kategoriSti).orElseThrow();
        assertEquals("Steder", hentet.path("categories").path(0).path("name").asText(),
                "meta-dokumenterne ligger et niveau dybere end drømmene - hvis kun dreams virker, "
                        + "er den udgivne regel ikke jokertegns-udgaven i firestore.rules");
    }

    @Test
    @Order(11)
    void en_anden_bruger_kan_ikke_laese_mine_droemme() {
        FirestoreException e = assertThrows(FirestoreException.class,
                () -> firestore.getDocument(fremmed.idToken(), droemSti));

        assertEquals(403, e.getStatusCode(), "en fremmed fik adgang til mine drømme: " + e.getMessage());
    }

    @Test
    @Order(12)
    void en_anden_bruger_kan_ikke_liste_min_samling() {
        FirestoreException e = assertThrows(FirestoreException.class,
                () -> firestore.listDocuments(fremmed.idToken(), droemmeSti));

        assertEquals(403, e.getStatusCode(), "en fremmed kunne liste mine drømme: " + e.getMessage());
    }

    @Test
    @Order(13)
    void en_anden_bruger_kan_ikke_skrive_i_mit_rum() {
        ObjectNode haervaerk = SyncObjectMapper.INSTANCE.createObjectNode();
        haervaerk.put("indhold", "skrevet af en fremmed");

        FirestoreException e = assertThrows(FirestoreException.class,
                () -> firestore.patchDocument(fremmed.idToken(), droemSti, haervaerk));

        assertEquals(403, e.getStatusCode(), "en fremmed kunne skrive i mine drømme: " + e.getMessage());
    }

    @Test
    @Order(14)
    void uden_gyldigt_login_er_der_ingen_adgang() {
        FirestoreException e = assertThrows(FirestoreException.class,
                () -> firestore.getDocument("ikke-et-rigtigt-token", droemSti));

        assertTrue(e.getStatusCode() == 401 || e.getStatusCode() == 403,
                "et ugyldigt token gav " + e.getStatusCode() + ": " + e.getMessage());
    }

    // ---------- Hjælpere ----------

    private static DreamData byggDroem() {
        CategoryDTO personer = new CategoryDTO();
        personer.name = "Personer";
        personer.symbols = new TreeSet<>(List.of("Mormor", "Ukendt kvinde"));
        personer.customOrder = new ArrayList<>(List.of("Mormor", "Ukendt kvinde"));

        DreamData d = new DreamData();
        d.id = DROEM_ID;
        d.categories = new ArrayList<>(List.of(personer));
        d.indhold = "Røgtest: æøå ÆØÅ, et linjeskift\nog et \"citat\".";
        d.dagrest = "Ingen dagrest";
        d.tolkning = "Ingen tolkning";
        d.dato = LocalDate.of(2026, 8, 27);
        d.updatedAt = Instant.parse("2026-08-27T08:30:00Z");
        return d;
    }

    private static AuthResult opretKonto() throws FirebaseAuthException {
        return auth.signUp(
                "roegtest-" + UUID.randomUUID() + "@dreams-roegtest.invalid",
                "roegtest-" + UUID.randomUUID());
    }

    // FirestoreClient har med vilje ingen slet-metode (appen bruger gravsten i stedet), så
    // oprydningen kalder REST-API'et direkte.
    private static void sletDokument(String idToken, String sti) {
        send(HttpRequest.newBuilder()
                .uri(URI.create(DOKUMENT_URL + sti))
                .header("Authorization", "Bearer " + idToken)
                .DELETE());
    }

    private static void sletKonto(String idToken) {
        send(HttpRequest.newBuilder()
                .uri(URI.create(SLET_KONTO_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"idToken\":\"" + idToken + "\"}")));
    }

    private static void send(HttpRequest.Builder builder) {
        try {
            HttpResponse<String> svar = HTTP.send(
                    builder.timeout(Duration.ofSeconds(20)).build(), HttpResponse.BodyHandlers.ofString());
            if (svar.statusCode() != 200) {
                System.err.println("Oprydning fejlede (" + svar.statusCode() + "): " + svar.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Oprydning fejlede: " + e);
        }
    }
}
