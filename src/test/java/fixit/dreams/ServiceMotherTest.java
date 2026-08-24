package fixit.dreams;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ServiceMotherTest {

    private ServiceMother serviceMother;

    @BeforeEach
    void setUp() {
        User.resetForTests();
        serviceMother = new ServiceMother(User.getInstance());
    }

    @Test
    void isInRange_dato_lig_start_er_i_range() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        assertTrue(serviceMother.isInRange(start, start, end));
    }

    @Test
    void isInRange_dato_lig_slut_er_i_range() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        assertTrue(serviceMother.isInRange(end, start, end));
    }

    @Test
    void isInRange_dato_strengt_imellem_er_i_range() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate midt = LocalDate.of(2026, 1, 15);
        LocalDate end = LocalDate.of(2026, 1, 31);
        assertTrue(serviceMother.isInRange(midt, start, end));
    }

    @Test
    void isInRange_dato_for_start_er_ikke_i_range() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        LocalDate for_tidligt = LocalDate.of(2025, 12, 31);
        assertFalse(serviceMother.isInRange(for_tidligt, start, end));
    }

    @Test
    void isInRange_dato_efter_slut_er_ikke_i_range() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        LocalDate for_sent = LocalDate.of(2026, 2, 1);
        assertFalse(serviceMother.isInRange(for_sent, start, end));
    }

    @Test
    void isInRange_enkeltdags_range_hvor_alle_tre_er_ens() {
        LocalDate dato = LocalDate.of(2026, 1, 15);
        assertTrue(serviceMother.isInRange(dato, dato, dato));
    }
}
