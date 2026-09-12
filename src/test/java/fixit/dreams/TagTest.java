package fixit.dreams;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reglerne for hvornår to skrevne ord er det samme tag.
 *
 * De ser små ud, og det er derfor de står her: fri tekst har præcis én reel omkostning -
 * at "Hav", " hav " og "#hav" bliver til tre tags i stedet for ét - og normaliseringen er
 * det ene af de to steder den betales. Det andet er omdøbningen i Indstillinger, som
 * TagAdministrationTest dækker.
 */
class TagTest {

    // ---------- Mellemrum ----------

    @Test
    void mellemrum_i_enderne_fjernes() {
        assertEquals("hav", Tag.normaliser("  hav  "));
    }

    @Test
    void indre_mellemrum_trykkes_sammen() {
        assertEquals("gamle huse", Tag.normaliser("gamle    huse"));
    }

    @Test
    void flere_ord_er_tilladt_i_et_tag() {
        // Det er ikke en hashtag, og Enter afslutter tagget - så mellemrum er ikke tvetydigt.
        assertEquals("gamle huse", Tag.normaliser("gamle huse"));
    }

    @Test
    void tabulator_og_linjeskift_taeller_ogsaa_som_mellemrum() {
        assertEquals("gamle huse", Tag.normaliser("gamle\thuse"));
    }

    // ---------- Store bogstaver og havelåge ----------

    @Test
    void store_bogstaver_bliver_smaa() {
        assertEquals("hav", Tag.normaliser("Hav"));
    }

    @Test
    void ledende_havelaage_fjernes() {
        assertEquals("hav", Tag.normaliser("#hav"));
    }

    @Test
    void havelaage_med_mellemrum_efter_fjernes_ogsaa() {
        assertEquals("hav", Tag.normaliser("# hav"));
    }

    @Test
    void havelaage_inde_i_tagget_bliver_staaende() {
        // Kun den ledende er brugerens skrivevane. En inde i ordet er en del af ordet.
        assertEquals("c#", Tag.normaliser("C#"));
    }

    @Test
    void to_skrivemaader_af_samme_tag_normaliserer_ens() {
        assertEquals(Tag.normaliser(" Hav "), Tag.normaliser("#hav"));
    }

    @Test
    void danske_bogstaver_roeres_ikke() {
        // Tagget er data, ikke et id - her skal der ikke slugges.
        assertEquals("mørke", Tag.normaliser("Mørke"));
    }

    // ---------- Når der ikke er noget tag ----------

    @Test
    void tom_tekst_er_ikke_et_tag() {
        assertNull(Tag.normaliser(""));
    }

    @Test
    void kun_mellemrum_er_ikke_et_tag() {
        assertNull(Tag.normaliser("    "));
    }

    @Test
    void kun_en_havelaage_er_ikke_et_tag() {
        assertNull(Tag.normaliser("#"));
    }

    @Test
    void null_er_ikke_et_tag() {
        assertNull(Tag.normaliser(null));
    }

    @Test
    void erEtTag_svarer_paa_det_samme() {
        assertTrue(Tag.erEtTag(" Hav "));
        assertFalse(Tag.erEtTag("   "));
        assertFalse(Tag.erEtTag(null));
    }

    // ---------- Id'et ----------

    @Test
    void id_et_udledes_af_navnet_alene() {
        // Bærende for to-maskiners brug: begge maskiner skal nå frem til "tags" uden at tale
        // sammen, ellers ville de ikke genkende hinandens tagkategori som den samme. Derfor
        // forIndbygget (ren slug) og ikke forNy (slug + hash af oprettelsestidspunktet).
        assertEquals("tags", Tag.ID);
        assertEquals(Kategoriid.forIndbygget(Tag.NAVN), Tag.ID);
    }

    @Test
    void tagkategorien_hedder_Tags() {
        assertEquals("Tags", Tag.NAVN);
    }
}
