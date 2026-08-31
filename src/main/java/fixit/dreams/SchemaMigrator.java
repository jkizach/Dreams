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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

// Migrerer de rå JSON-datafiler til den nyeste skema-version FØR IOutils' typede load-metoder
// læser dem - så migrationen er uafhængig af hvordan de typede klasser (DreamData osv.) ser ud
// lige nu, og gammel data ikke stille tabes når felter fjernes fra dem.
class SchemaMigrator {
    static final int CURRENT_SCHEMA_VERSION = 4;

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
        if (onDiskVersion < 3) {
            migrateV2ToV3();
        }
        if (onDiskVersion < 4) {
            migrateV3ToV4();
        }

        writeRawSchemaVersion(CURRENT_SCHEMA_VERSION);
    }

    private static void migrateV0ToV1() {
        migrateDreamsV0ToV1();
        migrateCatsV0ToV1();
    }

    // Giver en EKSISTERENDE installations kategori-definitioner, temaer og indstillinger et
    // starttidsstempel i meta.json (se MetaDTO) - forudsætning for at kunne afgøre hvem der er
    // nyest, når også de tre ting skal kunne synkroniseres.
    //
    // Denne migration er den mest forsigtige af dem alle: den LÆSER ikke og RØRER ikke
    // cats.json, temaer.json eller user.json. Den opretter kun en ny fil ved siden af. Går der
    // noget galt her, kan der derfor ikke tabes brugerdata - i værste fald mangler stemplerne,
    // og de bliver dannet igen ved næste gem.
    //
    // Bemærk at der bevidst IKKE skrives nogen hash: den kender vi først, når filerne næste gang
    // gemmes gennem IOutils. Første gem registrerer så hash'en uden at flytte tidsstemplet
    // (se MetaDTO.Stamp.stampIfChanged), og vi undgår at gætte på et fingeraftryk der ikke
    // ville matche - hvilket ville se ud som en ændring brugeren aldrig har foretaget.
    private static void migrateV2ToV3() {
        MetaDTO meta = IOutils.loadMeta();
        Instant now = Instant.now();

        boolean ændret = false;
        if (meta.categories.updatedAt == null) {
            meta.categories.updatedAt = now;
            ændret = true;
        }
        if (meta.temaer.updatedAt == null) {
            meta.temaer.updatedAt = now;
            ændret = true;
        }
        if (meta.settings.updatedAt == null) {
            meta.settings.updatedAt = now;
            ændret = true;
        }

        if (ændret) {
            IOutils.saveMeta(meta);
        }
    }

    // Giver hver kategori et stabilt id, og skriver id'et - ikke navnet - ind i drømmenes tags.
    // Derefter er en omdøbning ét felt i ét dokument i stedet for en ændring i hver eneste drøm
    // der bruger kategorien, og en drøm kan ikke længere pege på en kategori der ikke findes
    // fordi omdøbningen kun er nået halvvejs gennem synkroniseringen (se Stats.updateStats).
    //
    // To ting er afgørende for at den kan køre uafhængigt på begge maskiner uden at de bagefter
    // er uenige:
    //
    //   1. Id'erne udledes af navnet alene (Kategoriid.forIndbygget), og nummereringen ved et
    //      sammenfald følger kategoriernes rækkefølge i filen. Begge maskiner har den samme
    //      kategoriliste, så de når frem til det samme uden at tale sammen.
    //
    //   2. Drømmenes updatedAt røres IKKE. Intet ved drømmen er ændret - kun hvordan den peger
    //      på sin kategori - og begge maskiner udleder selv det samme resultat. Stemplede vi
    //      dem, ville hver eneste drøm se ændret ud og skulle uploades igen, og en migrering på
    //      den ene maskine ville slå ægte redigeringer på den anden.
    private static void migrateV3ToV4() {
        Map<String, String> navnTilId = migrerCatsV3ToV4();
        migrerDreamsV3ToV4(navnTilId);
    }

    /** @return navn -> id for kategorilisten, som drømmenes tags slås op i. */
    private static Map<String, String> migrerCatsV3ToV4() {
        Map<String, String> navnTilId = new LinkedHashMap<>();
        if (Files.notExists(FILE_PATH_CAT)) {
            return navnTilId;
        }
        try {
            JsonNode root = objectMapper.readTree(FILE_PATH_CAT.toFile());
            if (!(root instanceof ArrayNode cats)) {
                return navnTilId;
            }

            // Første gennemløb: saml de id'er der allerede står i filen, så en migrering der
            // blev afbrudt midtvejs kan køres igen uden at give nogen et nyt id.
            Set<String> optagede = new LinkedHashSet<>();
            for (JsonNode node : cats) {
                if (node instanceof ObjectNode cat && harTekst(cat, "id")) {
                    optagede.add(cat.get("id").asText());
                }
            }

            boolean ændret = false;
            for (JsonNode node : cats) {
                if (!(node instanceof ObjectNode cat)) {
                    continue;
                }
                String navn = cat.path("name").asText("");
                String id;
                if (harTekst(cat, "id")) {
                    id = cat.get("id").asText();
                } else {
                    id = Kategoriid.gørUnik(Kategoriid.forIndbygget(navn), optagede);
                    optagede.add(id);
                    cat.put("id", id);
                    ændret = true;
                }
                if (!navn.isEmpty()) {
                    navnTilId.put(navn, id);
                }
            }

            if (ændret) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(FILE_PATH_CAT.toFile(), cats);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return navnTilId;
    }

    private static void migrerDreamsV3ToV4(Map<String, String> navnTilId) {
        if (Files.notExists(FILE_PATH_DREAM)) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(FILE_PATH_DREAM.toFile());
            if (!(root instanceof ArrayNode dreams)) {
                return;
            }

            for (JsonNode dreamNode : dreams) {
                if (!(dreamNode instanceof ObjectNode dream) || !(dream.get("categories") instanceof ArrayNode tags)) {
                    continue;
                }
                for (JsonNode tagNode : tags) {
                    if (!(tagNode instanceof ObjectNode tag)) {
                        continue;
                    }
                    if (!harTekst(tag, "id")) {
                        String navn = tag.path("name").asText("");
                        if (navn.isEmpty()) {
                            continue;
                        }
                        // Står navnet ikke i kategorilisten, er taggen forældreløs - fx efter en
                        // omdøbning der kun nåede halvvejs gennem syncen. Den får sit id udledt
                        // efter nøjagtig samme regel, så begge maskiner ender med det samme, og
                        // taggen kan finde hjem igen hvis kategorien dukker op.
                        tag.put("id", navnTilId.getOrDefault(navn, Kategoriid.forIndbygget(navn)));
                    }
                    tag.remove("name"); // en drøms tag bærer id, ikke navn - se CategoryDTO
                }
            }

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(FILE_PATH_DREAM.toFile(), dreams);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean harTekst(ObjectNode node, String felt) {
        return node.hasNonNull(felt) && !node.get(felt).asText("").isBlank();
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
                            lucid, praktiserer, modsat, arketypisk, ompraksis, mareridt, advarsel, kollektiv, false);
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
                dto.id = Category.ID_KVALITETER;
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
