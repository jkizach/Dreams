package fixit.dreams;

import org.apache.lucene.analysis.hunspell.Dictionary;
import org.apache.lucene.analysis.hunspell.Hunspell;
import org.apache.lucene.store.ByteBuffersDirectory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dansk stavekontrol: Stavekontrolden's Hunspell-ordbog (da_DK.dic/.aff, se da_DK-LICENS.txt)
 * læst af Lucene's Hunspell-implementation. Ren Java og rigtige moduler hele vejen - det er
 * hele grunden til valget. Hunspell via JNA ville kræve native biblioteker pr. platform, og
 * RichTextFX (som kunne have tegnet stregerne for os) er automatiske moduler, som jlink ikke
 * vil have med. Stregerne tegnes derfor af Stavefelt oven på en almindelig TextArea.
 *
 * Hunspell frem for en ordliste med afstandsberegning, fordi dansk sætter ord sammen:
 * "sommerhusgrunden" og "drømmedagbog" står ikke i nogen liste, men affiksreglerne kender dem.
 *
 * Klassen er ren logik uden JavaFX, så den kan testes. Hunspell-objektet er ikke trådsikkert,
 * derfor er alle opslag synchronized.
 */
class Stavekontrol {

    /** Et forkert stavet ord: [start, slut) i teksten. */
    record Fejl(int start, int slut, String ord) {}

    /** Bogstaver, gerne bundet sammen af bindestreg eller apostrof: "tv-serie", "Gull-Maja". */
    private static final Pattern ORD = Pattern.compile("\\p{L}+(?:[-'’]\\p{L}+)*");

    private static final String ORDBOG_FIL = "egen-ordbog.txt";

    private static CompletableFuture<Stavekontrol> indlaesning;

    private final Hunspell hunspell;

    Stavekontrol(InputStream aff, InputStream dic) throws IOException, ParseException {
        Dictionary ordbog = new Dictionary(new ByteBuffersDirectory(), "da", aff, dic);
        this.hunspell = new Hunspell(ordbog);
    }

