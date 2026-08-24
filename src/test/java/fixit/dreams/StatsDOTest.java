package fixit.dreams;

import org.junit.jupiter.api.Test;

import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class StatsDOTest {

    private CategoryDTO categoryWithSymbols(String... symbols) {
        CategoryDTO dto = new CategoryDTO();
        dto.name = "Test";
        dto.symbols = new java.util.TreeSet<>(java.util.List.of(symbols));
        return dto;
    }

    @Test
    void updateStatsDO_accumulerer_samme_symbol_pa_samme_datonogle() {
        StatsDO statsDO = new StatsDO("Test");
        statsDO.updateStatsDO("2026-01-01", categoryWithSymbols("krone"));
        statsDO.updateStatsDO("2026-01-01", categoryWithSymbols("krone"));

        assertEquals(2, statsDO.getCatStats().get("2026-01-01").get("krone"));
    }

    @Test
    void updateStatsDO_holder_forskellige_datonogler_adskilt() {
        StatsDO statsDO = new StatsDO("Test");
        statsDO.updateStatsDO("2026-01-01", categoryWithSymbols("krone"));
        statsDO.updateStatsDO("2026-01-02", categoryWithSymbols("krone"));

        TreeMap<String, TreeMap<String, Integer>> catStats = statsDO.getCatStats();
        assertEquals(1, catStats.get("2026-01-01").get("krone"));
        assertEquals(1, catStats.get("2026-01-02").get("krone"));
        assertEquals(2, catStats.size());
    }

    @Test
    void updateStatsDO_holder_forskellige_symboler_adskilt_under_samme_dato() {
        StatsDO statsDO = new StatsDO("Test");
        statsDO.updateStatsDO("2026-01-01", categoryWithSymbols("krone", "hjerte"));

        TreeMap<String, Integer> dagStats = statsDO.getCatStats().get("2026-01-01");
        assertEquals(1, dagStats.get("krone"));
        assertEquals(1, dagStats.get("hjerte"));
    }

    @Test
    void getName_returnerer_konstruktor_vaerdien() {
        StatsDO statsDO = new StatsDO("Chakraer");
        assertEquals("Chakraer", statsDO.getName());
    }
}
