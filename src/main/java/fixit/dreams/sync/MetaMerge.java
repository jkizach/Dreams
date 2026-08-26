package fixit.dreams.sync;

import java.time.Instant;

// Ren, netværksfri "hvem vinder"-logik for de tre meta-dokumenter (kategori-definitioner,
// temaer, indstillinger). Modsat drømmene, der flettes stykke for stykke, er hvert af disse
// dokumenter ÉN enhed: hele listen vinder eller taber samlet. Til gengæld følger sletninger
// gratis med - en fjernet kategori er bare en kortere liste, og der er ingen gravsten at føre.
//
// Tidsstemplerne kommer fra meta.json (se MetaDTO), hvor null betyder "aldrig redigeret her".
public final class MetaMerge {
    private MetaMerge() {}

    // true hvis skyens udgave skal overtage den lokale.
    public static boolean cloudWins(Instant localUpdatedAt, Instant cloudUpdatedAt, boolean cloudExists) {
        if (!cloudExists || cloudUpdatedAt == null) {
            return false; // intet i skyen, eller noget uden tidsstempel: aldrig noget at hente
        }
        if (localUpdatedAt == null) {
            return true; // aldrig redigeret her - skyens rigtige udgave slår vores standarddata
        }
        return cloudUpdatedAt.isAfter(localUpdatedAt);
    }

    // true hvis den lokale udgave skal sendes til skyen.
    //
    // Bemærk det ellers overraskende tilfælde nederst: data der aldrig er redigeret (null)
    // sendes KUN når dokumentet slet ikke findes i skyen endnu. Så kan det pr. definition ikke
    // overskrive noget - og den allerførste maskine får fyldt skyen op med det samme i stedet
    // for at vente på den første redigering. Findes dokumentet derimod, holder vi os fra det:
    // vores urørte standardkategorier må aldrig skubbe en rigtig udgave væk.
    public static boolean shouldPush(Instant localUpdatedAt, Instant cloudUpdatedAt, boolean cloudExists) {
        if (!cloudExists) {
            return true;
        }
        if (localUpdatedAt == null) {
            return false;
        }
        return cloudUpdatedAt == null || localUpdatedAt.isAfter(cloudUpdatedAt);
    }
}
