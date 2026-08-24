package fixit.dreams.sync;

import java.time.Instant;

// Ren, netværksfri "hvem vinder"-logik for last-write-wins-synkronisering pr. drøm.
public final class SyncMerge {
    private SyncMerge() {}

    // true hvis sky-versionen skal overskrive den lokale: enten findes drømmen slet ikke
    // lokalt endnu, eller sky-versionen er strengt nyere end den lokale.
    public static boolean cloudWins(Instant localUpdatedAt, Instant cloudUpdatedAt) {
        if (localUpdatedAt == null) return true;
        if (cloudUpdatedAt == null) return false;
        return cloudUpdatedAt.isAfter(localUpdatedAt);
    }
}
