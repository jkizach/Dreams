package fixit.dreams;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.*;

/**
 * En ny version skal have sine nye stilregler med ud, også hos brugere der har haft appen
 * længe - uden at miste temaets farver. Surefire peger user.home ind i target/test-home, så
 * filerne her er testens egne; eventuelle filer i forvejen lægges tilbage bagefter.
 */
class CSSUpdaterTest {

    private static final Path CURRENT = AppPaths.APP_DATA_PATH.resolve("currentTema.css");
    private static final Path TEMP = AppPaths.APP_DATA_PATH.resolve("tempTema.css");

    private Path gemtCurrent;
    private Path gemtTemp;

    @BeforeEach
    void gemEksisterende() throws IOException {
        gemtCurrent = flytVaek(CURRENT);
        gemtTemp = flytVaek(TEMP);
    }

    @AfterEach
    void laegTilbage() throws IOException {
        laegTilbage(gemtCurrent, CURRENT);
        laegTilbage(gemtTemp, TEMP);
    }

    @Test
    void en_gammel_fil_i_datamappen_erstattes_af_skabelonen_med_temaets_farver() throws IOException {
        Files.writeString(CURRENT, "/* gammel version */\n.root {\n\t-fx-hovedbg-background: #000000;\n}\n");
        Files.writeString(TEMP, "/* gammel version */\n");

        Tema tema = new Tema(Color.web("#112233"), Color.web("#445566"), Color.web("#262626"),
                Color.web("#333333"), Color.web("#d1d1d1"), Color.web("#1a1a1a"),
                Color.web("#62cd84"), Color.web("#778899"), "Mit eget", "Findes Slet Ikke 12345");
        CSSUpdater.init(tema.getTemaForCSSUpdater());

        for (Path fil : new Path[]{CURRENT, TEMP}) {
            String css = Files.readString(fil);
            assertFalse(css.contains("gammel version"), fil + " blev ikke overskrevet");
            assertTrue(css.contains("-fx-hovedbg-background: #112233;"), fil + " mistede temaets farve");
            assertTrue(css.contains("-fx-border-border: #778899;"), fil + " mistede temaets kantfarve");
            assertTrue(css.contains("-fx-font-family: '" + Fonte.FALLBACK + "';"), fil + " mistede fonten");
        }
    }

    @Test
    void uden_tema_staar_skabelonens_egne_farver() throws IOException {
        Files.writeString(CURRENT, "/* gammel version */\n");

        CSSUpdater.init(null);

        String css = Files.readString(CURRENT);
        assertFalse(css.contains("gammel version"));
        assertTrue(css.contains("-fx-hovedbg-background: #1a1a1a;"));
    }

    private static Path flytVaek(Path fil) throws IOException {
        if (Files.notExists(fil)) {
            return null;
        }
        Path gemt = Files.createTempFile("css-backup", ".css");
        Files.move(fil, gemt, StandardCopyOption.REPLACE_EXISTING);
        return gemt;
    }

    private static void laegTilbage(Path gemt, Path fil) throws IOException {
        if (gemt == null) {
            Files.deleteIfExists(fil);
        } else {
            Files.move(gemt, fil, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
