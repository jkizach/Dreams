package fixit.dreams;

import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hjælpere til fontvalg i temaerne.
 *
 * Baggrund: beder man JavaFX om en font der ikke er installeret, siger den ikke fra - den
 * erstatter stiltiende med den proportionale System-font. Så knækker kolonnerne i Tal-tabben,
 * uden at det er til at se hvorfor. JavaFX understøtter heller ikke fallback-lister i
 * -fx-font-family (den tager kun det første navn), så vi må selv falde tilbage.
 */
public class Fonte {

    /** Logisk JavaFX-familie der altid findes, og som er monospace på alle platforme. */
    public static final String FALLBACK = "Monospaced";

    /** Returnerer fontnavnet hvis det faktisk er installeret, ellers en monospace-fallback. */
    public static String tilgaengelig(String font) {
        if (font != null && !font.isBlank() && Font.getFamilies().contains(font)) {
            return font;
        }
        return FALLBACK;
    }

    /**
     * Alle installerede monospace-familier, sorteret. En familie regnes som monospace når
     * et smalt og et bredt tegn fylder præcis det samme.
     */
    public static List<String> monospaceFamilier() {
        List<String> ud = new ArrayList<>();
        Text smal = new Text("i");
        Text bred = new Text("W");

        for (String familie : Font.getFamilies()) {
            Font f = Font.font(familie, 14);
            smal.setFont(f);
            bred.setFont(f);
            if (Math.abs(smal.getLayoutBounds().getWidth() - bred.getLayoutBounds().getWidth()) < 0.01) {
                ud.add(familie);
            }
        }

        if (ud.isEmpty()) {
            ud.add(FALLBACK);
        }
        Collections.sort(ud);
        return ud;
    }
}
