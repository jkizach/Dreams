package fixit.dreams;

import java.util.Locale;

/**
 * Et tag er et frit ord brugeren selv finder på, og teknisk set bare et symbol i én indbygget
 * kategori. Derfor dukker tags op i Cirkel, Graf, Tal og Filtre uden en linje ny kode: alt
 * nedstrøms er generisk over kategori-id.
 *
 * Reglerne for hvordan et skrevet ord bliver til et tag ligger her - uden JavaFX, uden User og
 * uden filer, i samme form som Startdato og Periode, så de kan testes uden toolkit.
 *
 * Hele pointen med normaliseringen er at "Hav", " hav " og "#hav" skal ende som ét tag og ikke
 * som tre. Fri tekst har præcis én reel omkostning - tastefejl - og den betales to steder:
 * her, hvor de harmløse varianter samles op, og i Indstillinger, hvor to tags kan flettes.
 */
public final class Tag {

    private Tag() {}

    public static final String NAVN = "Tags";

    /**
     * "tags". Id'et kommer fra forIndbygget og IKKE fra forNy: forIndbygget er en ren slug uden
     * hash, så to maskiner udleder uafhængigt det samme id og genkender hinandens tagkategori
     * som den samme. forNy er bygget til det stik modsatte og ville give to forskellige id'er.
     */
    public static final String ID = Kategoriid.forIndbygget(NAVN);

    /**
     * Længere end dette tages der ikke imod i tagfeltet. Et lydløst afkortet tag ville lave en
     * dublet man ikke kan se forskel på, så feltet afviser i stedet - se Tagfelt.
     * Tags der kommer ind fra skyen røres ikke; de er data, ikke inddata.
     */
    public static final int MAKS_LAENGDE = 40;

    /**
     * Teksten som den skal stå i data - eller null hvis der ikke er noget tag i den.
     *
     * Mellemrum er tilladt INDE i et tag ("gamle huse"). Det er ikke en hashtag, og Enter
     * afslutter tagget, så der er ingen tvetydighed at tage hensyn til.
     */
    public static String normaliser(String raa) {
        if (raa == null) {
            return null;
        }
        String tekst = raa.trim();

        // Ledende # ryger. Brugerens egen instinktive skrivemåde skal ikke give tagget "#hav"
        // ved siden af "hav" - det er den samme tanke skrevet på to måder.
        while (tekst.startsWith("#")) {
            tekst = tekst.substring(1).trim();
        }

        // Indre mellemrum trykkes til ét: "gamle   huse" og "gamle huse" er det samme tag.
        // NB: regexen skal skrives "\\s+". "\s" er siden Java 15 et gyldigt escape for ét
        // mellemrum, så en enkelt backslash kompilerer fint og betyder noget helt andet -
        // den ville ramme mellemrum, men ikke tabulator.
        tekst = tekst.replaceAll("\\s+", " ").trim();

        // Små bogstaver - samme regel som UserService.addNytSymbol bruger for alle andre
        // kategorier end Personer, så "Hav" og "hav" ikke bliver to ting.
        tekst = tekst.toLowerCase(Locale.ROOT);

        return tekst.isEmpty() ? null : tekst;
    }

    /** Sandt hvis teksten overhovedet indeholder et tag. */
    public static boolean erEtTag(String raa) {
        return normaliser(raa) != null;
    }
}
