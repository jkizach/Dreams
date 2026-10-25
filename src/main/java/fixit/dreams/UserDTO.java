package fixit.dreams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    // Additivt felt, nyt i 2.2: navne brugeren har sagt nej til i symbolforslagsdialogen, så
    // de ikke foreslås som personer igen (se Symbolgenkendelse.find). Ligger her og ikke i en
    // fil for sig, så listen følger med i syncen som en af indstillingerne. Ingen skemaændring:
    // en ældre udgave springer feltet over (se @JsonIgnoreProperties ovenfor).
    // null betyder "filen eller sky-dokumentet er ældre end feltet" - IKKE "ingen nej'er". Så
    // beholder User den liste den har, i stedet for at tømme den (se User og SyncService).
    public List<String> ikkeNavne;

    // Additivt felt, nyt i 2.3: ord brugeren har føjet til stavekontrollens ordbog (se
    // Stavefelt). Samme regler som ikkeNavne: følger med i syncen, ingen skemaændring, og null
    // betyder "ældre end feltet", ikke "ingen ord".
    public List<String> egneOrd;

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
        // Sorteret (User holder et TreeSet), så samme liste altid giver samme hash i meta.json.
        this.ikkeNavne = new ArrayList<>(user.getIkkeNavne());
        this.egneOrd = new ArrayList<>(user.getEgneOrd());
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
