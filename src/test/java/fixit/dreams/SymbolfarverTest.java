package fixit.dreams;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stigen i Symbolfarver har fire trin, og hvert trin fanger sin egen slags ord. Testene her går
 * trin for trin, og slutter med de to der betyder mest i praksis: at HELE den indbyggede
 * farvekategori kan udledes, og at ingen af de kategorier der ikke handler om farver
 * (Dyr, Arketyper, Forløb) rammes ved et uheld.
 */
class SymbolfarverTest {

    private static double lysstyrke(Color c) {
        // Simpel luminans - nok til at afgøre om noget er lysnet eller mørknet
        return 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
    }

    private static List<String> symbolerI(String kategoriNavn) {
        User.resetForTests();
        return User.getInstance().getCategories().stream()
                .filter(c -> c.getName().equals(kategoriNavn))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Kategorien " + kategoriNavn + " findes ikke"))
                .getSymbols().stream().toList();
    }

    // ---- Trin 1: eksplicitte opslag ----

    @Test
    void chakraerne_gaar_fra_roed_i_bunden_til_violet_i_toppen() {
        assertEquals(Color.web("#d32f2f"), Symbolfarver.forSymbol("rod"));
        assertEquals(Color.web("#f57c00"), Symbolfarver.forSymbol("hara"));
        assertEquals(Color.web("#fbc02d"), Symbolfarver.forSymbol("solar plexus"));
        assertEquals(Color.web("#43a047"), Symbolfarver.forSymbol("hjerte"));
        assertEquals(Color.web("#1e88e5"), Symbolfarver.forSymbol("hals"));
        assertEquals(Color.web("#3949ab"), Symbolfarver.forSymbol("pineal"));
        assertEquals(Color.web("#8e24aa"), Symbolfarver.forSymbol("krone"));
    }

    @Test
    void alle_chakraer_har_hver_sin_farve() {
        List<String> chakraer = symbolerI("Chakraer");
        assertEquals(7, chakraer.size());
        assertEquals(7, chakraer.stream().map(Symbolfarver::forSymbol).distinct().count());
    }

    @Test
    void guld_er_ikke_bare_gul() {
        assertNotNull(Symbolfarver.forSymbol("guld"));
        assertNotEquals(Symbolfarver.forSymbol("gul"), Symbolfarver.forSymbol("guld"));
    }

    @Test
    void okker_er_en_varm_gul_der_traekker_mod_det_brunlige() {
        Color okker = Symbolfarver.forSymbol("okker");
        assertNotNull(okker);
        assertTrue(okker.getRed() > okker.getGreen(), "okker skal være varm");
        assertTrue(okker.getGreen() > okker.getBlue(), "okker skal være gullig");
        assertTrue(lysstyrke(okker) < lysstyrke(Symbolfarver.forSymbol("gul")), "okker er mørkere end gul");
    }

    @Test
    void olivengroen_er_ikke_bare_groen() {
        assertNotEquals(Symbolfarver.forSymbol("grøn"), Symbolfarver.forSymbol("olivengrøn"));
    }

    // ---- Trin 2: dansk kompositum ----

    @Test
    void grundfarver_slaas_direkte_op() {
        assertEquals(Color.web("#d32f2f"), Symbolfarver.forSymbol("rød"));
        assertEquals(Color.web("#1e88e5"), Symbolfarver.forSymbol("blå"));
        assertEquals(Color.web("#2b2b2b"), Symbolfarver.forSymbol("sort"));
    }

    @Test
    void lyse_og_moerke_praefikser_flytter_lysstyrken_den_rigtige_vej() {
        double grøn = lysstyrke(Symbolfarver.forSymbol("grøn"));
        assertTrue(lysstyrke(Symbolfarver.forSymbol("lysegrøn")) > grøn, "lysegrøn skal være lysere");
        assertTrue(lysstyrke(Symbolfarver.forSymbol("mørkegrøn")) < grøn, "mørkegrøn skal være mørkere");
    }

    @Test
    void ukendt_sammensat_ord_arver_grundfarvens_kulor() {
        // Det er hele pointen med trin 2: ord vi aldrig har set skal stadig ramme rigtigt
        Color grøn = Symbolfarver.forSymbol("grøn");
        assertEquals(grøn, Symbolfarver.forSymbol("skovgrøn"));
        assertEquals(grøn, Symbolfarver.forSymbol("flaskegrøn"));
        assertEquals(grøn, Symbolfarver.forSymbol("mosgrøn"));
        assertEquals(Symbolfarver.forSymbol("blå"), Symbolfarver.forSymbol("stålblå"));
    }

    @Test
    void praefiks_virker_ogsaa_paa_et_ukendt_sammensat_ord() {
        assertTrue(lysstyrke(Symbolfarver.forSymbol("mørkeskovgrøn"))
                < lysstyrke(Symbolfarver.forSymbol("skovgrøn")));
    }

