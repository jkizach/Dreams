package fixit.dreams;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tester fallback'en for fonte der ikke er installeret. Der testes bevidst ikke på konkrete
 * fontnavne ud over den logiske "Monospaced", da resten afhænger af hvad maskinen har.
 */
class FonteTest {

    @Test
    void ukendtFontFalderTilbageTilMonospace() {
        assertEquals(Fonte.FALLBACK, Fonte.tilgaengelig("Findes Slet Ikke 12345"));
    }

    @Test
    void tomtEllerNulFontnavnFalderTilbage() {
        assertEquals(Fonte.FALLBACK, Fonte.tilgaengelig(null));
        assertEquals(Fonte.FALLBACK, Fonte.tilgaengelig(""));
        assertEquals(Fonte.FALLBACK, Fonte.tilgaengelig("   "));
    }

    @Test
    void installeretFontBevares() {
        // Den forste familie JavaFX selv oplyser maa per definition vaere installeret
        String installeret = javafx.scene.text.Font.getFamilies().get(0);
        assertEquals(installeret, Fonte.tilgaengelig(installeret));
    }

    @Test
    void monospaceListenErIkkeTomOgIndeholderIngenProportionaleFonte() {
        List<String> familier = Fonte.monospaceFamilier();
        assertFalse(familier.isEmpty(), "der bor findes mindst en monospace-font");
        for (String f : familier) {
            javafx.scene.text.Font font = javafx.scene.text.Font.font(f, 14);
            javafx.scene.text.Text smal = new javafx.scene.text.Text("i");
            javafx.scene.text.Text bred = new javafx.scene.text.Text("W");
            smal.setFont(font);
            bred.setFont(font);
            assertEquals(smal.getLayoutBounds().getWidth(), bred.getLayoutBounds().getWidth(), 0.01,
                    f + " er ikke monospace");
        }
    }

    @Test
    void tilbudteFonteKanViseDanskTekst() {
        for (String f : Fonte.monospaceFamilier()) {
            java.awt.Font awt = new java.awt.Font(f, java.awt.Font.PLAIN, 12);
            assertEquals(-1, awt.canDisplayUpTo("Drømme æøå ÆØÅ 0123 -%"),
                    f + " mangler glyffer til dansk tekst");
        }
    }

    @Test
    void symbolfonteErSorteretFra() {
        // TINspireKeys er monospace, men har ikke engang en mellemrums-glyf
        assertFalse(Fonte.monospaceFamilier().contains("TINspireKeys"));
    }
}
