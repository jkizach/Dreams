package fixit.dreams;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// De to regler om startdatoen. Reglerne er trivielle at læse og var stadig i stand til at æde
// en indstilling ved hver eneste opstart i 2.0 - derfor står de her, skrevet ud som påstande.
class StartdatoTest {

    private static final LocalDate FOERSTE_DROEM = LocalDate.of(2019, 3, 4);
    private static final LocalDate VALGT = LocalDate.of(2026, 1, 1);

    /* ---- valgtAfBruger ---- */

    @Test
    void en_dato_med_flaget_sat_er_brugerens_valg() {
        assertTrue(Startdato.valgtAfBruger(VALGT, Boolean.TRUE));
    }

    @Test
    void en_dato_med_flaget_nulstillet_er_udledt() {
        assertFalse(Startdato.valgtAfBruger(VALGT, Boolean.FALSE));
    }

    // En user.json eller et sky-dokument fra før 2.1 har ingen flag. Der antages et valg -
    // hellere lade en udledt dato stå end at flytte en brugeren selv har sat.
    @Test
    void en_dato_uden_flag_regnes_som_valgt() {
        assertTrue(Startdato.valgtAfBruger(VALGT, null));
    }

    @Test
    void ingen_dato_er_aldrig_et_valg() {
        assertFalse(Startdato.valgtAfBruger(null, Boolean.TRUE));
        assertFalse(Startdato.valgtAfBruger(null, null));
    }

    /* ---- filterFra ---- */

    // Kernen i 2.1-rettelsen: hentede drømme åbner visningen, men startdatoen bliver stående.
    @Test
    void aeldre_hentede_droemme_aabner_visningen() {
        assertEquals(FOERSTE_DROEM, Startdato.filterFra(VALGT, FOERSTE_DROEM));
    }

    @Test
    void hentede_droemme_inden_for_filtret_flytter_ikke_noget() {
        assertEquals(VALGT, Startdato.filterFra(VALGT, VALGT.plusDays(30)));
    }

    @Test
    void ingen_hentede_droemme_lader_startdatoen_staa() {
        assertEquals(VALGT, Startdato.filterFra(VALGT, null));
    }

    @Test
    void en_hentet_droem_paa_selve_startdatoen_flytter_ikke_noget() {
        assertEquals(VALGT, Startdato.filterFra(VALGT, VALGT));
    }

    @Test
    void uden_startdato_bruges_den_aeldste_hentede() {
        assertEquals(FOERSTE_DROEM, Startdato.filterFra(null, FOERSTE_DROEM));
    }

    @Test
    void uden_noget_som_helst_bliver_det_i_dag() {
        assertEquals(LocalDate.now(), Startdato.filterFra(null, null));
    }
}
