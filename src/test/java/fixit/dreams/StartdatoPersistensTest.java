package fixit.dreams;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Hvad user.json husker om startdatoen - og hvad den med vilje IKKE husker.
//
// Skriver rigtige filer i den delte test-home-mappe (se pom.xml's surefire-konfiguration)
// og rydder op efter sig.
class StartdatoPersistensTest {

    private static final LocalDate FOERSTE_DROEM = LocalDate.of(2019, 3, 4);
    private static final LocalDate VALGT = LocalDate.of(2026, 1, 1);

    private Path userJson;

    @BeforeEach
    void setUp() throws IOException {
        User.resetForTests();
        userJson = AppPaths.APP_DATA_PATH.resolve("user.json");
        ryd();
    }

    @AfterEach
    void tearDown() throws IOException {
        ryd();
        User.resetForTests();
    }

    private void ryd() throws IOException {
        Files.deleteIfExists(userJson);
        Files.deleteIfExists(AppPaths.APP_DATA_PATH.resolve("meta.json"));
        Files.deleteIfExists(AppPaths.APP_DATA_PATH.resolve("dreams.json"));
        Files.deleteIfExists(AppPaths.APP_DATA_PATH.resolve("cats.json"));
    }

    private void tilføjDrøm(User user, LocalDate dato) {
        DreamData data = new DreamData();
        data.categories = new ArrayList<>();
        data.indhold = "test";
        data.dagrest = "";
        data.tolkning = "";
        data.dato = dato;
        user.addDream(new Dream(data));
    }

