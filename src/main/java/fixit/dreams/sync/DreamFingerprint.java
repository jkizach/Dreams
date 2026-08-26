package fixit.dreams.sync;

import java.util.Locale;

// Rent, netværksfrit "er det her reelt den samme drøm?"-fingeraftryk, brugt til at skelne to
// uafhængigt migrerede kopier af det SAMME datasæt fra ægte nye drømme skrevet på en ny maskine.
//
// Baggrund: hver drøm får et tilfældigt UUID ved oprettelse. Er det samme drøm migreret
// uafhængigt to steder, har de to forskellige ID'er men identisk indhold - og en sammenkøring
// ville lave dubletter. Er det en ægte ny drøm, er indholdet nyt og der er ingen dublet-risiko.
// ID'er kan altså ikke afgøre spørgsmålet; det kan indholdet.
//
// Dato + indhold er bevidst valgt som nøgle: strengere (fx også dagrest/tolkning) ville MISSE
// dubletter hvor et bifelt var redigeret på den ene maskine, løsere (fx dato alene) ville
// fejlagtigt udråbe to forskellige drømme fra samme nat som dubletter.
public final class DreamFingerprint {

    // Linjeskift kan hverken optræde i en ISO-dato eller i whitespace-normaliseret indhold, og
    // er derfor et sikkert skilletegn: et almindeligt mellemrum ville kunne stå på begge sider
    // af skillet og lade to forskellige dato/indhold-par give samme fingeraftryk.
    private static final String SKILLETEGN = "\n";

    private DreamFingerprint() {}

    // null betyder "kan ikke fingeraftrykkes" - drømmen tælles aldrig som dublet. Det gælder
    // tomt indhold, som ikke identificerer noget som helst: uden den regel ville to blanke
    // drømme fra samme dato ligne hinanden perfekt og udløse en falsk dublet-advarsel.
    public static String of(String isoDato, String indhold) {
        if (indhold == null) {
            return null;
        }
        String normaliseret = indhold.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (normaliseret.isEmpty()) {
            return null;
        }
        return (isoDato == null ? "" : isoDato) + SKILLETEGN + normaliseret;
    }
}
