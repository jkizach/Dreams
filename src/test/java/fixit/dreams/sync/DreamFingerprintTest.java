package fixit.dreams.sync;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DreamFingerprintTest {

    private static final String DATO = "2026-03-15";

    @Test
    void samme_dato_og_indhold_giver_samme_fingeraftryk() {
        // Kernen i dublet-detektionen: samme drøm migreret to steder har forskellige UUID'er,
        // men identisk dato og indhold.
        assertEquals(
                DreamFingerprint.of(DATO, "Jeg fløj over en skov"),
                DreamFingerprint.of(DATO, "Jeg fløj over en skov"));
    }

    @Test
    void forskelligt_indhold_giver_forskelligt_fingeraftryk() {
        assertNotEquals(
                DreamFingerprint.of(DATO, "Jeg fløj over en skov"),
                DreamFingerprint.of(DATO, "Jeg gik i en tunnel"));
    }

    @Test
    void samme_indhold_paa_forskellig_dato_giver_forskelligt_fingeraftryk() {
        assertNotEquals(
                DreamFingerprint.of("2026-03-15", "Jeg fløj over en skov"),
                DreamFingerprint.of("2026-03-16", "Jeg fløj over en skov"));
    }

    @Test
    void whitespace_og_store_bogstaver_ignoreres() {
        // Samme drøm gemt via to forskellige kodestier kan afvige i afsluttende linjeskift
        // eller indryk uden at være en anden drøm.
        assertEquals(
                DreamFingerprint.of(DATO, "Jeg fløj over en skov"),
                DreamFingerprint.of(DATO, "  Jeg  fløj\n over EN skov  "));
    }

    @Test
    void tomt_indhold_kan_ikke_fingeraftrykkes() {
        // Vigtigt: ellers ville to blanke drømme fra samme dato ligne perfekte dubletter.
        assertNull(DreamFingerprint.of(DATO, null));
        assertNull(DreamFingerprint.of(DATO, ""));
        assertNull(DreamFingerprint.of(DATO, "   \n  "));
    }

    @Test
    void manglende_dato_forhindrer_ikke_fingeraftryk() {
        assertNotNull(DreamFingerprint.of(null, "Jeg fløj over en skov"));
    }

    @Test
    void dato_og_indhold_kan_ikke_glide_sammen_over_skillet() {
        // Uden et skilletegn der ikke kan optræde i indholdet, ville disse to kollidere.
        assertNotEquals(
                DreamFingerprint.of("2026-03-15", "abc"),
                DreamFingerprint.of("2026-03", "15 abc"));
    }
}
