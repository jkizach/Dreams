package fixit.dreams;

// Kastes når en sammenkøring reelt risikerer at skabe dubletter: der findes lokale drømme som
// indholdsmæssigt allerede ligger i skyen, men under et ANDET ID. Det er fingeraftrykket for to
// uafhængigt migrerede kopier af samme datasæt (fx to maskiner der aldrig har synkroniseret
// sammen før), hvor hver maskine har givet den samme drøm sit eget tilfældige UUID.
//
// Den kastes bevidst IKKE bare fordi ID-mængderne ikke overlapper. Det sker jo også helt
// normalt og ufarligt, når man logger på en frisk installation og har nået at skrive et par nye
// drømme først: de har naturligvis ID'er skyen aldrig har set, men de er ægte nye drømme og kan
// ikke blive til dubletter. Kun et INDHOLDS-sammenfald er ægte bevis for en dublet-risiko.
//
// Ingen data er rørt når denne kastes - hverken pull eller push er sket endnu.
public class SyncConflictException extends SyncException {
    private final int localCount;
    private final int cloudCount;
    private final int duplicateCount;

    public SyncConflictException(int localCount, int cloudCount, int duplicateCount) {
        super(duplicateCount + " af dine " + localCount + " lokale drømme findes allerede i skyen "
                + "under et andet ID. Automatisk sammenkøring ville gemme dem som dubletter.");
        this.localCount = localCount;
        this.cloudCount = cloudCount;
        this.duplicateCount = duplicateCount;
    }

    public int getLocalCount() {
        return localCount;
    }

    public int getCloudCount() {
        return cloudCount;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }
}
