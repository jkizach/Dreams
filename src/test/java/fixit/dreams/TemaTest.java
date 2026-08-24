package fixit.dreams;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class TemaTest {

    private HashMap<String, String> hexMap(String hex) {
        HashMap<String, String> map = new HashMap<>();
        for (String key : new String[]{"baggrundA", "baggrundB", "baggrundC", "baggrundD", "tekstA", "tekstB", "tekstC", "kant"}) {
            map.put(key, hex);
        }
        map.put("temaName", "TestTema");
        map.put("font", "Courier New");
        return map;
    }

    @Test
    void hex_til_farve_og_tilbage_rundtripper_sort() {
        Tema tema = new Tema(hexMap("#000000"));
        HashMap<String, String> saved = tema.getTemaForSaving();

        assertEquals("#000000", saved.get("baggrundA"));
        assertEquals("#000000", saved.get("kant"));
    }

    @Test
    void hex_til_farve_og_tilbage_rundtripper_hvid() {
        Tema tema = new Tema(hexMap("#FFFFFF"));
        HashMap<String, String> saved = tema.getTemaForSaving();

        assertEquals("#FFFFFF", saved.get("baggrundA"));
        assertEquals("#FFFFFF", saved.get("kant"));
    }

    @Test
    void getTemaForSaving_bevarer_temaName_og_font() {
        Tema tema = new Tema(hexMap("#1A2B3C"));
        HashMap<String, String> saved = tema.getTemaForSaving();

        assertEquals("TestTema", saved.get("temaName"));
        assertEquals("Courier New", saved.get("font"));
    }

    @Test
    void getTemaForCSSUpdater_returnerer_de_ni_forventede_nogler() {
        Tema tema = new Tema(
                Color.web("#111111"), Color.web("#222222"), Color.web("#333333"), Color.web("#444444"),
                Color.web("#555555"), Color.web("#666666"), Color.web("#777777"), Color.web("#888888"),
                "mit tema", "Source Code Pro"
        );

        HashMap<String, String> cssMap = tema.getTemaForCSSUpdater();

        assertEquals(9, cssMap.size());
        for (String key : new String[]{
                "-fx-hovedbg-background", "-fx-alternativbg-background", "-fx-andenalternativbg-background",
                "-fx-textfelt-background", "-fx-hovedtxt-text", "-fx-alttext-text", "-fx-alternativtxt-text",
                "-fx-border-border", "-fx-font-family"}) {
            assertNotNull(cssMap.get(key), "Mangler nøgle: " + key);
        }
        assertEquals("Source Code Pro", cssMap.get("-fx-font-family"));
    }

    @Test
    void getTemaName_og_getFont_returnerer_konstruktor_vaerdier() {
        Tema tema = new Tema(
                Color.web("#111111"), Color.web("#222222"), Color.web("#333333"), Color.web("#444444"),
                Color.web("#555555"), Color.web("#666666"), Color.web("#777777"), Color.web("#888888"),
                "mit tema", "Source Code Pro"
        );

        assertEquals("mit tema", tema.getTemaName());
        assertEquals("Source Code Pro", tema.getFont());
    }
}
