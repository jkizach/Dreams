package fixit.dreams;

import javafx.scene.paint.Color;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Oversætter et symbolnavn til den farve symbolet selv handler om - så et cirkeldiagram over
 * Farver eller Chakraer kan tegnes i de rigtige farver i stedet for JavaFX' vilkårlige palet.
 *
 * Det svære er ikke de symboler vi kender på forhånd, men dem brugeren selv finder på: begynder
 * man at drømme om chartreuse eller skovgrøn, skal de også kunne farves. Derfor er opslaget en
 * stige med fire trin, hvor hvert trin fanger sin egen slags ord:
 *
 *   1. EKSPLICIT   - chakraerne, og de få farveord hvor et udledt svar ville være direkte
 *                    forkert (guld er ikke gul, oliven er ikke grøn).
 *   2. DANSK       - danske farveord er kompositionelle og ender næsten altid på grundfarven:
 *                    skovgrøn, flaskegrøn, mosgrøn. Et suffiks-opslag på fjorten grundfarver
 *                    rammer altså den rigtige kulør for ord vi aldrig har set, og præfikset
 *                    lyse-/mørke- giver lysstyrken.
 *   3. CSS         - Color.web kender de ~147 CSS-navne. Det er dem der redder låneordene:
 *                    chartreuse, magenta, cyan, khaki, indigo, teal, salmon.
 *   4. UKENDT      - null. Kalderen giver et neutralt gråt stykke.
 *
 * Trin 2 kommer bevidst FØR trin 3: et dansk ord skal have sin danske betydning. CSS' "violet"
 * er fx et lyst rosalilla, mens dansk violet er blålilla. De steder hvor et CSS-navn tilfældigvis
 * ender på en dansk grundfarve (hotpink, darkviolet, darkorange) peger de to trin alligevel på
 * samme kulør, så der er ingen kollisioner der gør skade.
 *
 * Ren regning uden JavaFX-toolkit - Color.web og interpolate kan køre i en almindelig unittest.
 */
final class Symbolfarver {

    private Symbolfarver() {}

    /** Vises for symboler vi ikke kan udlede noget om. Neutralt gråt lyver ikke om dataene. */
    static final Color UKENDT = Color.web("#7a7a7a");

    /** Chakraerne nedefra og op: rød, orange, gul, grøn, blå, indigo, violet. */
    private static final Map<String, Color> CHAKRAER = Map.of(
            "rod",          Color.web("#d32f2f"),
            "hara",         Color.web("#f57c00"),
            "solar plexus", Color.web("#fbc02d"),
            "hjerte",       Color.web("#43a047"),
            "hals",         Color.web("#1e88e5"),
            "pineal",       Color.web("#3949ab"),
            "krone",        Color.web("#8e24aa"));

    /**
     * Kun de farveord hvor stigens senere trin ville svare forkert. "guld" ender fx ikke på
     * "gul", og "olivengrøn" ville blive almindelig grøn i stedet for den brunlige olivenfarve.
     */
    private static final Map<String, Color> SÆRTILFÆLDE = Map.ofEntries(
            Map.entry("guld",        Color.web("#d4af37")),
            Map.entry("sølv",        Color.web("#c0c0c0")),
            Map.entry("bronze",      Color.web("#cd7f32")),
            Map.entry("blond",       Color.web("#e8d2a0")),
            Map.entry("okker",       Color.web("#c9a227")),
            Map.entry("dueblå",      Color.web("#6b8299")),
            Map.entry("himmelblå",   Color.web("#7ec0ee")),
            Map.entry("koksgrå",     Color.web("#4a4a4a")),
            Map.entry("laksefarvet", Color.web("#fa8072")),
            Map.entry("lavendel",    Color.web("#c9b6e4")),
            Map.entry("limegrøn",    Color.web("#a4c400")),
            Map.entry("olivengrøn",  Color.web("#8a8f2a")));

    /**
     * Grundfarverne der genkendes som ENDELSE på et sammensat ord. Sort og hvid er trukket lidt
     * ind mod midten: appens baggrund er næsten sort, så et ægte sort stykke ville forsvinde.
     */
    private static final Map<String, Color> GRUNDFARVER = Map.ofEntries(
            Map.entry("rød",    Color.web("#d32f2f")),
            Map.entry("orange", Color.web("#f57c00")),
            Map.entry("gul",    Color.web("#fbc02d")),
            Map.entry("grøn",   Color.web("#43a047")),
            Map.entry("blå",    Color.web("#1e88e5")),
            Map.entry("turkis", Color.web("#26c6da")),
            Map.entry("lilla",  Color.web("#8e24aa")),
            Map.entry("violet", Color.web("#7b3fbf")),
            Map.entry("pink",   Color.web("#ec407a")),
            Map.entry("rosa",   Color.web("#f8bbd0")),
            Map.entry("brun",   Color.web("#795548")),
            Map.entry("grå",    Color.web("#9e9e9e")),
            Map.entry("hvid",   Color.web("#f5f5f5")),
            Map.entry("sort",   Color.web("#2b2b2b")));

