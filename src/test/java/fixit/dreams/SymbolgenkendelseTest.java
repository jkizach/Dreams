package fixit.dreams;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Funktionen skal kunne to ting: finde de symboler der faktisk står i teksten, også når de er
 * bøjet - og holde mund om alt den ikke er sikker på, frem for at gætte. Testene er skrevet
 * omkring netop de to krav, og omkring de danske faldgruber der ligger imellem dem.
 */
class SymbolgenkendelseTest {

    // Bygger kortet som HovedmenuController gør det: kategori-id til kategoriens symboler.
    private Map<String, Collection<String>> register(Object... parvis) {
        LinkedHashMap<String, Collection<String>> kort = new LinkedHashMap<>();
        for (int i = 0; i < parvis.length; i += 2) {
            @SuppressWarnings("unchecked")
            Collection<String> symboler = new TreeSet<>((Collection<String>) parvis[i + 1]);
            kort.put((String) parvis[i], symboler);
        }
        return kort;
    }

    private Set<String> sikreSymboler(Symbolgenkendelse.Resultat r) {
        Set<String> ud = new TreeSet<>();
        for (Symbolgenkendelse.Fund f : r.sikre()) {
            ud.add(f.symbol());
        }
        return ud;
    }

    // ---- Bøjning: teksten skriver sjældent symbolet i grundform ----

    @Test
    void et_symbol_der_staar_uboejet_findes() {
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find(
                "Jeg så en hund på vejen.", register("dyr", List.of("hund", "kat")));
        assertEquals(Set.of("hund"), sikreSymboler(r));
    }

    @Test
    void bestemt_form_findes_ogsaa() {
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find(
                "Hunden løb forbi huset.", register("dyr", List.of("hund"), "steder", List.of("hus")));
        assertEquals(Set.of("hund", "hus"), sikreSymboler(r));
    }

    @Test
    void flertal_i_begge_danske_boejningsraekker_findes() {
        // -e-rækken: hund, hunden, hunde, hundene. -r-rækken: flaske, flasken, flasker, flaskerne.
        Map<String, Collection<String>> reg = register("ting", List.of("hund", "flaske"));
        assertEquals(Set.of("hund"), sikreSymboler(Symbolgenkendelse.find("to hunde", reg)));
        assertEquals(Set.of("hund"), sikreSymboler(Symbolgenkendelse.find("alle hundene", reg)));
        assertEquals(Set.of("flaske"), sikreSymboler(Symbolgenkendelse.find("tre flasker", reg)));
        assertEquals(Set.of("flaske"), sikreSymboler(Symbolgenkendelse.find("flaskerne stod der", reg)));
    }

    @Test
    void samme_symbol_fundet_via_to_ord_staar_kun_een_gang() {
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find(
                "En hund. Hunden gøede.", register("dyr", List.of("hund")));
        assertEquals(1, r.sikre().size(), "hund må ikke stå to gange: " + r.sikre());
    }

    // ---- Støjgrænsen ----

    @Test
    void et_symbol_findes_ikke_i_et_ord_der_bare_begynder_ens() {
        // "farlig" er ikke far + en bøjningsendelse, og "-lig" står ikke på listen.
        Map<String, Collection<String>> reg = register("personer", List.of("Far"));
        assertTrue(sikreSymboler(Symbolgenkendelse.find("Det var farligt", reg)).isEmpty());
        assertEquals(Set.of("Far"), sikreSymboler(Symbolgenkendelse.find("Min far kom", reg)));
    }

    @Test
    void korte_symboler_kan_give_falske_traef_og_det_er_en_bevidst_pris() {
        // "farer" ER far + endelsen -er, så symbolet Far bliver foreslået her. Det er forkert,
        // og det er accepteret: alternativet er at kræve fire tegn for bøjning, og så mister
        // hus, bil, sol, kat og ild alle deres bøjede former - langt dyrere end et forslag der
        // koster ét klik at fjerne i dialogen. Testen står her for at fangsten er bevidst og
        // ikke en overraskelse næste gang nogen læser BOEJNINGER.
        Map<String, Collection<String>> reg = register("personer", List.of("Far"));
        assertEquals(Set.of("Far"), sikreSymboler(Symbolgenkendelse.find("Jeg farer rundt", reg)));
    }

    @Test
    void de_almindelige_treboghstavsnavneord_beholder_deres_boejninger() {
        // Det er dem prisen ovenfor er betalt for.
        Map<String, Collection<String>> reg = register("ting", List.of("hus", "bil", "sol", "kat", "ild"));
        assertEquals(Set.of("hus"), sikreSymboler(Symbolgenkendelse.find("inde i huset", reg)));
        assertEquals(Set.of("bil"), sikreSymboler(Symbolgenkendelse.find("to biler", reg)));
        assertEquals(Set.of("sol"), sikreSymboler(Symbolgenkendelse.find("solen stod op", reg)));
        assertEquals(Set.of("kat"), sikreSymboler(Symbolgenkendelse.find("fire katte", reg)));
        assertEquals(Set.of("ild"), sikreSymboler(Symbolgenkendelse.find("ilden brændte", reg)));
    }

    @Test
    void et_symbol_inde_i_et_laengere_ord_findes_ikke() {
        // Delstrengsmatch ville finde "kat" i "katastrofe" - ordgrænserne skal holde.
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find(
                "Det var en katastrofe", register("dyr", List.of("kat")));
        assertTrue(r.sikre().isEmpty(), "kat må ikke findes i katastrofe: " + r.sikre());
    }

