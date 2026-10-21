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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * Ét ord i teksten som kan betyde flere forskellige symboler - brugeren må vælge.
     *
     * forvalgt er det svar dialogen står med fra start, eller null for "Ingen af dem". Det sættes
     * kun når ét af symbolerne står præcist i teksten, og de andre blot er længere navne der
     * begynder med ordet: teksten siger "Anna", registret har "Anna" og "Anna Fasting". Havde
     * brugeren ment Anna Fasting, havde der stået Anna Fasting.
     */
    public record Tvetydighed(String ord, List<Fund> kandidater, Fund forvalgt) {
        public Tvetydighed(String ord, List<Fund> kandidater) {
            this(ord, kandidater, null);
        }
    }

    /**
     * nyeNavne er navne i teksten som ikke står i registret endnu - kandidater til at blive
     * oprettet som nye symboler i Personer. Se findNyeNavne.
     *
     * kvaliteter er de binære flueben (Lucid, Praktiserer...) fundet af Kvalitetsgenkendelse.
     * De lægges på med medKvaliteter, for de findes efter helt andre regler end symbolerne.
     */
    public record Resultat(List<Fund> sikre, List<Tvetydighed> tvetydige, List<Fund> nyeNavne,
                           List<Fund> kvaliteter) {
        public Resultat(List<Fund> sikre, List<Tvetydighed> tvetydige, List<Fund> nyeNavne) {
            this(sikre, tvetydige, nyeNavne, List.of());
        }

        public Resultat(List<Fund> sikre, List<Tvetydighed> tvetydige) {
            this(sikre, tvetydige, List.of());
        }

        public Resultat medKvaliteter(List<Fund> kvaliteter) {
            return new Resultat(sikre, tvetydige, nyeNavne, List.copyOf(kvaliteter));
        }

        public boolean erTom() {
            return sikre.isEmpty() && tvetydige.isEmpty() && nyeNavne.isEmpty() && kvaliteter.isEmpty();
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
     * "sker" er med af samme grund som "anden": -r er en gyldig endelse, men flertal af ske er
     * skeer, så "det sker" er aldrig en ske.
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
            "saadan", "sådan", "lige", "bare", "helt", "igen", "stadig", "saa", "så", "ja", "nej",
            "sker");

    /**
     * Navneord der i præcis disse former også er et almindeligt udsagnsord: "jeg får en gave",
     * "jeg tog hjem", "det sker tit". De former tæller kun som symbolet med et kendeord eller
     * talord foran (se erVerbumHer). De entydige former - fåret, fårene, toget, skeen - tæller
     * altid, for dem kan intet udsagnsord ligne.
     *
     * Nøglen er symbolet, værdien de tvetydige tekstformer.
     */
    private static final Map<String, Set<String>> HOMOGRAFER = Map.of(
            "får", Set.of("får"),
            "tog", Set.of("tog"),
            "ske", Set.of("ske"),
            "fly", Set.of("fly"),
            "bad", Set.of("bad"),
            "sang", Set.of("sang"));

    /** Ord der kan stå lige foran et navneord, men ikke foran et udsagnsord: "et får", "mit tog". */
    private static final Set<String> BESTEMMERE = Set.of(
            "en", "et", "min", "mit", "mine", "din", "dit", "dine", "sin", "sit", "sine",
            "hans", "hendes", "vores", "jeres", "deres", "denne", "dette", "disse", "hvert",
            "intet", "par", "to", "tre", "fire", "fem", "seks", "syv", "otte", "ni", "ti");

    private static final Set<String> TALORD = Set.of(
            "to", "tre", "fire", "fem", "seks", "syv", "otte", "ni", "ti", "mange", "få");

    /**
     * Ord der kun tæller med et ord imellem - et tillægsord eller talord: "det store får", "de
     * to får", "alle de får". Lige foran er de næsten altid et grundled: "det sker tit", "det
     * tog lang tid", "de får lov", "alle får en gave". Bestemt form uden tillægsord skrives på
     * dansk med endelse ("fåret"), og den finder vi i forvejen.
     */
    private static final Set<String> BESTEMMERE_MED_ORD_IMELLEM = Set.of(
            "den", "det", "de", "alle", "mange", "nogle", "flere", "ingen", "hver");

    /**
     * Ord der ofte indleder en sætning, og som derfor står med stort lige foran et navn uden selv
     * at være en del af det: "Pludselig Atta kom ind" skal foreslå Atta, ikke "Pludselig Atta".
     * Stopordene ovenfor gør samme tjeneste for "Så", "Da", "Men" og resten af de korte.
     */
    private static final Set<String> SAETNINGSINDLEDERE = Set.of(
            "pludselig", "bagefter", "senere", "først", "derefter", "dernæst", "endelig",
            "imens", "mens", "måske", "desuden", "alligevel", "samtidig", "straks", "derpå",
            "inden", "før", "siden", "hvorfor", "hvordan", "hvem", "hvornår", "hej", "nogen",
            "ingen", "nogle", "tilsidst", "herefter", "derfor", "dengang", "engang", "omkring");

    /** "Fru Jensen" og "Onkel Ib": titlen hører ikke med i symbolet, kun navnet. */
    private static final Set<String> TITLER = Set.of(
            "fru", "frøken", "herr", "onkel", "tante", "mormor", "morfar", "farmor", "farfar",
            "dronning", "kong", "prins", "prinsesse", "præst", "pastor", "doktor", "professor");

    /** Tegn der afslutter en sætning eller åbner en replik - ordet efter står med stort uanset hvad. */
    private static final String SAETNINGSSKEL = ".!?:;\"“”„«»\n\r–—";

    /** Et ord med bindestreg eller apostrof hænger sammen: Anne-Marie, O'Connor. */
    private static final Pattern ORD_MED_FORM = Pattern.compile("\\p{L}+(?:[-'’]\\p{L}+)*");

    /** Længere end det er det sjældent et navn, men en overskrift eller en titel skrevet med stort. */
    private static final int LAENGSTE_NAVN = 4;

    /** "Nr. Snede", "Skt. Hans", "Hr. Hansen" - forkortelsen er ikke en del af noget navn. */
    private static final Set<String> FORKORTELSER = Set.of(
            "nr", "skt", "sct", "st", "hr", "fr", "frk", "dr", "gl", "kbh", "ca", "mr", "mrs",
            "ms", "jr", "sr", "prof");

    /**
     * Endelser der afslører et sted frem for en person: Solbakkevej, Oremandsgaard, Porthuset,
     * Vækstcenteret. Gælder kun når ordet er længere end endelsen - "Vej" alene er ikke et sted.
     */
    private static final List<String> STEDSENDELSER = List.of(
            "vej", "vejen", "gade", "gaden", "gaard", "gaards", "gård", "gårds", "centeret",
            "center", "huset", "parken", "kirke", "kirken", "torv", "torvet", "plads", "pladsen",
            "stræde", "skov", "skoven", "bakken", "havn", "havnen", "instituttet", "skolen",
            "retreatet", "paladset", "bjerget", "fjorden", "bugten");

    /**
     * De lande, landsdele og byer der oftest står i en dansk tekst. Listen er ikke udtømmende og
     * skal ikke være det: den tager de hyppigste, og resten klarer brugeren med ét klik, fordi
     * nye navne aldrig står afkrydset fra start.
     */
    private static final Set<String> STEDER = Set.of(
            "danmark", "sverige", "norge", "island", "grønland", "færøerne", "finland",
            "tyskland", "england", "skotland", "irland", "frankrig", "italien", "spanien",
            "portugal", "holland", "belgien", "schweiz", "østrig", "polen", "tjekkiet", "ungarn",
            "grækenland", "tyrkiet", "rusland", "ukraine", "usa", "amerika", "canada", "mexico",
            "brasilien", "argentina", "kina", "japan", "indien", "irak", "iran", "israel",
            "palæstina", "gaza", "egypten", "afrika", "europa", "asien", "australien",
            "jylland", "sjælland", "fyn", "bornholm", "midtjylland", "nordjylland",
            "sønderjylland", "vestjylland", "østjylland",
            "københavn", "aarhus", "århus", "odense", "aalborg", "ålborg", "esbjerg", "vejle",
            "silkeborg", "randers", "horsens", "kolding", "roskilde", "herning", "viborg",
            "london", "paris", "berlin", "rom", "prag", "stockholm", "oslo", "moskva",
            "new york", "jorden", "månen");

    /**
     * @param tekst              drømmeteksten som den står i skrivefeltet
     * @param symbolerPrKategori kategori-id til kategoriens symbolnavne. Brug et kort med fast
     *                           rækkefølge (LinkedHashMap), så to kald på samme data giver
     *                           samme svar i samme orden.
     */
    public static Resultat find(String tekst, Map<String, Collection<String>> symbolerPrKategori) {
        return find(tekst, symbolerPrKategori, null, Set.of());
    }

    public static Resultat find(String tekst, Map<String, Collection<String>> symbolerPrKategori,
                                String personKategoriId) {
        return find(tekst, symbolerPrKategori, personKategoriId, Set.of());
    }

    /** Et navn som det står i listen over ikke-navne: trimmet, ét mellemrum mellem ord, småt. */
    public static String ikkeNavnNoegle(String navn) {
        return smaat(navn);
    }

    /**
     * Som find(tekst, symbolerPrKategori), men foreslår desuden navne fra teksten som nye
     * symboler i personkategorien. Er personKategoriId null, eller findes kategorien ikke i
     * registret (brugeren kan have slettet den), foreslås ingen nye navne.
     *
     * @param ikkeNavne navne brugeren tidligere har sagt nej til, skrevet som smaat() skriver
     *                  dem. De foreslås ikke igen - se UserDTO.ikkeNavne.
     */
    public static Resultat find(String tekst, Map<String, Collection<String>> symbolerPrKategori,
                                String personKategoriId, Set<String> ikkeNavne) {
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

        if (personKategoriId == null || !symbolerPrKategori.containsKey(personKategoriId)) {
            return sammenstil(praecise, gaet);
        }

        List<Fund> nye = new ArrayList<>();
        for (String navn : findNyeNavne(tekst, symbolerPrKategori.values(), ikkeNavne)) {
            nye.add(new Fund(personKategoriId, navn, navn));
            // Står der "Anna Smith", er det ikke Anna Hansen eller Anna Berg - så skal appen
            // heller ikke spørge hvilken af dem der menes.
            String[] dele = smaat(navn).split(" ");
            if (dele.length > 1) {
                gaet.remove(dele[0]);
            }
        }

        Resultat r = sammenstil(praecise, gaet);
        return new Resultat(r.sikre(), r.tvetydige(), List.copyOf(nye));
    }

    /**
     * Navne i teksten som ikke står i registret: ord med stort begyndelsesbogstav midt i en
     * sætning, og følger af dem ("Atta Bahati").
     *
     * Det holder på dansk, fordi navneord skrives med lille - et ord med stort midt i sætningen
     * er næsten altid et egennavn. Første ord i en sætning står med stort uanset hvad, så dér
     * gælder et enkelt ord ikke; en følge af to eller flere gør, når indledende småord som
     * "Pludselig" og "Så" er skrællet af.
     *
     * Resten skal falde fra:
     *  - ord der også står med lille et andet sted i teksten - så er det et almindeligt ord;
     *  - et enkeltord der er et ord fra et symbol i registret, også bøjet: "Annas" når Anna
     *    findes, "Hage" når Anna Hage findes. Det har dialogen allerede sat eller spurgt om;
     *  - et længere navn der ER et symbol, eller som rummer et kendt etordsnavn ("Peter Pan" når
     *    Peter findes) - men IKKE et navn der blot begynder som et kendt: "Anna Smith" er netop
     *    ikke Anna Hansen;
     *  - ord helt med store bogstaver - det er råb, ikke navne - og forkortelser som "Nr." og
     *    "Skt.";
     *  - sammensætninger med bindestreg og lille bogstav efter: "DNA-strimlen", "Ecco-sko";
     *  - veje, gårde og bygninger kendt på endelsen, og de mest almindelige lande og byer;
     *  - et enkeltord der også står som del af et længere navn i samme tekst, og en genitiv
     *    ("Attas") når navnet også står uden -s.
     *
     * Listen blev til ved en prøvekørsel mod de rigtige drømme, hvor de fem sidste punkter stod
     * for størstedelen af støjen. Hvad der så bliver tilbage, kan stadig være et sted eller en
     * ting ("Lovns", "Minecraft") - og det er derfor dialogen viser forslagene uden flueben.
     */
    private static List<String> findNyeNavne(String tekst, Collection<Collection<String>> registret,
                                             Set<String> ikkeNavne) {
        // Symbolerne som de skal sammenlignes. En parentes er brugerens egen præcisering -
        // "Allan (skolen)" - og står aldrig i drømmeteksten.
        Set<String> kendteSymboler = new HashSet<>();
        Set<String> kendteEtordsnavne = new HashSet<>();
        Set<String> kendteOrd = new HashSet<>();
        for (Collection<String> symboler : registret) {
            if (symboler == null) {
                continue;
            }
            for (String symbol : symboler) {
                if (symbol == null) {
                    continue;
                }
                String uden = smaat(symbol.replaceAll("\\(.*?\\)", " "));
                if (uden.isEmpty()) {
                    continue;
                }
                kendteSymboler.add(uden);
                String[] dele = uden.split(" ");
                if (dele.length == 1) {
                    kendteEtordsnavne.add(uden);
                }
                kendteOrd.addAll(List.of(dele));
            }
        }

        record Ord(String tekst, boolean saetningsstart, boolean haengerPaaForrige) {}

        List<Ord> alle = new ArrayList<>();
        Set<String> medLille = new HashSet<>();
        Matcher m = ORD_MED_FORM.matcher(tekst);
        int forrigeSlut = -1;
        while (m.find()) {
            String mellem = forrigeSlut < 0 ? "" : tekst.substring(forrigeSlut, m.start());
            boolean start = forrigeSlut < 0 || indeholderSkel(mellem);
            boolean haenger = forrigeSlut >= 0 && mellem.isBlank() && !start;
            alle.add(new Ord(m.group(), start, haenger));
            if (Character.isLowerCase(m.group().charAt(0))) {
                medLille.add(m.group().toLowerCase(Locale.ROOT));
            }
            forrigeSlut = m.end();
        }

        // Følger af ord med stort, brudt af alt andet end mellemrum.
        List<List<Ord>> foelger = new ArrayList<>();
        List<Ord> igang = new ArrayList<>();
        for (Ord ord : alle) {
            boolean navneagtigt = erNavneagtigt(ord.tekst());
            if (navneagtigt && ord.haengerPaaForrige() && !igang.isEmpty()) {
                igang.add(ord);
                continue;
            }
            if (!igang.isEmpty()) {
                foelger.add(igang);
            }
            igang = new ArrayList<>();
            if (navneagtigt) {
                igang.add(ord);
            }
        }
        if (!igang.isEmpty()) {
            foelger.add(igang);
        }

        LinkedHashMap<String, String> navne = new LinkedHashMap<>();
        for (List<Ord> foelge : foelger) {
            List<Ord> rest = new ArrayList<>(foelge);
            while (!rest.isEmpty()) {
                String smaa = rest.get(0).tekst().toLowerCase(Locale.ROOT);
                if (STOPORD.contains(smaa) || SAETNINGSINDLEDERE.contains(smaa) || TITLER.contains(smaa)
                        || medLille.contains(smaa)) {
                    rest.remove(0);
                } else {
                    break;
                }
            }
            if (rest.isEmpty() || rest.size() > LAENGSTE_NAVN) {
                continue;
            }
            // Et skrællet ord efterlader et navn der ikke længere står først i sætningen - så
            // er saetningsstart på det første tilbageværende ord falsk, som det skal være.
            if (rest.size() == 1 && rest.get(0).saetningsstart()) {
                continue;
            }

            List<String> ordene = new ArrayList<>();
            for (Ord ord : rest) {
                // "Jes's" og "Anders's" - genitiv med apostrof er entydig og kan skrælles af.
                ordene.add(ord.tekst().replaceAll("['’]s$", ""));
            }
            String navn = String.join(" ", ordene);
            String smaat = smaat(navn);
            String[] dele = smaat.split(" ");

            boolean optaget = false;
            for (String del : dele) {
                if (medLille.contains(del) || STOPORD.contains(del)) {
                    optaget = true;
                }
            }
            if (dele.length == 1) {
                // Genitiv-s gælder også korte navne, som bøjningsreglen ellers springer over: "Ibs".
                optaget |= matcherNogen(smaat, kendteOrd)
                        || (smaat.endsWith("s") && kendteOrd.contains(smaat.substring(0, smaat.length() - 1)));
            } else {
                optaget |= kendteSymboler.contains(smaat)
                        || (smaat.endsWith("s") && kendteSymboler.contains(smaat.substring(0, smaat.length() - 1)));
                for (String del : dele) {
                    optaget |= matcherNogen(del, kendteEtordsnavne);
                }
            }
            optaget |= erSted(smaat, dele[dele.length - 1]);
            optaget |= ikkeNavne != null && ikkeNavne.contains(smaat);
            if (!optaget) {
                navne.putIfAbsent(smaat, navn);
            }
        }

        Set<String> delAfLaengre = new HashSet<>();
        for (String smaat : navne.keySet()) {
            String[] dele = smaat.split(" ");
            if (dele.length > 1) {
                delAfLaengre.addAll(List.of(dele));
            }
        }
        List<String> ud = new ArrayList<>();
        for (Map.Entry<String, String> post : navne.entrySet()) {
            String smaat = post.getKey();
            if (!smaat.contains(" ") && delAfLaengre.contains(smaat)) {
                continue;
            }
            if (smaat.endsWith("s") && navne.containsKey(smaat.substring(0, smaat.length() - 1))) {
                continue;
            }
            // "Attas" ved siden af "Atta Bahati": genitiven af et navn der allerede er foreslået.
            if (smaat.endsWith("s") && delAfLaengre.contains(smaat.substring(0, smaat.length() - 1))) {
                continue;
            }
            ud.add(post.getValue());
        }
        return ud;
    }

    /** Stort begyndelsesbogstav, men ikke råb, ikke en forkortelse og ikke en sammensætning. */
    private static boolean erNavneagtigt(String ord) {
        if (ord.length() < 2 || !Character.isUpperCase(ord.charAt(0))
                || ord.equals(ord.toUpperCase(Locale.ROOT))
                || FORKORTELSER.contains(ord.toLowerCase(Locale.ROOT))) {
            return false;
        }
        // Anne-Marie er et navn, DNA-strimlen og Ecco-sko er ikke.
        for (String del : ord.split("[-'’]")) {
            if (del.length() > 1 && Character.isLowerCase(del.charAt(0))) {
                return false;
            }
        }
        return true;
    }

    private static boolean matcherNogen(String ordet, Set<String> kendte) {
        for (String kendt : kendte) {
            if (matcherMedBoejning(ordet, kendt)) {
                return true;
            }
        }
        return false;
    }

    private static boolean erSted(String smaat, String sidsteOrd) {
        if (STEDER.contains(smaat)) {
            return true;
        }
        for (String endelse : STEDSENDELSER) {
            if (sidsteOrd.length() > endelse.length() && sidsteOrd.endsWith(endelse)) {
                return true;
            }
        }
        return false;
    }

    private static boolean indeholderSkel(String mellem) {
        if (mellem.trim().equals("-")) {
            return true; // tankestreg skrevet som bindestreg med mellemrum om
        }
        for (int i = 0; i < mellem.length(); i++) {
            if (SAETNINGSSKEL.indexOf(mellem.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
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
            if (kandidater.isEmpty()) {
                continue;
            }
            // Kun når netop ét præcist træf er tilbage OG ordet også er starten på længere navne.
            // To præcise træf ("rød" i to kategorier) er ægte tvivl, og der forbliver "Ingen af
            // dem" svaret.
            List<Fund> praeciseTilbage = new ArrayList<>(praecise.getOrDefault(tvivl.ord(), new LinkedHashSet<>()));
            praeciseTilbage.retainAll(kandidater);
            Fund forvalgt = praeciseTilbage.size() == 1 && gaet.containsKey(tvivl.ord())
                    ? praeciseTilbage.get(0) : null;
            tilbage.add(new Tvetydighed(tvivl.ord(), List.copyOf(kandidater), forvalgt));
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
            if (symbolOrd.length == 1 && erVerbumHer(ord, i, symbolOrd[0])) {
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

    /**
     * Sand når ordet på plads i er et af de tvetydige udsagnsords-former i HOMOGRAFER, og der
     * ikke står et kendeord eller talord lige foran - eller to ord foran, så et tillægsord kan
     * stå imellem: "et får", "et stort får", "to får", men ikke "jeg får".
     */
    private static boolean erVerbumHer(List<String> ord, int i, String symbol) {
        Set<String> tvetydige = HOMOGRAFER.get(symbol);
        if (tvetydige == null || !tvetydige.contains(ord.get(i))) {
            return false;
        }
        if (i >= 1 && BESTEMMERE.contains(ord.get(i - 1))) {
            return false;
        }
        if (i >= 2 && (BESTEMMERE.contains(ord.get(i - 2)) || BESTEMMERE_MED_ORD_IMELLEM.contains(ord.get(i - 2)))) {
            // Ordet imellem skal kunne være et tillægsord: efter et kendeord ender det på -e
            // eller -t (store, stort, lille, hvide) - eller et talord ("de to får"). Ellers er
            // "en mand får en gave" et får. Tillægsord på andre endelser ("en gammel ske")
            // mister vi; dem sætter brugeren selv.
            String imellem = ord.get(i - 1);
            // Småord som "ikke", "andre" og "samme" ender også på -e, men er ingen tillægsord
            // her: "det ikke sker", "nogle andre får", "det samme sker" - så de tæller ikke.
            boolean tillaegsord = (imellem.endsWith("e") || imellem.endsWith("t"))
                    && !STOPORD.contains(imellem) && !imellem.equals("samme");
            if (tillaegsord || TALORD.contains(imellem) || BESTEMMERE_MED_ORD_IMELLEM.contains(imellem)) {
                return false;
            }
        }
        return true;
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
