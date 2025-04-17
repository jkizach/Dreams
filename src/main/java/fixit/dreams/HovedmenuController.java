package fixit.dreams;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.controlsfx.control.CheckComboBox;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HovedmenuController {
    private UserService userService;
    private ObservableList<String> temaer;
    ObservableList<String> kategoriLabels;

    private boolean analyseIsLoaded = false;

    @FXML
    private TextArea skriveFelt, dagrestFelt;

    @FXML
    private ColorPicker baggrundAPicker, baggrundBPicker, baggrundCPicker, baggrundDPicker, tekstAPicker, tekstBPicker, tekstCPicker, kantPicker;

    @FXML
    private Button gemDream, seTemaKnap, gemNytTemaKnap, addSymbolKnap, addNyKategoriKnap;

    @FXML
    private DatePicker newDreamDate, fromDatePicker, toDatePicker;

    @FXML
    private TabPane tabPane;

    @FXML
    public Tab analyseTab;

    @FXML
    private TextField tfNytTemaNavn, tfNytSymbol, tfNyKategori;

    @FXML
    private ComboBox<String> cbKategoriRemove, cbKategoriAdd, cbTemaer, cbFonts;

    @FXML
    private VBox fjernSymbolVbox = new VBox();

    @FXML
    private CheckComboBox<String> fjernSymbolCCB;

    @FXML
    private VBox vBoxSymboler = new VBox();

    @FXML
    private CheckBox lucid, praktiserer, modsat, arketypisk, praksis, mareridt, kollektiv, advarsel, kollektivStyrer, advarselStyrer;

    @FXML
    private Tab settingsTab;

    @FXML
    private ImageView settingsIcon;

    // Indlæs billederne
    Image icona = new Image(getClass().getResourceAsStream("/settingsa.png"));
    Image iconb = new Image(getClass().getResourceAsStream("/settingsb.png"));

    @FXML
    private ListView<DreamDTO> dreamListView;

    @FXML
    protected void skiftTandhjul() {
        if (settingsTab.isSelected()) {
            settingsIcon.setImage(icona);
        } else {
            settingsIcon.setImage(iconb);
        }
    }

    @FXML
    public void opretCCbox() {
        String valgteKategori = cbKategoriRemove.getSelectionModel().getSelectedItem();
        if (valgteKategori == null || valgteKategori.isEmpty()) {
            return; // Stopper metoden tidligt, hvis intet er valgt
        }
        fjernSymbolCCB = new CheckComboBox<>();

        for (Category c : userService.getCats()) {
            if (valgteKategori.equals(c.getName())) {
                fjernSymbolCCB.getItems().addAll(c.getSymbols());
                break;
            }
        }

        //fjernSymbolCCB.getCheckModel().getCheckedItems();
        fjernSymbolCCB.setMaxWidth(235); // måske 235, ligesom ved ny drøm, og måske counter?
        fjernSymbolCCB.setMinWidth(235);
        fjernSymbolCCB.setTitle("Symboler");

        fjernSymbolCCB.setShowCheckedCount(true);

        fjernSymbolVbox.getChildren().clear();
        fjernSymbolVbox.getChildren().add(fjernSymbolCCB);
    }

    @FXML
    public void handleFjernSymbolKnap() {
        String valgteKategori = cbKategoriRemove.getSelectionModel().getSelectedItem();
        if (valgteKategori == null || valgteKategori.isEmpty() || fjernSymbolCCB.getCheckModel().isEmpty()) {
            return; // Stopper metoden tidligt, hvis intet er valgt
        }
        for (String symbol : fjernSymbolCCB.getCheckModel().getCheckedItems()) {
            userService.fjernSymbol(valgteKategori,symbol);
        }
        updateKategoriCCB(valgteKategori);
        opretCCbox();
    }

    @FXML
    private void placeholderCCB() {
        CheckComboBox<String> placeholder = new CheckComboBox<>();
        placeholder.setMaxWidth(235); // måske 235, ligesom ved ny drøm, og måske counter?
        placeholder.setMinWidth(235);
        placeholder.setTitle("Vælg symboler");
        placeholder.setDisable(true);
        fjernSymbolVbox.getChildren().clear();
        fjernSymbolVbox.getChildren().add(placeholder);
    }

    @FXML
    private void setTema(Tema temaet) {
        baggrundAPicker.setValue(temaet.getBaggrundA());
        baggrundBPicker.setValue(temaet.getBaggrundB());
        baggrundCPicker.setValue(temaet.getBaggrundC());
        baggrundDPicker.setValue(temaet.getBaggrundD());
        tekstAPicker.setValue(temaet.getTekstA());
        tekstBPicker.setValue(temaet.getTekstB());
        tekstCPicker.setValue(temaet.getTekstC());
        kantPicker.setValue(temaet.getKant());
        cbFonts.setValue(temaet.getFont());
    }

    public void changeStylesheet(Boolean temp) {
        Scene scene = DreamApp.getCurrentScene();
        if (scene != null) {
            if (temp) {
                File tempFile = new File("src/main/resources/fixit/dreams/tempTema.css");
                scene.getStylesheets().clear();
                scene.getStylesheets().add(tempFile.toURI().toString()); // Indlæs direkte fra resources
                scene.getRoot().applyCss();
            } else {
                File tempFile = new File("src/main/resources/fixit/dreams/currentTema.css");
                scene.getStylesheets().clear();
                scene.getStylesheets().add(tempFile.toURI().toString()); // Indlæs direkte fra resources
                scene.getRoot().applyCss();
            }
        }
    }

    @FXML
    public void initialize() {
        User user = User.getInstance();
        this.userService = new UserService(user);

        setTema(userService.getCurrentTema());
        temaer = userService.getTemaerForDisplay();

        // Sæt currentTemas farver til de foretrukne:
        System.out.println(userService.getTema().getTemaName());

        ObservableList<DreamDTO> dreams = userService.getDreamsForDisplay();
        dreamListView.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                label.setMaxWidth(600); // Justér denne værdi efter behov
            }
            @Override
            protected void updateItem(DreamDTO dream, boolean empty) {
                super.updateItem(dream, empty);
                if (empty || dream == null) {
                    setText(null);
                } else {
                    label.setText(dream.getVisbartIndhold());
                    setGraphic(label);
                }
            }
        });

        dreamListView.setItems(dreams);

        settingsIcon.setImage(iconb);

        // ccb i edit-tab - placeholder
        placeholderCCB();

        newDreamDate.setValue(LocalDate.now());
        toDatePicker.setValue(LocalDate.now());

        // Og kunne man så lave et Søren-trick med Categories her i stedet? vv
        loadCCBs();

        kategoriLabels = user.getKategoriLabels(); // skal løses så det bruger categories og ikke noget andet...

        cbKategoriRemove.setItems(kategoriLabels);
        cbKategoriAdd.setItems(kategoriLabels);

        cbTemaer.setItems(temaer);
        cbTemaer.setValue(userService.getTemaNavn());
        cbFonts.getItems().addAll("Courier New", "Lucida Bright");

        /* ADVARSEL OG KOLLEKTIV - VIS ELLER EJ */
        kollektiv.setVisible(userService.isVisKollektiv());
        advarsel.setVisible(userService.isVisAdvarsel());
        kollektivStyrer.setSelected(userService.isVisKollektiv());
        advarselStyrer.setSelected(userService.isVisAdvarsel());

        /* ANALYSETAB */
        analyseTab.setOnSelectionChanged(event -> {
            if (analyseTab.isSelected() && !analyseIsLoaded) {
                loadAnalyseTab();
                analyseIsLoaded = true;
            }
        });

    }

    private void loadCCBs() {
        for (Category c : userService.getCats()) {
            CheckComboBox<String> ccb = new CheckComboBox<>();
            ccb.getItems().addAll(c.getSymbols());
            vBoxSymboler.getChildren().add(ccb);
            ccb.setMaxWidth(280);
            ccb.setMinWidth(280);
            ccb.setTitle(c.getName());
            ccb.setShowCheckedCount(true);
            c.addDreamCCB(ccb);
        }
    }

    @FXML
    private void handleAddDream() {
        DreamData dreamData = new DreamData();
        dreamData.categories = new ArrayList<>();

        for (Category c : userService.getCats()) {
            dreamData.categories.add(c.getccbDreamSelections());
        }

        dreamData.lucid = lucid.isSelected();
        dreamData.praktiserer = praktiserer.isSelected();
        dreamData.modsat = modsat.isSelected();
        dreamData.arketypisk = arketypisk.isSelected();
        dreamData.ompraksis = praksis.isSelected();
        dreamData.mareridt = mareridt.isSelected();
        dreamData.kollektiv = kollektiv.isSelected();
        dreamData.advarsel = advarsel.isSelected();

        dreamData.indhold = skriveFelt.getText();
        dreamData.dagrest = dagrestFelt.getText();
        dreamData.dato = newDreamDate.getValue();

        userService.addDream(dreamData);
        resetNewDreamTab();
    }

    private void resetNewDreamTab() {
        skriveFelt.clear();
        dagrestFelt.clear();
        newDreamDate.setValue(LocalDate.now());

        List<CheckBox> cbs = Arrays.asList(
                lucid, praktiserer, modsat, arketypisk, praksis, mareridt, kollektiv, advarsel
        );
        for (CheckBox cb : cbs) {
            cb.setSelected(false);
        }

        for (Category c : userService.getCats()) {
            c.resetDreamCCBs();
        }
    }

    @FXML
    private void farveEllerFontChanged() {
        TemaDTO data = new TemaDTO();
        data.baggrundA = baggrundAPicker.getValue();
        data.baggrundB = baggrundBPicker.getValue();
        data.baggrundC = baggrundCPicker.getValue();
        data.baggrundD = baggrundDPicker.getValue();
        data.tekstA = tekstAPicker.getValue();
        data.tekstB = tekstBPicker.getValue();
        data.tekstC = tekstCPicker.getValue();
        data.kant = kantPicker.getValue();
        data.font = cbFonts.getValue();
        if (seTemaKnap.isDisabled()){
            if (!userService.checkTemaErEns(data)) {
                seTemaKnap.setDisable(false);
            }
        } else {
            if (userService.checkTemaErEns(data)) {
                seTemaKnap.setDisable(true);
            }
        }
    }

    private TemaDTO createTemaDTO() {
        TemaDTO data = new TemaDTO();
        data.baggrundA = baggrundAPicker.getValue();
        data.baggrundB = baggrundBPicker.getValue();
        data.baggrundC = baggrundCPicker.getValue();
        data.baggrundD = baggrundDPicker.getValue();
        data.tekstA = tekstAPicker.getValue();
        data.tekstB = tekstBPicker.getValue();
        data.tekstC = tekstCPicker.getValue();
        data.kant = kantPicker.getValue();
        data.font = cbFonts.getValue();
        return data;
    }

    @FXML
    public void handleSeTemaKnap() {
        TemaDTO data = createTemaDTO();
        // brug en TemaDTO til at sætte temp tema i userService, og brug så tempTema til at ændre en tempCSS-fil...
        // og sæt så temaet til det temp tema...
        userService.setTempTema(data);
        changeStylesheet(true);
        seTemaKnap.setDisable(true);
        cbTemaer.setValue("Midlertidigt"); // viser bare et tomt felt i cb - men det fungerer jo fint :-)

        // Enable temanavn og gem knappen...
        tfNytTemaNavn.setDisable(false);
    }

    @FXML
    public void handleSkiftTema() {
        String temaNavn = cbTemaer.getSelectionModel().getSelectedItem();
        if (!temaNavn.equals("Midlertidigt")) {
            // Få fat i tema-info fra userService somehow...
            Tema nytTema = userService.getTema(temaNavn);
            // ændrer css-filen currentTema.css:
            userService.setCurrentTema(nytTema);
            // ændrer String foretrukne i User-klassen og sikrer at userService's currentTema (Tema-klasse) er updateret
            userService.setForetrukneTema(temaNavn);
            // ændrer color-pickers og font:
            setTema(userService.getCurrentTema());
            // skifter udseendet i gui:
            changeStylesheet(false);

            // disable nyt-tema vælgerwidgets:
            tfNytTemaNavn.setDisable(true);
            gemNytTemaKnap.setDisable(true);
        }
    }

    @FXML
    private void enableGemKnap() {
        gemNytTemaKnap.setDisable(false);
    }

    @FXML
    private void handleAddSymbolKnap() {
        if (cbKategoriAdd.getValue() != null && !tfNytSymbol.getText().trim().isEmpty()) {
            // her skal så kaldes en userService-funktion som faktisk tilføjer det nye symbol...
            // Fedest hvis jeg ikke behøver vide hvilken kategori det er?
            userService.addNytSymbol(cbKategoriAdd.getValue(),tfNytSymbol.getText());
            tfNytSymbol.clear();

            // sørger for at CCBen viser forskellen!
            updateKategoriCCB(cbKategoriAdd.getValue());
        }
    }

    @FXML
    public void handleAddNyKategoriKnap(){
        if (!tfNyKategori.getText().trim().isEmpty() && userService.okToAddNewUserDefinedCat()) {
            userService.addNyKategori(tfNyKategori.getText());
            tfNyKategori.clear();
        }
    }

    @FXML
    public void handleGemNytTema() {
        // Tema skal oprettes, smides til user, laves til foretrukne
        userService.addNytTema(createTemaDTO(),tfNytTemaNavn.getText());

        // Kan jeg ikke bare sætte værdien in min CB og så kører det automatisk? JO!
        cbTemaer.setValue(tfNytTemaNavn.getText());

        tfNytTemaNavn.setDisable(true);
        tfNytTemaNavn.setText("Navngiv tema");
        gemNytTemaKnap.setDisable(true);
    }

    @FXML
    public void handleCbStyrerAdv() {
        if (advarselStyrer.isSelected()) {
            advarsel.setVisible(true);
            userService.setVisAdvarsel(true);
        } else {
            advarsel.setVisible(false);
            userService.setVisAdvarsel(false);
        }
    }

    @FXML
    public void handleCbStyrerKol() {
        if (kollektivStyrer.isSelected()) {
            kollektiv.setVisible(true);
            userService.setVisKollektiv(true);
        } else {
            kollektiv.setVisible(false);
            userService.setVisKollektiv(false);
        }
    }

    private void updateKategoriCCB(String ccb) {
        for (Category c : userService.getCats()) {
            if (ccb.equals(c.getName())) {
                c.resetDreamCCBs();
            }
        }

//        if (List.of("Arketyper", "Chakraer", "Dyr", "Farver","Forløb", "Personer").contains(ccb)) {
//            switch (ccb) {
//                case "Arketyper":
//                    arketyper.getCheckModel().clearChecks();
//                    break;
//                case "Chakraer":
//                    chakraer.getCheckModel().clearChecks();
//                    break;
//                case "Dyr":
//                    dyr.getCheckModel().clearChecks();
//                    break;
//                case "Farver":
//                    farver.getCheckModel().clearChecks();
//                    break;
//                case "Forløb":
//                    forloeb.getCheckModel().clearChecks();
//                    break;
//                case "Personer":
//                    personer.getCheckModel().clearChecks();
//                    break;
//            }
//        } else {
//            String interntNavn = userService.getNewCategoryInternalName(ccb);
//            if (interntNavn.endsWith("A")) {
//                userDefinedA.getCheckModel().clearChecks();
//            } else if (interntNavn.endsWith("B")) {
//                userDefinedB.getCheckModel().clearChecks();
//            } else {
//                userDefinedC.getCheckModel().clearChecks();
//            }
//        }
    }

    /* LOADING AF ANALYSETAB */
    private void loadAnalyseTab() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("analyse-view.fxml"));
            Node content = loader.load();
            analyseTab.setContent(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}