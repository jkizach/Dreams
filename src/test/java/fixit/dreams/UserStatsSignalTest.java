package fixit.dreams;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Signalet der fortæller Analyse-fanen at statistikken skal regnes om.
 *
 * Det var før et boolsk flag, og fejlen var at JavaFX kun fyrer listeners når værdien
 * faktisk ændrer sig: skete der noget der krævede genberegning før Analyse-fanen var
 * åbnet første gang, stod flaget allerede på true uden at nogen havde nulstillet det,
 * og hver eneste senere besked var et no-op. Tal-fanen frøs så resten af sessionen.
 * Testene her holder fast i at HVER besked når frem, uanset hvad der gik forud.
 */
class UserStatsSignalTest {

    private User user;

    @BeforeEach
    void setUp() {
        User.resetForTests();
        user = User.getInstance();
    }

    private Dream enDrøm() {
        DreamData data = new DreamData();
        data.categories = new ArrayList<>();
        data.indhold = "test";
        data.dagrest = "";
        data.tolkning = "";
        data.dato = LocalDate.of(2026, 1, 1);
        return new Dream(data);
    }

    private AtomicInteger tælBeskeder() {
        AtomicInteger beskeder = new AtomicInteger();
        user.statsGenberegningProperty().addListener((obs, gammel, ny) -> beskeder.incrementAndGet());
        return beskeder;
    }

    @Test
    void hver_besked_naar_frem_ogsaa_flere_i_traek() {
        AtomicInteger beskeder = tælBeskeder();

        user.genberegnStatsPlease();
        user.genberegnStatsPlease();
        user.genberegnStatsPlease();

        assertEquals(3, beskeder.get());
    }

    @Test
    void beskeder_fra_foer_lytteren_kom_til_blokerer_ikke_senere_beskeder() {
        // Det er præcis den gamle fejl: noget skete (fx en sync eller en ny drøm) inden
        // Analyse-fanen blev åbnet, og bagefter kom der aldrig flere beskeder igennem.
        user.genberegnStatsPlease();
        user.genberegnStatsPlease();

        AtomicInteger beskeder = tælBeskeder();

        user.genberegnStatsPlease();

        assertEquals(1, beskeder.get());
    }

    @Test
    void redigering_af_en_droem_giver_besked_hver_gang() {
        Dream d = enDrøm();
        user.addDream(d);

        AtomicInteger beskeder = tælBeskeder();

        // Samme drøm gemt to gange i træk - fx to hak sat i checkbokse efter hinanden
        user.addDream(d);
        user.addDream(d);

        assertEquals(2, beskeder.get());
    }

    @Test
    void sletning_giver_ogsaa_besked() {
        Dream d = enDrøm();
        user.addDream(d);

        AtomicInteger beskeder = tælBeskeder();

        user.deleteDream(d.getId());

        assertEquals(1, beskeder.get());
    }
}
