package fixit.dreams;

import java.time.LocalDate;

// De to regler om startdatoen, samlet ét sted og uden noget omkring sig - ingen filer, intet
// netværk, ingen brugerflade. Begge blev tidligere skrevet ud i hånden hvert sted de skulle
// bruges (User, UserDTO, SyncService, HovedmenuController), og det var netop dér 2.0-fejlen
// kunne gemme sig: reglen om hvornår datoen må flyttes stod ikke skrevet nogen steder, den
// var underforstået i en if-sætning i en UI-metode.
public final class Startdato {
    private Startdato() {}

    // Er den dato vi har fået (fra user.json eller fra skyen) brugerens eget valg?
    //
    // Flaget mangler i alt der er skrevet før 2.1, og så antages et valg. At tage fejl den vej
    // lader i værste fald en udledt dato blive stående til brugeren selv retter den; den
    // modsatte antagelse ville flytte en dato brugeren havde valgt, og det er ikke til at
    // fortryde - det var præcis det 2.0 gjorde ved hver eneste opstart.
    //
    // Uden en dato er der ingenting at have en mening om, og svaret er nej.
    public static boolean valgtAfBruger(LocalDate dato, Boolean flag) {
        return dato != null && (flag == null || flag);
    }

    // Hvilken dato datofiltret skal stå på efter en sync.
    //
    // Normalt startdatoen. Men har syncen hentet drømme ned der er ældre end den, ville de falde
    // uden for filtret og listen se tom ud, selvom alt var kommet ned. Så åbnes VISNINGEN ned
    // til den ældste af dem - kun visningen, kun denne session. Startdatoen selv røres ikke.
    public static LocalDate filterFra(LocalDate startdato, LocalDate ældsteHentedeDrøm) {
        if (startdato == null) {
            return (ældsteHentedeDrøm != null) ? ældsteHentedeDrøm : LocalDate.now();
        }
        if (ældsteHentedeDrøm != null && ældsteHentedeDrøm.isBefore(startdato)) {
            return ældsteHentedeDrøm;
        }
        return startdato;
    }
}
