package fixit.dreams.sync;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MetaMergeTest {

    private static final Instant GAMMEL = Instant.parse("2026-01-01T12:00:00Z");
    private static final Instant NY = Instant.parse("2026-06-01T12:00:00Z");

    @Test
    void nyere_sky_vinder_over_aeldre_lokal() {
        assertTrue(MetaMerge.cloudWins(GAMMEL, NY, true));
        assertFalse(MetaMerge.shouldPush(GAMMEL, NY, true));
    }

    @Test
    void nyere_lokal_vinder_over_aeldre_sky() {
        assertFalse(MetaMerge.cloudWins(NY, GAMMEL, true));
        assertTrue(MetaMerge.shouldPush(NY, GAMMEL, true));
    }

    @Test
    void ens_tidsstempler_giver_hverken_hent_eller_send() {
        assertFalse(MetaMerge.cloudWins(NY, NY, true));
        assertFalse(MetaMerge.shouldPush(NY, NY, true));
    }

    @Test
    void tomt_sky_dokument_hentes_aldrig() {
        assertFalse(MetaMerge.cloudWins(GAMMEL, null, false));
        assertFalse(MetaMerge.cloudWins(null, null, false));
    }

    @Test
    void findes_dokumentet_slet_ikke_i_skyen_sendes_vores_udgave() {
        assertTrue(MetaMerge.shouldPush(NY, null, false));
        assertTrue(MetaMerge.shouldPush(null, null, false), "også urørte standarddata - de kan intet overskrive");
    }

    // Kernen i "aldrig redigeret her": en frisk installations standardkategorier skal tabe til
    // skyens rigtige udgave, og må aldrig sende sig selv oven i den.
    @Test
    void urorte_lokale_data_taber_til_en_rigtig_sky_udgave() {
        assertTrue(MetaMerge.cloudWins(null, GAMMEL, true));
        assertFalse(MetaMerge.shouldPush(null, GAMMEL, true));
    }

    @Test
    void sky_uden_tidsstempel_hentes_ikke_men_overskrives_af_vores_egen() {
        assertFalse(MetaMerge.cloudWins(GAMMEL, null, true));
        assertTrue(MetaMerge.shouldPush(GAMMEL, null, true));
    }

    // Begge sider urørte: der er intet at gøre, og slet ikke noget at overskrive.
    @Test
    void urorte_paa_begge_sider_giver_ingenting() {
        assertFalse(MetaMerge.cloudWins(null, null, true));
        assertFalse(MetaMerge.shouldPush(null, null, true));
    }
}
