package fixit.dreams;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sætningerne er for de flestes vedkommende taget fra prøvekørslen mod de rigtige drømme - både
 * dem der skal findes, og dem der fandt noget forkert før reglerne om "jeg" og forbehold.
 */
class KvalitetsgenkendelseTest {

    private static final List<String> ALLE = List.of("Lucid", "Praktiserer", "Modsatkønnet", "Mareridt", "Holografisk");

    private List<String> find(String tekst) {
        return Kvalitetsgenkendelse.find(tekst, ALLE).stream().map(Symbolgenkendelse.Fund::symbol).toList();
    }

    // ---- Lucid ----

    @Test
    void lucid_findes_naar_jeg_bliver_det() {
        assertEquals(List.of("Lucid"), find("Så ser jeg et køleskab og nu bliver jeg delvist lucid og ønsker en øl."));
        assertEquals(List.of("Lucid"), find("Jeg gider ikke være her og er nu lucid."));
        assertEquals(List.of("Lucid"), find("Nu bliver jeg klar over at det er en drøm, og jeg slapper af."));
    }

    @Test
    void lucid_med_forbehold_findes_ikke() {
        assertEquals(List.of(), find("Jeg står i døren. Jeg er ikke lucid, men jeg er ligesom klar over noget."));
        assertEquals(List.of(), find("Nu bliver jeg næsten lucid, men vågner bare i stedet."));
        assertEquals(List.of(), find("Restaurant med Anna. Ikke lucid, men jeg kan mærke min krop."));
        assertEquals(List.of(), find("Jeg kommenterer drømmen fra et andet sted - mere lucidt, men uden at være det."));
    }

    @Test
    void lucid_om_en_anden_findes_ikke() {
        assertEquals(List.of(), find("Det ser frisk ud og hun er ligesom mere lucid."));
        assertEquals(List.of(), find("Det minder om drømme hvor man næsten er lucid."));
        assertEquals(List.of(), find("Det er svært at læse (fordi det er en drøm)."));
    }

    // ---- Praktiserer ----

    @Test
    void praktiserer_findes_naar_det_er_mig() {
        assertEquals(List.of("Praktiserer"), find("Jeg sidder og mediterer ved min seng."));
        assertEquals(List.of("Praktiserer"), find("Der er ingen udsigt, og der mediterer jeg."));
        assertEquals(List.of("Praktiserer"), find("Jeg sætter mig i skrædderstilling og kigger ud og mediterer lidt."));
        assertEquals(List.of("Praktiserer"), find("Vi mediterer."));
        assertEquals(List.of("Praktiserer"), find("Hun fornemmer at jeg har mediteret."));
        assertEquals(List.of("Praktiserer"), find("Jeg dobbeltretter, mens han taler."));
    }

    @Test
    void praksisnavneord_med_jeg_findes() {
        assertEquals(List.of("Praktiserer"), find("Jeg siger mit mantra."));
        assertEquals(List.of("Praktiserer"), find("Jeg siger hjertebøn."));
        assertEquals(List.of("Praktiserer"), find("Jeg er i dyb bøn."));
        assertEquals(List.of("Praktiserer"), find("Jeg laver neutral iagttagelse."));
        assertEquals(List.of("Praktiserer"), find("Jeg laver vipassana."));
        assertEquals(List.of("Praktiserer"), find("Jeg laver vypasana."));
        assertEquals(List.of("Praktiserer"), find("Jeg er i stilhedsmeditation."));
        assertEquals(List.of("Praktiserer"), find("Jeg er i en dyb meditation."));
        assertEquals(List.of("Praktiserer"), find("Jeg er i en meditativ tilstand."));
    }

    @Test
    void meditation_som_emne_er_ikke_at_praktisere() {
        assertEquals(List.of(), find("Vi skal have undervisning, muligvis om meditation."));
        assertEquals(List.of(), find("Jeg skal på meditationskursus."));
    }

