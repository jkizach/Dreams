package fixit.dreams;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Objects;

/**
 * Den periode Cirkel-fanen viser: en type - uge, måned, år eller frit interval - og de to
 * datoer den dækker. Datovælgerne på skærmen er facit for hvad cirklen viser; det her er
 * reglerne for hvad der må stå i dem.
 *
 * Perioden gør sig selv hel i konstruktøren: beder man om MÅNED med en dato midt i september,
 * får man 1.-30. september. Det betyder to ting, som resten af koden hviler på:
 *
 *  1) At gøre en hel periode hel igen ændrer ingenting. Derfor kan Cirkel-fanen bygge sin
 *     Periode op fra datovælgerne hver eneste gang den skal bruge den, uden at perioden
 *     skrider en dag for hver aflæsning - og så er der kun ét sted sandheden ligger.
 *  2) Der bliver flyttet FRA en hel periode. plusMonths på den 1. rammer altid den 1., mens
 *     plusMonths på den 31. januar rammer den 28. februar og aldrig finder tilbage til en
 *     31'er igen. Den fælde findes ikke her, fordi ankeret er gjort helt først.
 *
 * Klassen kender hverken User, filer eller brugerflade - den kan afprøves med to datoer og
 * en påstand (se PeriodeTest).
 */
public record Periode(Type type, LocalDate fra, LocalDate til) {

    /** De fire måder Cirkel-fanen kan afgrænse tid på. INTERVAL er brugerens egne datoer. */
    public enum Type { UGE, MÅNED, ÅR, INTERVAL }

    // Månedsnavnene står her frem for at blive slået op i Locale.getDefault(). Etiketten skal
    // hedde det samme uanset hvad maskinen er sat til, og en unittest skal kunne slå ordlyden
    // fast uden at rode med JVM'ens sprog undervejs - surefire sætter ikke sproget, det gør
    // kun Main, og den køres aldrig af en test.
    private static final String[] MÅNEDER = {
            "januar", "februar", "marts", "april", "maj", "juni",
            "juli", "august", "september", "oktober", "november", "december"};

    public Periode {
        Objects.requireNonNull(type, "En periode skal have en type");
        Objects.requireNonNull(fra, "En periode skal have en fra-dato");
        Objects.requireNonNull(til, "En periode skal have en til-dato");

        switch (type) {
            case UGE -> {
                // ISO-uge: mandag til søndag. previousOrSame frem for WeekFields, så ugens
                // første dag ikke afhænger af hvilket land maskinen står i.
                fra = fra.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                til = fra.plusDays(6);
            }
            case MÅNED -> {
                fra = fra.withDayOfMonth(1);
                til = fra.with(TemporalAdjusters.lastDayOfMonth()); // 28, 29, 30 eller 31
            }
            case ÅR -> {
                fra = fra.withDayOfYear(1);
                til = fra.with(TemporalAdjusters.lastDayOfYear());
            }
            case INTERVAL -> {
                // Brugerens egne datoer. De rettes ikke - heller ikke hvis Til ligger før Fra;
                // skærmen skal vise det der er valgt, ikke det vi tror der blev ment.
            }
        }
    }

    /** Den periode af den givne type som rummer datoen. */
    public static Periode omkring(Type type, LocalDate dato) {
        return new Periode(type, dato, dato);
    }

    public Periode forrige() {
        return skift(-1);
    }

    public Periode næste() {
        return skift(1);
    }

    private Periode skift(int retning) {
        return switch (type) {
            case UGE   -> omkring(type, fra.plusWeeks(retning));
            case MÅNED -> omkring(type, fra.plusMonths(retning));
            case ÅR    -> omkring(type, fra.plusYears(retning));
            // Et frit interval har ingen naturlig næste - så flytter vi vinduet med dets egen
            // længde: "de forrige 17 dage". Så er pilene aldrig døde, uanset hvad der er valgt.
            case INTERVAL -> new Periode(type,
                    fra.plusDays(retning * længde()),
                    til.plusDays(retning * længde()));
        };
    }

    /**
     * Antal dage i perioden, begge ender med - samme regel som ServiceMother.isInRange tæller
     * efter. Det er præcis det +1 der får 1.-17. september til at blive efterfulgt af 18.
     * september og ikke af den 17. igen; uden det ville én dags drømme blive talt med to gange
     * hver gang man klikkede videre, og det ville se helt rimeligt ud på skærmen.
     *
     * Ligger Til før Fra, er der ingen meningsfuld længde, og der flyttes én dag, så pilene
     * stadig virker og brugeren kan se sig ud af det igen.
     */
    private long længde() {
        return Math.max(1, ChronoUnit.DAYS.between(fra, til) + 1);
    }

    /** Den ene linje der står mellem pilene. */
    public String etiket() {
        return switch (type) {
            // Ugenummeret SKAL parres med det ugebaserede år, ikke med kalenderåret: mandag
            // den 29. december 2025 er uge 1 i 2026. Samme fælde som weekKey i Stats.
            case UGE -> "uge " + fra.get(WeekFields.ISO.weekOfWeekBasedYear())
                    + " · " + fra.get(WeekFields.ISO.weekBasedYear());
            case MÅNED -> MÅNEDER[fra.getMonthValue() - 1] + " " + fra.getYear();
            case ÅR -> String.valueOf(fra.getYear());
            // Datoerne står allerede i vælgerne lige ovenover, så at gentage dem ville være
            // støj. Længden derimod siger noget nyt: præcis hvor langt pilene flytter.
            case INTERVAL -> længde() == 1 ? "1 dag" : længde() + " dage";
        };
    }
}
