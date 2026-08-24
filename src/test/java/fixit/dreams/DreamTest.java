package fixit.dreams;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class DreamTest {

    private DreamData fullDreamData() {
        DreamData data = new DreamData();
        data.categories = new ArrayList<>();
        data.indhold = "Jeg fløj over en skov";
        data.dagrest = "Så en fugl i dag";
        data.tolkning = "Frihed";
        data.dato = LocalDate.of(2026, 3, 15);
        data.lucid = true;
        data.praktiserer = true;
        data.modsat = false;
        data.arketypisk = true;
        data.ompraksis = false;
        data.mareridt = false;
        data.kollektiv = true;
        data.advarsel = false;
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
        assertEquals(data.lucid, dream.getLucid());
        assertEquals(data.praktiserer, dream.getPraktiserer());
        assertEquals(data.modsat, dream.getModsat());
        assertEquals(data.arketypisk, dream.getArketypisk());
        assertEquals(data.ompraksis, dream.getOmpraksis());
        assertEquals(data.mareridt, dream.getMareridt());
        assertEquals(data.kollektiv, dream.getKollektiv());
        assertEquals(data.advarsel, dream.getAdvarsel());
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
    void addCategoryDTO_tilfojer_til_categories() {
        Dream dream = new Dream(fullDreamData());
        CategoryDTO cdto = new CategoryDTO();
        cdto.name = "Farver";

        dream.addCategoryDTO(cdto);

        assertTrue(dream.getCategories().contains(cdto));
    }
}
