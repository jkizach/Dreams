package fixit.dreams;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reglerne for Cirkel-fanens periode. De ser trivielle ud, og det er netop derfor de står her:
 * en måned der flyttes fra den 31. januar rammer den 28. februar og finder aldrig en 31'er
 * igen, og et ugenummer parret med kalenderåret i stedet for det ugebaserede år splitter
 * nytårsugen i to - præcis den fejl har appen haft før, i Stats' weekKey (se StatsWeekKeyTest).
 *
 * Testene slår tre ting fast: at perioden gøres hel FØR der flyttes, at et frit interval
 * flytter sig med sin egen længde uden hverken hul eller overlap, og at etiketten ikke
 * afhænger af hvilket sprog maskinen er sat til.
 */
class PeriodeTest {

    // ---------- Perioden gør sig selv hel ----------

    @Test
    void maaned_snapper_til_foerste_og_sidste_dag() {
        Periode p = Periode.omkring(Periode.Type.MÅNED, LocalDate.of(2026, 9, 17));
        assertEquals(LocalDate.of(2026, 9, 1), p.fra());
        assertEquals(LocalDate.of(2026, 9, 30), p.til());
    }

    @Test
    void maaned_snapper_ud_fra_fra_datoen_ikke_til_datoen() {
        // Ankeret er Fra. Står der 5. sep - 20. nov, er det september man får, ikke november.
        Periode p = new Periode(Periode.Type.MÅNED, LocalDate.of(2026, 9, 5), LocalDate.of(2026, 11, 20));
        assertEquals(LocalDate.of(2026, 9, 1), p.fra());
        assertEquals(LocalDate.of(2026, 9, 30), p.til());
    }

    @Test
    void februar_i_skudaar_slutter_den_29() {
        Periode p = Periode.omkring(Periode.Type.MÅNED, LocalDate.of(2024, 2, 15));
        assertEquals(LocalDate.of(2024, 2, 29), p.til());
    }

    @Test
    void februar_uden_skud_slutter_den_28() {
        Periode p = Periode.omkring(Periode.Type.MÅNED, LocalDate.of(2025, 2, 15));
        assertEquals(LocalDate.of(2025, 2, 28), p.til());
    }

    @Test
    void uge_snapper_til_mandag_og_soendag() {
        // 9. sep 2026 er en onsdag.
        Periode p = Periode.omkring(Periode.Type.UGE, LocalDate.of(2026, 9, 9));
        assertEquals(LocalDate.of(2026, 9, 7), p.fra());
        assertEquals(LocalDate.of(2026, 9, 13), p.til());
    }

    @Test
    void en_soendag_hoerer_til_ugen_der_gik() {
        // ISO, ikke søndag-først: søndag den 13. slutter ugen, den begynder den ikke.
        Periode p = Periode.omkring(Periode.Type.UGE, LocalDate.of(2026, 9, 13));
        assertEquals(LocalDate.of(2026, 9, 7), p.fra());
    }

    @Test
    void en_mandag_bliver_staaende() {
        Periode p = Periode.omkring(Periode.Type.UGE, LocalDate.of(2026, 9, 7));
        assertEquals(LocalDate.of(2026, 9, 7), p.fra());
    }

    @Test
    void aar_snapper_til_1_januar_og_31_december() {
        Periode p = Periode.omkring(Periode.Type.ÅR, LocalDate.of(2026, 9, 17));
        assertEquals(LocalDate.of(2026, 1, 1), p.fra());
        assertEquals(LocalDate.of(2026, 12, 31), p.til());
    }

    @Test
    void interval_lader_datoerne_staa() {
        Periode p = new Periode(Periode.Type.INTERVAL, LocalDate.of(2026, 9, 5), LocalDate.of(2026, 11, 20));
        assertEquals(LocalDate.of(2026, 9, 5), p.fra());
        assertEquals(LocalDate.of(2026, 11, 20), p.til());
    }

