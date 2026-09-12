package fixit.dreams;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tagkategorien og de to operationer der retter tags bagefter.
 *
 * Omdøbningen er den vigtigste af dem: den er svaret på tastefejl, og tastefejl er hele prisen
 * for at lade brugeren skrive frit. Derfor står både flettningen ("havet" -> "hav" lægger de tre
 * drømme til de toogfyrre) og dét at kun de berørte drømme får nyt updatedAt beskrevet her.
 *
 * user.home peger under test på target/test-home (se surefire i pom.xml), så User bygger sig
 * selv op fra standardkategorierne og rører ikke brugerens rigtige data.
 */
class TagAdministrationTest {

    // To af testene skriver rigtige filer i den delte test-home-mappe (se pom.xml's
    // surefire-konfiguration) for at kunne genstarte User oven på dem. Uden oprydningen ville
    // den ene tests drømme blive læst af den næste - og det ER sket: alle antalDroemmeMedTag
    // gav én for meget, fordi dreams.json fra en tidligere test stadig lå der.
    @BeforeEach
    void setUp() throws IOException {
        User.resetForTests();
        sletFilerne();
    }

    @AfterEach
    void tearDown() throws IOException {
        sletFilerne();
        User.resetForTests();
    }

    private void sletFilerne() throws IOException {
        Files.deleteIfExists(AppPaths.APP_DATA_PATH.resolve("cats.json"));
        Files.deleteIfExists(AppPaths.APP_DATA_PATH.resolve("dreams.json"));
        Files.deleteIfExists(AppPaths.APP_DATA_PATH.resolve("meta.json"));
    }

    // ---------- Hjælpere ----------

    private Dream droemMedTags(LocalDate dato, String... tags) {
        DreamData data = new DreamData();
        data.categories = new ArrayList<>();

        CategoryDTO tagDto = new CategoryDTO();
        tagDto.id = Tag.ID;
        tagDto.symbols = new TreeSet<>(List.of(tags));
        data.categories.add(tagDto);

        data.indhold = "test";
        data.dagrest = "";
        data.tolkning = "";
        data.dato = dato;
        data.updatedAt = Instant.parse("2020-01-01T00:00:00Z"); // fast, så touch() kan ses
        return new Dream(data);
    }

    // ---------- Kategorien skal findes ----------

    @Test
    void tagkategorien_saas_paa_en_installation_der_ikke_har_den() {
        User user = User.getInstance();
        Category tags = user.getTagkategori();

        assertNotNull(tags, "Tagkategorien skal være sået ved indlæsning");
        assertEquals("tags", tags.getId());
        assertEquals("Tags", tags.getName());
    }

    @Test
    void tagkategorien_staar_sidst_i_kategorilisten() {
        // Tal-fanen parrer statistik og kategorier positionelt over getUiCategories, så
        // rækkefølgen skal være den samme begge steder. Sidst er også det sted hvor en ny
        // brugerkategori bagefter kan blive den nye getLast() uden at støde ind i noget.
        User user = User.getInstance();
        ArrayList<Category> ui = user.getUiCategories();
        assertEquals(Tag.ID, ui.get(ui.size() - 1).getId());
    }

    @Test
    void tagkategorien_staar_i_kategorietiketterne() {
        // Uden dette ville Tags ikke kunne vælges i Cirkel-fanens kategorivælger.
        User user = User.getInstance();
        assertTrue(user.getKategoriLabels().contains(Tag.NAVN));
    }

    @Test
    void saaningen_er_idempotent() {
        // Præcis den situation der opstår hver eneste gang appen åbnes efter første gang:
        // kategorilisten indeholder allerede Tags, og der må ikke komme en til.
        User user = User.getInstance();
        IOutils.saveCategories(user.getCategories());
        int antalFoer = user.getUiCategories().size();

        User.resetForTests();
        User genstartet = User.getInstance();

        int antalTagkategorier = 0;
        for (Category c : genstartet.getCategories()) {
            if (Tag.ID.equals(c.getId())) {
                antalTagkategorier++;
            }
        }
        assertEquals(1, antalTagkategorier, "Anden opstart må ikke give to tagkategorier");
        assertEquals(antalFoer, genstartet.getUiCategories().size());
        assertEquals(1, genstartet.getKategoriLabels().stream().filter(Tag.NAVN::equals).count(),
                "Etiketten må heller ikke stå to gange");
    }

