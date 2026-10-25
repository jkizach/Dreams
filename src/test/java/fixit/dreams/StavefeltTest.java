package fixit.dreams;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Kun stregfarven - resten af Stavefelt kræver et JavaFX-toolkit. */
class StavefeltTest {

    @Test
    void standardtemaetsMoerkeTekstfeltGiverRoed() {
        assertEquals(Color.RED, Stavefelt.stregfarve(Color.web("#333333")));
    }

    @Test
    void hvidBaggrundGiverRoed() {
        assertEquals(Color.RED, Stavefelt.stregfarve(Color.WHITE));
    }

    @Test
    void moerkeroedBaggrundGiverLysRoed() {
        assertEquals(Color.web("#ff6b6b"), Stavefelt.stregfarve(Color.web("#8b0000")));
    }

    @Test
    void lyseroedBaggrundGiverMoerkRoed() {
        assertEquals(Color.web("#b00000"), Stavefelt.stregfarve(Color.web("#ffc0cb")));
    }

    @Test
    void roedBaggrundGiverSortEllerHvidMedKontrast() {
        for (String baggrund : new String[]{"#ff0000", "#e01010", "#cc3333"}) {
            Color c = Stavefelt.stregfarve(Color.web(baggrund));
            assertTrue(c.equals(Color.BLACK) || c.equals(Color.WHITE), baggrund);
            assertTrue(Stavefelt.kontrast(c, Color.web(baggrund)) >= 3, baggrund);
        }
    }

    @Test
    void stregenHarAltidMindst3TilEnKontrast() {
        // Hele farverummet i grove trin: ingen baggrund må gøre stregen usynlig.
        for (int r = 0; r <= 255; r += 15) {
            for (int g = 0; g <= 255; g += 15) {
                for (int b = 0; b <= 255; b += 15) {
                    Color baggrund = Color.rgb(r, g, b);
                    assertTrue(Stavefelt.kontrast(Stavefelt.stregfarve(baggrund), baggrund) >= 3, baggrund.toString());
                }
            }
        }
    }
}
