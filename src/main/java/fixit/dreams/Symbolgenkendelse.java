package fixit.dreams;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Finder brugerens egne symboler i en drømmetekst, så fluebenene kan sættes uden at man skal
 * bladre hver eneste CheckComboBox igennem.
 *
 * Her ligger kun reglerne - uden JavaFX, uden User og uden filer, i samme form som Tag,
 * Startdato og Kategoriid, så de kan testes uden toolkit. Derfor tager find() et kort over
 * kategori-id til symbolnavne og ikke en List&lt;Category&gt;: Category har CheckComboBox'e som
 * felter og ville trække hele toolkittet med ind i en unittest.
 *
 * To ting er bevidst IKKE afgjort her: intet bliver afkrydset automatisk, og intet bliver
 * gemt. Klassen svarer kun "det her fandt jeg, og det her er jeg i tvivl om" - resten er
 * Symbolforslagsdialogs og HovedmenuControllers arbejde. Et symbol sat på en drøm er data
 * brugeren senere analyserer på, så et forkert flueben sat i tavshed er dyrere end et flueben
 * der ikke blev sat.
 */
public final class Symbolgenkendelse {

    private Symbolgenkendelse() {}

    /** Ét symbol fundet i teksten. fundetSom er det ord i teksten der udløste det. */
    public record Fund(String kategoriId, String symbol, String fundetSom) {}

    /** Ét ord i teksten som kan betyde flere forskellige symboler - brugeren må vælge. */
    public record Tvetydighed(String ord, List<Fund> kandidater) {}

    public record Resultat(List<Fund> sikre, List<Tvetydighed> tvetydige) {
        public boolean erTom() {
            return sikre.isEmpty() && tvetydige.isEmpty();
        }
    }

    private static final Resultat INTET = new Resultat(List.of(), List.of());

    /**
     * Danske bøjningsendelser, så "hunden" finder symbolet "hund". Listen dækker de regelmæssige
     * navneords- og tillægsordsformer; uregelmæssige former ("bøger" af "bog") fanges ikke, og
     * det er med vilje: alternativet er en ordbog, og den hører ikke hjemme i en drømmeapp.
     *
     * Både -e-rækken (hund, hunden, hunde, hundene) og -r-rækken (flaske, flasken, flasker,
     * flaskerne) skal med, for de er begge helt almindelige.
     */
    private static final Set<String> BOEJNINGER = Set.of(
            "e", "en", "et", "er", "ene", "erne", "t", "te",
            "n", "r", "rne",
            "s", "es", "ns", "rs", "ens", "ets", "ers", "enes", "ernes", "rnes");

    /**
     * Under denne længde kræves et præcist ordtræf. Ellers ville "far" blive fundet i "farer",
     * og korte ord er så almindelige at listen ville drukne i støj. Tre tegn er grænsen og ikke
     * fire, fordi hus, bil, sol, kat og ild er helt centrale drømmesymboler, og de ville alle
     * miste deres bøjede former ved fire.
     *
     * Falske træf er ikke gratis, men de er billige: dialogen viser alt til godkendelse, så
     * prisen for et forkert forslag er ét klik, mens prisen for et overset symbol er at
     * funktionen ikke gør sit arbejde.
     */
    private static final int MINDSTE_LAENGDE_FOR_BOEJNING = 3;

    /**
     * Danske ord uden indholdsværdi. To steder gav de ren støj mod rigtige drømme:
     *
     * Symbolet "and" plus endelsen -en eller -et bliver til "anden" og "andet" - altså "den
     * anden", ikke en fugl. Det ramte fem ud af tolv drømme.
     *
     * Og symbolerne "det ukendte" og "det nye ukendte" begynder med "det", så gættet på
     * fornavne (se find) spurgte "hvad menes der med det?" i elleve ud af tolv drømme.
     *
     * Listen bruges kun de to steder: på et BØJET træf, og på gættet. Står symbolet
     * præcist i teksten, står det der, og så skal det med uanset hvad ordet ellers kan betyde.
     */
    private static final Set<String> STOPORD = Set.of(
            "og", "i", "jeg", "det", "at", "en", "den", "til", "er", "som", "paa", "på", "de",
            "med", "han", "af", "for", "ikke", "der", "var", "mig", "sig", "men", "et", "har",
            "om", "vi", "min", "havde", "ham", "hun", "nu", "over", "da", "fra", "du", "ud",
            "sin", "dem", "os", "op", "man", "hans", "hvor", "eller", "hvad", "skal", "selv",
            "her", "alle", "vil", "blev", "kunne", "ind", "naar", "når", "vaere", "være",
            "dog", "noget", "ville", "jo", "deres", "efter", "ned", "skulle", "denne", "end",
            "dette", "mit", "ogsaa", "også", "under", "have", "dig", "anden", "andet", "andre",
            "hende", "mine", "alt", "meget", "sit", "sine", "mod", "disse", "hvis", "din",
            "nogle", "hos", "blive", "mange", "bliver", "hendes", "vaeret", "været", "jer",
            "saadan", "sådan", "lige", "bare", "helt", "igen", "stadig", "saa", "så", "ja", "nej");

