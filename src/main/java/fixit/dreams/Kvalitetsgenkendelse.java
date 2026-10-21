package fixit.dreams;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finder kvaliteterne - de binære flueben som Lucid og Praktiserer - i en drømmetekst.
 *
 * Symbolerne findes på ordet alene, men det holder ikke her. En prøvekørsel mod 868 rigtige
 * drømme viste at "lucid" oftest står som "ikke lucid", "næsten lucid" eller om en anden ("hun
 * er mere lucid"), og at "mediterer" lige så tit er Erik der mediterer. Derfor to krav ud over
 * ordet:
 *  - det skal handle om drømmeren selv: nærmeste grundled foran er "jeg" eller "vi", eller
 *    "jeg" står lige efter ("der mediterer jeg") - se omMig;
 *  - der må ikke stå et forbehold lige foran: "ikke", "næsten", "tæt på" - se forbeholdForan.
 * Med de to krav gav reglerne ingen falske træf mod de drømme. Derfor står fundene med
 * flueben fra start i Symbolforslagsdialog, som de sikre symboler.
 *
 * Arketypisk, Om praksis, Advarsel og Kollektiv er bevidst ikke med: de er fortolkninger,
 * ikke ord. Prøvekørslen fandt dem i en tredjedel af drømmene eller færre, med flest falske træf.
 *
 * Kun reglerne - uden JavaFX - i samme form som Symbolgenkendelse, så de kan testes uden toolkit.
 */
public final class Kvalitetsgenkendelse {

    private Kvalitetsgenkendelse() {}

    public static final String LUCID = "Lucid";
    public static final String PRAKTISERER = "Praktiserer";
    public static final String MODSATKOENNET = "Modsatkønnet";
    public static final String MARERIDT = "Mareridt";
    public static final String HOLOGRAFISK = "Holografisk";

    /** Hvad der sætter en sætning eller en del-sætning af. Komma er IKKE med: "jeg sidder, mediterer". */
    private static final Pattern SAETNING = Pattern.compile("[.!?;:\\n\\r\"“”()–—]+");
    private static final Pattern ORD = Pattern.compile("\\p{L}+");

    /** Grundled der ikke er drømmeren selv. Står et af dem nærmest foran, handler det om en anden. */
    private static final Set<String> ANDRE_GRUNDLED = Set.of(
            "han", "hun", "de", "den", "det", "man", "du", "nogen", "ingen", "alle", "folk",
            "begge", "hver", "flere", "mange");

    /** Småord der indleder en sætning med stort uden at være et navn: "Nu mediterer jeg". */
    private static final Set<String> INDLEDERE = Set.of(
            "nu", "så", "der", "da", "men", "og", "her", "pludselig", "bagefter", "senere",
            "derefter", "først", "imens", "mens", "måske", "endelig", "samtidig", "stadig",
            "efter", "til", "inden", "lidt", "også", "bare", "igen", "egentlig", "alligevel");

    /** Her begynder en ny del-sætning - et forbehold længere tilbage gælder ikke. */
    private static final Set<String> BINDEORD = Set.of("og", "men", "så", "mens", "eller", "fordi", "da");

    private static final Set<String> FORBEHOLD_LUCID = Set.of(
            "ikke", "næsten", "mere", "halvt", "aldrig", "tæt", "nippet");

    /** "Delvist lucid" har flueben i de rigtige drømme, "delvist holografisk" har ikke. */
    private static final Set<String> FORBEHOLD_HOLOGRAFISK = Set.of(
            "ikke", "næsten", "tæt", "vej", "nippet", "delvist", "lidt", "lille", "halvt", "aldrig");

    private static final Set<String> FORBEHOLD_PRAKSIS = Set.of("ikke", "aldrig", "glemmer");

    private static final Set<String> FORBEHOLD_MARERIDT = Set.of("ikke", "intet", "ingen", "aldrig");

    /** Udsagnsord for at praktisere, som ordstammer: mediterer, mediterede, meditere... */
    private static final List<String> PRAKSISVERBER = List.of("mediter", "praktiser", "dobbeltret");

    /**
     * Navneord for praksis. Står de med "jeg" som grundled, praktiserer drømmeren. Meditation
     * tæller kun efter "i" - "jeg er i stilhedsmeditation" - for "undervisning om meditation"
     * handler om praksis uden at være det, og har sit eget flueben (Om praksis).
     */
    private static final Pattern PRAKSISNAVNEORD = Pattern.compile(
            "mantra\\p{L}*|hjertebøn\\p{L}*|mindfulness|v[iy]pas+an+a|neutral iagttagelse"
                    + "|(i |dyb )+bøn(nen)?(?!\\p{L})"
                    + "|i (en |dyb )*\\p{L}*meditation(en)?(?!\\p{L})"
                    + "|meditativ\\p{L}* tilstand");

    private static final Pattern LUCID_ERKENDELSE = Pattern.compile(
            "(klar over|ved|indser|opdager|forstår|bevidst om|går op for mig)( at)? (det( her)? er en drøm|jeg drømmer)");

    private static final Pattern KOEN = Pattern.compile(
            "jeg (er|var|bliver|blev)( nu| pludselig| selv| også| her)? (en |et )?(\\p{L}+ )?"
                    + "(kvinde|mand|pige|dreng|kvindelig\\p{L}*|mandlig\\p{L}*)(?!\\p{L})"
                    + "|jeg som (kvinde|mand|pige|dreng)(?!\\p{L})");

    /** Ét ord i en sætning, med sin originale form - stort begyndelsesbogstav afslører navne. */
    private record Ord(String smaat, String original) {}

    /**
     * @param tekst    drømmeteksten
     * @param tilladte de kvaliteter der må foreslås - fx ikke dem der allerede har flueben, og
     *                 ikke Holografisk når boksen er slået fra under Indstillinger
     * @return ét Fund pr. kvalitet, i Category.FLAGS_SYMBOLS_IN_ORDER-rækkefølge. fundetSom er
     *         sætningsstykket der udløste det, så dialogen kan vise hvorfor.
     */
    public static List<Symbolgenkendelse.Fund> find(String tekst, Collection<String> tilladte) {
        if (tekst == null || tekst.isBlank() || tilladte == null || tilladte.isEmpty()) {
            return List.of();
        }
        Map<String, String> fund = new LinkedHashMap<>();
        for (String saetning : SAETNING.split(tekst)) {
            List<Ord> ord = delIOrd(saetning);
            if (ord.isEmpty()) {
                continue;
            }
            soeg(LUCID, tilladte, fund, () -> lucid(ord));
            soeg(PRAKTISERER, tilladte, fund, () -> praktiserer(ord));
            soeg(MODSATKOENNET, tilladte, fund, () -> modsatkoennet(ord));
            soeg(MARERIDT, tilladte, fund, () -> mareridt(ord));
            soeg(HOLOGRAFISK, tilladte, fund, () -> holografisk(ord));
        }
        List<Symbolgenkendelse.Fund> ud = new ArrayList<>();
        for (String kvalitet : Category.FLAGS_SYMBOLS_IN_ORDER) {
            if (fund.containsKey(kvalitet)) {
                ud.add(new Symbolgenkendelse.Fund(Category.ID_KVALITETER, kvalitet, fund.get(kvalitet)));
            }
        }
        return List.copyOf(ud);
    }

    private interface Soegning {
        String find();
    }

    private static void soeg(String kvalitet, Collection<String> tilladte, Map<String, String> fund, Soegning s) {
        if (!tilladte.contains(kvalitet) || fund.containsKey(kvalitet)) {
            return;
        }
        String fundetSom = s.find();
        if (fundetSom != null) {
            fund.put(kvalitet, fundetSom);
        }
    }

    // ---- De enkelte kvaliteter. Hver returnerer det udløsende stykke tekst, eller null. ----

    private static String lucid(List<Ord> ord) {
        for (int i = 0; i < ord.size(); i++) {
            if (ord.get(i).smaat().startsWith("lucid")
                    && !forbeholdForan(ord, i, 3, FORBEHOLD_LUCID) && omMig(ord, i)) {
                return stykke(ord, i);
            }
        }
        return frase(ord, LUCID_ERKENDELSE, FORBEHOLD_LUCID, true);
    }

    private static String praktiserer(List<Ord> ord) {
        for (int i = 0; i < ord.size(); i++) {
            String w = ord.get(i).smaat();
            boolean verbum = PRAKSISVERBER.stream().anyMatch(w::startsWith);
            // Fem ord og ikke tre: "er ikke vågen nok til at praktisere".
            if (verbum && !forbeholdForan(ord, i, 5, FORBEHOLD_PRAKSIS) && omMig(ord, i)) {
                return stykke(ord, i);
            }
        }
        return frase(ord, PRAKSISNAVNEORD, FORBEHOLD_PRAKSIS, true);
    }

    private static String modsatkoennet(List<Ord> ord) {
        // "jeg" står i selve udtrykket, så omMig er unødvendig - og "ikke" kan ikke stå imellem.
        return frase(ord, KOEN, Set.of(), false);
    }

    private static String mareridt(List<Ord> ord) {
        for (int i = 0; i < ord.size(); i++) {
            String w = ord.get(i).smaat();
            if ((w.equals("mareridt") || w.equals("mareridtet"))
                    && !forbeholdForan(ord, i, 2, FORBEHOLD_MARERIDT)) {
                return stykke(ord, i);
            }
        }
        return null;
    }

    private static String holografisk(List<Ord> ord) {
        // Om drømmen, ikke om drømmeren: "nu bliver drømmen nok holografisk". Intet omMig.
        for (int i = 0; i < ord.size(); i++) {
            String w = ord.get(i).smaat();
            if ((w.equals("holografisk") || w.equals("holografiske"))
                    && !forbeholdForan(ord, i, 4, FORBEHOLD_HOLOGRAFISK)) {
                return stykke(ord, i);
            }
        }
        return null;
    }

    // ---- Fælles ----

    /**
     * Handler ordet på plads i om drømmeren selv? Ja hvis "jeg" eller "vi" står lige efter
     * ("der mediterer jeg"), eller hvis det nærmeste grundled foran i sætningen er "jeg" eller
     * "vi". Et andet stedord eller et navn foran betyder en anden: "Erik sidder og mediterer".
     */
    private static boolean omMig(List<Ord> ord, int i) {
        if (i + 1 < ord.size() && erMig(ord.get(i + 1).smaat())) {
            return true;
        }
        for (int j = i - 1; j >= 0; j--) {
            Ord o = ord.get(j);
            if (erMig(o.smaat())) {
                return true;
            }
            if (ANDRE_GRUNDLED.contains(o.smaat()) || erNavn(o, j)) {
                return false;
            }
        }
        return false;
    }

    private static boolean erMig(String w) {
        return w.equals("jeg") || w.equals("vi");
    }

    private static boolean erNavn(Ord o, int plads) {
        if (!Character.isUpperCase(o.original().charAt(0))) {
            return false;
        }
        // Midt i sætningen er stort bogstav et navn. Først i sætningen kun hvis det ikke er et småord.
        return plads > 0 || !INDLEDERE.contains(o.smaat());
    }

    /**
     * Står der et forbehold højst afstand ord foran? Søgningen stopper ved et bindeord, for et
     * "ikke" i en anden del-sætning gælder ikke her: "jeg vil ikke forstyrre ham, så jeg mediterer".
     */
    private static boolean forbeholdForan(List<Ord> ord, int i, int afstand, Set<String> forbehold) {
        for (int j = i - 1; j >= Math.max(0, i - afstand); j--) {
            String w = ord.get(j).smaat();
            if (forbehold.contains(w)) {
                return true;
            }
            if (BINDEORD.contains(w)) {
                return false;
            }
        }
        return false;
    }

    /**
     * Søger et flerordsudtryk i sætningen, skrevet med småt og ét mellemrum mellem ordene, så
     * tegnsætning og linjeskift ikke spænder ben. Forbehold og omMig måles fra udtrykkets første ord.
     */
    private static String frase(List<Ord> ord, Pattern p, Set<String> forbehold, boolean kraevMig) {
        StringBuilder sb = new StringBuilder();
        List<Integer> starter = new ArrayList<>();
        for (Ord o : ord) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            starter.add(sb.length());
            sb.append(o.smaat());
        }
        Matcher m = p.matcher(sb);
        while (m.find()) {
            if (m.start() > 0 && sb.charAt(m.start() - 1) != ' ') {
                continue; // midt i et ord
            }
            int i = starter.indexOf(m.start());
            if (i < 0 || forbeholdForan(ord, i, 3, forbehold) || (kraevMig && !omMig(ord, i))) {
                continue;
            }
            return m.group();
        }
        return null;
    }

    /** Op til fire ord foran og to efter - nok til at brugeren kan genkende stedet i teksten. */
    private static String stykke(List<Ord> ord, int i) {
        int fra = Math.max(0, i - 4);
        int til = Math.min(ord.size() - 1, i + 2);
        StringBuilder sb = new StringBuilder(fra > 0 ? "…" : "");
        for (int j = fra; j <= til; j++) {
            if (j > fra) {
                sb.append(' ');
            }
            sb.append(ord.get(j).original());
        }
        return sb.append(til < ord.size() - 1 ? "…" : "").toString();
    }

    private static List<Ord> delIOrd(String saetning) {
        List<Ord> ud = new ArrayList<>();
        Matcher m = ORD.matcher(saetning);
        while (m.find()) {
            ud.add(new Ord(m.group().toLowerCase(Locale.ROOT), m.group()));
        }
        return ud;
    }
}
