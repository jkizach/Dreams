package fixit.dreams;

// Kastes når lokale og sky-drømme begge er ikke-tomme, men slet ikke deler nogen ID'er -
// fingeraftrykket for to uafhængigt migrerede kopier (fx to maskiner der aldrig har
// synkroniseret sammen før), hvor en automatisk sammenkøring ville risikere dubletter
// (forskellige tilfældige ID'er for det der reelt er "samme" drøm på hver maskine). Findes
// der bare ét overlappende ID, kastes den IKKE - det er tydeligvis samme datasæt med
// inkrementelle ændringer. Ingen data er rørt når denne kastes - hverken pull eller push er sket endnu.
public class SyncConflictException extends SyncException {
    private final int localCount;
    private final int cloudCount;

    public SyncConflictException(int localCount, int cloudCount) {
        super("Der findes både " + localCount + " lokale drømme og " + cloudCount
                + " drømme i skyen på denne konto. Automatisk sammenkøring kan skabe dubletter, "
                + "hvis det er to uafhængige kopier af de samme drømme.");
        this.localCount = localCount;
        this.cloudCount = cloudCount;
    }

    public int getLocalCount() {
        return localCount;
    }

    public int getCloudCount() {
        return cloudCount;
    }
}
