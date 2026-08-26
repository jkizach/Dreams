package fixit.dreams;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class IOutilsDeletedDreamsTest {

    private Path deletedJson;

    @BeforeEach
    void setUp() throws IOException {
        deletedJson = AppPaths.APP_DATA_PATH.resolve("deleted.json");
        Files.deleteIfExists(deletedJson);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(deletedJson);
    }

    @Test
    void sletningskoeen_rundtripper_id_og_tidspunkt() {
        // Tidspunktet skal med og overleve: det er dét gravstenen kommer til at bære, og dermed
        // dét der afgør om en redigering på en anden maskine var før eller efter sletningen.
        LinkedHashMap<String, Instant> koe = new LinkedHashMap<>();
        koe.put("abc-123", Instant.parse("2026-08-26T10:00:00Z"));
        koe.put("def-456", Instant.parse("2026-08-26T11:30:00Z"));

        IOutils.saveDeletedDreams(koe);
        LinkedHashMap<String, Instant> loaded = IOutils.loadDeletedDreams();

        assertEquals(2, loaded.size());
        assertEquals(Instant.parse("2026-08-26T10:00:00Z"), loaded.get("abc-123"));
        assertEquals(Instant.parse("2026-08-26T11:30:00Z"), loaded.get("def-456"));
    }

    @Test
    void manglende_fil_giver_tom_koe_og_ikke_null() {
        // pushTombstones kalder .isEmpty() direkte på resultatet - null ville vælte hver sync
        // på en maskine hvor der endnu aldrig er slettet en drøm.
        assertNotNull(IOutils.loadDeletedDreams());
        assertTrue(IOutils.loadDeletedDreams().isEmpty());
    }

    @Test
    void tom_koe_kan_gemmes_og_laeses_igen() {
        IOutils.saveDeletedDreams(new LinkedHashMap<>());

        assertTrue(IOutils.loadDeletedDreams().isEmpty());
    }
}