    @Test
    void at_goere_en_hel_periode_hel_igen_aendrer_ingenting() {
        // Cirkel-fanen bygger sin Periode op fra datovælgerne HVER gang den skal bruge den.
        // Var det ikke en no-op, ville perioden skride en smule for hver aflæsning.
        LocalDate dato = LocalDate.of(2026, 9, 17);
        for (Periode.Type type : Periode.Type.values()) {
            Periode en = Periode.omkring(type, dato);
            Periode igen = new Periode(type, en.fra(), en.til());
            assertEquals(en, igen, "at gøre " + type + " hel igen må ikke ændre noget");
        }
    }

    // ---------- Der flyttes fra en hel periode ----------

    @Test
    void naeste_maaned_fra_den_31_januar_rammer_hele_februar_og_derefter_hele_marts() {
        // Fælden: 31. jan + 1 måned = 28. feb, og +1 igen = 28. marts - marts' sidste tre dage
        // ville forsvinde for altid. Fordi perioden gøres hel først, flyttes der fra den 1.
        Periode januar = Periode.omkring(Periode.Type.MÅNED, LocalDate.of(2026, 1, 31));
        Periode februar = januar.næste();
        assertEquals(LocalDate.of(2026, 2, 1), februar.fra());
        assertEquals(LocalDate.of(2026, 2, 28), februar.til());

        Periode marts = februar.næste();
        assertEquals(LocalDate.of(2026, 3, 1), marts.fra());
        assertEquals(LocalDate.of(2026, 3, 31), marts.til(), "marts skal have alle 31 dage med");
    }

    @Test
    void forrige_maaned_fra_marts_i_skudaar_rammer_29_februar() {
        Periode marts = Periode.omkring(Periode.Type.MÅNED, LocalDate.of(2024, 3, 10));
        Periode februar = marts.forrige();
        assertEquals(LocalDate.of(2024, 2, 1), februar.fra());
        assertEquals(LocalDate.of(2024, 2, 29), februar.til());
    }

    @Test
    void naeste_uge_er_praecis_syv_dage_frem() {
        Periode uge = Periode.omkring(Periode.Type.UGE, LocalDate.of(2026, 9, 9));
        assertEquals(LocalDate.of(2026, 9, 14), uge.næste().fra());
        assertEquals(LocalDate.of(2026, 9, 20), uge.næste().til());
    }

    @Test
    void uger_springer_ikke_en_dag_over_ved_aarsskiftet() {
        Periode uge53 = Periode.omkring(Periode.Type.UGE, LocalDate.of(2026, 12, 28));
        assertEquals(LocalDate.of(2027, 1, 3), uge53.til());
        assertEquals(LocalDate.of(2027, 1, 4), uge53.næste().fra(), "dagen efter, hverken mere eller mindre");
    }

    @Test
    void naeste_aar_fra_2026_er_hele_2027() {
        Periode p = Periode.omkring(Periode.Type.ÅR, LocalDate.of(2026, 9, 17)).næste();
        assertEquals(LocalDate.of(2027, 1, 1), p.fra());
        assertEquals(LocalDate.of(2027, 12, 31), p.til());
    }

