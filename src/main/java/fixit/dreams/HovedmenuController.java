package fixit.dreams;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.CheckComboBox;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HovedmenuController {
    private UserService userService;
    private ObservableList<String> temaer;
    ObservableList<String> kategoriLabels;

    private boolean deleteButtonPressed = false;
    private boolean analyseIsLoaded = false;

    private FileChooser fileChooser = new FileChooser();

    @FXML
    private TextArea skriveFelt, dagrestFelt;

    @FXML
    private ColorPicker baggrundAPicker, baggrundBPicker, baggrundCPicker, baggrundDPicker, tekstAPicker, tekstBPicker, tekstCPicker, kantPicker;

    @FXML
    private Button deleteDream, seTemaKnap, gemNytTemaKnap, addSymbolKnap, addNyKategoriKnap, eksportBtn;

    @FXML
    private DatePicker newDreamDate, fromDatePicker, toDatePicker;

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
        fjernSymbolCCB.setMaxWidth(260); // måske 235, ligesom ved ny drøm, og måske counter?
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
        placeholder.setMaxWidth(260); // måske 235, ligesom ved ny drøm, og måske counter?
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
                Path cssPath = Paths.get(System.getProperty("user.home"), "Documents", "DrømmeappenData", "tempTema.css");
                File cssFile = cssPath.toFile();
                //File tempFile = new File("src/main/resources/fixit/dreams/tempTema.css");
                scene.getStylesheets().clear();
                scene.getStylesheets().add(cssFile.toURI().toString()); // Indlæs direkte fra resources
                scene.getRoot().applyCss();
            } else {
                Path cssPath = Paths.get(System.getProperty("user.home"), "Documents", "DrømmeappenData", "currentTema.css");
                File cssFile = cssPath.toFile();
                //File tempFile = new File("src/main/resources/fixit/dreams/currentTema.css");
                scene.getStylesheets().clear();
                scene.getStylesheets().add(cssFile.toURI().toString()); // Indlæs direkte fra resources
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

        dreamListView.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                label.setMaxWidth(650);
                // Dynamisk justering baseret på ListView'ens bredde
                // "param" er her ListView'en selv
                param.widthProperty().addListener((obs, oldVal, newVal) -> {
                    double newWidth = Math.max(100, newVal.doubleValue() - 40); // 40 px margin til scrollbar + padding
                    label.setMaxWidth(newWidth);
                });
            }
            @Override
            protected void updateItem(DreamDTO dream, boolean empty) {
                super.updateItem(dream, empty);
                if (empty || dream == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    label.setText(dream.getVisbartIndhold());
                    setGraphic(label);
                }
            }
        });

        user.dreamEdited.addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                userService.updateDreamDTO();
                dreamListView.setItems(userService.getDreamsForDisplay());
            }
        });

        user.addVbox(vBoxSymboler);
        dreamListView.setItems(userService.getDreamsForDisplay());

        settingsIcon.setImage(iconb);

        // ccb i edit-tab - placeholder
        placeholderCCB();

        newDreamDate.setValue(LocalDate.now());
        toDatePicker.setValue(LocalDate.now());
        fromDatePicker.setValue(userService.getFirstDreamDate());

        // Og kunne man så lave et Søren-trick med Categories her i stedet? vv
        loadCCBs();

        kategoriLabels = user.getKategoriLabels(); // skal løses så det bruger categories og ikke noget andet...

        cbKategoriRemove.setItems(kategoriLabels);
        cbKategoriAdd.setItems(kategoriLabels);

        cbTemaer.setItems(temaer);
        cbTemaer.setValue(userService.getTemaNavn());
        cbFonts.getItems().addAll("Courier New", "Source Code Pro");

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

        /* check for updates */
        GITHUBUpdater.checkForUpdateIfNeeded();

    }

    private void loadCCBs() {
        for (Category c : userService.getCats()) {
            CheckComboBox<String> ccb = new CheckComboBox<>();
            ccb.getItems().addAll(c.getSymbolsForDisplay());
            vBoxSymboler.getChildren().add(ccb);
            //ccb.setMaxWidth(280);
            ccb.setMaxWidth(Double.MAX_VALUE);

            ccb.setMinWidth(280);
            ccb.setTitle(c.getName());
            ccb.setShowCheckedCount(true);
            c.addDreamCCB(ccb);
        }
    }

    @FXML
    private void handleAddDream() {
        if (!skriveFelt.getText().isEmpty()) {
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
    }

    @FXML
    public void filtrerDreamList() {
        userService.refreshDreamList(fromDatePicker.getValue(),toDatePicker.getValue());
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
    private void handleEditDream() {
        if (dreamListView.getSelectionModel().getSelectedItem() != null) {
            String id = dreamListView.getSelectionModel().getSelectedItem().getId();
            Dream toBeEdited = userService.getDream(id);
            openEditPopup(toBeEdited); // åbner mit pop-up vindue!
        };
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

            // sørger for at CBen viser forskellen!
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
    }

    @FXML
    private void deleteDreamClick() {
        if (dreamListView.getSelectionModel().getSelectedItem() != null) {
            if (!deleteButtonPressed) {
                deleteDream.setText("Er du sikker?");
                deleteButtonPressed = true;
                // Start en timer, der nulstiller knappen efter 4 sekunder
                Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
                    deleteDream.setText("Slet");
                    deleteButtonPressed = false;
                }));
                timeline.setCycleCount(1);
                timeline.play();
            } else {
                String id = dreamListView.getSelectionModel().getSelectedItem().getId();
                userService.deleteDream(id);
                userService.refreshDreamList(fromDatePicker.getValue(),toDatePicker.getValue());
                deleteDream.setText("Slet drøm");
                deleteButtonPressed = false;
            }
        }
    }

    @FXML
    public void clickBtnOmHelp(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        openTxtView(clicked.getText());
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

    /* LOADING AF EDIT-POPUP*/
    @FXML
    private void openEditPopup(Dream d)  {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("editDream-view.fxml"));
            Parent root = fxmlLoader.load();

            EditDreamController edc = fxmlLoader.getController();
            edc.setDream(d);

            Stage popupStage = new Stage();

            Image icon = new Image(getClass().getResourceAsStream("/moona.png"));
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            //File tempFile = new File("src/main/resources/fixit/dreams/currentTema.css");
            Path cssPath = Paths.get(System.getProperty("user.home"), "Documents", "DrømmeappenData", "currentTema.css");
            File cssFile = cssPath.toFile();
            root.getStylesheets().clear();
            root.getStylesheets().add(cssFile.toURI().toString()); // Indlæs direkte fra resources
            root.applyCss();
            popupStage.setTitle("Rediger drøm");
            popupStage.getIcons().add(icon);

            popupStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* Loading af om- og hjælp-vinduerne */
    @FXML
    private void openTxtView(String type)  {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("txt-view.fxml"));
            Parent root = fxmlLoader.load();

            TxtController tc = fxmlLoader.getController();
            tc.setType(type);

            Stage popupStage = new Stage();

            Image icon = new Image(getClass().getResourceAsStream("/moona.png"));
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));
            //File tempFile = new File("src/main/resources/fixit/dreams/currentTema.css");
            Path cssPath = Paths.get(System.getProperty("user.home"), "Documents", "DrømmeappenData", "currentTema.css");
            File cssFile = cssPath.toFile();
            root.getStylesheets().clear();
            root.getStylesheets().add(cssFile.toURI().toString()); // Indlæs direkte fra resources
            root.applyCss();
            popupStage.setTitle(type);
            popupStage.getIcons().add(icon);

            popupStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /* FileChooser når man eksporterer drømmeliste som txt.fil */
    @FXML
    public void eksportBtn() {
        fileChooser.setTitle("Gem drømme som .txt");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("txt", "*.txt")
        );
        File fileToSave = fileChooser.showSaveDialog(dreamListView.getScene().getWindow());
        if (fileToSave != null) {
            IOutils.eksporterDreamlist(dreamListView.getItems(), fileToSave.getAbsolutePath());
        } else {
            System.out.println("No path selected");
        }
    }
}