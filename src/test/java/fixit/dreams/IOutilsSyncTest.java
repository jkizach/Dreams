package fixit.dreams;

import fixit.dreams.sync.SyncDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class IOutilsSyncTest {

    private Path syncJson;

    @BeforeEach
    void setUp() throws IOException {
        syncJson = AppPaths.APP_DATA_PATH.resolve("sync.json");
        Files.deleteIfExists(syncJson);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(syncJson);
    }

    private SyncDTO fixture() {
        SyncDTO dto = new SyncDTO();
        dto.email = "test@example.com";
        dto.refreshToken = "refresh-token-123";
        dto.uid = "uid-abc";
        dto.syncEnabled = true;
        dto.lastSyncedAt = Instant.parse("2026-01-01T12:00:00Z");
        return dto;
    }

    @Test
    void saveSync_og_loadSync_rundtripper_alle_felter() {
        IOutils.saveSync(fixture());

        SyncDTO loaded = IOutils.loadSync();

        assertNotNull(loaded);
        assertEquals("test@example.com", loaded.email);
        assertEquals("refresh-token-123", loaded.refreshToken);
        assertEquals("uid-abc", loaded.uid);
        assertTrue(loaded.syncEnabled);
        assertEquals(Instant.parse("2026-01-01T12:00:00Z"), loaded.lastSyncedAt);
    }

    @Test
    void loadSync_returnerer_null_naar_fil_mangler() {
        assertNull(IOutils.loadSync());
    }

    @Test
    void deleteSync_fjerner_filen() {
        IOutils.saveSync(fixture());
        assertTrue(Files.exists(syncJson));

        IOutils.deleteSync();

        assertFalse(Files.exists(syncJson));
        assertNull(IOutils.loadSync());
    }
}