    /**
     * Ordbogen er knap 4 MB og tager et par sekunder at læse, så den læses én gang, i
     * baggrunden, og deles af alle felter. Kaldes første gang når Ny drøm bygges.
     */
    static synchronized CompletableFuture<Stavekontrol> indlaes() {
        if (indlaesning == null) {
            indlaesning = CompletableFuture.supplyAsync(() -> {
                try (InputStream aff = Stavekontrol.class.getResourceAsStream("/fixit/dreams/da_DK.aff");
                     InputStream dic = Stavekontrol.class.getResourceAsStream("/fixit/dreams/da_DK.dic")) {
                    return new Stavekontrol(aff, dic);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (ParseException e) {
                    throw new IllegalStateException("Ordbogen kunne ikke læses", e);
                }
            });
        }
        return indlaesning;
    }

    /**
     * Alle forkert stavede ord i teksten. egneOrd er brugerens egen ordbog plus symbolernes
     * navne, med små bogstaver - så "Anna Fasting" og "Gull-Maja" aldrig bliver markeret.
     */
    synchronized List<Fejl> find(String tekst, Set<String> egneOrd) {
        List<Fejl> fejl = new ArrayList<>();
        Matcher m = ORD.matcher(tekst);
        while (m.find()) {
            if (springOver(tekst, m.start(), m.end())) {
                continue;
            }
            String ord = m.group();
            if (!erKorrekt(ord, egneOrd, tekst, m.end())) {
                fejl.add(new Fejl(m.start(), m.end(), ord));
            }
        }
        return fejl;
    }

    synchronized List<String> forslag(String ord) {
        return hunspell.suggest(ord);
    }

    private boolean erKorrekt(String ord, Set<String> egneOrd, String tekst, int slut) {
        if (kendt(ord, egneOrd)) {
            return true;
        }
        // Forkortelser med punktum: ordbogen kender "osv." men ikke "osv".
        if (slut < tekst.length() && tekst.charAt(slut) == '.' && hunspell.spell(ord + ".")) {
            return true;
        }
        // Står hver del for sig, er sammensætningen god nok: "Gull-Maja" når begge er navne.
        if (ord.indexOf('-') > 0) {
            for (String del : ord.split("-")) {
                if (!kendt(del, egneOrd)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean kendt(String ord, Set<String> egneOrd) {
        return egneOrd.contains(ord.toLowerCase(Locale.ROOT)) || hunspell.spell(ord);
    }

    /**
     * Ord der ikke giver mening at tjekke: enkelte bogstaver, forkortelser i versaler (TV, DR),
     * og alt i en "klump" uden mellemrum med tal, @, / eller punktum midt i: 3-årig, kl.14,
     * www.dr.dk, f.eks., bl.a.
     */
    private static boolean springOver(String tekst, int start, int slut) {
        if (slut - start < 2) {
            return true;
        }
        int versaler = 0;
        for (int i = start; i < slut; i++) {
            if (Character.isUpperCase(tekst.charAt(i))) {
                versaler++;
            }
        }
        if (versaler > 1 && versaler == slut - start) {
            return true;
        }
        int kStart = start;
        while (kStart > 0 && !Character.isWhitespace(tekst.charAt(kStart - 1))) {
            kStart--;
        }
        int kSlut = slut;
        while (kSlut < tekst.length() && !Character.isWhitespace(tekst.charAt(kSlut))) {
            kSlut++;
        }
        for (int i = kStart; i < kSlut; i++) {
            char c = tekst.charAt(i);
            if (Character.isDigit(c) || c == '@' || c == '/' || c == '\\') {
                return true;
            }
            if (c == '.' && i > kStart && i + 1 < kSlut
                    && Character.isLetter(tekst.charAt(i - 1)) && Character.isLetter(tekst.charAt(i + 1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Flytter fejlene med, når teksten ændres, så stregerne ikke står forskudt det korte øjeblik
     * før næste tjek. Fejl før ændringen bliver stående, fejl efter rykkes, og fejl der rører
     * ved selve ændringen smides ud - det ord er ved at blive skrevet om.
     */
    static List<Fejl> forskyd(List<Fejl> fejl, String gammel, String ny) {
        int max = Math.min(gammel.length(), ny.length());
        int prefiks = 0;
        while (prefiks < max && gammel.charAt(prefiks) == ny.charAt(prefiks)) {
            prefiks++;
        }
        int suffiks = 0;
        while (suffiks < max - prefiks
                && gammel.charAt(gammel.length() - 1 - suffiks) == ny.charAt(ny.length() - 1 - suffiks)) {
            suffiks++;
        }
        int gammelSlut = gammel.length() - suffiks;
        int delta = ny.length() - gammel.length();

        List<Fejl> resultat = new ArrayList<>();
        for (Fejl f : fejl) {
            if (f.slut() < prefiks) {
                resultat.add(f);
            } else if (f.start() > gammelSlut) {
                resultat.add(new Fejl(f.start() + delta, f.slut() + delta, f.ord()));
            }
        }
        return resultat;
    }

    /** Ordene i symbolernes navne, med små bogstaver: "Anna Fasting" giver anna og fasting. */
    static Set<String> ordFraSymboler(Collection<Category> kategorier) {
        Set<String> ord = new TreeSet<>();
        for (Category kategori : kategorier) {
            for (String symbol : kategori.getSymbols()) {
                Matcher m = ORD.matcher(symbol);
                while (m.find()) {
                    String fundet = m.group().toLowerCase(Locale.ROOT);
                    ord.add(fundet);
                    for (String del : fundet.split("-")) {
                        ord.add(del);
                    }
                }
            }
        }
        return ord;
    }

    // ---- Brugerens egen ordbog: én linje pr. ord i datamappen. Kun på denne maskine. ----

    static Path egenOrdbogSti() {
        return AppPaths.APP_DATA_PATH.resolve(ORDBOG_FIL);
    }

    static Set<String> laesEgenOrdbog(Path fil) {
        Set<String> ord = new TreeSet<>();
        if (Files.exists(fil)) {
            try {
                for (String linje : Files.readAllLines(fil, StandardCharsets.UTF_8)) {
                    if (!linje.isBlank()) {
                        ord.add(linje.strip().toLowerCase(Locale.ROOT));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ord;
    }

    static void gemEgenOrdbog(Path fil, Set<String> ord) {
        try {
            Files.write(fil, new TreeSet<>(ord), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