    @Test
    void andre_der_mediterer_eller_beder_findes_ikke() {
        assertEquals(List.of(), find("Erik sidder i skrædderstilling på en måtte og mediterer med klare øjne."));
        assertEquals(List.of(), find("Nu har han også taget den identitet at han mediterer."));
        assertEquals(List.of(), find("Det er som om hun sidder og praktiserer der om natten."));
        assertEquals(List.of(), find("Hun beder mig bruse på hendes hoved."));
        assertEquals(List.of(), find("Jeg beder hende vente."));
    }

    @Test
    void praksis_med_ikke_foran_findes_ikke() {
        // "jeg ved jeg drømmer" er til gengæld lucid
        assertEquals(List.of("Lucid"), find("Jeg ved jeg drømmer, men er ikke vågen nok til at praktisere."));
    }

    @Test
    void et_ikke_i_en_anden_del_saetning_gaelder_ikke() {
        assertEquals(List.of("Praktiserer"), find("Jeg vil ikke forstyrre ham, så jeg mediterer."));
    }

    // ---- Modsatkønnet ----

    @Test
    void modsatkoennet_findes_for_begge_koen() {
        assertEquals(List.of("Modsatkønnet"), find("Jeg er en kvinde med krøllet brunt hår."));
        assertEquals(List.of("Modsatkønnet"), find("Jeg er en kvindelig soldat i en mørkeblå uniform."));
        assertEquals(List.of("Modsatkønnet"), find("Jeg er nu en kvinde."));
        assertEquals(List.of("Modsatkønnet"), find("Jeg er en ung mand med fuldskæg."));
        assertEquals(List.of("Modsatkønnet"), find("Jeg kan mærke at jeg som kvinde er attraktiv."));
    }

    @Test
    void en_mand_i_naerheden_er_ikke_modsatkoennet() {
        assertEquals(List.of(), find("Jeg er hos en mand i hans lejlighed."));
        assertEquals(List.of(), find("Jeg er med en kvinde i en bil."));
        assertEquals(List.of(), find("Hun er en kvinde med sort hår."));
    }

    // ---- Mareridt og Holografisk ----

    @Test
    void mareridt_findes_paa_ordet_men_ikke_med_ikke_foran() {
        assertEquals(List.of("Mareridt"), find("Det er sjovt først, men så bliver det til et mareridt."));
        assertEquals(List.of(), find("Det er heldigvis ikke et mareridt."));
    }

    @Test
    void holografisk_findes_men_ikke_naesten() {
        assertEquals(List.of("Holografisk"), find("Nu bliver drømmen nok holografisk, fordi jeg sidder to steder."));
        assertEquals(List.of(), find("Drømmen er næsten holografisk, så tingene sker samtidig."));
        assertEquals(List.of(), find("Det sker samtidig (tæt på holografisk)."));
        assertEquals(List.of(), find("Nu er drømmen sådan på vej til holografisk."));
        assertEquals(List.of(), find("Mens hun taler er drømmen delvist holografisk."));
        assertEquals(List.of(), find("Nu er der et lille holografisk element."));
    }

    // ---- Fælles ----

    @Test
    void kun_tilladte_kvaliteter_foreslaas_og_i_fast_raekkefoelge() {
        String tekst = "Jeg er en kvinde. Jeg bliver lucid. Drømmen bliver holografisk. Jeg mediterer.";
        assertEquals(List.of("Lucid", "Praktiserer", "Modsatkønnet", "Holografisk"), find(tekst));
        assertEquals(List.of("Praktiserer"),
                Kvalitetsgenkendelse.find(tekst, List.of("Praktiserer")).stream().map(Symbolgenkendelse.Fund::symbol).toList());
        assertEquals(List.of(), Kvalitetsgenkendelse.find(tekst, List.of()));
    }

    @Test
    void fundet_har_kvaliteternes_kategori_og_stedet_i_teksten() {
        Symbolgenkendelse.Fund fund = Kvalitetsgenkendelse.find("Jeg sidder og mediterer ved min seng.", ALLE).get(0);
        assertEquals(Category.ID_KVALITETER, fund.kategoriId());
        assertEquals("Jeg sidder og mediterer ved min…", fund.fundetSom());
    }

    @Test
    void tom_tekst_giver_intet() {
        assertEquals(List.of(), find(""));
        assertEquals(List.of(), find(null));
    }
}
