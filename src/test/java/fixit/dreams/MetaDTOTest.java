package fixit.dreams;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MetaDTOTest {

    @Test
    void foerste_indhold_registrerer_hash_uden_at_saette_tidsstempel() {
        MetaDTO.Stamp stamp = new MetaDTO.Stamp();

        assertTrue(stamp.stampIfChanged(MetaDTO.hashOf("[]")));

        assertNotNull(stamp.hash);
        assertNull(stamp.updatedAt, "en frisk installations standarddata er ikke 'redigeret lige nu'");
    }

    @Test
    void uaendret_indhold_bumper_ikke_tidsstemplet() {
        MetaDTO.Stamp stamp = new MetaDTO.Stamp();
        stamp.stampIfChanged(MetaDTO.hashOf("[]"));
        stamp.updatedAt = Instant.parse("2026-01-01T12:00:00Z");

        assertFalse(stamp.stampIfChanged(MetaDTO.hashOf("[]")));

        assertEquals(Instant.parse("2026-01-01T12:00:00Z"), stamp.updatedAt);
    }

    @Test
    void aendret_indhold_saetter_nyt_tidsstempel() {
        MetaDTO.Stamp stamp = new MetaDTO.Stamp();
        stamp.stampIfChanged(MetaDTO.hashOf("[]"));
        stamp.updatedAt = Instant.parse("2026-01-01T12:00:00Z");

        assertTrue(stamp.stampIfChanged(MetaDTO.hashOf("[{\"name\":\"Farver\"}]")));

        assertTrue(stamp.updatedAt.isAfter(Instant.parse("2026-01-01T12:00:00Z")));
    }

    // Migrations-tilfældet: SchemaMigrator sætter et tidsstempel uden at kende hash'en, og
    // det første gem bagefter må registrere hash'en uden at flytte tidspunktet.
    @Test
    void foerste_hash_efter_migration_bevarer_migrationens_tidsstempel() {
        MetaDTO.Stamp stamp = new MetaDTO.Stamp();
        stamp.updatedAt = Instant.parse("2026-01-01T12:00:00Z");

        assertTrue(stamp.stampIfChanged(MetaDTO.hashOf("[]")));

        assertEquals(Instant.parse("2026-01-01T12:00:00Z"), stamp.updatedAt);
    }

    @Test
    void hashOf_er_stabil_og_skelner_mellem_indhold() {
        assertEquals(MetaDTO.hashOf("abc"), MetaDTO.hashOf("abc"));
        assertNotEquals(MetaDTO.hashOf("abc"), MetaDTO.hashOf("abd"));
        assertEquals(64, MetaDTO.hashOf("abc").length());
    }

    @Test
    void ny_metaDTO_har_alle_tre_stempler_klar() {
        MetaDTO meta = new MetaDTO();

        assertNotNull(meta.categories);
        assertNotNull(meta.temaer);
        assertNotNull(meta.settings);
        assertNull(meta.categories.updatedAt);
    }
}