    // ---------- Omdøbning ----------

    @Test
    void omdoeb_flytter_tagget_paa_alle_droemme() {
        User user = User.getInstance();
        user.addDream(droemMedTags(LocalDate.of(2026, 9, 1), "havet", "mørke"));
        user.addDream(droemMedTags(LocalDate.of(2026, 9, 2), "havet"));
        UserService service = new UserService(user);

        service.omdoebTag("havet", "hav");

        for (Dream d : user.getDreams().values()) {
            assertTrue(user.tagsPaaDroem(d).contains("hav"), "hav skal stå på drømmen");
            assertFalse(user.tagsPaaDroem(d).contains("havet"), "havet skal være væk");
        }
        assertTrue(user.getTagkategori().getSymbols().contains("hav"));
        assertFalse(user.getTagkategori().getSymbols().contains("havet"));
    }

    @Test
    void omdoeb_til_et_navn_der_findes_i_forvejen_fletter_de_to() {
        // Dét er svaret på en tastefejl: "havet"-drømmene lægges til "hav"-drømmene, og ingen
        // af dem får tagget to gange. TreeSet klarer flettningen uden en linje særkode.
        User user = User.getInstance();
        user.addDream(droemMedTags(LocalDate.of(2026, 9, 1), "hav"));
        user.addDream(droemMedTags(LocalDate.of(2026, 9, 2), "havet"));
        Dream begge = droemMedTags(LocalDate.of(2026, 9, 3), "hav", "havet");
        user.addDream(begge);
        UserService service = new UserService(user);

        service.omdoebTag("havet", "hav");

        assertEquals(3, service.antalDroemmeMedTag("hav"));
        assertEquals(0, service.antalDroemmeMedTag("havet"));
        assertEquals(1, user.tagsPaaDroem(begge).size(),
                "Drømmen der havde begge må kun have hav én gang");
    }

    @Test
    void omdoeb_normaliserer_det_nye_navn() {
        User user = User.getInstance();
        user.addDream(droemMedTags(LocalDate.of(2026, 9, 1), "havet"));
        UserService service = new UserService(user);

        service.omdoebTag("havet", "  #Hav  ");

        assertEquals(1, service.antalDroemmeMedTag("hav"));
        assertTrue(user.getTagkategori().getSymbols().contains("hav"));
    }

    @Test
    void omdoeb_roerer_kun_de_droemme_der_har_tagget() {
        // Hver drøm der får touch() skal uploades igen. En omdøbning der rørte dem alle ville
        // sende hele samlingen afsted for en rettelse der ikke angik den.
        User user = User.getInstance();
        Dream med = droemMedTags(LocalDate.of(2026, 9, 1), "havet");
        Dream uden = droemMedTags(LocalDate.of(2026, 9, 2), "skov");
        user.addDream(med);
        user.addDream(uden);
        UserService service = new UserService(user);
        Instant foer = uden.getUpdatedAt();

        service.omdoebTag("havet", "hav");

        assertNotEquals(foer, med.getUpdatedAt(), "Den berørte drøm skal have nyt updatedAt");
        assertEquals(foer, uden.getUpdatedAt(), "Den uberørte drøm må ikke røres");
    }

    @Test
    void omdoeb_til_samme_navn_goer_ingenting() {
        User user = User.getInstance();
        Dream d = droemMedTags(LocalDate.of(2026, 9, 1), "hav");
        user.addDream(d);
        UserService service = new UserService(user);
        Instant foer = d.getUpdatedAt();

        service.omdoebTag("hav", "hav");

        assertEquals(foer, d.getUpdatedAt());
        assertEquals(1, service.antalDroemmeMedTag("hav"));
    }

