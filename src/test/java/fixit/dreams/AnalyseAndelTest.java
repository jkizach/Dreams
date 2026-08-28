package fixit.dreams;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Tester formateringen af "antal (andel%)" på Tal-tabbens kategorioverskrifter. */
class AnalyseAndelTest {

    @Test
    void formatererAntalOgAfrundetProcent() {
        assertEquals("101 [13%]", AnalyseService.formatAntalOgAndel(101, 807));
    }

    @Test
    void halveProcenterRundesOp() {
        assertEquals("1 [13%]", AnalyseService.formatAntalOgAndel(1, 8));   // 12,5 -> 13
        assertEquals("3 [38%]", AnalyseService.formatAntalOgAndel(3, 8));   // 37,5 -> 38
        assertEquals("1 [3%]", AnalyseService.formatAntalOgAndel(1, 40));   // 2,5  -> 3
    }

    @Test
    void afrunderNedUnderHalv() {
        assertEquals("1 [11%]", AnalyseService.formatAntalOgAndel(1, 9));   // 11,1 -> 11
    }

    @Test
    void yderpunkter() {
        assertEquals("0 [0%]", AnalyseService.formatAntalOgAndel(0, 807));
        assertEquals("807 [100%]", AnalyseService.formatAntalOgAndel(807, 807));
    }

    @Test
    void udeladerProcentNaarDerIkkeErDroemme() {
        assertEquals("0", AnalyseService.formatAntalOgAndel(0, 0));
    }

    // ---------- Andelen alene, til Tal-tabbens binære symboler ----------

    @Test
    void andelAleneBrugerSammeAfrunding() {
        assertEquals("[13%]", AnalyseService.formatAndel(101, 807));
        assertEquals("[13%]", AnalyseService.formatAndel(1, 8));    // 12,5 -> 13
        assertEquals("[11%]", AnalyseService.formatAndel(1, 9));    // 11,1 -> 11
        assertEquals("[0%]", AnalyseService.formatAndel(0, 807));
        assertEquals("[100%]", AnalyseService.formatAndel(807, 807));
    }

    // Kolonnen skal stå tom, ikke vise "0%": uden drømme i intervallet findes brøken ikke.
    @Test
    void ingenDroemmeGiverTomAndel() {
        assertEquals("", AnalyseService.formatAndel(0, 0));
    }

    // De to formatteringer må aldrig kunne vise hver sit tal for den samme brøk - derfor
    // bygger den lange på den korte. Går de fra hinanden, står der to sandheder på skærmen.
    @Test
    void deToFormatteringerErEnige() {
        for (int antal = 0; antal <= 40; antal++) {
            assertEquals(antal + " " + AnalyseService.formatAndel(antal, 40),
                    AnalyseService.formatAntalOgAndel(antal, 40),
                    "uenighed ved antal=" + antal);
        }
    }
}
