package fixit.dreams;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

// Disse tests skriver rigtige filer i den delte test-home-mappe (se pom.xml's
// surefire-konfiguration) og rydder op efter sig, så de ikke lækker ind i andre testklasser.
class IOutilsMetaTest {

    private Path metaJson;
    private Path catsJson;
    private Path temaerJson;
    private Path userJson;

    @BeforeEach
    void setUp() throws IOException {
        User.resetForTests();
        metaJson = AppPaths.APP_DATA_PATH.resolve("meta.json");
        catsJson = AppPaths.APP_DATA_PATH.resolve("cats.json");
        temaerJson = AppPaths.APP_DATA_PATH.resolve("temaer.json");
        userJson = AppPaths.APP_DATA_PATH.resolve("user.json");
        sletFilerne();
    }

    @AfterEach
    void tearDown() throws IOException {
        sletFilerne();
        User.resetForTests();
    }

    private void sletFilerne() throws IOException {
        Files.deleteIfExists(metaJson);
        Files.deleteIfExists(catsJson);
        Files.deleteIfExists(temaerJson);
        Files.deleteIfExists(userJson);
    }

    private ArrayList<Category> kategorier(String... navne) {
        ArrayList<Category> cats = new ArrayList<>();
        for (String navn : navne) {
            cats.add(new Category(navn));
        }
        return cats;
    }

    private HashMap<String, Tema> temaer(String temaNavn) {
        HashMap<String, String> map = new HashMap<>();
        for (String key : new String[]{"baggrundA", "baggrundB", "baggrundC", "baggrundD", "tekstA", "tekstB", "tekstC", "kant"}) {
            map.put(key, "#000000");
        }
        map.put("temaName", temaNavn);
        map.put("font", "Courier New");

        HashMap<String, Tema> result = new HashMap<>();
        result.put(temaNavn, new Tema(map));
        return result;
    }

    @Test
    void loadMeta_giver_tomt_objekt_naar_filen_mangler() {
        MetaDTO meta = IOutils.loadMeta();

        assertNotNull(meta);
        assertNull(meta.categories.updatedAt);
        assertNull(meta.categories.hash);
    }

    @Test
    void saveMeta_og_loadMeta_rundtripper_alle_tre_stempler() {
        MetaDTO meta = new MetaDTO();
        meta.categories.updatedAt = Instant.parse("2026-01-01T12:00:00Z");
        meta.categories.hash = "hash-cat";
        meta.temaer.updatedAt = Instant.parse("2026-02-02T12:00:00Z");
        meta.temaer.hash = "hash-tema";
        meta.settings.updatedAt = Instant.parse("2026-03-03T12:00:00Z");
        meta.settings.hash = "hash-settings";

        IOutils.saveMeta(meta);
        MetaDTO loaded = IOutils.loadMeta();

        assertEquals(Instant.parse("2026-01-01T12:00:00Z"), loaded.categories.updatedAt);
        assertEquals("hash-cat", loaded.categories.hash);
        assertEquals(Instant.parse("2026-02-02T12:00:00Z"), loaded.temaer.updatedAt);
        assertEquals("hash-tema", loaded.temaer.hash);
        assertEquals(Instant.parse("2026-03-03T12:00:00Z"), loaded.settings.updatedAt);
        assertEquals("hash-settings", loaded.settings.hash);
    }

    @Test
    void loadMeta_taaler_oedelagt_fil() throws IOException {
        Files.writeString(metaJson, "{ dette er ikke JSON");

        MetaDTO meta = IOutils.loadMeta();

        assertNotNull(meta);
        assertNull(meta.categories.hash);
    }

    @Test
    void foerste_gem_af_kategorier_registrerer_hash_men_ikke_tidsstempel() {
        IOutils.saveCategories(kategorier("Farver"));

        MetaDTO meta = IOutils.loadMeta();
        assertNotNull(meta.categories.hash);
        assertNull(meta.categories.updatedAt);
    }

    @Test
    void uaendrede_kategorier_roerer_ikke_meta_filen() throws IOException {
        IOutils.saveCategories(kategorier("Farver"));
        String efterFoerste = Files.readString(metaJson);

        IOutils.saveCategories(kategorier("Farver"));

        assertEquals(efterFoerste, Files.readString(metaJson));
    }

    @Test
    void aendrede_kategorier_saetter_tidsstempel() {
        IOutils.saveCategories(kategorier("Farver"));
        Instant foer = Instant.now();

        IOutils.saveCategories(kategorier("Farver", "Personer"));

        MetaDTO meta = IOutils.loadMeta();
        assertNotNull(meta.categories.updatedAt);
        assertFalse(meta.categories.updatedAt.isBefore(foer));
    }

    @Test
    void aendrede_temaer_saetter_tidsstempel_paa_temaer_alene() {
        IOutils.saveTemaer(temaer("TestTema"));
        IOutils.saveTemaer(temaer("AndetTema"));

        MetaDTO meta = IOutils.loadMeta();
        assertNotNull(meta.temaer.updatedAt);
        assertNull(meta.categories.updatedAt, "et temaskifte må ikke stemple kategorierne");
    }

    @Test
    void foerste_gem_af_indstillinger_registrerer_hash_men_ikke_tidsstempel() {
        IOutils.saveUser(User.getInstance());

        MetaDTO meta = IOutils.loadMeta();
        assertNotNull(meta.settings.hash);
        assertNull(meta.settings.updatedAt);
    }

    @Test
    void aendrede_indstillinger_saetter_tidsstempel() {
        User user = User.getInstance();
        IOutils.saveUser(user);

        user.setVisAdvarsel(!user.isVisAdvarsel());
        IOutils.saveUser(user);

        MetaDTO meta = IOutils.loadMeta();
        assertNotNull(meta.settings.updatedAt);
    }

    // schemaVersion er appens eget felt og må ikke tælle med i indstillingernes fingeraftryk -
    // ellers ville en fremtidig skema-opgradering ligne en brugerændring. Derfor kan stemplet
    // heller ikke bare være en hash af selve user.json.
    @Test
    void indstillingernes_hash_er_ikke_bare_filens_indhold() throws IOException {
        IOutils.saveUser(User.getInstance());

        MetaDTO meta = IOutils.loadMeta();
        assertNotEquals(MetaDTO.hashOf(Files.readString(userJson)), meta.settings.hash);
    }
}
