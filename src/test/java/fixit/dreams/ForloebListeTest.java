package fixit.dreams;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Forløbsfanens to lister, og hvad der skal til for at de følger med når en drøm ændrer sig.
//
// Til venstre (forloebValgListe) står de drømme der ER markeret med en forløbsfase; den bygges
// om af statistiksignalet. Til højre (forloebListe) står søgeresultatet omkring den valgte
// drøm; den bygges kun om når nogen beder om det - og gjorde den ikke det efter en redigering,
// blev den stående med den gamle tekst.
class ForloebListeTest {

    private static final LocalDate DATO = LocalDate.of(2026, 5, 5);

    private User user;
    private AnalyseService service;

    @BeforeEach
    void setUp() {
        User.resetForTests();
        user = User.getInstance();
        service = new AnalyseService(user);
    }

    @AfterEach
    void tearDown() {
        User.resetForTests();
    }

    private Dream droem(String id, String indhold, LocalDate dato) {
        DreamData data = new DreamData();
        data.id = id;
        data.categories = new ArrayList<>();
        data.indhold = indhold;
        data.dagrest = "";
        data.tolkning = "";
        data.dato = dato;
        return new Dream(data);
    }

    private void giv(Dream d, String forloebssymbol) {
        CategoryDTO cdto = new CategoryDTO();
        cdto.id = Category.ID_FORLOEB;
        cdto.symbols = new TreeSet<>(List.of(forloebssymbol));
        d.setCategory(cdto);
    }

    /* ---- venstre liste: hvem er markeret med en fase ---- */

    @Test
    void kun_droemme_med_forloebssymboler_kommer_med() {
        Dream uden = droem("a", "ingen fase", DATO);
        Dream med = droem("b", "har en fase", DATO);
        giv(med, "begyndelse");
        user.addDream(uden);
        user.addDream(med);

        service.updateForloeb();

        assertEquals(1, service.getForloeb().size());
        assertEquals("b", service.getForloeb().get(0).getId());
    }

    // Det er den her vej rundt en redigering går: drømmen havde ingen fase, og får en.
    @Test
    void en_droem_der_faar_en_fase_kommer_med_i_listen() {
        Dream d = droem("a", "en drøm", DATO);
        user.addDream(d);
        service.updateForloeb();
        assertTrue(service.getForloeb().isEmpty());

        giv(d, "begyndelse");
        user.addDream(d);
        service.updateForloeb();

        assertEquals(1, service.getForloeb().size(), "drømmen kom ikke med efter redigeringen");
    }

    @Test
    void en_droem_der_mister_sin_fase_falder_ud_igen() {
        Dream d = droem("a", "en drøm", DATO);
        giv(d, "begyndelse");
        user.addDream(d);
        service.updateForloeb();
        assertEquals(1, service.getForloeb().size());

        CategoryDTO tom = new CategoryDTO();
        tom.id = Category.ID_FORLOEB;
        tom.symbols = new TreeSet<>();
        d.setCategory(tom);
        user.addDream(d);
        service.updateForloeb();

        assertTrue(service.getForloeb().isEmpty());
    }

    /* ---- højre liste: søgeresultatet skal vise den nye tekst ---- */

    // En DreamDTO er et øjebliksbillede, ikke en henvisning. Uden genopfriskningen står
    // søgeresultatet med den tekst drømmen havde da man trykkede "Vis liste".
    @Test
    void en_redigeret_droem_vises_med_sin_nye_tekst() {
        Dream d = droem("a", "gammel tekst", DATO);
        user.addDream(d);
        service.refreshForloebDreams(DATO, 0, 0);
        assertEquals(1, service.getForloebDreams().size());

        d.setIndhold("ny tekst");
        assertTrue(service.getForloebDreams().get(0).getVisbartIndhold().contains("gammel tekst"),
                "forudsætningen holder ikke: DTO'en er åbenbart ikke et øjebliksbillede");

        service.genopfriskForloebDreams();

        assertTrue(service.getForloebDreams().get(0).getVisbartIndhold().contains("ny tekst"),
                "søgeresultatet står stadig med den gamle tekst efter en redigering");
    }

    // Slettes drømmen mens søgeresultatet står med den, må listen ikke blive ved med at vise
    // den - der er ikke længere noget at åbne.
    @Test
    void en_slettet_droem_falder_ud_af_soegeresultatet() {
        user.addDream(droem("a", "en drøm", DATO));
        user.addDream(droem("b", "en anden drøm", DATO));
        service.refreshForloebDreams(DATO, 0, 0);
        assertEquals(2, service.getForloebDreams().size());

        user.deleteDream("a");
        service.genopfriskForloebDreams();

        assertEquals(1, service.getForloebDreams().size());
        assertEquals("b", service.getForloebDreams().get(0).getId());
    }
}
