package fixit.dreams;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regressionstest for ISO-ugebaseret år/uge-nøglen i Stats.java (rettet 2026-08-24).
 * Den gamle nøgle ("" + date.getYear() + weekOfYear()) blandede kalenderår med
 * ISO-uge og splittede drømme fra samme ISO-uge i forskellige buckets omkring
 * årsskiftet. Den nye nøgle bruger WeekFields.ISO.weekBasedYear()+weekOfWeekBasedYear().
 */
class StatsWeekKeyTest {

    @BeforeEach
    void setUp() {
        User.resetForTests();
    }

    private Dream lucidDreamOn(LocalDate dato) {
        DreamData data = new DreamData();
        data.categories = new ArrayList<>();
        data.categories.add(Category.buildFlagsCategoryDTO(true, false, false, false, false, false, false, false));
        data.indhold = "test";
        data.dagrest = "";
        data.tolkning = "";
        data.dato = dato;
        return new Dream(data);
    }

    @Test
    void dromme_omkring_arsskiftet_i_samme_iso_uge_tælles_sammen() {
        User user = User.getInstance();
        // 30. dec 2024 og 2. jan 2025 ligger begge i ISO-ugebaseret-år 2025, uge 1.
        user.addDream(lucidDreamOn(LocalDate.of(2024, 12, 30)));
        user.addDream(lucidDreamOn(LocalDate.of(2025, 1, 2)));

        Stats stats = new Stats();

        int countViaDecDato = stats.getBoolStatsPerUge(stats.getFlagStats("Lucid"),LocalDate.of(2024, 12, 30));
        int countViaJanDato = stats.getBoolStatsPerUge(stats.getFlagStats("Lucid"),LocalDate.of(2025, 1, 2));

        assertEquals(2, countViaDecDato, "Begge drømme skal tælles i samme uge-bucket");
        assertEquals(2, countViaJanDato, "Begge drømme skal tælles i samme uge-bucket");
    }

    @Test
    void dromme_i_en_klart_anden_uge_taeller_ikke_med_i_arsskifte_bucket() {
        User user = User.getInstance();
        user.addDream(lucidDreamOn(LocalDate.of(2024, 12, 30))); // uge-baseret-år 2025, uge 1
        user.addDream(lucidDreamOn(LocalDate.of(2024, 12, 20))); // uge-baseret-år 2024, uge 51

        Stats stats = new Stats();

        int nytårsBucket = stats.getBoolStatsPerUge(stats.getFlagStats("Lucid"),LocalDate.of(2024, 12, 30));
        int decemberBucket = stats.getBoolStatsPerUge(stats.getFlagStats("Lucid"),LocalDate.of(2024, 12, 20));

        assertEquals(1, nytårsBucket, "Kun én drøm hører til i årsskifte-ugen");
        assertEquals(1, decemberBucket, "Den tidligere december-drøm hører til i sin egen uge");
    }
}
