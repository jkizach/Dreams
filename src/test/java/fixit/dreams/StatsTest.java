package fixit.dreams;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

class StatsTest {

    private static final LocalDate D1 = LocalDate.of(2026, 1, 5);  // uge 2, lucid
    private static final LocalDate D2 = LocalDate.of(2026, 1, 8);  // samme uge som D1, mareridt
    private static final LocalDate D3 = LocalDate.of(2026, 1, 15); // uge 3, lucid + mareridt
    private static final LocalDate D4 = LocalDate.of(2026, 2, 2);  // anden måned, ingen af delene

    @BeforeEach
    void setUp() {
        User.resetForTests();
    }

    private CategoryDTO farverDTO(String... symbols) {
        CategoryDTO dto = new CategoryDTO();
        dto.name = "Farver";
        dto.symbols = new TreeSet<>(List.of(symbols));
        return dto;
    }

    private void addDream(User user, LocalDate dato, boolean lucid, boolean mareridt, CategoryDTO... categories) {
        DreamData data = new DreamData();
        data.categories = new ArrayList<>(List.of(categories));
        data.categories.add(Category.buildFlagsCategoryDTO(lucid, false, false, false, false, mareridt, false, false, false));
        data.indhold = "test";
        data.dagrest = "";
        data.tolkning = "";
        data.dato = dato;
        user.addDream(new Dream(data));
    }

    private Stats buildFixtureStats() {
        User user = User.getInstance();
        addDream(user, D1, true, false, farverDTO("rød"));
        addDream(user, D2, false, true, farverDTO("rød", "blå"));
        addDream(user, D3, true, true, farverDTO("blå"));
        addDream(user, D4, false, false);
        return new Stats();
    }

    @Test
    void getFirstDream_returnerer_tidligste_dato_i_fixturen() {
        Stats stats = buildFixtureStats();
        assertEquals(D1, stats.getFirstDream());
    }

    @Test
    void getBoolStatsPerDag_taeller_kun_den_praecise_dag() {
        Stats stats = buildFixtureStats();
        assertEquals(1, stats.getBoolStatsPerDag(stats.getFlagStats("Lucid"), D1));
        assertEquals(0, stats.getBoolStatsPerDag(stats.getFlagStats("Lucid"), D2));
    }

    @Test
    void getBoolStatsPerUge_slar_dromme_i_samme_iso_uge_sammen() {
        Stats stats = buildFixtureStats();
        // D1 og D2 ligger i samme ISO-uge: 1 lucid (D1) + 1 mareridt (D2)
        assertEquals(1, stats.getBoolStatsPerUge(stats.getFlagStats("Lucid"), D1));
        assertEquals(1, stats.getBoolStatsPerUge(stats.getFlagStats("Mareridt"), D1));
        // D3 ligger i en anden uge for sig selv
        assertEquals(1, stats.getBoolStatsPerUge(stats.getFlagStats("Lucid"), D3));
    }

    @Test
    void getBoolStatsPerM_slar_hele_maneden_sammen() {
        Stats stats = buildFixtureStats();
        // Januar: D1(lucid) + D3(lucid) = 2, D4 ligger i februar
        assertEquals(2, stats.getBoolStatsPerM(stats.getFlagStats("Lucid"), D1));
        assertEquals(0, stats.getBoolStatsPerM(stats.getFlagStats("Lucid"), D4));
    }

    @Test
    void getTalBinary_summerer_lucid_og_mareridt_over_hele_perioden() {
        Stats stats = buildFixtureStats();
        int[] result = stats.getTalBinary(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        // Rækkefølge fra Category.FLAGS_SYMBOLS_IN_ORDER: lucid, praktiserer, modsat, arketypisk, praksis, mareridt, advarsel, kollektiv, holografisk
        assertEquals(2, result[0], "lucid: D1 + D3");
        assertEquals(2, result[5], "mareridt: D2 + D3");
        assertEquals(0, result[1]);
        assertEquals(0, result[2]);
        assertEquals(0, result[3]);
        assertEquals(0, result[4]);
        assertEquals(0, result[6]);
        assertEquals(0, result[7]);
        assertEquals(0, result[8]);
    }

    @Test
    void getCategoryStats_akkumulerer_symboler_pr_dag() {
        Stats stats = buildFixtureStats();
        TreeMap<String, TreeMap<String, Integer>> farverStats = stats.getCategoryStats("Farver");

        assertEquals(1, stats.getStatsPerDag(farverStats, D1).get("rød"));
        assertEquals(1, stats.getStatsPerDag(farverStats, D2).get("rød"));
        assertEquals(1, stats.getStatsPerDag(farverStats, D2).get("blå"));
        assertEquals(1, stats.getStatsPerDag(farverStats, D3).get("blå"));
    }

    @Test
    void getCategoryStats_akkumulerer_symboler_pr_uge_og_maned() {
        Stats stats = buildFixtureStats();
        TreeMap<String, TreeMap<String, Integer>> farverStats = stats.getCategoryStats("Farver");

        // D1 + D2 i samme uge: rød=2 (D1+D2), blå=1 (kun D2)
        TreeMap<String, Integer> ugeStats = stats.getStatsPerUge(farverStats, D1);
        assertEquals(2, ugeStats.get("rød"));
        assertEquals(1, ugeStats.get("blå"));

        // Hele januar: D1+D2+D3: rød=2 (D1+D2), blå=2 (D2+D3)
        TreeMap<String, Integer> manedStats = stats.getStatsPerM(farverStats, D1);
        assertEquals(2, manedStats.get("rød"));
        assertEquals(2, manedStats.get("blå"));
    }
}
