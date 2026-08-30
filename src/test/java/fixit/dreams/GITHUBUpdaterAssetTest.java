package fixit.dreams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

// Selve opdateringsdialogen og downloaden kræver et JavaFX-toolkit og testes ikke her (se
// resten af suitens afgrænsning). Men VALGET af hvilken fil der skal hentes er ren logik -
// og det er samtidig det sikkerhedsfølsomme sted, fordi filen bagefter bliver eksekveret.
class GITHUBUpdaterAssetTest {

    private static final String SHA = "85e088e897fdcecd7fa996fcffd0aef7674593d0bc1f9b4e14dfc60623110cfa";

    private JsonNode assets(String json) {
        try {
            return new ObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Formen her er klippet efter et rigtigt svar fra GitHub-API'et.
    private JsonNode beggePlatforme() {
        return assets("""
                [
                  {"name": "Drommeappen-2.1.dmg",
                   "browser_download_url": "https://example.invalid/dmg",
                   "digest": "sha256:aaaa",
                   "size": 99810044},
                  {"name": "Drommeappen-2.1.msi",
                   "browser_download_url": "https://example.invalid/msi",
                   "digest": "sha256:%s",
                   "size": 73947176}
                ]
                """.formatted(SHA));
    }

    @Test
    @DisplayName("Windows får .msi'en, ikke .dmg'en")
    void windows_faar_msi() {
        GITHUBUpdater.Asset asset = GITHUBUpdater.vælgAsset(beggePlatforme(), ".msi");

        assertNotNull(asset);
        assertEquals("Drommeappen-2.1.msi", asset.navn());
        assertEquals("https://example.invalid/msi", asset.url());
        assertEquals(73947176L, asset.størrelse());
    }

    @Test
    @DisplayName("Mac får .dmg'en")
    void mac_faar_dmg() {
        GITHUBUpdater.Asset asset = GITHUBUpdater.vælgAsset(beggePlatforme(), ".dmg");

        assertNotNull(asset);
        assertEquals("Drommeappen-2.1.dmg", asset.navn());
    }

    @Test
    @DisplayName("Præfikset skrælles af, så kun den rene hex-sum står tilbage")
    void digest_praefikset_skraelles_af() {
        GITHUBUpdater.Asset asset = GITHUBUpdater.vælgAsset(beggePlatforme(), ".msi");

        assertEquals(SHA, asset.sha256());
    }

    @Test
    @DisplayName("Ukendt platform giver ingen fil - så åbnes release-siden i stedet")
    void ukendt_platform_giver_null() {
        assertNull(GITHUBUpdater.vælgAsset(beggePlatforme(), null));
    }

    // De to næste er hele pointen: mangler eller er checksummen af en anden slags, kan vi ikke
    // stå inde for filen, og så henter vi den slet ikke - frem for at hente den uverificeret.
    @Test
    @DisplayName("Manglende digest giver ingen fil")
    void manglende_digest_giver_null() {
        JsonNode uden = assets("""
                [{"name": "Drommeappen-2.1.msi",
                  "browser_download_url": "https://example.invalid/msi",
                  "size": 73947176}]
                """);

        assertNull(GITHUBUpdater.vælgAsset(uden, ".msi"));
    }

    @Test
    @DisplayName("Digest i et andet format end sha256 giver ingen fil")
    void fremmed_digest_giver_null() {
        JsonNode md5 = assets("""
                [{"name": "Drommeappen-2.1.msi",
                  "browser_download_url": "https://example.invalid/msi",
                  "digest": "md5:abc",
                  "size": 73947176}]
                """);

        assertNull(GITHUBUpdater.vælgAsset(md5, ".msi"));
    }

    @Test
    @DisplayName("Manglende download-url giver ingen fil")
    void manglende_url_giver_null() {
        JsonNode uden = assets("""
                [{"name": "Drommeappen-2.1.msi", "digest": "sha256:%s", "size": 1}]
                """.formatted(SHA));

        assertNull(GITHUBUpdater.vælgAsset(uden, ".msi"));
    }

    @Test
    @DisplayName("Et release uden filer til vores platform giver ingen fil")
    void ingen_matchende_endelse_giver_null() {
        JsonNode kunKildekode = assets("""
                [{"name": "Source code.zip",
                  "browser_download_url": "https://example.invalid/zip",
                  "digest": "sha256:%s",
                  "size": 1}]
                """.formatted(SHA));

        assertNull(GITHUBUpdater.vælgAsset(kunKildekode, ".msi"));
    }

    @Test
    @DisplayName("Filnavnets store og små bogstaver er ligegyldige")
    void endelse_matches_uanset_versaler() {
        JsonNode store = assets("""
                [{"name": "Drommeappen-2.1.MSI",
                  "browser_download_url": "https://example.invalid/msi",
                  "digest": "sha256:%s",
                  "size": 1}]
                """.formatted(SHA));

        assertNotNull(GITHUBUpdater.vælgAsset(store, ".msi"));
    }

    @Test
    @DisplayName("Tom eller manglende asset-liste vælter ikke noget")
    void tom_liste_giver_null() {
        assertNull(GITHUBUpdater.vælgAsset(assets("[]"), ".msi"));
        assertNull(GITHUBUpdater.vælgAsset(null, ".msi"));
        assertNull(GITHUBUpdater.vælgAsset(assets("{}"), ".msi"));
    }
}