    // En gammel fil, skrevet før feltet fandtes.
    private void skrivGammelUserJson(LocalDate startdato) throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt grønt","visAdvarsel":false,"visKollektiv":false,
                 "visHolografisk":false,"startFromThisDate":"%s","schemaVersion":4}
                """.formatted(startdato));
    }

    @Test
    void en_udledt_startdato_foelger_den_aeldste_droem() {
        User user = User.getInstance();
        tilføjDrøm(user, FOERSTE_DROEM);

        assertFalse(user.harValgtStartdato());
        assertEquals(FOERSTE_DROEM, user.getStartFromThisDate());

        // Kommer der en ældre drøm ned fra skyen midt i sessionen, følger datoen med af sig selv.
        tilføjDrøm(user, FOERSTE_DROEM.minusYears(2));
        assertEquals(FOERSTE_DROEM.minusYears(2), user.getStartFromThisDate());
    }

    // Kernen: en udledt dato må ALDRIG fryses ned i filen. Gør den det, kan næste opstart ikke
    // se forskel på den og et valg - og så tør ingenting flytte den igen.
    @Test
    void en_udledt_startdato_gemmes_ikke_som_en_dato() {
        User user = User.getInstance();
        tilføjDrøm(user, FOERSTE_DROEM);

        IOutils.saveUser(user);

        UserDTO paaDisken = IOutils.loadUser();
        assertNull(paaDisken.startFromThisDate);
        assertFalse(paaDisken.startDatoValgtAfBruger);
    }

    @Test
    void et_valg_gemmes_og_overlever_en_genstart() {
        User user = User.getInstance();
        tilføjDrøm(user, FOERSTE_DROEM);
        user.vælgStartFromThisDate(VALGT);

        IOutils.saveUser(user);
        User.resetForTests();

        User genstartet = User.getInstance();
        assertTrue(genstartet.harValgtStartdato());
        assertEquals(VALGT, genstartet.getStartFromThisDate());
    }

    // 2.0-fejlen, sat på skrift: dengang blev valget rullet tilbage til første drøm ved hver
    // opstart. Nu er en valgt dato fredet, også med langt ældre drømme i samlingen.
    @Test
    void et_valg_roeres_ikke_af_aeldre_droemme() {
        User user = User.getInstance();
        tilføjDrøm(user, FOERSTE_DROEM);
        user.vælgStartFromThisDate(VALGT);

        tilføjDrøm(user, FOERSTE_DROEM.minusYears(5));

        assertEquals(VALGT, user.getStartFromThisDate());
    }

    // Migrering: en fil fra før 2.1 siger ikke om datoen var valgt. Den behandles som et valg
    // og bliver stående præcis hvor brugeren så den sidst.
    @Test
    void en_gammel_fil_uden_flag_beholder_sin_dato() throws IOException {
        skrivGammelUserJson(VALGT);

        User user = User.getInstance();

        assertTrue(user.harValgtStartdato());
        assertEquals(VALGT, user.getStartFromThisDate());
    }

    /* ---- migrering: det nye felt må ikke gøre noget ved data der allerede findes ---- */

    // En rigtig 1.5-installation: ingen schemaVersion (data er version 0), intet visHolografisk,
    // og selvfølgelig intet startDatoValgtAfBruger. Hele migreringen 0 -> 4 løber igennem her.
    // Startdatoen ligger EFTER første drøm - altså præcis den slags valg 2.0 rullede tilbage.
    @Test
    void data_fra_1_5_beholder_sin_startdato_hele_vejen_op() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt grønt","visAdvarsel":false,"visKollektiv":false,"startFromThisDate":"2026-01-01"}
                """);
        Files.writeString(AppPaths.APP_DATA_PATH.resolve("cats.json"), """
                [{"name":"Farver","symbols":["rød","blå"],"customOrder":[]}]
                """);
        Files.writeString(AppPaths.APP_DATA_PATH.resolve("dreams.json"), """
                [{"categories":[],"lucid":true,"praktiserer":false,"modsat":false,"arketypisk":false,
                  "ompraksis":false,"mareridt":true,"kollektiv":false,"advarsel":false,
                  "indhold":"test","dagrest":"","tolkning":"","dato":"2025-06-01"}]
                """);

        User user = User.getInstance();

        assertTrue(user.harValgtStartdato(), "en dato uden flag skal regnes som brugerens valg");
        assertEquals(VALGT, user.getStartFromThisDate(),
                "startdatoen blev rullet tilbage til første drøm - det var netop 2.0-fejlen");

        // Og den bliver liggende hen over et gem: filen får flaget, datoen står uændret.
        IOutils.saveUser(user);
        UserDTO paaDisken = IOutils.loadUser();
        assertEquals(VALGT, paaDisken.startFromThisDate);
        assertTrue(paaDisken.startDatoValgtAfBruger);
    }

    // En 2.0-installation: skema v4, alle felter på plads - kun det nye flag mangler.
    @Test
    void data_fra_2_0_beholder_sin_startdato() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt grønt","visAdvarsel":true,"visKollektiv":false,
                 "visHolografisk":true,"startFromThisDate":"2026-01-01","schemaVersion":4}
                """);

        User user = User.getInstance();
        tilføjDrøm(user, FOERSTE_DROEM);

        assertTrue(user.harValgtStartdato());
        assertEquals(VALGT, user.getStartFromThisDate());
        assertTrue(user.isVisHolografisk(), "resten af indstillingerne skal være uberørte");
    }

    // Et valg skal på disken med det samme - ikke først ved appluk, hvor et gem kan blive sprunget
    // over fordi syncen har hentet indstillingerne ned. Og det må kun røre selve datoen: resten af
    // filen kan være det, syncen lige har lagt der.
    @Test
    void gemStartDato_skriver_kun_datoen_og_lader_resten_staa() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"hentet fra skyen","visAdvarsel":true,"visKollektiv":false,
                 "visHolografisk":true,"startFromThisDate":null,"startDatoValgtAfBruger":false,
                 "schemaVersion":4}
                """);

        User user = User.getInstance();
        user.vælgStartFromThisDate(VALGT);
        IOutils.gemStartDato(user);

        UserDTO paaDisken = IOutils.loadUser();
        assertEquals(VALGT, paaDisken.startFromThisDate);
        assertTrue(paaDisken.startDatoValgtAfBruger);
        assertEquals("hentet fra skyen", paaDisken.foretrukneTema);
        assertTrue(paaDisken.visAdvarsel);
        assertTrue(paaDisken.visHolografisk);
    }

    // Modstykket til de to ovenfor: en user.json skrevet af en FREMTIDIG udgave, med et felt vi
    // ikke kender. Uden @JsonIgnoreProperties på UserDTO afviser Jackson hele filen, loadUser
    // returnerer null, og indstillingerne falder tilbage til standardtema og nulstillede flag.
    // Her skal den bare springe feltet over og læse resten.
    @Test
    void en_nyere_fil_med_et_ukendt_felt_laeses_stadig() throws IOException {
        Files.writeString(userJson, """
                {"foretrukneTema":"mørkt blåt","visAdvarsel":true,"visKollektiv":false,
                 "visHolografisk":true,"startFromThisDate":"2026-01-01","startDatoValgtAfBruger":true,
                 "etFeltFra_2_2":"noget vi ikke kender endnu","schemaVersion":4}
                """);

        UserDTO paaDisken = IOutils.loadUser();

        assertNotNull(paaDisken, "hele filen blev afvist på grund af ét ukendt felt");
        assertEquals("mørkt blåt", paaDisken.foretrukneTema);
        assertEquals(VALGT, paaDisken.startFromThisDate);
        assertTrue(paaDisken.startDatoValgtAfBruger);
    }

    /* ---- beskeden fra syncen til brugerfladen ---- */

    @Test
    void hentede_droemme_husker_den_aeldste_dato() {
        User user = User.getInstance();

        user.noterHentetDrøm(VALGT);
        user.noterHentetDrøm(FOERSTE_DROEM);
        user.noterHentetDrøm(VALGT.plusDays(10));
        user.noterHentetDrøm(null);

        assertEquals(FOERSTE_DROEM, user.getÆldsteHentedeDrøm());

        user.glemHentedeDrømme();
        assertNull(user.getÆldsteHentedeDrøm());
    }
}
