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
        // ...men det korte står valgt fra start: havde Peter Jensen været ment, stod der det.
        assertEquals("Peter", r.tvetydige().get(0).forvalgt().symbol());
    }

    @Test
    void uden_et_praecist_traef_er_intet_forvalgt() {
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find(
                "Peter kom.", register("personer", List.of("Peter Jensen", "Peter Høeg")));
        assertNull(r.tvetydige().get(0).forvalgt());
    }

    @Test
    void samme_korte_navn_i_to_kategorier_er_ikke_forvalgt() {
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find(
                "Peter kom.", register("personer", List.of("Peter", "Peter Jensen"), "dyr", List.of("Peter")));
        assertNull(r.tvetydige().get(0).forvalgt());
    }

    @Test
    void samme_navn_i_to_kategorier_bliver_et_spoergsmaal() {
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find(
                "Alt var rødt.", register("farver", List.of("rød"), "foelelser", List.of("rød")));

        assertTrue(r.sikre().isEmpty(), "appen kan ikke vide hvilken rød der menes: " + r.sikre());
        assertEquals(1, r.tvetydige().size());
        assertEquals(List.of("farver", "foelelser"),
                r.tvetydige().get(0).kandidater().stream().map(Symbolgenkendelse.Fund::kategoriId).toList());
        assertNull(r.tvetydige().get(0).forvalgt());
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

    // ---- Nye navne: personer der står i teksten, men ikke i registret ----

    private List<String> nyeNavne(String tekst, Map<String, Collection<String>> reg) {
        List<String> ud = new java.util.ArrayList<>();
        for (Symbolgenkendelse.Fund f : Symbolgenkendelse.find(tekst, reg, "personer").nyeNavne()) {
            assertEquals("personer", f.kategoriId());
            ud.add(f.symbol());
        }
        return ud;
    }

    @Test
    void et_fuldt_navn_midt_i_saetningen_foreslaas() {
        assertEquals(List.of("Atta Bahati"),
                nyeNavne("Jeg mødte Atta Bahati på stationen.", register("personer", List.of())));
    }

    @Test
    void et_fuldt_navn_foerst_i_saetningen_foreslaas_ogsaa() {
        assertEquals(List.of("Atta Bahati"),
                nyeNavne("Atta Bahati stod i døren.", register("personer", List.of())));
    }

    @Test
    void et_enkelt_ord_foerst_i_saetningen_er_ikke_et_navn() {
        // "Huset" står med stort fordi sætningen begynder - det siger intet om ordet.
        assertEquals(List.of(), nyeNavne("Huset var tomt. Vi gik ind.", register("personer", List.of())));
    }

    @Test
    void indledende_smaaord_skraelles_af_navnet() {
        Map<String, Collection<String>> reg = register("personer", List.of());
        assertEquals(List.of("Atta"), nyeNavne("Pludselig Atta kom ind.", reg));
        assertEquals(List.of("Mette"), nyeNavne("Så Mette mig.", reg));
    }

    @Test
    void ord_der_ogsaa_staar_med_lille_er_ikke_navne() {
        assertEquals(List.of(), nyeNavne("Jeg drømte om en hest. Hest efter hest løb forbi, og så Hest igen.",
                register("personer", List.of())));
    }

    @Test
    void kendte_personer_foreslaas_ikke_igen() {
        Map<String, Collection<String>> reg = register("personer", List.of("Peter", "Atta Bahati"));
        assertEquals(List.of(), nyeNavne("Jeg så Peter og Atta Bahati.", reg));
        // Heller ikke bøjet, og heller ikke når en kendt person står med efternavn på.
        assertEquals(List.of(), nyeNavne("Det var Peters bil.", reg));
        assertEquals(List.of(), nyeNavne("Jeg så Peter Pan.", reg));
    }

    @Test
    void et_symbol_i_en_anden_kategori_er_ikke_et_nyt_navn() {
        assertEquals(List.of(), nyeNavne("Vi var i Paris.", register("personer", List.of(), "steder", List.of("Paris"))));
    }

    @Test
    void et_fuldt_navn_fjerner_spoergsmaalet_om_fornavnet() {
        Map<String, Collection<String>> reg = register("personer", List.of("Anna Hansen", "Anna Berg"));
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find("Jeg talte med Anna Smith.", reg, "personer");
        assertEquals(List.of("Anna Smith"), r.nyeNavne().stream().map(Symbolgenkendelse.Fund::symbol).toList());
        assertTrue(r.tvetydige().isEmpty(), "Anna Smith er hverken Anna Hansen eller Anna Berg");
    }

    @Test
    void et_fornavn_registret_gaetter_paa_foreslaas_ikke_som_nyt() {
        Map<String, Collection<String>> reg = register("personer", List.of("Anna Hansen", "Anna Berg"));
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find("Jeg talte med Anna.", reg, "personer");
        assertTrue(r.nyeNavne().isEmpty());
        assertEquals(1, r.tvetydige().size());
    }

    @Test
    void fornavn_alene_og_genitiv_fylder_ikke_ved_siden_af_det_fulde_navn() {
        assertEquals(List.of("Atta Bahati"), nyeNavne(
                "Jeg mødte Atta Bahati. Bagefter kørte jeg med Atta i Attas bil.",
                register("personer", List.of())));
    }

    @Test
    void komma_og_replikker_bryder_navnet() {
        assertEquals(List.of("Anna", "Bo"), nyeNavne("Jeg så Anna, Bo og en hund.", register("personer", List.of())));
        assertEquals(List.of("Bo"), nyeNavne("Hun sagde: \"Kom her\" til Bo.", register("personer", List.of())));
    }

    @Test
    void raab_med_store_bogstaver_er_ikke_navne() {
        assertEquals(List.of(), nyeNavne("Jeg råbte NEJ og STOP.", register("personer", List.of())));
    }

    @Test
    void navne_med_bindestreg_holdes_samlet() {
        assertEquals(List.of("Anne-Marie"), nyeNavne("Jeg så Anne-Marie.", register("personer", List.of())));
    }

    @Test
    void uden_personkategori_foreslaas_ingen_nye_navne() {
        Map<String, Collection<String>> reg = register("dyr", List.of("hund"));
        assertTrue(Symbolgenkendelse.find("Jeg så Atta Bahati.", reg, "personer").nyeNavne().isEmpty());
        assertTrue(Symbolgenkendelse.find("Jeg så Atta Bahati.", reg).nyeNavne().isEmpty());
    }

    @Test
    void kun_nye_navne_er_ikke_et_tomt_resultat() {
        assertFalse(Symbolgenkendelse.find("Jeg så Atta.", register("personer", List.of()), "personer").erTom());
    }

    @Test
    void genitiv_af_en_kendt_person_er_ikke_et_nyt_navn() {
        Map<String, Collection<String>> reg = register("personer", List.of("Anna", "Ib", "Anna Hage"));
        assertEquals(List.of(), nyeNavne("Vi var i Annas have.", reg));
        assertEquals(List.of(), nyeNavne("Vi kørte i Ibs bil.", reg));
        assertEquals(List.of(), nyeNavne("Fru Hage var der.", reg), "efternavnet på en kendt person");
    }

    @Test
    void parentesen_i_et_symbol_er_brugerens_egen_og_staar_ikke_i_teksten() {
        assertEquals(List.of(), nyeNavne("Jeg så Allan igen.", register("personer", List.of("Allan (skolen)"))));
    }

    @Test
    void forkortelser_og_sammensaetninger_er_ikke_navne() {
        Map<String, Collection<String>> reg = register("personer", List.of());
        assertEquals(List.of(), nyeNavne("Vi boede i Nr. Snede.", reg).stream().filter(n -> n.startsWith("Nr")).toList());
        assertEquals(List.of(), nyeNavne("Jeg havde Ecco-sko og en DNA-strimmel.", reg));
    }

    @Test
    void steder_kendt_paa_endelsen_eller_fra_listen_er_ikke_navne() {
        Map<String, Collection<String>> reg = register("personer", List.of());
        assertEquals(List.of(), nyeNavne("Vi gik ned ad Solbakkevej mod Porthuset.", reg));
        assertEquals(List.of(), nyeNavne("Vi rejste fra Danmark til New York.", reg));
    }

    // ---- Homografer: navneord der også er et udsagnsord ----

    @Test
    void faar_som_udsagnsord_er_ikke_dyret() {
        Map<String, Collection<String>> reg = register("dyr", List.of("får"));
        assertTrue(sikreSymboler(Symbolgenkendelse.find("Jeg får en gave af min mor.", reg)).isEmpty());
        assertTrue(sikreSymboler(Symbolgenkendelse.find("Hun får lov til at gå.", reg)).isEmpty());
    }

    @Test
    void faar_med_kendeord_talord_eller_tillaegsord_foran_er_dyret() {
        Map<String, Collection<String>> reg = register("dyr", List.of("får"));
        assertEquals(Set.of("får"), sikreSymboler(Symbolgenkendelse.find("Der stod et får på marken.", reg)));
        assertEquals(Set.of("får"), sikreSymboler(Symbolgenkendelse.find("Der stod et stort får.", reg)));
        assertEquals(Set.of("får"), sikreSymboler(Symbolgenkendelse.find("Jeg så to får.", reg)));
    }

    @Test
    void entydige_former_findes_altid() {
        Map<String, Collection<String>> reg = register("dyr", List.of("får"), "ting", List.of("tog", "ske"));
        assertEquals(Set.of("får"), sikreSymboler(Symbolgenkendelse.find("Fårene græssede.", reg)));
        assertEquals(Set.of("får"), sikreSymboler(Symbolgenkendelse.find("Jeg klappede fåret.", reg)));
        assertEquals(Set.of("tog"), sikreSymboler(Symbolgenkendelse.find("Jeg tog toget hjem.", reg)));
        assertEquals(Set.of("ske"), sikreSymboler(Symbolgenkendelse.find("Han gav mig skeen.", reg)));
    }

    @Test
    void tog_og_ske_som_udsagnsord_findes_ikke() {
        Map<String, Collection<String>> reg = register("ting", List.of("tog", "ske"));
        assertTrue(sikreSymboler(Symbolgenkendelse.find("Jeg tog hjem, og det sker tit.", reg)).isEmpty());
        assertEquals(Set.of("tog"), sikreSymboler(Symbolgenkendelse.find("Jeg tog et tog.", reg)));
    }

    // ---- Ikke-navne: et nej huskes ----

    @Test
    void et_afvist_navn_foreslaas_ikke_igen() {
        Map<String, Collection<String>> reg = register("personer", List.of());
        Set<String> ikkeNavne = Set.of(Symbolgenkendelse.ikkeNavnNoegle("Lovns"),
                Symbolgenkendelse.ikkeNavnNoegle("Ringenes  Herre"));
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find(
                "Vi var i Lovns og så Ringenes Herre med Atta.", reg, "personer", ikkeNavne);
        assertEquals(List.of("Atta"), r.nyeNavne().stream().map(Symbolgenkendelse.Fund::symbol).toList());
    }

    @Test
    void det_og_de_lige_foran_er_grundled_ikke_kendeord() {
        Map<String, Collection<String>> reg = register("dyr", List.of("får"), "ting", List.of("tog"));
        assertTrue(sikreSymboler(Symbolgenkendelse.find("Det tog lang tid, og de får lov.", reg)).isEmpty());
        assertTrue(sikreSymboler(Symbolgenkendelse.find("Alle får en gave.", reg)).isEmpty());
        assertEquals(Set.of("får"), sikreSymboler(Symbolgenkendelse.find("Jeg så de hvide får.", reg)));
        assertEquals(Set.of("tog"), sikreSymboler(Symbolgenkendelse.find("Vi tog det store tog.", reg)));
    }

    @Test
    void et_navneord_imellem_goer_ikke_udsagnsordet_til_et_dyr() {
        Map<String, Collection<String>> reg = register("dyr", List.of("får"));
        assertTrue(sikreSymboler(Symbolgenkendelse.find("En mand får en gave.", reg)).isEmpty());
    }

    @Test
    void et_afvist_navn_brugeren_siden_selv_har_oprettet_findes_som_kendt_person() {
        // Listen over ikke-navne gælder kun forslag til NYE navne. Står navnet i registret,
        // er det et almindeligt symbol og findes som alle andre - afkrydset fra start.
        Map<String, Collection<String>> reg = register("personer", List.of("Gerd"));
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find(
                "Jeg talte med Gerd.", reg, "personer", Set.of(Symbolgenkendelse.ikkeNavnNoegle("Gerd")));
        assertEquals(Set.of("Gerd"), sikreSymboler(r));
        assertTrue(r.nyeNavne().isEmpty());
    }

    // ---- Chakraer: tre af navnene betyder næsten altid noget andet ----

    @Test
    void hals_rod_og_krone_findes_ikke_som_chakraer() {
        Map<String, Collection<String>> reg = register("chakraer", List.of("hals", "rod", "krone", "hjerte"));
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find(
                "Jeg havde ondt i halsen, roder i en skuffe ved roden af et træ og finder ti kroner. En krone.", reg);
        assertTrue(r.erTom());
    }

    @Test
    void hjerte_findes_stadig_som_chakra() {
        Map<String, Collection<String>> reg = register("chakraer", List.of("hals", "hjerte"));
        assertEquals(Set.of("hjerte"), sikreSymboler(Symbolgenkendelse.find("Hjertet bankede i halsen.", reg)));
    }

    // ---- Forløb er fortolkning, ikke ord ----

    @Test
    void forloeb_soeges_ikke_i_teksten() {
        Map<String, Collection<String>> reg = register("forloeb", List.of("møde", "det ukendte"));
        assertTrue(Symbolgenkendelse.find("Vi har et møde om det ukendte.", reg).erTom());
    }

    // ---- Bindestreg ----

    @Test
    void et_navn_med_bindestreg_findes() {
        Map<String, Collection<String>> reg = register("personer", List.of("Gull-Maja"));
        assertEquals(Set.of("Gull-Maja"), sikreSymboler(Symbolgenkendelse.find("Så kommer Gull-Maja ind.", reg)));
    }

    @Test
    void bindestregsnavnet_skygges_ikke_af_varianten_med_parentes() {
        // Som "Peter Jensen": står hele navnet der, spørges der ikke om fornavnet.
        Map<String, Collection<String>> reg = register("personer", List.of("Anne-Mette", "Anne-Mette (klient)"));
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find("Jeg taler med Anne-Mette.", reg);
        assertEquals(Set.of("Anne-Mette"), sikreSymboler(r));
        assertTrue(r.tvetydige().isEmpty());
    }

    // ---- Fordoblet slutkonsonant ----

    @Test
    void fordoblet_slutkonsonant_findes() {
        Map<String, Collection<String>> reg = register("ting", List.of("grøn", "kat", "edderkop"));
        assertEquals(Set.of("grøn", "kat", "edderkop"),
                sikreSymboler(Symbolgenkendelse.find("De grønne katte jagede edderkopperne.", reg)));
    }

    @Test
    void lamme_er_ikke_flertal_af_lam() {
        Map<String, Collection<String>> reg = register("dyr", List.of("lam"));
        assertTrue(Symbolgenkendelse.find("Mine ben er lamme.", reg).erTom());
    }

    // ---- Farvernes egne endelser ----

    @Test
    void farver_findes_med_lig_og_farvet() {
        Map<String, Collection<String>> reg = register("farver", List.of("rød", "sølv", "gul", "brun"));
        assertEquals(Set.of("rød", "sølv", "gul", "brun"),
                sikreSymboler(Symbolgenkendelse.find("Et rødligt lys, en sølvfarvet bil, gulligt papir og en brunhåret mand.", reg)));
    }

    @Test
    void farveendelserne_gaelder_kun_i_farver() {
        Map<String, Collection<String>> reg = register("personer", List.of("Far"));
        assertTrue(Symbolgenkendelse.find("Det er farligt.", reg).erTom());
    }

    // ---- Efternavn alene ----

    @Test
    void efternavnet_alene_er_et_spoergsmaal_uden_forvalg() {
        Map<String, Collection<String>> reg = register("personer", List.of("Jacob Piet"));
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find("Så kom Piet ind, og vi spillede.", reg, "personer");
        assertTrue(r.sikre().isEmpty());
        assertEquals(1, r.tvetydige().size());
        assertEquals("Jacob Piet", r.tvetydige().get(0).kandidater().get(0).symbol());
        assertNull(r.tvetydige().get(0).forvalgt());
        assertTrue(r.nyeNavne().isEmpty());
    }

    @Test
    void et_fornavn_er_ikke_et_efternavn() {
        // "John" er Elton Johns efternavn, men også et fornavn i registret.
        Map<String, Collection<String>> reg = register("personer", List.of("Elton John", "John Katzeff", "Anne-Mette"));
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find("Så kom Mette ind.", reg, "personer");
        assertTrue(r.tvetydige().isEmpty());
        r = Symbolgenkendelse.find("Så kom John ind.", reg, "personer");
        assertEquals(List.of("John Katzeff"), r.tvetydige().get(0).kandidater().stream().map(Symbolgenkendelse.Fund::symbol).toList());
    }

    @Test
    void efternavnet_i_genitiv_er_ogsaa_et_spoergsmaal() {
        Map<String, Collection<String>> reg = register("personer", List.of("Jacob Piet"));
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find("Huset er Piets.", reg, "personer");
        assertEquals(1, r.tvetydige().size());
    }

    @Test
    void efternavnet_skrevet_med_lille_eller_i_hele_navnet_spoerges_der_ikke_om() {
        Map<String, Collection<String>> reg = register("personer", List.of("Rikke Sand"));
        assertTrue(Symbolgenkendelse.find("Der var Sand overalt og sand i skoene.", reg, "personer").tvetydige().isEmpty());
        Symbolgenkendelse.Resultat r = Symbolgenkendelse.find("Jeg mødte Rikke Sand.", reg, "personer");
        assertEquals(Set.of("Rikke Sand"), sikreSymboler(r));
        assertTrue(r.tvetydige().isEmpty());
    }

    @Test
    void hals_i_en_anden_kategori_findes_stadig() {
        Map<String, Collection<String>> reg = register("krop", List.of("hals"));
        assertEquals(Set.of("hals"), sikreSymboler(Symbolgenkendelse.find("Jeg havde ondt i halsen.", reg)));
    }
}