    /**
     * @param tekst              drømmeteksten som den står i skrivefeltet
     * @param symbolerPrKategori kategori-id til kategoriens symbolnavne. Brug et kort med fast
     *                           rækkefølge (LinkedHashMap), så to kald på samme data giver
     *                           samme svar i samme orden.
     */
    public static Resultat find(String tekst, Map<String, Collection<String>> symbolerPrKategori) {
        if (tekst == null || tekst.isBlank() || symbolerPrKategori == null || symbolerPrKategori.isEmpty()) {
            return INTET;
        }
        List<String> ord = delIOrd(tekst);
        if (ord.isEmpty()) {
            return INTET;
        }

        // Præcise træf: symbolet står i teksten, eventuelt bøjet. Nøglen er det ord teksten
        // faktisk bruger - det er dét brugeren skal se, når vi spørger.
        LinkedHashMap<String, LinkedHashSet<Fund>> praecise = new LinkedHashMap<>();

        // Ord hvor et flerordssymbol allerede er fundet i sin helhed. Står "Peter Jensen" i
        // teksten, skal vi ikke bagefter spørge hvilken Peter der menes.
        Set<String> daekketAfFrase = new HashSet<>();

        for (Map.Entry<String, Collection<String>> post : symbolerPrKategori.entrySet()) {
            if (post.getValue() == null) {
                continue;
            }
            for (String symbol : post.getValue()) {
                if (symbol == null || symbol.isBlank()) {
                    continue;
                }
                String fundetSom = findITekst(ord, symbol);
                if (fundetSom == null) {
                    continue;
                }
                praecise.computeIfAbsent(fundetSom, k -> new LinkedHashSet<>())
                        .add(new Fund(post.getKey(), symbol, fundetSom));

                String[] symbolOrd = smaat(symbol).split(" ");
                if (symbolOrd.length > 1) {
                    daekketAfFrase.add(symbolOrd[0]);
                }
            }
        }

        // Gæt: teksten siger "Peter", og registret rummer "Peter Jensen", "Peter Høeg" og
        // "Peter Peter". Ingen af dem står i teksten, så intet af det ovenfor fandt dem - og
        // netop dét er tilfældet hvor appen ikke kan vide hvem der menes.
        //
        // Bemærk at der ikke KAN ligge tre symboler der alle hedder præcis "Peter" i samme
        // kategori: symbolerne er et TreeSet<String>, så navnet er unikt. Tvetydigheden er
        // altid det korte ord over for de længere navne.
        LinkedHashMap<String, LinkedHashSet<Fund>> gaet = new LinkedHashMap<>();

        for (String ordet : new LinkedHashSet<>(ord)) {
            if (ordet.length() < MINDSTE_LAENGDE_FOR_BOEJNING
                    || STOPORD.contains(ordet)
                    || daekketAfFrase.contains(ordet)) {
                continue;
            }
            for (Map.Entry<String, Collection<String>> post : symbolerPrKategori.entrySet()) {
                if (post.getValue() == null) {
                    continue;
                }
                for (String symbol : post.getValue()) {
                    if (symbol == null || symbol.isBlank()) {
                        continue;
                    }
                    String small = smaat(symbol);
                    int mellemrum = small.indexOf(' ');
                    if (mellemrum < 0) {
                        continue; // ét-ords-symboler er afgjort ovenfor
                    }
                    if (small.substring(0, mellemrum).equals(ordet)) {
                        gaet.computeIfAbsent(ordet, k -> new LinkedHashSet<>())
                                .add(new Fund(post.getKey(), symbol, ordet));
                    }
                }
            }
        }

        return sammenstil(praecise, gaet);
    }

