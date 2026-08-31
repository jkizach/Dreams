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
            if (Category.ID_KVALITETER.equals(cat.path("id").asText())) {
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

    // Kernegarantien for eksisterende brugere: en opgradering maa ikke aendre indholdet af de
    // data de allerede har liggende. v3 -> v4 tilfoejer et id, og DET er ogsaa alt hvad den maa
    // goere - navne, symboler og raekkefoelge skal staa uroert tilbage.
    @Test
    void opgraderingen_tilfoejer_kun_id_til_brugerens_kategorier() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt grønt","visAdvarsel":false,"visKollektiv":false,"startFromThisDate":"2026-01-01","schemaVersion":2}
                """);
        Files.writeString(catsJson, """
                [{"name":"Farver","symbols":["rød","blå"],"customOrder":[]}]
                """);
        Files.writeString(dreamsJson, """
                [{"id":"abc-123","categories":[],"indhold":"test","dagrest":"","tolkning":"","dato":"2026-01-05","updatedAt":"2020-01-01T00:00:00Z"}]
                """);

        SchemaMigrator.migrateIfNeeded();

        JsonNode kategori = mapper.readTree(catsJson.toFile()).get(0);
        assertEquals("farver", kategori.get("id").asText(), "v3 -> v4: kategorien skal have faaet sit id");
        assertEquals("Farver", kategori.get("name").asText(), "navnet skal staa uroert");
        List<String> symboler = new ArrayList<>();
        kategori.get("symbols").forEach(s -> symboler.add(s.asText()));
        assertEquals(List.of("rød", "blå"), symboler, "symbolerne skal staa uroert");

        JsonNode droem = mapper.readTree(dreamsJson.toFile()).get(0);
        assertEquals("test", droem.get("indhold").asText(), "droemmens indhold skal staa uroert");
    }

    // Det afgoerende ved v3 -> v4: intet ved droemmen er aendret, kun HVORDAN den peger paa sin
    // kategori - og begge maskiner udleder selv det samme resultat. Flyttede migreringen
    // updatedAt, ville hver eneste droem se aendret ud og skulle uploades igen, og en migrering
    // paa den ene maskine ville slaa aegte redigeringer paa den anden.
    @Test
    void v3_til_v4_flytter_ikke_droemmenes_updatedAt() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt grønt","visAdvarsel":false,"visKollektiv":false,"startFromThisDate":"2026-01-01","schemaVersion":3}
                """);
        Files.writeString(catsJson, """
                [{"name":"Dyr","symbols":["ræv"],"customOrder":[]}]
                """);
        Files.writeString(dreamsJson, """
                [{"id":"abc-123","categories":[{"name":"Dyr","symbols":["ræv"]}],"indhold":"test","dagrest":"","tolkning":"","dato":"2026-01-05","updatedAt":"2020-01-01T00:00:00Z"}]
                """);

        SchemaMigrator.migrateIfNeeded();

        JsonNode droem = mapper.readTree(dreamsJson.toFile()).get(0);
        assertEquals("2020-01-01T00:00:00Z", droem.get("updatedAt").asText(),
                "migreringen maa ikke stemple droemmen som aendret");

        JsonNode tag = droem.get("categories").get(0);
        assertEquals("dyr", tag.get("id").asText(), "taggen skal pege paa kategoriens id");
        assertFalse(tag.has("name"), "en droems tag baerer id, ikke navn - se CategoryDTO");
    }

    // En tag hvis navn slet ikke staar i kategorilisten - fx efterladt af en omdoebning der kun
    // naaede halvvejs gennem syncen - skal stadig faa et id, udledt efter samme regel, saa begge
    // maskiner ender med det samme og taggen kan finde hjem hvis kategorien dukker op.
    @Test
    void en_foraeldreloes_tag_faar_ogsaa_et_udledt_id() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt grønt","visAdvarsel":false,"visKollektiv":false,"startFromThisDate":"2026-01-01","schemaVersion":3}
                """);
        Files.writeString(catsJson, """
                [{"name":"Dyr","symbols":["ræv"],"customOrder":[]}]
                """);
        Files.writeString(dreamsJson, """
                [{"id":"abc-123","categories":[{"name":"Væsner","symbols":["ork"]}],"indhold":"test","dagrest":"","tolkning":"","dato":"2026-01-05","updatedAt":"2020-01-01T00:00:00Z"}]
                """);

        SchemaMigrator.migrateIfNeeded();

        JsonNode tag = mapper.readTree(dreamsJson.toFile()).get(0).get("categories").get(0);
        assertEquals(Kategoriid.forIndbygget("Væsner"), tag.get("id").asText());
    }

    // Migreringen skal kunne koeres igen uden at give nogen et nyt id - fx hvis appen doede
    // midt i den foerste koersel, saa cats.json naaede at blive skrevet men user.json ikke.
    @Test
    void en_gentaget_migrering_aendrer_ingen_id_er() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt grønt","visAdvarsel":false,"visKollektiv":false,"startFromThisDate":"2026-01-01","schemaVersion":3}
                """);
        Files.writeString(catsJson, """
                [{"id":"noget-helt-andet","name":"Dyr","symbols":["ræv"],"customOrder":[]}]
                """);
        Files.writeString(dreamsJson, """
                [{"id":"abc-123","categories":[{"name":"Dyr","symbols":["ræv"]}],"indhold":"test","dagrest":"","tolkning":"","dato":"2026-01-05","updatedAt":"2020-01-01T00:00:00Z"}]
                """);

        SchemaMigrator.migrateIfNeeded();

        assertEquals("noget-helt-andet", mapper.readTree(catsJson.toFile()).get(0).get("id").asText(),
                "et id der allerede staar i filen skal blive staaende");
        assertEquals("noget-helt-andet", mapper.readTree(dreamsJson.toFile()).get(0).get("categories").get(0).get("id").asText(),
                "droemmens tag skal pege paa netop det id");
    }

    @Test
    void frisk_installation_faar_ingen_meta_fil_af_migrationen() {
        SchemaMigrator.migrateIfNeeded();

        assertFalse(Files.exists(metaJson));
    }

    // DEN RIGTIGE OPGRADERINGSVEJ for alle nuværende brugere: den released udgave (1.5) skriver
    // slet ingen schemaVersion, så deres data er version 0 og skal hele vejen op i ét spring,
    // første gang de starter den nye udgave. Alle trin skal virke i samme kørsel.
    @Test
    void released_data_uden_versionsnummer_migreres_hele_vejen_til_v4() throws IOException {
        writeOldFormatFixtures();

        SchemaMigrator.migrateIfNeeded();

        JsonNode dream = mapper.readTree(dreamsJson.toFile()).get(0);
        assertTrue(dream.hasNonNull("id"), "v0 -> v1: drømmen skal have fået et stabilt id");
        assertFalse(dream.has("lucid"), "v0 -> v1: de flade flag skal være væk");
        assertTrue(dream.hasNonNull("updatedAt"), "v1 -> v2: drømmen skal have fået updatedAt");

        MetaDTO meta = IOutils.loadMeta();
        assertNotNull(meta.categories.updatedAt, "v2 -> v3: kategorierne skal have fået et stempel");
        assertNotNull(meta.temaer.updatedAt);
        assertNotNull(meta.settings.updatedAt);

        assertEquals(SchemaMigrator.CURRENT_SCHEMA_VERSION, mapper.readTree(userJson.toFile()).get("schemaVersion").asInt());
    }

    // Og anden opstart må ikke røre noget: uden dette ville stemplerne blive sat forfra hver
    // gang, og den ene maskine ville altid se ud som den nyeste.
    @Test
    void anden_opstart_efter_fuld_migration_stempler_ikke_forfra() throws IOException {
        writeOldFormatFixtures();
        SchemaMigrator.migrateIfNeeded();
        Instant foersteStempel = IOutils.loadMeta().categories.updatedAt;

        SchemaMigrator.migrateIfNeeded();

        assertEquals(foersteStempel, IOutils.loadMeta().categories.updatedAt);
    }
}
