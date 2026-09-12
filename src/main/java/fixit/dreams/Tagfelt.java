package fixit.dreams;

import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.stage.Popup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/**
 * Binder et tekstfelt og en chip-række sammen til ét tagfelt. Kontrollerne selv står i FXML,
 * som konventionen i projektet er; det er kun opførslen der ligger her - og den ligger her ÉT
 * sted, fordi feltet bruges to steder (Ny drøm og Rediger drøm) og ellers ville kunne nå at
 * opføre sig forskelligt de to steder uden at nogen opdagede det.
 *
 *     Tags
 *     [ hav_                    ]
 *        | havet
 *        | havfrue
 *     ( hav x ) ( mørke x )
 *
 * Chippen er en helt almindelig Button med appens globale knapstyling. Det er ikke bare
 * bekvemt: CSSUpdater.init() kopierer kun temafilen ud hvis den ikke findes i forvejen, så en
 * ny CSS-regel ville aldrig nå frem til en installation der allerede kører.
 *
 * Forslagslisten er skrevet i hånden oven på Popup og ListView, og det er med vilje.
 * ControlsFX har en færdig TextFields.bindAutoCompletion(), men dens AutoCompletionBinding
 * bruger com.sun.javafx.event.EventHandlerManager - en intern JavaFX-klasse som javafx.base
 * ikke eksporterer til org.controlsfx.controls. På modulstien kaster den IllegalAccessError,
 * og det eneste plaster er et --add-exports der SKAL følge med hele vejen gennem jlink og ind
 * i den installerede MSI's JLINK_VM_OPTIONS. Præcis den slags modulafhængighed har kostet
 * appen dyrt før (se kommentarerne i module-info.java om jdk.localedata og jdk.crypto.ec),
 * hvor fejlen kun viste sig i den installerede udgave. Her er der ingen at glemme.
 */
class Tagfelt {

    private static final int RAEKKEHOEJDE = 24;
    private static final int MAKS_SYNLIGE_FORSLAG = 6;

    private final TextField felt;
    private final FlowPane chips;
    private final Category kategori;

    /** Tags på drømmen lige nu. TreeSet: sorteret, og uden dubletter uden særkode. */
    private final TreeSet<String> valgte = new TreeSet<>();

    private final Popup forslagPopup = new Popup();
    private final ListView<String> forslagListe = new ListView<>();

    /** Sandt mens vi selv retter i feltet, så vores egen ændring ikke åbner forslagslisten igen. */
    private boolean stille = false;

    Tagfelt(TextField felt, FlowPane chips, Category kategori) {
        this.felt = felt;
        this.chips = chips;
        this.kategori = kategori;

        // For langt tages der slet ikke imod. Alternativet - at afkorte lydløst - ville lave to
        // tags man ikke kan se forskel på, og det er præcis den slags dublet tags skal undgå.
        felt.setTextFormatter(new TextFormatter<>(aendring ->
                aendring.getControlNewText().length() <= Tag.MAKS_LAENGDE ? aendring : null));

        forslagListe.getStyleClass().add("custom-list-view"); // samme styling som de andre lister
        forslagPopup.getContent().add(forslagListe);
        forslagPopup.setAutoHide(true);
        forslagPopup.setHideOnEscape(true);
        forslagListe.setOnMouseClicked(e -> tagValgtForslag());

        felt.textProperty().addListener((obs, gammel, ny) -> {
            if (!stille) {
                opdaterForslag(ny);
            }
        });
        // Klikker man videre til noget andet, skal listen ikke blive hængende over brugerfladen.
        felt.focusedProperty().addListener((obs, gammel, harFokus) -> {
            if (!harFokus) {
                skjulForslag();
            }
        });
        felt.addEventFilter(KeyEvent.KEY_PRESSED, this::taster);

        felt.setOnAction(e -> commit());
    }

    /* ---------- Forslag ---------- */

    private Collection<String> forslag(String indtastet) {
        List<String> ud = new ArrayList<>();
        if (indtastet == null) {
            return ud;
        }
        String soeg = indtastet.trim().toLowerCase(Locale.ROOT);
        if (soeg.isEmpty()) {
            return ud;
        }
        // Hentes FRISKT ved hvert opslag fra kategoriens symbolliste, ikke fra en kopi taget ved
        // opstart - så et tag man lige har gemt kan foreslås med det samme.
        for (String tag : kategori.getSymbols()) {
            // contains, ikke startsWith: skriver man "hav", skal "gamle havne" også kunne findes.
            // Allerede valgte tags foreslås ikke - de sidder som chips lige nedenunder.
            if (tag.contains(soeg) && !valgte.contains(tag)) {
                ud.add(tag);
            }
        }
        return ud;
    }

