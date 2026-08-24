package fixit.dreams;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    // NB: addSymbol/removeSymbol/addSymbols kalder internt updateAllCCBs(), som normalt
    // ville kræve JavaFX-toolkit for at bygge CheckComboBox-controls. Det er kun sikkert
    // her fordi ccbDream/ccbFilter forbliver tomme (addDreamCCB/addFilterCCB kaldes aldrig
    // i disse tests) - updateAllCCBs() looper nul gange over tomme lister.

    @Test
    void addSymbol_tilfojer_til_symbols() {
        Category category = new Category("Farver");
        category.addSymbol("rød");
        assertTrue(category.getSymbols().contains("rød"));
    }

    @Test
    void addSymbol_dublet_dublikerer_ikke_i_symbols() {
        Category category = new Category("Farver");
        category.addSymbol("rød");
        category.addSymbol("rød");
        assertEquals(1, category.getSymbols().size());
    }

    @Test
    void hasCustomOrder_er_sand_for_chakraer_og_forlob() {
        assertTrue(new Category("Chakraer").hasCustomOrder());
        assertTrue(new Category("Forløb").hasCustomOrder());
    }

    @Test
    void hasCustomOrder_er_falsk_for_andre_kategorier() {
        assertFalse(new Category("Farver").hasCustomOrder());
        assertFalse(new Category("Dyr").hasCustomOrder());
    }

    @Test
    void addSymbol_tilfojer_ogsa_til_customOrder_naar_hasCustomOrder() {
        Category category = new Category("Chakraer");
        category.addSymbol("krone");
        category.addSymbol("hjerte");

        assertEquals(List.of("krone", "hjerte"), category.getCustomOrder());
    }

    @Test
    void addSymbol_dublet_dublikerer_ikke_i_customOrder() {
        Category category = new Category("Chakraer");
        category.addSymbol("krone");
        category.addSymbol("krone");

        assertEquals(List.of("krone"), category.getCustomOrder());
    }

    @Test
    void getSymbolsForDisplay_respekterer_indsaettelsesrakkefolge_for_customOrder_kategori() {
        Category category = new Category("Forløb");
        category.addSymbol("afslutning");
        category.addSymbol("begyndelse");

        assertIterableEquals(List.of("afslutning", "begyndelse"), category.getSymbolsForDisplay());
    }

    @Test
    void getSymbolsForDisplay_returnerer_sorteret_treeset_for_normal_kategori() {
        Category category = new Category("Farver");
        category.addSymbol("rød");
        category.addSymbol("blå");

        assertIterableEquals(List.of("blå", "rød"), category.getSymbolsForDisplay());
    }

    @Test
    void removeSymbol_fjerner_fra_symbols_og_customOrder() {
        Category category = new Category("Chakraer");
        category.addSymbol("krone");
        category.addSymbol("hjerte");

        category.removeSymbol("krone");

        assertFalse(category.getSymbols().contains("krone"));
        assertFalse(category.getCustomOrder().contains("krone"));
        assertTrue(category.getSymbols().contains("hjerte"));
    }

    @Test
    void addSymbols_tilfojer_alle_givne_symboler() {
        Category category = new Category("Dyr");
        category.addSymbols(List.of("hund", "kat", "hest"));

        assertEquals(3, category.getSymbols().size());
        assertTrue(category.getSymbols().containsAll(List.of("hund", "kat", "hest")));
    }

    @Test
    void getName_returnerer_konstruktor_navn() {
        assertEquals("Farver", new Category("Farver").getName());
    }

    @Test
    void isFlagsCategory_er_sand_kun_for_kvaliteter() {
        assertTrue(new Category("Kvaliteter").isFlagsCategory());
        assertFalse(new Category("Farver").isFlagsCategory());
    }

    @Test
    void hasCustomOrder_er_sand_for_kvaliteter() {
        assertTrue(new Category("Kvaliteter").hasCustomOrder());
    }

    @Test
    void buildFlagsCategoryDTO_indeholder_kun_de_afkrydsede_symboler() {
        CategoryDTO dto = Category.buildFlagsCategoryDTO(true, false, false, false, false, true, false, false);

        assertEquals("Kvaliteter", dto.name);
        assertEquals(2, dto.symbols.size());
        assertTrue(dto.symbols.contains("Lucid"));
        assertTrue(dto.symbols.contains("Mareridt"));
    }

    @Test
    void buildFlagsCategoryDTO_uden_flag_giver_tomme_symboler() {
        CategoryDTO dto = Category.buildFlagsCategoryDTO(false, false, false, false, false, false, false, false);
        assertTrue(dto.symbols.isEmpty());
    }
}
