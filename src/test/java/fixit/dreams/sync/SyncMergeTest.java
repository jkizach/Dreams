package fixit.dreams.sync;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SyncMergeTest {

    private static final Instant EARLIER = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant LATER = Instant.parse("2026-01-02T00:00:00Z");

    @Test
    void sky_vinder_naar_drommen_ikke_findes_lokalt() {
        assertTrue(SyncMerge.cloudWins(null, LATER));
    }

    @Test
    void sky_vinder_ikke_naar_sky_data_mangler() {
        assertFalse(SyncMerge.cloudWins(EARLIER, null));
    }

    @Test
    void sky_vinder_naar_sky_er_nyere() {
        assertTrue(SyncMerge.cloudWins(EARLIER, LATER));
    }

    @Test
    void lokal_vinder_naar_lokal_er_nyere() {
        assertFalse(SyncMerge.cloudWins(LATER, EARLIER));
    }

    @Test
    void lokal_vinder_ved_ens_tidsstempler() {
        assertFalse(SyncMerge.cloudWins(EARLIER, EARLIER));
    }
}