    @Test
    void farvet_endelsen_falder_tilbage_paa_stammen() {
        assertEquals(Symbolfarver.forSymbol("bronze"), Symbolfarver.forSymbol("bronzefarvet"));
        assertEquals(Symbolfarver.forSymbol("rød"), Symbolfarver.forSymbol("rødfarvet"));
        assertEquals(Symbolfarver.forSymbol("chartreuse"), Symbolfarver.forSymbol("chartreusefarvet"));
        assertNull(Symbolfarver.forSymbol("solnedgangsfarvet"));
    }

    // ---- Trin 3: CSS-navne ----

    @Test
    void chartreuse_og_andre_laaneord_kommer_fra_css() {
        assertEquals(Color.web("#7fff00"), Symbolfarver.forSymbol("chartreuse"));
        assertEquals(Color.web("#ff00ff"), Symbolfarver.forSymbol("magenta"));
        assertEquals(Color.web("#4b0082"), Symbolfarver.forSymbol("indigo"));
        assertEquals(Color.web("#008080"), Symbolfarver.forSymbol("teal"));
    }

    @Test
    void dansk_betydning_vinder_over_css_naar_de_er_uenige() {
        // CSS' "violet" er et lyst rosalilla; dansk violet er blålilla
        assertNotEquals(Color.web("violet"), Symbolfarver.forSymbol("violet"));
        assertEquals(Color.web("#7b3fbf"), Symbolfarver.forSymbol("violet"));
    }

    // ---- Trin 4: ukendt ----

    @Test
    void almindelige_ord_der_tilfaeldigvis_er_hextal_bliver_ikke_til_farver() {
        // Color.web tager også bart hextal uden havelåge: "abe" ville ellers blive #aabbee,
        // "facade" til #facade og "decade" til #decade. Ét sådant ord er nok til at
        // farvelægge et helt diagram der ikke handler om farver.
        assertNull(Symbolfarver.forSymbol("abe"));
        assertNull(Symbolfarver.forSymbol("facade"));
        assertNull(Symbolfarver.forSymbol("decade"));
        assertNull(Symbolfarver.forSymbol("bad"));
        // ... men CSS-navnene skal stadig komme igennem
        assertNotNull(Symbolfarver.forSymbol("chartreuse"));
    }

    @Test
    void ord_uden_farveindhold_giver_null() {
        assertNull(Symbolfarver.forSymbol("chartreux"));
        assertNull(Symbolfarver.forSymbol("solnedgangsfarvet"));
        assertNull(Symbolfarver.forSymbol(null));
        assertNull(Symbolfarver.forSymbol("   "));
    }

    // ---- Robusthed ----

    @Test
    void store_bogstaver_og_mellemrum_er_lige_meget() {
        Color rød = Symbolfarver.forSymbol("rød");
        assertEquals(rød, Symbolfarver.forSymbol("Rød"));
        assertEquals(rød, Symbolfarver.forSymbol("  RØD  "));
    }

    @Test
    void tilWeb_giver_en_streng_css_kan_laese() {
        assertEquals("#d32f2f", Symbolfarver.tilWeb(Color.web("#d32f2f")));
        assertEquals("#000000", Symbolfarver.tilWeb(Color.BLACK));
        assertEquals("#ffffff", Symbolfarver.tilWeb(Color.WHITE));
    }

    // ---- De to der betyder mest i praksis ----

    @Test
    void hele_den_indbyggede_farvekategori_kan_udledes() {
        for (String farve : symbolerI("Farver")) {
            assertNotNull(Symbolfarver.forSymbol(farve), "Kunne ikke udlede en farve for: " + farve);
        }
    }

    @Test
    void symboler_der_ikke_er_farveord_giver_null() {
        // Cirkeldiagrammet spørger kun for Farver og Chakraer, så det her er ikke sidste værn -
        // men Symbolfarver skal kunne stå alene og være konservativ. Det var netop denne test
        // der afslørede at Color.web læste dyret "abe" som hextallet #aabbee.
        for (String kategori : List.of("Dyr", "Arketyper", "Forløb")) {
            for (String symbol : symbolerI(kategori)) {
                assertNull(Symbolfarver.forSymbol(symbol),
                        kategori + "-symbolet \"" + symbol + "\" blev opfattet som en farve");
            }
        }
    }

    @Test
    void kun_farver_og_chakraer_farvelaegges() {
        assertTrue(Category.harNaturligeFarver("Farver"));
        assertTrue(Category.harNaturligeFarver("Chakraer"));
        assertFalse(Category.harNaturligeFarver("Dyr"));
        assertFalse(Category.harNaturligeFarver("Arketyper"));
        assertFalse(Category.harNaturligeFarver("Personer"));
        assertFalse(Category.harNaturligeFarver("Forløb"));
        assertFalse(Category.harNaturligeFarver(Category.FLAGS_CATEGORY_NAME));
        assertFalse(Category.harNaturligeFarver(null));
    }
}
