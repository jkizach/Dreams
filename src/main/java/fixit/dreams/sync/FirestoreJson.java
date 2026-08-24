package fixit.dreams.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;

// Rene, netværksfrie oversættelser mellem almindelig JSON (Jackson's JsonNode) og Firestores
// "typed-field" dokumentformat (se Firestore REST API's Value-type). Ingen I/O i denne klasse -
// derfor fuldt testbar uden noget rigtigt Firebase-projekt.
final class FirestoreJson {
    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private FirestoreJson() {}

    // Bygger en komplet Firestore-dokument-krop: {"fields": {...}}
    static ObjectNode toDocument(JsonNode plainObject) {
        ObjectNode doc = NODES.objectNode();
        doc.set("fields", toFields(plainObject));
        return doc;
    }

    // Firestore-dokument (helt objekt med "fields") -> almindeligt JSON-objekt
    static ObjectNode fromDocument(JsonNode document) {
        return fromFields(document.path("fields"));
    }

    static ObjectNode toFields(JsonNode plainObject) {
        ObjectNode fields = NODES.objectNode();
        Iterator<Map.Entry<String, JsonNode>> it = plainObject.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            fields.set(entry.getKey(), toValue(entry.getValue()));
        }
        return fields;
    }

    static ObjectNode fromFields(JsonNode fields) {
        ObjectNode plain = NODES.objectNode();
        Iterator<Map.Entry<String, JsonNode>> it = fields.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            plain.set(entry.getKey(), fromValue(entry.getValue()));
        }
        return plain;
    }

    static ObjectNode toValue(JsonNode node) {
        ObjectNode value = NODES.objectNode();
        if (node == null || node.isNull()) {
            value.putNull("nullValue");
        } else if (node.isBoolean()) {
            value.put("booleanValue", node.booleanValue());
        } else if (node.isIntegralNumber()) {
            value.put("integerValue", node.asText());
        } else if (node.isFloatingPointNumber()) {
            value.put("doubleValue", node.asDouble());
        } else if (node.isTextual()) {
            value.put("stringValue", node.asText());
        } else if (node.isArray()) {
            ObjectNode arrayValue = NODES.objectNode();
            ArrayNode values = arrayValue.putArray("values");
            for (JsonNode element : node) {
                values.add(toValue(element));
            }
            value.set("arrayValue", arrayValue);
        } else if (node.isObject()) {
            ObjectNode mapValue = NODES.objectNode();
            mapValue.set("fields", toFields(node));
            value.set("mapValue", mapValue);
        } else {
            value.put("stringValue", node.asText(""));
        }
        return value;
    }

    static JsonNode fromValue(JsonNode typedValue) {
        if (typedValue.has("nullValue")) {
            return NODES.nullNode();
        }
        if (typedValue.has("booleanValue")) {
            return NODES.booleanNode(typedValue.get("booleanValue").asBoolean());
        }
        if (typedValue.has("integerValue")) {
            return NODES.numberNode(Long.parseLong(typedValue.get("integerValue").asText()));
        }
        if (typedValue.has("doubleValue")) {
            return NODES.numberNode(typedValue.get("doubleValue").asDouble());
        }
        if (typedValue.has("stringValue")) {
            return NODES.textNode(typedValue.get("stringValue").asText());
        }
        if (typedValue.has("timestampValue")) {
            return NODES.textNode(typedValue.get("timestampValue").asText());
        }
        if (typedValue.has("arrayValue")) {
            ArrayNode array = NODES.arrayNode();
            for (JsonNode element : typedValue.path("arrayValue").path("values")) {
                array.add(fromValue(element));
            }
            return array;
        }
        if (typedValue.has("mapValue")) {
            return fromFields(typedValue.path("mapValue").path("fields"));
        }
        return NODES.nullNode();
    }
}
