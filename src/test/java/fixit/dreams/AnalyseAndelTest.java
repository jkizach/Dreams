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
}
