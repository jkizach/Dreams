package fixit.dreams.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fixit.dreams.Dream;
import fixit.dreams.DreamData;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TombstoneTest {

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private static final Instant SLETTET = Instant.parse("2026-08-26T10:00:00Z");
    private static final Instant FOER = Instant.parse("2026-08-26T09:00:00Z");
    private static final Instant EFTER = Instant.parse("2026-08-26T11:00:00Z");

    // Maskine A skriver en gravsten; det er sådan den ser ud når maskine B har hentet den ned.
    private static JsonNode somMaskineBSerDen(ObjectNode gravsten) {
        return FirestoreJson.fromDocument(FirestoreJson.toDocument(gravsten));
    }

    @Test
    void gravsten_genkendes_efter_rundturen_gennem_firestore() {
        // Det er HELE mekanismen: overlevede deletedAt ikke trådformatet, ville maskine B læse
        // gravstenen som en almindelig drøm og genoplive den slettede drøm.
        JsonNode hosB = somMaskineBSerDen(Tombstone.of("abc-123", SLETTET));

        assertTrue(Tombstone.isTombstone(hosB));
        assertEquals(SLETTET, Instant.parse(hosB.get("updatedAt").asText()));
    }

    @Test
    void en_almindelig_droem_er_ikke_en_gravsten() {
        ObjectNode droem = NODES.objectNode();
        droem.put("id", "abc-123");
        droem.put("indhold", "Jeg fløj over en skov");
        droem.put("updatedAt", SLETTET.toString());

        assertFalse(Tombstone.isTombstone(somMaskineBSerDen(droem)));
        assertFalse(Tombstone.isTombstone(null));
    }

    @Test
    void gravsten_vinder_over_aeldre_lokal_droem() {
        // Maskine B's kopi er ældre end sletningen -> B skal slette sin kopi.
        JsonNode hosB = somMaskineBSerDen(Tombstone.of("abc-123", SLETTET));
        Instant gravstenensTid = Instant.parse(hosB.get("updatedAt").asText());

        assertTrue(SyncMerge.cloudWins(FOER, gravstenensTid));
    }

    @Test
    void redigering_efter_sletningen_overlever_gravstenen() {
        // Redigerede man drømmen på B EFTER at A slettede den, må gravstenen ikke vinde -
        // ellers ville en nyere redigering blive slettet af en ældre sletning.
        JsonNode hosB = somMaskineBSerDen(Tombstone.of("abc-123", SLETTET));
        Instant gravstenensTid = Instant.parse(hosB.get("updatedAt").asText());

        assertFalse(SyncMerge.cloudWins(EFTER, gravstenensTid));
    }

    @Test
    void gravsten_maa_aldrig_laeses_som_en_droem() {
        // Dokumenterer hvorfor pullNewerDreams SKAL tjekke isTombstone og springe videre:
        // en gravsten har hverken kategorier eller indhold, så Dream-konstruktøren falder over den.
        JsonNode hosB = somMaskineBSerDen(Tombstone.of("abc-123", SLETTET));

        DreamData data = assertDoesNotThrow(
                () -> SyncObjectMapper.INSTANCE.treeToValue(hosB, DreamData.class));
        assertNull(data.categories);
        assertThrows(NullPointerException.class, () -> new Dream(data));
    }
}
