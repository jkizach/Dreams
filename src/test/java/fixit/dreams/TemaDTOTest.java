package fixit.dreams;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class TemaDTOTest {

    @Test
    void getTemaForCSSUpdater_returnerer_de_ni_forventede_nogler() {
        TemaDTO dto = new TemaDTO();
        dto.baggrundA = Color.web("#111111");
        dto.baggrundB = Color.web("#222222");
        dto.baggrundC = Color.web("#333333");
        dto.baggrundD = Color.web("#444444");
        dto.tekstA = Color.web("#555555");
        dto.tekstB = Color.web("#666666");
        dto.tekstC = Color.web("#777777");
        dto.kant = Color.web("#888888");
        dto.font = "Source Code Pro";

        HashMap<String, String> cssMap = dto.getTemaForCSSUpdater();

        assertEquals(9, cssMap.size());
        for (String key : new String[]{
                "-fx-hovedbg-background", "-fx-alternativbg-background", "-fx-andenalternativbg-background",
                "-fx-textfelt-background", "-fx-hovedtxt-text", "-fx-alttext-text", "-fx-alternativtxt-text",
                "-fx-border-border", "-fx-font-family"}) {
            assertNotNull(cssMap.get(key), "Mangler nøgle: " + key);
        }
        assertEquals("Source Code Pro", cssMap.get("-fx-font-family"));
    }
}
