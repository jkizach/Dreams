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
 *
 * Listen af fonte slås op på den maskine appen kører på, så en Mac-bruger får sine egne
 * fonte (Menlo, Monaco osv.) uden at der skal ændres noget i koden.
 */
public class Fonte {

    /** Logisk JavaFX-familie der altid findes, og som er monospace på alle platforme. */
    public static final String FALLBACK = "Monospaced";

    /** Tegn en font skal kunne vise for at være brugbar - inkl. mellemrum og de danske bogstaver. */
    private static final String PROVETEKST = "ABCabcxyz0123456789 -%:.,()[]æøåÆØÅ";

    /** Returnerer fontnavnet hvis det faktisk er installeret, ellers en monospace-fallback. */
    public static String tilgaengelig(String font) {
        if (font != null && !font.isBlank() && Font.getFamilies().contains(font)) {
            return font;
        }
        return FALLBACK;
    }

    /**
     * Alle installerede monospace-familier der kan vise dansk tekst, sorteret. En familie
     * regnes som monospace når et smalt og et bredt tegn fylder præcis det samme.
     */
    public static List<String> monospaceFamilier() {
        List<String> ud = new ArrayList<>();
        Text smal = new Text("i");
        Text bred = new Text("W");

        for (String familie : Font.getFamilies()) {
            Font f = Font.font(familie, 14);
            smal.setFont(f);
            bred.setFont(f);
            boolean monospace = Math.abs(smal.getLayoutBounds().getWidth() - bred.getLayoutBounds().getWidth()) < 0.01;
            if (monospace && kanViseTekst(familie)) {
                ud.add(familie);
            }
        }

        if (ud.isEmpty()) {
            ud.add(FALLBACK);
        }
        Collections.sort(ud);
        return ud;
    }

    /**
     * Sorterer symbolfonte fra. Nogle fonte er teknisk monospace, men mangler glyffer til
     * almindelig tekst - fx TINspireKeys, der ikke engang har et mellemrum, og SimSun-ExtB,
     * der mangler æøå. AWT deler systemets fonte med JavaFX og kan som den eneste af de to
     * svare på hvilke tegn en font dækker. Kender AWT ikke familien, falder den tilbage til
     * Dialog, som dækker alt - så kontrollen slipper i værste fald en ubrugelig font igennem,
     * men den kan aldrig komme til at fjerne en font der virker.
     */
    private static boolean kanViseTekst(String familie) {
        java.awt.Font awtFont = new java.awt.Font(familie, java.awt.Font.PLAIN, 12);
        return awtFont.canDisplayUpTo(PROVETEKST) == -1;
    }
}
