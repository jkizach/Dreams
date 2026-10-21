package fixit.dreams;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Navnene brugeren har sagt nej til som personer: før i ikkenavne.json, nu i user.json, så de
// følger med i syncen. Overgangen må ikke koste de nej'er der allerede er givet.
//
// Skriver rigtige filer i den delte test-home-mappe (se pom.xml's surefire-konfiguration)
// og rydder op efter sig.
class IkkeNavnePersistensTest {

    private Path userJson;
    private Path gammelFil;

    @BeforeEach
    void setUp() throws IOException {
        User.resetForTests();
        userJson = AppPaths.APP_DATA_PATH.resolve("user.json");
        gammelFil = AppPaths.APP_DATA_PATH.resolve("ikkenavne.json");
        ryd();
    }

    @AfterEach
    void tearDown() throws IOException {
        ryd();
        User.resetForTests();
    }

    private void ryd() throws IOException {
        Files.deleteIfExists(userJson);
        Files.deleteIfExists(gammelFil);
        Files.deleteIfExists(AppPaths.APP_DATA_PATH.resolve("meta.json"));
        Files.deleteIfExists(AppPaths.APP_DATA_PATH.resolve("dreams.json"));
        Files.deleteIfExists(AppPaths.APP_DATA_PATH.resolve("cats.json"));
    }

    private void genstart() {
        User.resetForTests();
    }

    @Test
    void en_gammel_user_json_henter_listen_fra_den_gamle_fil() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt grønt","visAdvarsel":false,"visKollektiv":false,
                 "visHolografisk":false,"startFromThisDate":null,"schemaVersion":4}
                """);
        Files.writeString(gammelFil, "[\"lovns\", \"minecraft\"]");

        User user = User.getInstance();

        assertEquals(List.of("lovns", "minecraft"), List.copyOf(user.getIkkeNavne()));
    }

    @Test
    void efter_foerste_gem_ligger_listen_i_user_json_og_den_gamle_fil_er_ligegyldig() throws IOException {
        Files.writeString(gammelFil, "[\"lovns\"]");
        IOutils.saveUser(User.getInstance());

        // Står feltet i user.json, læses den gamle fil ikke - heller ikke hvis den siger noget andet.
        Files.writeString(gammelFil, "[\"lovns\", \"skulle ikke komme med\"]");
        genstart();

        assertEquals(List.of("lovns"), IOutils.loadUser().ikkeNavne);
        assertEquals(List.of("lovns"), List.copyOf(User.getInstance().getIkkeNavne()));
    }

    @Test
    void en_tom_liste_i_user_json_er_ogsaa_et_svar() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt grønt","ikkeNavne":[],"schemaVersion":4}
                """);
        Files.writeString(gammelFil, "[\"lovns\"]");

        assertTrue(User.getInstance().getIkkeNavne().isEmpty());
    }

    @Test
    void et_nej_overlever_en_genstart() {
        UserService service = new UserService(User.getInstance());
        service.tilføjIkkeNavne(List.of("paris"));
        genstart();

        assertEquals(List.of("paris"), List.copyOf(User.getInstance().getIkkeNavne()));
    }

    // Samme grund som gemStartDato: har syncen hentet indstillinger i sessionen, er disken den
    // nyeste udgave, og et nej må ikke tage hukommelsens gamle temanavn med sig derned.
    @Test
    void gemIkkeNavne_skriver_kun_listen_og_lader_resten_staa() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"hentet fra skyen","visAdvarsel":true,"visKollektiv":false,
                 "visHolografisk":true,"startFromThisDate":null,"startDatoValgtAfBruger":false,
                 "ikkeNavne":[],"schemaVersion":4}
                """);
        User user = User.getInstance();
        user.tilføjIkkeNavne(List.of("gud"));

        IOutils.gemIkkeNavne(user);

        UserDTO paaDisken = IOutils.loadUser();
        assertEquals(List.of("gud"), paaDisken.ikkeNavne);
        assertEquals("hentet fra skyen", paaDisken.foretrukneTema);
        assertTrue(paaDisken.visHolografisk);
    }

    @Test
    void et_navn_der_allerede_er_afvist_gemmer_ikke_igen() {
        User user = User.getInstance();
        assertTrue(user.tilføjIkkeNavne(List.of("gud")));
        assertFalse(user.tilføjIkkeNavne(List.of("gud")));
    }
}
