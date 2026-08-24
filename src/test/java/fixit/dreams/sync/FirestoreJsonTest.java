package fixit.dreams.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FirestoreJsonTest {

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    @Test
    void toValue_streng_bliver_stringValue() {
        JsonNode value = FirestoreJson.toValue(NODES.textNode("hej"));
        assertEquals("hej", value.get("stringValue").asText());
    }

    @Test
    void toValue_boolean_bliver_booleanValue() {
        JsonNode value = FirestoreJson.toValue(NODES.booleanNode(true));
        assertTrue(value.get("booleanValue").asBoolean());
    }

    @Test
    void toValue_heltal_bliver_integerValue_som_streng() {
        JsonNode value = FirestoreJson.toValue(NODES.numberNode(42));
        assertEquals("42", value.get("integerValue").asText());
    }

    @Test
    void toValue_decimaltal_bliver_doubleValue() {
        JsonNode value = FirestoreJson.toValue(NODES.numberNode(3.14));
        assertEquals(3.14, value.get("doubleValue").asDouble(), 0.0001);
    }

    @Test
    void toValue_null_bliver_nullValue() {
        JsonNode value = FirestoreJson.toValue(NODES.nullNode());
        assertTrue(value.has("nullValue"));
        assertTrue(value.get("nullValue").isNull());
    }

    @Test
    void toValue_array_bliver_arrayValue_med_values() {
        var array = NODES.arrayNode();
        array.add("a");
        array.add("b");

        JsonNode value = FirestoreJson.toValue(array);

        JsonNode values = value.get("arrayValue").get("values");
        assertEquals(2, values.size());
        assertEquals("a", values.get(0).get("stringValue").asText());
        assertEquals("b", values.get(1).get("stringValue").asText());
    }

    @Test
    void toValue_objekt_bliver_mapValue_med_fields() {
        ObjectNode obj = NODES.objectNode();
        obj.put("navn", "Kvaliteter");
        obj.put("antal", 3);

        JsonNode value = FirestoreJson.toValue(obj);

        JsonNode fields = value.get("mapValue").get("fields");
        assertEquals("Kvaliteter", fields.get("navn").get("stringValue").asText());
        assertEquals("3", fields.get("antal").get("integerValue").asText());
    }

    @Test
    void fromValue_er_den_omvendte_af_toValue_for_alle_typer() {
        ObjectNode original = NODES.objectNode();
        original.put("navn", "test");
        original.put("aktiv", true);
        original.put("antal", 5);
        original.put("vaegt", 2.5);
        original.putNull("tom");
        var symboler = original.putArray("symboler");
        symboler.add("rød").add("blå");
        ObjectNode nested = original.putObject("nested");
        nested.put("dybde", 1);

        JsonNode roundtripped = FirestoreJson.fromValue(FirestoreJson.toValue(original));

        // Sammenlign JSON-teksten, ikke Jackson-nodetyperne direkte: fromValue rekonstruerer
        // heltal som LongNode, mens .put(int) gav en IntNode - samme JSON-værdi, forskellig
        // intern Jackson-klasse, så .equals() fejler selvom data er identisk.
        assertEquals(original.toString(), roundtripped.toString());
    }

    @Test
    void toDocument_og_fromDocument_rundtripper_et_helt_dokument() {
        ObjectNode dream = NODES.objectNode();
        dream.put("id", "abc-123");
        dream.put("indhold", "Jeg fløj over en skov");
        dream.put("dato", "2026-03-15");
        var categories = dream.putArray("categories");
        ObjectNode cat = NODES.objectNode();
        cat.put("name", "Kvaliteter");
        var symbols = cat.putArray("symbols");
        symbols.add("Lucid");
        categories.add(cat);

        ObjectNode document = FirestoreJson.toDocument(dream);
        assertTrue(document.has("fields"));

        JsonNode roundtripped = FirestoreJson.fromDocument(document);
        assertEquals(dream, roundtripped);
    }

    @Test
    void fromValue_haandterer_timestampValue_som_streng() {
        ObjectNode typed = NODES.objectNode();
        typed.put("timestampValue", "2026-01-01T12:00:00Z");

        JsonNode result = FirestoreJson.fromValue(typed);

        assertEquals("2026-01-01T12:00:00Z", result.asText());
    }
}
