package fixit.dreams;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

class StavekontrolTest {

    private static Stavekontrol stavekontrol;

    @BeforeAll
    static void indlaesOrdbog() {
        stavekontrol = Stavekontrol.indlaes().join();
    }

    private static List<String> fejlord(String tekst) {
        return fejlord(tekst, Set.of());
    }

    private static List<String> fejlord(String tekst, Set<String> egneOrd) {
        return stavekontrol.find(tekst, egneOrd).stream().map(Stavekontrol.Fejl::ord).toList();
    }

    @Test
    void almindeligDanskTekstGiverIngenFejl() {
        assertEquals(List.of(), fejlord("Jeg drømte at jeg fløj over skovsøen, og så vågnede jeg."));
    }

    @Test
    void sammensatteOrdGodkendes() {
        assertEquals(List.of(), fejlord("sommerhusgrunden drømmedagbog togperronen kæmpeslange"));
    }

    @Test
    void stavefejlFindesMedPlacering() {
        List<Stavekontrol.Fejl> fejl = stavekontrol.find("Det var ikek sjovt", Set.of());
        assertEquals(List.of(new Stavekontrol.Fejl(8, 12, "ikek")), fejl);
    }

    @Test
    void storBogstavISaetningsstartErIkkeEnFejl() {
        assertEquals(List.of(), fejlord("Drømmen var lang. Mareridt bagefter."));
    }

    @Test
    void forslagIndeholderDetRigtigeOrd() {
        assertTrue(stavekontrol.forslag("ikek").contains("ikke"));
        assertTrue(stavekontrol.forslag("hvda").contains("hvad"));
    }

    @Test
    void egneOrdOgSymbolnavneGodkendes() {
        assertEquals(List.of("Fasting"), fejlord("Anna Fasting kom"));
        assertEquals(List.of(), fejlord("Anna Fasting kom", Set.of("fasting")));
    }

    @Test
    void bindestregsnavnGodkendesNaarHverDelErKendt() {
        assertEquals(List.of(), fejlord("Gull-Maja var der", Set.of("gull", "maja")));
    }

    @Test
    void talForkortelserOgAdresserSpringesOver() {
        assertEquals(List.of(), fejlord("en 3-årig pige kl.14 på www.dr.dk, f.eks. TV og DR, mail@xyz.dk"));
    }

    @Test
    void forkortelseMedPunktum() {
        assertEquals(List.of(), fejlord("hus, bil osv. og så videre"));
    }

    @Test
    void ordFraSymbolerDelerVedMellemrumOgBindestreg() {
        Category personer = new Category("Personer");
        personer.addSymbol("Anna Fasting");
        personer.addSymbol("Gull-Maja");
        assertEquals(new TreeSet<>(Set.of("anna", "fasting", "gull-maja", "gull", "maja")),
                Stavekontrol.ordFraSymboler(List.of(personer)));
    }

    @Test
    void forskydRykkerFejlEfterAendringen() {
        List<Stavekontrol.Fejl> fejl = List.of(new Stavekontrol.Fejl(8, 12, "ikek"));
        // Tekst indsat i starten. ("ikek" -> "ikke ikek" ville være tvetydigt: det ligner lige så
        // meget en rettelse inde i ordet, og så skal fejlen netop smides ud.)
        assertEquals(List.of(new Stavekontrol.Fejl(12, 16, "ikek")),
                Stavekontrol.forskyd(fejl, "Det var ikek", "Nå. Det var ikek"));
    }

    @Test
    void forskydBeholderFejlFoerAendringen() {
        List<Stavekontrol.Fejl> fejl = List.of(new Stavekontrol.Fejl(0, 4, "ikek"));
        assertEquals(fejl, Stavekontrol.forskyd(fejl, "ikek og", "ikek og så"));
    }

    @Test
    void forskydSmiderFejlUdDerRoeresVed() {
        List<Stavekontrol.Fejl> fejl = List.of(new Stavekontrol.Fejl(0, 3, "drø"));
        assertEquals(List.of(), Stavekontrol.forskyd(fejl, "drø", "drøm"));
        assertEquals(List.of(), Stavekontrol.forskyd(fejl, "drø", "dø"));
    }
}
