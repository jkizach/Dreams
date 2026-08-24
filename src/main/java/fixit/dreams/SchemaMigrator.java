package fixit.dreams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

// Migrerer de rå JSON-datafiler til den nyeste skema-version FØR IOutils' typede load-metoder
// læser dem - så migrationen er uafhængig af hvordan de typede klasser (DreamData osv.) ser ud
// lige nu, og gammel data ikke stille tabes når felter fjernes fra dem.
class SchemaMigrator {
    static final int CURRENT_SCHEMA_VERSION = 2;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Path FILE_PATH_USER = AppPaths.APP_DATA_PATH.resolve("user.json");
    private static final Path FILE_PATH_CAT = AppPaths.APP_DATA_PATH.resolve("cats.json");
    private static final Path FILE_PATH_DREAM = AppPaths.APP_DATA_PATH.resolve("dreams.json");

    static void migrateIfNeeded() {
        if (Files.notExists(FILE_PATH_USER)) {
            return; // frisk installation - intet at migrere
        }

        int onDiskVersion = readRawSchemaVersion();
        if (onDiskVersion >= CURRENT_SCHEMA_VERSION) {
            return;
        }

        if (onDiskVersion < 1) {
            migrateV0ToV1();
        }
        if (onDiskVersion < 2) {
            migrateV1ToV2();
        }

        writeRawSchemaVersion(CURRENT_SCHEMA_VERSION);
    }

    private static void migrateV0ToV1() {
        migrateDreamsV0ToV1();
        migrateCatsV0ToV1();
    }

    // Stempler enhver drøm uden updatedAt med nu-tidspunktet - forudsætning for at kunne
    // afgøre hvilken version af en drøm der er nyest ved fremtidig cloud-synkronisering.
    private static void migrateV1ToV2() {
        if (Files.notExists(FILE_PATH_DREAM)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(FILE_PATH_DREAM.toFile());
            if (!(root instanceof ArrayNode dreams)) {
                return;
            }

            String now = Instant.now().toString();
            for (JsonNode node : dreams) {
                if (!(node instanceof ObjectNode dream)) {
                    continue;
                }
                if (!dream.hasNonNull("updatedAt") || dream.get("updatedAt").asText("").isBlank()) {
                    dream.put("updatedAt", now);
                }
            }

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(FILE_PATH_DREAM.toFile(), dreams);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Løfter gamle drømme (flade booleans, intet id) til det nye format: stabilt id +
    // en "Kvaliteter"-CategoryDTO bygget ud fra de 8 gamle felter, som derefter fjernes.
    private static void migrateDreamsV0ToV1() {
        if (Files.notExists(FILE_PATH_DREAM)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(FILE_PATH_DREAM.toFile());
            if (!(root instanceof ArrayNode dreams)) {
                return;
            }

            for (JsonNode node : dreams) {
                if (!(node instanceof ObjectNode dream)) {
                    continue;
                }

                if (!dream.hasNonNull("id") || dream.get("id").asText("").isBlank()) {
                    dream.put("id", UUID.randomUUID().toString());
                }

                boolean lucid = dream.path("lucid").asBoolean(false);
                boolean praktiserer = dream.path("praktiserer").asBoolean(false);
                boolean modsat = dream.path("modsat").asBoolean(false);
                boolean arketypisk = dream.path("arketypisk").asBoolean(false);
                boolean ompraksis = dream.path("ompraksis").asBoolean(false);
                boolean mareridt = dream.path("mareridt").asBoolean(false);
                boolean kollektiv = dream.path("kollektiv").asBoolean(false);
                boolean advarsel = dream.path("advarsel").asBoolean(false);

                ArrayNode categories = (dream.has("categories") && dream.get("categories").isArray())
                        ? (ArrayNode) dream.get("categories")
                        : dream.putArray("categories");

                boolean harAlleredeKvaliteter = false;
                for (JsonNode cat : categories) {
                    if (cat.has("name") && Category.FLAGS_CATEGORY_NAME.equals(cat.get("name").asText())) {
                        harAlleredeKvaliteter = true;
                        break;
                    }
                }

                if (!harAlleredeKvaliteter) {
                    CategoryDTO kvaliteter = Category.buildFlagsCategoryDTO(
                            lucid, praktiserer, modsat, arketypisk, ompraksis, mareridt, advarsel, kollektiv);
                    categories.add(objectMapper.valueToTree(kvaliteter));
                }

                dream.remove(List.of("lucid", "praktiserer", "modsat", "arketypisk", "ompraksis", "mareridt", "kollektiv", "advarsel"));
            }

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(FILE_PATH_DREAM.toFile(), dreams);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Patcher cats.json med en "Kvaliteter"-kategori, hvis den mangler - ellers vil eksisterende
    // brugeres allerede-indlæste cats.json aldrig få den (addDefaultCategories() kører kun for helt nye installationer).
    private static void migrateCatsV0ToV1() {
        if (Files.notExists(FILE_PATH_CAT)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(FILE_PATH_CAT.toFile());
            if (!(root instanceof ArrayNode cats)) {
                return;
            }

            boolean harAlleredeKvaliteter = false;
            for (JsonNode cat : cats) {
                if (cat.has("name") && Category.FLAGS_CATEGORY_NAME.equals(cat.get("name").asText())) {
                    harAlleredeKvaliteter = true;
                    break;
                }
            }

            if (!harAlleredeKvaliteter) {
                CategoryDTO dto = new CategoryDTO();
                dto.name = Category.FLAGS_CATEGORY_NAME;
                dto.symbols = new TreeSet<>(Category.FLAGS_SYMBOLS_IN_ORDER);
                dto.customOrder = new ArrayList<>(Category.FLAGS_SYMBOLS_IN_ORDER);
                cats.add(objectMapper.valueToTree(dto));
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(FILE_PATH_CAT.toFile(), cats);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int readRawSchemaVersion() {
        try {
            JsonNode root = objectMapper.readTree(FILE_PATH_USER.toFile());
            JsonNode versionNode = root.get("schemaVersion");
            return (versionNode != null) ? versionNode.asInt(0) : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    private static void writeRawSchemaVersion(int version) {
        try {
            JsonNode root = objectMapper.readTree(FILE_PATH_USER.toFile());
            if (!(root instanceof ObjectNode userNode)) {
                return;
            }
            userNode.put("schemaVersion", version);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(FILE_PATH_USER.toFile(), userNode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private SchemaMigrator() {}
}