    @Test
    void interval_flytter_sig_med_sin_egen_laengde() {
        // 1.-17. sep er 17 dage, begge ender med.
        Periode p = new Periode(Periode.Type.INTERVAL, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 17));
        Periode næste = p.næste();
        assertEquals(LocalDate.of(2026, 9, 18), næste.fra());
        assertEquals(LocalDate.of(2026, 10, 4), næste.til());
    }

    @Test
    void interval_efterlader_hverken_hul_eller_overlap() {
        // Uden det inklusive +1 i længden ville næste vindue begynde på den 17. igen, og den
        // dags drømme ville blive talt med to gange - hvilket ville se helt rimeligt ud.
        Periode p = new Periode(Periode.Type.INTERVAL, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 17));
        assertEquals(p.til().plusDays(1), p.næste().fra());
        assertEquals(p.fra().minusDays(1), p.forrige().til());
    }

    @Test
    void interval_paa_en_enkelt_dag_flytter_en_dag() {
        LocalDate dag = LocalDate.of(2026, 9, 17);
        Periode p = new Periode(Periode.Type.INTERVAL, dag, dag);
        assertEquals(dag.plusDays(1), p.næste().fra());
        assertEquals(dag.plusDays(1), p.næste().til());
    }

    @Test
    void et_omvendt_interval_flytter_stadig_en_dag() {
        // Til før Fra giver ingen meningsfuld længde. Pilene må ikke stå af - så kunne brugeren
        // ikke se sig ud af det igen.
        Periode p = new Periode(Periode.Type.INTERVAL, LocalDate.of(2026, 9, 17), LocalDate.of(2026, 9, 1));
        assertEquals(LocalDate.of(2026, 9, 18), p.næste().fra());
        assertEquals(LocalDate.of(2026, 9, 16), p.forrige().fra());
    }

    @Test
    void frem_og_tilbage_ender_samme_sted() {
        for (LocalDate dato : new LocalDate[]{
                LocalDate.of(2026, 1, 31), LocalDate.of(2024, 2, 29), LocalDate.of(2026, 12, 28)}) {
            for (Periode.Type type : Periode.Type.values()) {
                Periode p = Periode.omkring(type, dato);
                assertEquals(p, p.næste().forrige(), type + " frem og tilbage fra " + dato);
                assertEquals(p, p.forrige().næste(), type + " tilbage og frem fra " + dato);
            }
        }
    }

    // ---------- Etiketten mellem pilene ----------

    @Test
    void uge_etiketten_bruger_det_ugebaserede_aar() {
        // Mandag den 29. december 2025 er uge 1 i 2026 - ikke uge 1 i 2025.
        Periode p = Periode.omkring(Periode.Type.UGE, LocalDate.of(2025, 12, 29));
        assertEquals("uge 1 · 2026", p.etiket());
    }

    @Test
    void uge_53_findes_naar_aaret_har_53_uger() {
        Periode uge53 = Periode.omkring(Periode.Type.UGE, LocalDate.of(2026, 12, 28));
        assertEquals("uge 53 · 2026", uge53.etiket());
        assertEquals("uge 1 · 2027", uge53.næste().etiket());
    }

    @Test
    void maaned_etiketten_er_dansk_og_med_lille() {
        assertEquals("september 2026", Periode.omkring(Periode.Type.MÅNED, LocalDate.of(2026, 9, 17)).etiket());
    }

    @Test
    void aar_etiketten_er_bare_aarstallet() {
        assertEquals("2026", Periode.omkring(Periode.Type.ÅR, LocalDate.of(2026, 9, 17)).etiket());
    }

    @Test
    void interval_etiketten_viser_antal_dage() {
        Periode p = new Periode(Periode.Type.INTERVAL, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 17));
        assertEquals("17 dage", p.etiket());
    }

    @Test
    void interval_paa_en_dag_er_i_ental() {
        LocalDate dag = LocalDate.of(2026, 9, 17);
        assertEquals("1 dag", new Periode(Periode.Type.INTERVAL, dag, dag).etiket());
    }

    @Test
    void etiketten_afhaenger_ikke_af_maskinens_sprog() {
        // Main sætter da-DK, men en test kører aldrig Main, og surefire sætter ikke sproget.
        // Slog vi månedsnavnet op i standardlocalen, ville denne test bestå her og fejle i CI.
        Locale gammel = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            assertEquals("september 2026",
                    Periode.omkring(Periode.Type.MÅNED, LocalDate.of(2026, 9, 17)).etiket());
        } finally {
            Locale.setDefault(gammel);
        }
    }

    // ---------- Værn ----------

    @Test
    void en_periode_uden_datoer_afvises() {
        LocalDate dag = LocalDate.of(2026, 9, 17);
        assertThrows(NullPointerException.class, () -> new Periode(Periode.Type.MÅNED, null, dag));
        assertThrows(NullPointerException.class, () -> new Periode(Periode.Type.MÅNED, dag, null));
    }
}
