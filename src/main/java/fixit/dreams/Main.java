package fixit.dreams;

import java.util.Locale;

public class Main {
    public static void main(String[] args) {
        // "da" alene har intet LAND, og WeekFields slår ugens første dag op på landet - uden det
        // falder den tilbage til søndag. Med DK begynder ugen om mandagen, som en dansk kalender.
        Locale.setDefault(Locale.forLanguageTag("da-DK"));
        DreamApp.main(args);
    }
}