    private void opdaterForslag(String indtastet) {
        List<String> fundne = new ArrayList<>(forslag(indtastet));
        if (fundne.isEmpty()) {
            skjulForslag();
            return;
        }
        forslagListe.getItems().setAll(fundne);
        forslagListe.getSelectionModel().clearSelection();
        forslagListe.setPrefHeight(Math.min(fundne.size(), MAKS_SYNLIGE_FORSLAG) * RAEKKEHOEJDE + 4);
        forslagListe.setPrefWidth(felt.getWidth());

        if (forslagPopup.isShowing()) {
            return;
        }
        // Uden et vindue er der ikke noget at hænge popup'en op på. Det sker i praksis kun hvis
        // feltet fyldes før scenen er bygget, og da skal der ingenting ske.
        if (felt.getScene() == null || felt.getScene().getWindow() == null) {
            return;
        }
        Bounds paaSkaermen = felt.localToScreen(felt.getBoundsInLocal());
        if (paaSkaermen != null) {
            forslagPopup.show(felt, paaSkaermen.getMinX(), paaSkaermen.getMaxY());
        }
    }

    private void skjulForslag() {
        if (forslagPopup.isShowing()) {
            forslagPopup.hide();
        }
    }

    private void taster(KeyEvent e) {
        if (!forslagPopup.isShowing()) {
            return; // Enter alene håndteres af felt.setOnAction
        }
        switch (e.getCode()) {
            case DOWN -> {
                flytValg(1);
                e.consume();
            }
            case UP -> {
                flytValg(-1);
                e.consume();
            }
            case ENTER -> {
                // Kun hvis brugeren faktisk har peget på et forslag. Ellers falder Enter
                // igennem til setOnAction og gør det der STÅR i feltet til et tag - man skal
                // kunne finde på et nyt tag selv om der tilfældigvis lignede noget i listen.
                if (forslagListe.getSelectionModel().getSelectedItem() != null) {
                    tagValgtForslag();
                    e.consume();
                }
            }
            case ESCAPE -> {
                skjulForslag();
                e.consume();
            }
            default -> { }
        }
    }

    private void flytValg(int skridt) {
        int antal = forslagListe.getItems().size();
        if (antal == 0) {
            return;
        }
        int nu = forslagListe.getSelectionModel().getSelectedIndex();
        int ny = (nu < 0) ? (skridt > 0 ? 0 : antal - 1) : Math.floorMod(nu + skridt, antal);
        forslagListe.getSelectionModel().select(ny);
        forslagListe.scrollTo(ny);
    }

    private void tagValgtForslag() {
        String valgt = forslagListe.getSelectionModel().getSelectedItem();
        skjulForslag();
        if (valgt == null) {
            return;
        }
        saetTekstStille(valgt);
        commit();
    }

    /* ---------- Chips ---------- */

    /** Gør det der står i feltet til et tag, hvis der overhovedet står noget. */
    private void commit() {
        String tag = Tag.normaliser(felt.getText());
        saetTekstStille("");
        skjulForslag();
        if (tag != null && valgte.add(tag)) {
            tegnChips();
        }
    }

    private void saetTekstStille(String tekst) {
        stille = true;
        felt.setText(tekst);
        stille = false;
    }

    private void tegnChips() {
        chips.getChildren().clear();
        for (String tag : valgte) {
            Button chip = new Button(tag + "  ✕");
            chip.setOnAction(e -> {
                valgte.remove(tag);
                tegnChips();
            });
            chips.getChildren().add(chip);
        }
    }

    /** Fylder feltet med de tags en drøm bærer i forvejen. */
    void vis(Collection<String> tags) {
        valgte.clear();
        valgte.addAll(tags);
        saetTekstStille("");
        skjulForslag();
        tegnChips();
    }

    void ryd() {
        vis(List.of());
    }

    /**
     * Drømmens tags, klar til at blive lagt i dens categories-liste.
     *
     * Symbolsættet er ALTID med og aldrig null - heller ikke på en tom formular. En CategoryDTO
     * med null i id eller symbols vælter AnalyseService.updateFilteredDreams med en NPE, og det
     * er netop dét getccbDreamSelections() gør for en kategori uden CheckComboBox. Derfor bygges
     * tag-DTO'en her i stedet.
     */
    CategoryDTO somCategoryDTO() {
        // Et færdigskrevet ord der aldrig fik sit Enter tæller med. Brugeren har skrevet det og
        // ville med rette blive overrasket over at det forsvandt ved at trykke Gem.
        commit();

        CategoryDTO dto = new CategoryDTO();
        dto.id = Tag.ID;
        dto.symbols = new TreeSet<>(valgte);

        // Ordforrådet følger det der faktisk bliver gemt - ikke alt hvad der blev skrevet
        // undervejs. Fortrød man et tag og fjernede chippen igen, skal det ikke ligge tilbage
        // i listen og blive foreslået resten af tiden.
        boolean nogetNyt = false;
        for (String tag : valgte) {
            if (!kategori.getSymbols().contains(tag)) {
                kategori.addSymbol(tag);
                nogetNyt = true;
            }
        }
        // Kun når der faktisk kom noget nyt: updateAllCCBs rydder afkrydsningerne, og et filter
        // man har sat i Analyse skal ikke nulstilles hver gang en drøm gemmes.
        if (nogetNyt) {
            kategori.updateAllCCBs();
        }
        return dto;
    }
}