    /** Længste præfiks først, så "mørke" vinder over "mørk". */
    private static final List<String> LYSPRÆFIKSER = List.of("lyse", "lys", "mørke", "mørk");

    /**
     * @return symbolets naturlige farve, eller null hvis symbolet ikke handler om en farve.
     */
    static Color forSymbol(String symbol) {
        if (symbol == null) {
            return null;
        }
        String navn = symbol.trim().toLowerCase(Locale.ROOT);
        if (navn.isEmpty()) {
            return null;
        }

        Color eksplicit = CHAKRAER.get(navn);
        if (eksplicit == null) {
            eksplicit = SÆRTILFÆLDE.get(navn);
        }
        if (eksplicit != null) {
            return eksplicit;
        }

        // "-farvet" er en produktiv endelse: laksefarvet, bronzefarvet, rødfarvet. Stammen bærer
        // farven, så den sendes gennem stigen forfra. Stammen kan aldrig selv ende på "farvet",
        // så rekursionen går præcis ét skridt.
        if (navn.endsWith("farvet") && navn.length() > "farvet".length()) {
            Color stamme = forSymbol(navn.substring(0, navn.length() - "farvet".length()));
            if (stamme != null) {
                return stamme;
            }
        }

        Color dansk = udledFraDansk(navn);
        if (dansk != null) {
            return dansk;
        }

        if (kanVæreCssNavn(navn)) {
            try {
                return Color.web(navn);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Color.web tager ikke kun CSS-navne, men også BART hextal uden havelåge. "abe" er tre
     * gyldige hexcifre og bliver til #aabbee - og "facade" og "decade" bliver til deres egne
     * farver. Dyr-kategorien rummer allerede den slags ord, og et enkelt af dem er nok til at
     * farvelægge hele dyrediagrammet, for den slår til så snart ét symbol kan udledes.
     *
     * Derfor må trin 3 kun prøve rene bogstavord der ikke udelukkende består af hexbogstaverne
     * a-f. Noget CSS-farvenavn af den slags findes ikke, så der tabes intet på gulvet. Danske
     * ord med æøå ryger samtidig fra, men dem har trin 2 allerede haft fat i.
     */
    private static boolean kanVæreCssNavn(String navn) {
        return navn.matches("[a-z]+") && !navn.matches("[a-f]+");
    }

    /**
     * Skiller et dansk farveord ad i lysstyrke-præfiks og grundfarve-endelse. Lysningen sker ved
     * at blande mod hvid (og mørkningen mod sort) frem for at skrue på lysstyrken alene - det
     * giver de afdæmpede pasteller man forventer af "lyseblå", ikke bare en kraftigere blå.
     */
    private static Color udledFraDansk(String navn) {
        for (String præfiks : LYSPRÆFIKSER) {
            if (navn.length() > præfiks.length() && navn.startsWith(præfiks)) {
                Color grund = grundfarveFraEndelse(navn.substring(præfiks.length()));
                if (grund != null) {
                    return præfiks.startsWith("lys")
                            ? grund.interpolate(Color.WHITE, 0.45)
                            : grund.interpolate(Color.BLACK, 0.40);
                }
            }
        }
        return grundfarveFraEndelse(navn);
    }

    /** Længste match vinder, så et ord der ender på flere grundfarver får den mest specifikke. */
    private static Color grundfarveFraEndelse(String navn) {
        Color bedste = null;
        int længste = 0;
        for (Map.Entry<String, Color> grundfarve : GRUNDFARVER.entrySet()) {
            if (navn.endsWith(grundfarve.getKey()) && grundfarve.getKey().length() > længste) {
                længste = grundfarve.getKey().length();
                bedste = grundfarve.getValue();
            }
        }
        return bedste;
    }

    /** Til inline-styles: JavaFX' Color.toString() giver "0xd32f2fff", som CSS ikke tager imod. */
    static String tilWeb(Color farve) {
        return String.format("#%02x%02x%02x",
                Math.round(farve.getRed() * 255),
                Math.round(farve.getGreen() * 255),
                Math.round(farve.getBlue() * 255));
    }
}
