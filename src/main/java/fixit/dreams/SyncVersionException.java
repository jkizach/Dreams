package fixit.dreams;

/**
 * Kastes når skyen er skrevet af en NYERE udgave af appen end den der kører.
 *
 * Skyen bærer ingen versionsmarkør på selve drømmene, og migreringen (SchemaMigrator) kører kun
 * på de lokale filer ved opstart - hentede dokumenter kommer aldrig forbi den. En app der møder
 * et dataformat den ikke kender, ville derfor bare læse det forkert: felter den ikke forstår
 * bliver til null, og symboler holder stille op med at tælle med. Ingen fejl, ingen advarsel.
 *
 * Derfor stopper vi i stedet. Det er bevidst et STOPSKILT og ikke en oversættelse: en udgave kan
 * umuligt kende et format der ikke fandtes da den blev skrevet. Den anden retning - en nyere app
 * der møder ældre data i skyen - kan derimod klares, men den oversættelse hører hjemme i den
 * udgave der indfører det nye format, og skrives på det tidspunkt.
 *
 * Det er grunden til at netop DENNE halvdel skal ligge i 2.0: et værn hjælper kun den maskine der
 * modtager noget uventet, så det skal være på plads i den udgave der en dag bliver den gamle.
 */
public class SyncVersionException extends SyncException {

    private final int skyensVersion;
    private final int voresVersion;

    public SyncVersionException(int skyensVersion, int voresVersion) {
        super("Skyen er skrevet af en nyere udgave af Drømmeappen (dataformat "
                + skyensVersion + ", denne udgave forstår " + voresVersion
                + "). Opdatér denne maskine før du synkroniserer igen - ellers kan drømme blive læst forkert.");
        this.skyensVersion = skyensVersion;
        this.voresVersion = voresVersion;
    }

    public int getSkyensVersion() {
        return skyensVersion;
    }

    public int getVoresVersion() {
        return voresVersion;
    }
}
