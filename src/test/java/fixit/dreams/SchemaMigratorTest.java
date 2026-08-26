package fixit.dreams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchemaMigratorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private Path userJson;
    private Path catsJson;
    private Path dreamsJson;
    private Path metaJson;

    @BeforeEach
    void setUp() throws IOException {
        User.resetForTests();
        userJson = AppPaths.APP_DATA_PATH.resolve("user.json");
        catsJson = AppPaths.APP_DATA_PATH.resolve("cats.json");
        dreamsJson = AppPaths.APP_DATA_PATH.resolve("dreams.json");
        metaJson = AppPaths.APP_DATA_PATH.resolve("meta.json");

        Files.deleteIfExists(userJson);
        Files.deleteIfExists(catsJson);
        Files.deleteIfExists(dreamsJson);
        Files.deleteIfExists(metaJson);
    }

    // Disse tests skriver rigtige JSON-fixtures til den delte test-home-mappe (se pom.xml's
    // surefire-konfiguration). Uden oprydning ville de lække ind i andre testklassers
    // User.getInstance()-kald, som IKKE selv rører disk og derfor ikke forventer data der.
    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(userJson);
        Files.deleteIfExists(catsJson);
        Files.deleteIfExists(dreamsJson);
        Files.deleteIfExists(metaJson);
        User.resetForTests();
    }

    private void writeOldFormatFixtures() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt grønt","visAdvarsel":false,"visKollektiv":false,"startFromThisDate":"2026-01-01"}
                """);

        Files.writeString(catsJson, """
                [{"name":"Farver","symbols":["rød","blå"],"customOrder":[]}]
                """);

        Files.writeString(dreamsJson, """
                [{"categories":[],"lucid":true,"praktiserer":false,"modsat":false,"arketypisk":false,"ompraksis":false,"mareridt":true,"kollektiv":false,"advarsel":false,"indhold":"test","dagrest":"","tolkning":"","dato":"2026-01-05"}]
                """);
    }

    @Test
    void migrerer_gammel_dream_til_stabilt_id_og_kvaliteter_kategori() throws IOException {
        writeOldFormatFixtures();

        SchemaMigrator.migrateIfNeeded();

        JsonNode dreams = mapper.readTree(dreamsJson.toFile());
        JsonNode dream = dreams.get(0);

        assertTrue(dream.hasNonNull("id"));
        assertFalse(dream.get("id").asText().isBlank());
        assertFalse(dream.has("lucid"));
        assertFalse(dream.has("mareridt"));

        JsonNode kvaliteter = null;
        for (JsonNode cat : dream.get("categories")) {
            if ("Kvaliteter".equals(cat.get("name").asText())) {
                kvaliteter = cat;
            }
        }
        assertNotNull(kvaliteter);

        List<String> symbols = new ArrayList<>();
        kvaliteter.get("symbols").forEach(s -> symbols.add(s.asText()));
        assertEquals(2, symbols.size());
        assertTrue(symbols.contains("Lucid"));
        assertTrue(symbols.contains("Mareridt"));
    }

    @Test
    void patcher_cats_json_med_kvaliteter_hvis_manglende() throws IOException {
        writeOldFormatFixtures();

        SchemaMigrator.migrateIfNeeded();

        JsonNode cats = mapper.readTree(catsJson.toFile());
        boolean found = false;
        for (JsonNode cat : cats) {
            if ("Kvaliteter".equals(cat.get("name").asText())) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    void bumper_schemaVersion_i_user_json() throws IOException {
        writeOldFormatFixtures();

        SchemaMigrator.migrateIfNeeded();

        JsonNode user = mapper.readTree(userJson.toFile());
        assertEquals(SchemaMigrator.CURRENT_SCHEMA_VERSION, user.get("schemaVersion").asInt());
    }

    @Test
    void andet_kald_er_idempotent() throws IOException {
        writeOldFormatFixtures();

        SchemaMigrator.migrateIfNeeded();
        String afterFirst = Files.readString(dreamsJson);

        SchemaMigrator.migrateIfNeeded();
        String afterSecond = Files.readString(dreamsJson);

        assertEquals(afterFirst, afterSecond);
    }

    @Test
    void frisk_installation_uden_user_json_rorer_ikke_filerne() {
        assertDoesNotThrow(SchemaMigrator::migrateIfNeeded);
        assertFalse(Files.exists(userJson));
    }

    @Test
    void migrerer_v1_dream_uden_updatedAt_til_v2() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt grønt","visAdvarsel":false,"visKollektiv":false,"startFromThisDate":"2026-01-01","schemaVersion":1}
                """);
        Files.writeString(dreamsJson, """
                [{"id":"abc-123","categories":[],"indhold":"test","dagrest":"","tolkning":"","dato":"2026-01-05"}]
                """);

        SchemaMigrator.migrateIfNeeded();

        JsonNode dream = mapper.readTree(dreamsJson.toFile()).get(0);
        assertTrue(dream.hasNonNull("updatedAt"));
        assertFalse(dream.get("updatedAt").asText().isBlank());
    }

    @Test
    void bevarer_eksisterende_updatedAt_ved_v1_til_v2_migration() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt grønt","visAdvarsel":false,"visKollektiv":false,"startFromThisDate":"2026-01-01","schemaVersion":1}
                """);
        Files.writeString(dreamsJson, """
                [{"id":"abc-123","categories":[],"indhold":"test","dagrest":"","tolkning":"","dato":"2026-01-05","updatedAt":"2020-01-01T00:00:00Z"}]
                """);

        SchemaMigrator.migrateIfNeeded();

        JsonNode dream = mapper.readTree(dreamsJson.toFile()).get(0);
        assertEquals("2020-01-01T00:00:00Z", dream.get("updatedAt").asText());
    }

    @Test
    void efter_migration_kan_dream_indlaeses_med_korrekte_flag_via_userinstans() throws IOException {
        writeOldFormatFixtures();

        User user = User.getInstance(); // trigger migration + load

        assertEquals(1, user.getDreams().size());
        Dream dream = user.getDreams().values().iterator().next();
        assertTrue(dream.hasFlag("Lucid"));
        assertTrue(dream.hasFlag("Mareridt"));
        assertFalse(dream.hasFlag("Advarsel"));
    }

    @Test
    void v2_til_v3_stempler_kategorier_temaer_og_indstillinger() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt grønt","visAdvarsel":false,"visKollektiv":false,"startFromThisDate":"2026-01-01","schemaVersion":2}
                """);

        SchemaMigrator.migrateIfNeeded();

        MetaDTO meta = IOutils.loadMeta();
        assertNotNull(meta.categories.updatedAt);
        assertNotNull(meta.temaer.updatedAt);
        assertNotNull(meta.settings.updatedAt);
    }

    // Hash'en kendes først, når filerne næste gang gemmes gennem IOutils - migrationen må
    // ikke gætte på et fingeraftryk, for et forkert gæt ville ligne en brugerændring.
    @Test
    void v2_til_v3_skriver_ingen_hash() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt grønt","visAdvarsel":false,"visKollektiv":false,"startFromThisDate":"2026-01-01","schemaVersion":2}
                """);

        SchemaMigrator.migrateIfNeeded();

        MetaDTO meta = IOutils.loadMeta();
        assertNull(meta.categories.hash);
        assertNull(meta.temaer.hash);
        assertNull(meta.settings.hash);
    }

    @Test
    void v2_til_v3_overskriver_ikke_et_stempel_der_allerede_findes() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt grønt","visAdvarsel":false,"visKollektiv":false,"startFromThisDate":"2026-01-01","schemaVersion":2}
                """);
        MetaDTO eksisterende = new MetaDTO();
        eksisterende.categories.updatedAt = Instant.parse("2020-01-01T00:00:00Z");
        eksisterende.categories.hash = "gammel-hash";
        IOutils.saveMeta(eksisterende);

        SchemaMigrator.migrateIfNeeded();

        MetaDTO meta = IOutils.loadMeta();
        assertEquals(Instant.parse("2020-01-01T00:00:00Z"), meta.categories.updatedAt);
        assertEquals("gammel-hash", meta.categories.hash);
        assertNotNull(meta.temaer.updatedAt);
    }

    // Kernegarantien for eksisterende brugere: opgraderingen til v3 tilføjer kun en ny fil,
    // den omskriver ikke de data de allerede har liggende.
    @Test
    void v2_til_v3_roerer_ikke_brugerens_egne_datafiler() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt grønt","visAdvarsel":false,"visKollektiv":false,"startFromThisDate":"2026-01-01","schemaVersion":2}
                """);
        Files.writeString(catsJson, """
                [{"name":"Farver","symbols":["rød","blå"],"customOrder":[]}]
                """);
        Files.writeString(dreamsJson, """
                [{"id":"abc-123","categories":[],"indhold":"test","dagrest":"","tolkning":"","dato":"2026-01-05","updatedAt":"2020-01-01T00:00:00Z"}]
                """);
        String catsFoer = Files.readString(catsJson);
        String dreamsFoer = Files.readString(dreamsJson);

        SchemaMigrator.migrateIfNeeded();

        assertEquals(catsFoer, Files.readString(catsJson));
        assertEquals(dreamsFoer, Files.readString(dreamsJson));
    }

    @Test
    void frisk_installation_faar_ingen_meta_fil_af_migrationen() {
        SchemaMigrator.migrateIfNeeded();

        assertFalse(Files.exists(metaJson));
    }
}
