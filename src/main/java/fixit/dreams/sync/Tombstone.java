package fixit.dreams.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;

// En "gravsten": et sky-dokument der markerer at drømmen med dette ID er SLETTET, i stedet for
// at fjerne dokumentet helt. Uden den ville en sletning kun være et lokalt faktum, og andre
// maskiner ville aldrig få det at vide - de ville beholde deres kopi og pushe den op igen, så
// den slettede drøm genopstod ved næste synkronisering.
//
// Gravsten bliver liggende permanent. En oprydning ville betyde at en maskine, der havde været
// offline længere end oprydningsvinduet, kunne genoplive drømmen; et næsten tomt dokument pr.
// slettet drøm er en billigere pris end den fejlkilde.
public final class Tombstone {
    public static final String DELETED_AT = "deletedAt";

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private Tombstone() {}

    // updatedAt sættes bevidst til samme tidspunkt som deletedAt, så gravstenen deltager i den
    // ALMINDELIGE last-write-wins-sammenligning på lige fod med en rigtig drøm. Det er dét, der
    // får de skæve tilfælde til at falde rigtigt ud: redigerer man drømmen på en anden maskine
    // EFTER sletningen, er redigeringen nyere og vinder - og opretter man en ny drøm der (helt
    // utænkeligt) fik samme UUID, vinder den nye drøm og overskriver gravstenen.
    public static ObjectNode of(String id, Instant deletedAt) {
        ObjectNode felter = NODES.objectNode();
        felter.put("id", id);
        felter.put("updatedAt", deletedAt.toString());
        felter.put(DELETED_AT, deletedAt.toString());
        return felter;
    }

    public static boolean isTombstone(JsonNode felter) {
        if (felter == null) {
            return false;
        }
        JsonNode markering = felter.get(DELETED_AT);
        return markering != null && !markering.isNull() && !markering.asText().isBlank();
    }
}
