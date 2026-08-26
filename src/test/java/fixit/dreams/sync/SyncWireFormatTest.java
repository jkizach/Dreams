package fixit.dreams.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fixit.dreams.CategoryDTO;
import fixit.dreams.DreamData;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

// Verificerer at en drøm overlever HELE vejen ud og hjem gennem Firestores trådformat med
// tidsstemplerne intakte. Det er ikke en teoretisk detalje: SyncService.extractUpdatedAt
// laver Instant.parse() på det der kommer retur, og fanger enhver exception med "return null".
// Hvis updatedAt kom hjem som fx et decimaltal i stedet for en ISO-streng, ville den altså
// STILTIENDE returnere null for hver eneste drøm - hvorefter "er skyen ajour?"-tjekket altid
// siger nej, og hver eneste uændrede drøm bliver genuploadet ved hver sync (kvote-bug'en).
class SyncWireFormatTest {

    private static final Instant UPDATED_AT = Instant.parse("2026-08-26T10:15:30Z");
    private static final LocalDate DATO = LocalDate.of(2026, 3, 15);

    private static DreamData enDroem() {
        CategoryDTO kategori = new CategoryDTO();
        kategori.name = "Kvaliteter";
        kategori.symbols = new TreeSet<>(List.of("Lucid", "Holografisk"));
        kategori.customOrder = new ArrayList<>();

        DreamData data = new DreamData();
        data.id = "abc-123";
        data.categories = List.of(kategori);
        data.indhold = "Jeg fløj over en skov";
        data.dagrest = "Så en dokumentar om fugle";
        data.tolkning = "Frihed";
        data.dato = DATO;
        data.updatedAt = UPDATED_AT;
        return data;
    }

    // Spejler præcis kodestien i SyncService: toPlainJson -> patchDocument -> listDocuments.
    private static JsonNode rundtur(DreamData data) {
        JsonNode udgaaende = SyncObjectMapper.INSTANCE.valueToTree(data);
        return FirestoreJson.fromDocument(FirestoreJson.toDocument(udgaaende));
    }

    @Test
    void updatedAt_overlever_rundturen_som_parsebar_iso_streng() {
        JsonNode hjemme = rundtur(enDroem());

        JsonNode ua = hjemme.get("updatedAt");
        assertNotNull(ua, "updatedAt forsvandt helt i rundturen");
        assertTrue(ua.isTextual(), "updatedAt skal komme hjem som streng, ikke " + ua.getNodeType()
                + " - ellers fejler Instant.parse() i extractUpdatedAt og kvote-bug'en er tilbage");
        assertEquals(UPDATED_AT, Instant.parse(ua.asText()));
    }

    @Test
    void dato_overlever_rundturen_uaendret() {
        JsonNode hjemme = rundtur(enDroem());

        JsonNode dato = hjemme.get("dato");
        assertNotNull(dato, "dato forsvandt helt i rundturen");
        assertEquals(DATO, LocalDate.parse(dato.asText()));
    }

    @Test
    void ukendte_felter_faar_ikke_parsningen_til_at_fejle() {
        // Fremadkompatibilitet: skyen kan indeholde dokumenter skrevet af en nyere version med
        // felter denne version ikke kender. Fejlede parsningen, ville pullNewerDreams' catch
        // springe drømmen over i stilhed, og den ville aldrig blive hentet ned.
        ObjectNode fraEnNyereVersion = (ObjectNode) SyncObjectMapper.INSTANCE.valueToTree(enDroem());
        fraEnNyereVersion.put("etHeltNytFelt", "som denne version ikke kender");

        DreamData data = assertDoesNotThrow(
                () -> SyncObjectMapper.INSTANCE.treeToValue(fraEnNyereVersion, DreamData.class));

        assertEquals("Jeg fløj over en skov", data.indhold);
        assertEquals(UPDATED_AT, data.updatedAt);
    }

    @Test
    void hele_droemmen_overlever_rundturen_uaendret() {
        DreamData data = enDroem();
        JsonNode udgaaende = SyncObjectMapper.INSTANCE.valueToTree(data);

        JsonNode hjemme = FirestoreJson.fromDocument(FirestoreJson.toDocument(udgaaende));

        assertEquals(udgaaende, hjemme);
    }
}