    @Test
    void omdoeb_til_tom_tekst_goer_ingenting() {
        User user = User.getInstance();
        user.addDream(droemMedTags(LocalDate.of(2026, 9, 1), "hav"));
        UserService service = new UserService(user);

        service.omdoebTag("hav", "   ");

        assertEquals(1, service.antalDroemmeMedTag("hav"));
    }

    // ---------- Sletning ----------

    @Test
    void slet_fjerner_tagget_fra_alle_droemme() {
        User user = User.getInstance();
        user.addDream(droemMedTags(LocalDate.of(2026, 9, 1), "hav", "mørke"));
        user.addDream(droemMedTags(LocalDate.of(2026, 9, 2), "hav"));
        UserService service = new UserService(user);

        service.fjernTag("hav");

        assertEquals(0, service.antalDroemmeMedTag("hav"));
        assertEquals(1, service.antalDroemmeMedTag("mørke"), "De andre tags skal stå urørt");
        assertFalse(user.getTagkategori().getSymbols().contains("hav"));
        assertEquals(2, user.getDreams().size(), "Selve drømmene røres ikke");
    }

    // ---------- Ordforrådet ----------

    @Test
    void ordforraadet_samler_tags_op_fra_droemmene() {
        // Selvhelbredelsen: cats.json synkroniseres som ét dokument hvor hele listen vinder
        // eller taber, så et tag kan nå at forsvinde fra ordforrådet uden at forsvinde fra
        // drømmene. Ved næste opstart skal det være tilbage.
        User user = User.getInstance();
        user.addDream(droemMedTags(LocalDate.of(2026, 9, 1), "hav", "mørke"));
        IOutils.saveDreams(user.getDreams());
        user.getTagkategori().setSymbols(new TreeSet<>()); // skyen tømte ordforrådet
        IOutils.saveCategories(user.getCategories());

        User.resetForTests();
        User genstartet = User.getInstance();

        assertTrue(genstartet.getTagkategori().getSymbols().contains("hav"));
        assertTrue(genstartet.getTagkategori().getSymbols().contains("mørke"));
    }

    // ---------- Værnene mod de skarpe kanter ----------

    @Test
    void en_droem_uden_tags_giver_et_tomt_men_ikke_null_symbolsaet() {
        // AnalyseService.updateFilteredDreams gør dto.id.equals(...) og dto.symbols.contains(...)
        // uden null-tjek. En CategoryDTO med null i felterne vælter hele filtreringen.
        User user = User.getInstance();
        Dream udenTags = droemMedTags(LocalDate.of(2026, 9, 1));

        assertNotNull(user.tagsPaaDroem(udenTags));
        assertTrue(user.tagsPaaDroem(udenTags).isEmpty());
    }

    @Test
    void tags_taeller_ikke_med_i_brugerens_tre_kategoripladser() {
        // Ellers ville tagfunktionen koste brugeren en kategoriplads uden at nogen spurgte.
        User user = User.getInstance();
        UserService service = new UserService(user);

        int brugerkategorier = 0;
        for (Category c : user.getUiCategories()) {
            if (!Tag.ID.equals(c.getId())) {
                brugerkategorier++;
            }
        }
        assertEquals(6, brugerkategorier, "De seks indbyggede UI-kategorier");
        assertEquals(7, user.getUiCategories().size(), "... plus Tags");
        assertTrue(service.okToAddNewUserDefinedCat());
    }

    @Test
    void tagkategorien_kan_ikke_omdoebes() {
        // Navnet ligger fast: id'et udledes af det, og de tre administrations-combobokse
        // filtreres på det.
        User user = User.getInstance();
        UserService service = new UserService(user);

        assertEquals("Kan ikke omdøbes!", service.renameKategori("Emner", Tag.NAVN));
        assertEquals(Tag.NAVN, user.getTagkategori().getName());
    }
}