    /**
     * Skiller de to bunker i "det her tør vi sætte" og "det her må brugeren afgøre".
     *
     * Et ord er sikkert når præcis ét symbol matchede det præcist, og der ikke samtidig ligger
     * et længere navn og skygger. Alt andet - samme navn i to kategorier, eller et ord der kan
     * være begyndelsen på flere symboler - er et spørgsmål.
     */
    private static Resultat sammenstil(Map<String, LinkedHashSet<Fund>> praecise,
                                       Map<String, LinkedHashSet<Fund>> gaet) {
        LinkedHashSet<String> alleOrd = new LinkedHashSet<>(praecise.keySet());
        alleOrd.addAll(gaet.keySet());

        List<Fund> sikre = new ArrayList<>();
        List<Tvetydighed> tvetydige = new ArrayList<>();
        Set<String> sikreNoegler = new HashSet<>();

        for (String ordet : alleOrd) {
            Set<Fund> praeciseHer = praecise.getOrDefault(ordet, new LinkedHashSet<>());
            Set<Fund> gaetHer = gaet.getOrDefault(ordet, new LinkedHashSet<>());

            if (gaetHer.isEmpty() && praeciseHer.size() == 1) {
                Fund fund = praeciseHer.iterator().next();
                // Samme symbol kan være fundet via to ord ("hund" og "hunden"). Det skal kun
                // stå én gang på listen.
                if (sikreNoegler.add(noegle(fund))) {
                    sikre.add(fund);
                }
                continue;
            }

            List<Fund> kandidater = new ArrayList<>(praeciseHer);
            kandidater.addAll(gaetHer);
            kandidater.sort(Comparator.comparing(Fund::kategoriId).thenComparing(Fund::symbol));
            tvetydige.add(new Tvetydighed(ordet, List.copyOf(kandidater)));
        }

        // Er et symbol allerede sikkert fra et andet ord, skal brugeren ikke spørges om det
        // igen. Bliver der ingen kandidater tilbage, er der intet spørgsmål.
        List<Tvetydighed> tilbage = new ArrayList<>();
        for (Tvetydighed tvivl : tvetydige) {
            List<Fund> kandidater = new ArrayList<>();
            for (Fund fund : tvivl.kandidater()) {
                if (!sikreNoegler.contains(noegle(fund))) {
                    kandidater.add(fund);
                }
            }
            if (!kandidater.isEmpty()) {
                tilbage.add(new Tvetydighed(tvivl.ord(), List.copyOf(kandidater)));
            }
        }

        sikre.sort(Comparator.comparing(Fund::kategoriId).thenComparing(Fund::symbol));
        return new Resultat(List.copyOf(sikre), List.copyOf(tilbage));
    }

    private static String noegle(Fund fund) {
        return fund.kategoriId() + " " + fund.symbol();
    }

    /**
     * Det sted i teksten hvor symbolet står, skrevet som teksten skriver det - eller null.
     *
     * Flerordssymboler matches som ordfølge og ikke med en regex: \b i Java bygger på [a-zA-Z0-9_],
     * så et ordskel lige før eller efter æ, ø og å opfører sig ikke som man tror, og "Peter Høeg"
     * ville falde på gulvet. Kun symbolets SIDSTE ord må være bøjet ("peter jensens", ikke
     * "peters jensen").
     */
    private static String findITekst(List<String> ord, String symbol) {
        String[] symbolOrd = smaat(symbol).split(" ");
        if (symbolOrd.length == 0 || symbolOrd[0].isEmpty()) {
            return null;
        }
        for (int i = 0; i + symbolOrd.length <= ord.size(); i++) {
            boolean traef = true;
            for (int j = 0; j < symbolOrd.length - 1; j++) {
                if (!ord.get(i + j).equals(symbolOrd[j])) {
                    traef = false;
                    break;
                }
            }
            if (!traef) {
                continue;
            }
            int sidste = symbolOrd.length - 1;
            if (!matcherMedBoejning(ord.get(i + sidste), symbolOrd[sidste])) {
                continue;
            }
            StringBuilder fundet = new StringBuilder();
            for (int j = 0; j < symbolOrd.length; j++) {
                if (j > 0) {
                    fundet.append(' ');
                }
                fundet.append(ord.get(i + j));
            }
            return fundet.toString();
        }
        return null;
    }

    private static boolean matcherMedBoejning(String ordet, String symbolOrdet) {
        if (ordet.equals(symbolOrdet)) {
            return true;
        }
        if (symbolOrdet.length() < MINDSTE_LAENGDE_FOR_BOEJNING || !ordet.startsWith(symbolOrdet)) {
            return false;
        }
        // "anden" og "andet" er ikke boejninger af fuglen and, uanset at de ser sådan ud.
        if (STOPORD.contains(ordet)) {
            return false;
        }
        return BOEJNINGER.contains(ordet.substring(symbolOrdet.length()));
    }

    /**
     * Teksten delt i ord, med små bogstaver.
     *
     * Æ, ø og å bevares. Kategoriid.tilSlug folder dem til ae/oe/aa med vilje, så "Får" og "Far"
     * ikke ender som samme id - men præcis den foldning ville her slå de to symboler sammen, og
     * så ville en drøm om en far få markeret et får. Derfor må tilSlug ikke bruges her.
     */
    private static List<String> delIOrd(String tekst) {
        List<String> ud = new ArrayList<>();
        for (String bid : tekst.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (!bid.isEmpty()) {
                ud.add(bid);
            }
        }
        return ud;
    }

    /** Symbolnavnet som det skal sammenlignes: trimmet, ét mellemrum mellem ord, småt. */
    private static String smaat(String tekst) {
        // NB: "\\s+", ikke "\s+". Sidstnævnte kompilerer siden Java 15 som ét mellemrum og ville
        // ramme mellemrum men ikke tabulator - samme fælde som i Tag.normaliser.
        return tekst.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
