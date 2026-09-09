package fixit.dreams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

// Ukendte felter ignoreres - men KUN her, ikke på IOutils' mapper som helhed. Forskellen er
// bevidst: strengheden på drømmene er en sikring, der får umigreret data til at fejle højlydt
// frem for at blive læst forkert og gemt tilbage i stumper. Indstillinger har intet at tabe på
// den måde, og til gengæld alt at vinde: møder en maskine en user.json skrevet af en nyere
// udgave, springer den det ene felt over den ikke kender, i stedet for at afvise hele filen og
// falde tilbage til standardtema og nulstillede flag. Den skal ligge i den udgave der en dag
// bliver den gamle - derfor nu, hvor der endnu ikke er noget den skal redde.
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDTO {
    public String foretrukneTema;
    public boolean visAdvarsel;
    public boolean visKollektiv;
    public boolean visHolografisk;
    public LocalDate startFromThisDate;

    // Additivt felt, nyt i 2.1: er datoen ovenfor brugerens eget valg, eller appens udregning?
    // Boolean og ikke boolean, fordi der er tre svar: ja, nej, og "filen er ældre end feltet".
    // Filer og sky-dokumenter fra før 2.1 mangler det, og læses som ja (se Startdato.valgtAfBruger).
    // Er startFromThisDate null, er der ingen dato at have en mening om.
    public Boolean startDatoValgtAfBruger;

    public int schemaVersion;

    public UserDTO() {}

    public UserDTO(User user) {
        this.foretrukneTema = user.getForetrukneTemaNavn();
        this.visAdvarsel = user.isVisAdvarsel();
        this.visKollektiv = user.isVisKollektiv();
        this.visHolografisk = user.isVisHolografisk();
        // Kun et VALGT tidspunkt skrives. Har brugeren ikke valgt noget, gemmes null, og datoen
        // udledes forfra ved næste indlæsning - så en frisk maskine, der får hele samlingen ned
        // fra skyen, ikke sidder fast på den dato den havde inden drømmene ankom.
        this.startFromThisDate = user.harValgtStartdato() ? user.getStartFromThisDate() : null;
        this.startDatoValgtAfBruger = user.harValgtStartdato();
        this.schemaVersion = SchemaMigrator.CURRENT_SCHEMA_VERSION;

    }

    public User toUser() {
        User user = User.getInstance();
        user.setForetrukneTema(this.foretrukneTema);
        user.setVisAdvarsel(this.visAdvarsel);
        user.setVisKollektiv(this.visKollektiv);
        user.setVisHolografisk(this.visHolografisk);
        user.setStartFromThisDate(this.startFromThisDate,
                Startdato.valgtAfBruger(this.startFromThisDate, this.startDatoValgtAfBruger));
        return user;
    }

}
