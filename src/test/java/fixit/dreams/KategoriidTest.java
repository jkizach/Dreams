package fixit.dreams;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hele pointen med stabile id'er er at to maskiner kan migrere og oprette uden at tale sammen og
 * alligevel aldrig komme til at mene noget forskelligt om hvad et id peger på. Testene her er
 * skrevet omkring netop det krav.
 */
class KategoriidTest {

    // ---- Migrering: samme kategori skal give samme id på begge maskiner ----

    @Test
    void indbyggede_id_er_udledes_af_navnet_alene() {
        assertEquals("dyr", Kategoriid.forIndbygget("Dyr"));
        assertEquals("farver", Kategoriid.forIndbygget("Farver"));
        assertEquals("chakraer", Kategoriid.forIndbygget("Chakraer"));
        assertEquals("kvaliteter", Kategoriid.forIndbygget(Category.FLAGS_CATEGORY_NAME));
        assertEquals("forloeb", Kategoriid.forIndbygget("Forløb"));
    }

    @Test
    void to_maskiner_udleder_samme_id_for_samme_eksisterende_kategori() {
        // Det er dette krav der gør at forIndbygget IKKE må hashe et tidsstempel ind:
        // cats.json rummer ikke noget oprettelsestidspunkt, så navnet er alt de er enige om.
        for (String navn : List.of("Dyr", "Farver", "Chakraer", "Personer", "Arketyper", "Forløb")) {
            assertEquals(Kategoriid.forIndbygget(navn), Kategoriid.forIndbygget(navn));
        }
    }

    @Test
    void alle_standardkategorier_faar_hver_sit_id() {
        User.resetForTests();
        List<String> id_er = User.getInstance().getCategories().stream()
                .map(c -> Kategoriid.forIndbygget(c.getName()))
                .toList();
        assertEquals(id_er.size(), Set.copyOf(id_er).size(), "to standardkategorier deler id: " + id_er);
    }

    // ---- Oprettelse: to maskiner skal udlede FORSKELLIGE id'er ----

    @Test
    void nye_kategorier_med_samme_navn_paa_to_maskiner_faar_forskellige_id_er() {
        // Opretter man "Musik" på begge maskiner mellem to synkroniseringer, er det to
        // forskellige kategorier. Fik de samme id, ville drømme havne i den forkerte.
        String maskineA = Kategoriid.forNy("Musik", Instant.parse("2026-09-01T10:00:00Z"));
        String maskineB = Kategoriid.forNy("Musik", Instant.parse("2026-09-01T10:04:17Z"));
        assertNotEquals(maskineA, maskineB);
    }

    @Test
    void et_nyt_id_er_stadig_til_at_laese() {
        String id = Kategoriid.forNy("Musik", Instant.parse("2026-09-01T10:00:00Z"));
        assertTrue(id.startsWith("musik-"), "id'et skal stadig kunne genkendes i dreams.json: " + id);
        assertTrue(id.matches("musik-[0-9a-f]{4}"), "forventede slug plus fire hexcifre, fik: " + id);
    }

    @Test
    void samme_navn_og_tidspunkt_giver_samme_id() {
        Instant t = Instant.parse("2026-09-01T10:00:00Z");
        assertEquals(Kategoriid.forNy("Musik", t), Kategoriid.forNy("Musik", t));
    }

    @Test
    void genbrugt_navn_stoeder_ikke_ind_i_den_gamle_kategori() {
        // Omdøb Dyr til Væsner - id'et bliver ved med at være "dyr" - og opret så en ny "Dyr".
        String gammel = Kategoriid.forIndbygget("Dyr");
        String ny = Kategoriid.forNy("Dyr", Instant.parse("2026-09-01T10:00:00Z"));
        assertNotEquals(gammel, ny);
    }

    // ---- Slug ----

    @Test
    void danske_bogstaver_skrives_ud_i_stedet_for_at_forsvinde() {
        assertEquals("forloeb", Kategoriid.tilSlug("Forløb"));
        assertEquals("vaesner", Kategoriid.tilSlug("Væsner"));
        assertEquals("aarstider", Kategoriid.tilSlug("Årstider"));
        // Ville de blive strippet, endte begge som "far"
        assertNotEquals(Kategoriid.tilSlug("Får"), Kategoriid.tilSlug("Far"));
    }

    @Test
    void mellemrum_og_tegnsaetning_bliver_til_bindestreger() {
        assertEquals("moerke-droemme", Kategoriid.tilSlug("Mørke drømme"));
        // Tegnet og mellemrummene omkring det falder sammen til én bindestreg
        assertEquals("mad-drikke", Kategoriid.tilSlug("Mad & drikke"));
        assertEquals("dyr", Kategoriid.tilSlug("  Dyr!  "));
    }

    @Test
    void et_navn_uden_brugbare_tegn_falder_tilbage_paa_en_reserveslug() {
        assertEquals("kategori", Kategoriid.tilSlug("???"));
        assertEquals("kategori", Kategoriid.tilSlug(""));
        assertEquals("kategori", Kategoriid.tilSlug(null));
    }

    // ---- Sidste værn ----

    @Test
    void to_navne_der_udleder_samme_slug_nummereres() {
        // "Dyr" og "Dyr!" giver begge slug'en "dyr"
        assertEquals("dyr", Kategoriid.gørUnik("dyr", List.of()));
        assertEquals("dyr-2", Kategoriid.gørUnik("dyr", List.of("dyr")));
        assertEquals("dyr-3", Kategoriid.gørUnik("dyr", List.of("dyr", "dyr-2")));
    }

    @Test
    void nummereringen_afhaenger_kun_af_hvad_der_er_optaget() {
        // Samme kategoriliste på begge maskiner giver samme nummerering
        List<String> optagede = List.of("dyr", "dyr-2", "farver");
        assertEquals("dyr-3", Kategoriid.gørUnik("dyr", optagede));
        assertEquals("dyr-3", Kategoriid.gørUnik("dyr", optagede));
    }
}
