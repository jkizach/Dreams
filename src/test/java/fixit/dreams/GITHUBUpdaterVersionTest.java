package fixit.dreams;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Den vigtigste test i suiten er nok den første herunder. Appen må ALDRIG tilbyde en ældre
// version end den brugeren kører: installeren for 1.5 oven på 2.0 efterlader en tom
// dreams.json, fordi den gamle udgave ikke kender det nye filformat. Sammenligningen er
// derfor ikke kosmetik, den er det eneste der står mellem en fejl i et release-tag og en
// brugers drømme.
class GITHUBUpdaterVersionTest {

    @Test
    @DisplayName("Et ældre release tilbydes ikke - det ville være en nedgradering")
    void aeldre_release_tilbydes_ikke() {
        assertFalse(GITHUBUpdater.erNyereEnd("v1.5", "v2.0"));
        assertFalse(GITHUBUpdater.erNyereEnd("v1.9", "v2.0"));
        assertFalse(GITHUBUpdater.erNyereEnd("v2.0", "v2.1"));
    }

    @Test
    @DisplayName("Et nyere release tilbydes")
    void nyere_release_tilbydes() {
        assertTrue(GITHUBUpdater.erNyereEnd("v2.1", "v2.0"));
        assertTrue(GITHUBUpdater.erNyereEnd("v3.0", "v2.9"));
        assertTrue(GITHUBUpdater.erNyereEnd("v2.0.1", "v2.0"));
    }

    @Test
    @DisplayName("Samme version tilbydes ikke")
    void samme_version_tilbydes_ikke() {
        assertFalse(GITHUBUpdater.erNyereEnd("v2.0", "v2.0"));
    }

    // 10 > 9, selvom "10" står før "9" i alfabetet. Det er den fælde en ren strengsammen-
    // ligning falder i, og den rammer os første gang vi når v2.10.
    @Test
    @DisplayName("Tal sammenlignes som tal, ikke som tekst")
    void tocifrede_led_sammenlignes_som_tal() {
        assertTrue(GITHUBUpdater.erNyereEnd("v2.10", "v2.9"));
        assertFalse(GITHUBUpdater.erNyereEnd("v2.9", "v2.10"));
        assertTrue(GITHUBUpdater.erNyereEnd("v10.0", "v9.9"));
    }

    // Tags har hidtil haft to led (v1.5), men et rettelses-release ville få tre. De to former
    // skal kunne sammenlignes med hinanden, og "v2" skal betyde det samme som "v2.0.0".
    @Test
    @DisplayName("Forskelligt antal led gør ingen forskel")
    void forskelligt_antal_led() {
        assertFalse(GITHUBUpdater.erNyereEnd("v2.0.0", "v2.0"));
        assertFalse(GITHUBUpdater.erNyereEnd("v2", "v2.0.0"));
        assertTrue(GITHUBUpdater.erNyereEnd("v2.0.0.1", "v2.0.0"));
    }

    @Test
    @DisplayName("Et v foran er valgfrit, og mellemrum omkring tagget er ligegyldige")
    void praefiks_og_mellemrum() {
        assertTrue(GITHUBUpdater.erNyereEnd("2.1", "v2.0"));
        assertTrue(GITHUBUpdater.erNyereEnd("V2.1", "v2.0"));
        assertTrue(GITHUBUpdater.erNyereEnd("  v2.1  ", "v2.0"));
    }

    @Test
    @DisplayName("Et prøve-release læses som sin version, ikke som volapyk")
    void suffiks_pilles_af() {
        assertTrue(GITHUBUpdater.erNyereEnd("v2.1-rc1", "v2.0"));
        assertFalse(GITHUBUpdater.erNyereEnd("v2.0-rc1", "v2.0")); // samme version, ikke nyere
    }

    // Her er tavshed det rigtige svar. Forstår vi ikke tagget, ved vi ikke om det peger op
    // eller ned - og et gæt der peger ned koster brugeren alle sine drømme.
    @Test
    @DisplayName("Et tag vi ikke forstår, tilbydes ikke")
    void ulaeseligt_tag_tilbydes_ikke() {
        assertFalse(GITHUBUpdater.erNyereEnd("nyeste", "v2.0"));
        assertFalse(GITHUBUpdater.erNyereEnd("v2.x", "v2.0"));
        assertFalse(GITHUBUpdater.erNyereEnd("release-2.1", "v2.0"));
        assertFalse(GITHUBUpdater.erNyereEnd("v2.", "v2.0"));
        assertFalse(GITHUBUpdater.erNyereEnd("v", "v2.0"));
        assertFalse(GITHUBUpdater.erNyereEnd("", "v2.0"));
        assertFalse(GITHUBUpdater.erNyereEnd(null, "v2.0"));
        assertFalse(GITHUBUpdater.erNyereEnd("v2.1", "ukendt"));
    }
}