    @Test
    void aeoeaa_bevares_saa_far_og_faar_ikke_bliver_det_samme() {
        // Kategoriid.tilSlug folder ø til oe og å til aa. Gjorde vi det her, ville en drøm om
        // en far få markeret et får.
        Map<String, Collection<String>> reg = register("dyr", List.of("får"), "personer", List.of("Far"));
        assertEquals(Set.of("Far"), sikreSymboler(Symbolgenkendelse.find("Min far sov", reg)));
        assertEquals(Set.of("får"), sikreSymboler(Symbolgenkendelse.find("Et får stod der", reg)));
    }

    @Test
    void store_og_smaa_bogstaver_er_lige_gyldige() {
        // Personer skrives med stort i registret, alt andet med småt - se UserService.addNytSymbol.
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find(
                "peter og RØD", register("personer", List.of("Peter"), "farver", List.of("rød")));
        assertEquals(Set.of("Peter", "rød"), sikreSymboler(r));
    }

    // ---- Tvetydighed: de tilfælde hvor appen ikke må gætte ----

    @Test
    void et_fornavn_med_flere_fulde_navne_bliver_et_spoergsmaal() {
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find(
                "Peter kom hen til mig.",
                register("personer", List.of("Peter Jensen", "Peter Høeg", "Peter Peter")));

        assertTrue(r.sikre().isEmpty(), "intet må sættes af sig selv: " + r.sikre());
        assertEquals(1, r.tvetydige().size());
        Symbolgenkendelse.Tvetydighed tvivl = r.tvetydige().get(0);
        assertEquals("peter", tvivl.ord());
        assertEquals(List.of("Peter Høeg", "Peter Jensen", "Peter Peter"),
                tvivl.kandidater().stream().map(Symbolgenkendelse.Fund::symbol).toList());
    }

    @Test
    void staar_det_fulde_navn_i_teksten_spoerges_der_ikke() {
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find(
                "Peter Høeg kom hen til mig.",
                register("personer", List.of("Peter Jensen", "Peter Høeg", "Peter Peter")));

        assertEquals(Set.of("Peter Høeg"), sikreSymboler(r));
        assertTrue(r.tvetydige().isEmpty(), "det fulde navn står jo der: " + r.tvetydige());
    }

    @Test
    void et_kort_navn_der_ogsaa_er_starten_paa_et_langt_bliver_et_spoergsmaal() {
        // Registret rummer både "Peter" og "Peter Jensen". Teksten siger kun "Peter", og så
        // kan appen ikke vide om den korte eller den lange er ment.
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find(
                "Peter kom.", register("personer", List.of("Peter", "Peter Jensen")));

        assertTrue(r.sikre().isEmpty());
        assertEquals(1, r.tvetydige().size());
        assertEquals(List.of("Peter", "Peter Jensen"),
                r.tvetydige().get(0).kandidater().stream().map(Symbolgenkendelse.Fund::symbol).toList());
    }

    @Test
    void samme_navn_i_to_kategorier_bliver_et_spoergsmaal() {
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find(
                "Alt var rødt.", register("farver", List.of("rød"), "foelelser", List.of("rød")));

        assertTrue(r.sikre().isEmpty(), "appen kan ikke vide hvilken rød der menes: " + r.sikre());
        assertEquals(1, r.tvetydige().size());
        assertEquals(List.of("farver", "foelelser"),
                r.tvetydige().get(0).kandidater().stream().map(Symbolgenkendelse.Fund::kategoriId).toList());
    }

    @Test
    void kandidaterne_baerer_deres_egen_kategori_med() {
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find(
                "En hund", register("dyr", List.of("hund")));
        assertEquals("dyr", r.sikre().get(0).kategoriId());
        assertEquals("hund", r.sikre().get(0).symbol());
        assertEquals("hund", r.sikre().get(0).fundetSom());
    }

    // ---- Tomme og skæve inddata ----

    @Test
    void tom_tekst_og_tomt_register_giver_tomt_resultat() {
        assertTrue(Symbolgenkendelse.find("", register("dyr", List.of("hund"))).erTom());
        assertTrue(Symbolgenkendelse.find("   ", register("dyr", List.of("hund"))).erTom());
        assertTrue(Symbolgenkendelse.find(null, register("dyr", List.of("hund"))).erTom());
        assertTrue(Symbolgenkendelse.find("En hund", register()).erTom());
        assertTrue(Symbolgenkendelse.find("En hund", null).erTom());
    }

    @Test
    void en_tekst_uden_traef_giver_tomt_resultat() {
        assertTrue(Symbolgenkendelse.find("Jeg fløj over et landskab",
                register("dyr", List.of("hund", "kat"))).erTom());
    }

    @Test
    void tegnsaetning_klaeber_ikke_til_ordene() {
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find(
                "En hund, en kat - og så en hest!",
                register("dyr", List.of("hund", "kat", "hest")));
        assertEquals(Set.of("hund", "kat", "hest"), sikreSymboler(r));
    }

    @Test
    void to_kald_paa_samme_data_giver_samme_svar_i_samme_raekkefoelge() {
        Map<String, Collection<String>> reg =
                register("dyr", List.of("hund", "kat"), "farver", List.of("rød", "blå"));
        String tekst = "Den blå hund og den røde kat";
        assertEquals(Symbolgenkendelse.find(tekst, reg).sikre(),
                Symbolgenkendelse.find(tekst, reg).sikre());
    }
}
