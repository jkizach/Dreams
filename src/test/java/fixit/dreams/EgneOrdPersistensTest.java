package fixit.dreams;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Stavekontrollens egne ord bor i user.json (UserDTO.egneOrd), så de følger med i syncen.
//
// Skriver rigtige filer i den delte test-home-mappe (se pom.xml's surefire-konfiguration)
// og rydder op efter sig.
class EgneOrdPersistensTest {

    private Path userJson;

    @BeforeEach
    void setUp() throws IOException {
        User.resetForTests();
        userJson = AppPaths.APP_DATA_PATH.resolve("user.json");
        ryd();
    }

    @AfterEach
    void tearDown() throws IOException {
        ryd();
        User.resetForTests();
    }

    private void ryd() throws IOException {
        Files.deleteIfExists(userJson);
        Files.deleteIfExists(AppPaths.APP_DATA_PATH.resolve("ikkenavne.json"));
        Files.deleteIfExists(AppPaths.APP_DATA_PATH.resolve("meta.json"));
        Files.deleteIfExists(AppPaths.APP_DATA_PATH.resolve("dreams.json"));
        Files.deleteIfExists(AppPaths.APP_DATA_PATH.resolve("cats.json"));
    }

    @Test
    void en_user_json_fra_foer_feltet_giver_en_tom_ordbog() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt grønt","ikkeNavne":[],"schemaVersion":4}
                """);
        assertTrue(User.getInstance().getEgneOrd().isEmpty());
    }

    @Test
    void ord_gemmes_med_smaa_bogstaver_og_overlever_en_genstart() {
        User user = User.getInstance();
        assertTrue(user.tilføjEgetOrd("Vipassana"));
        assertFalse(user.tilføjEgetOrd("vipassana"), "samme ord to gange er ikke nyt");
        IOutils.gemEgneOrd(user);

        User.resetForTests();
        assertEquals(List.of("vipassana"), List.copyOf(User.getInstance().getEgneOrd()));
    }

    @Test
    void gemEgneOrd_skriver_kun_listen_og_lader_resten_staa() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"hentet tema","ikkeNavne":["lovns"],"schemaVersion":4}
                """);
        User user = User.getInstance();
        user.setForetrukneTema("lokalt tema i hukommelsen");
        user.tilføjEgetOrd("hjertebøn");

        IOutils.gemEgneOrd(user);

        UserDTO paaDisken = IOutils.loadUser();
        assertEquals(List.of("hjertebøn"), paaDisken.egneOrd);
        assertEquals("hentet tema", paaDisken.foretrukneTema);
        assertEquals(List.of("lovns"), paaDisken.ikkeNavne);
    }
}
