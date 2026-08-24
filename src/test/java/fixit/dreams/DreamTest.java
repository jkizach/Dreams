package fixit.dreams;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class DreamTest {

    private DreamData fullDreamData() {
        DreamData data = new DreamData();
        data.categories = new ArrayList<>();
        data.categories.add(Category.buildFlagsCategoryDTO(true, true, false, true, false, false, false, true));
        data.indhold = "Jeg fløj over en skov";
        data.dagrest = "Så en fugl i dag";
        data.tolkning = "Frihed";
        data.dato = LocalDate.of(2026, 3, 15);
        return data;
    }

    @Test
    void constructor_rundtripper_alle_felter() {
        DreamData data = fullDreamData();
        Dream dream = new Dream(data);

        assertEquals(data.indhold, dream.getIndhold());
        assertEquals(data.dagrest, dream.getDagrest());
        assertEquals(data.tolkning, dream.getTolkning());
        assertEquals(data.dato, dream.getDato());
        assertTrue(dream.hasFlag("Lucid"));
        assertTrue(dream.hasFlag("Praktiserer"));
        assertFalse(dream.hasFlag("Modsatkønnet"));
        assertTrue(dream.hasFlag("Arketypisk"));
        assertFalse(dream.hasFlag("Om praksis"));
        assertFalse(dream.hasFlag("Mareridt"));
        assertTrue(dream.hasFlag("Kollektiv"));
        assertFalse(dream.hasFlag("Advarsel"));
    }

    @Test
    void id_genbruges_fra_dreamdata_naar_sat() {
        DreamData data = fullDreamData();
        data.id = "fast-id-123";

        Dream dream = new Dream(data);

        assertEquals("fast-id-123", dream.getId());
    }

    @Test
    void constructor_defaulter_null_tolkning_til_tom_streng() {
        DreamData data = fullDreamData();
        data.tolkning = null;

        Dream dream = new Dream(data);

        assertEquals("", dream.getTolkning());
    }

    @Test
    void getId_er_unik_pr_konstruktion() {
        DreamData data = fullDreamData();
        Dream first = new Dream(data);
        Dream second = new Dream(data);

        assertNotNull(first.getId());
        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void constructor_stempler_updatedAt_med_nu_hvis_ikke_sat() {
        Instant before = Instant.now();
        Dream dream = new Dream(fullDreamData());
        Instant after = Instant.now();

        assertNotNull(dream.getUpdatedAt());
        assertFalse(dream.getUpdatedAt().isBefore(before));
        assertFalse(dream.getUpdatedAt().isAfter(after));
    }

    @Test
    void constructor_genbruger_updatedAt_fra_dreamdata_naar_sat() {
        DreamData data = fullDreamData();
        data.updatedAt = Instant.parse("2020-01-01T00:00:00Z");

        Dream dream = new Dream(data);

        assertEquals(Instant.parse("2020-01-01T00:00:00Z"), dream.getUpdatedAt());
    }

    @Test
    void touch_opdaterer_updatedAt_til_nu() {
        DreamData data = fullDreamData();
        data.updatedAt = Instant.parse("2020-01-01T00:00:00Z");
        Dream dream = new Dream(data);

        dream.touch();

        assertTrue(dream.getUpdatedAt().isAfter(Instant.parse("2020-01-01T00:00:00Z")));
    }

    @Test
    void addCategoryDTO_tilfojer_til_categories() {
        Dream dream = new Dream(fullDreamData());
        CategoryDTO cdto = new CategoryDTO();
        cdto.name = "Farver";

        dream.addCategoryDTO(cdto);

        assertTrue(dream.getCategories().contains(cdto));
    }
}
