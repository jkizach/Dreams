package fixit.dreams;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.controlsfx.control.CheckComboBox;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;

public class AnalyseController {
    private AnalyseService analyseService;
    private ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
    private ObservableList<String> kategoriLabels;

    @FXML
    private AnchorPane analyseRoot;

    @FXML
    private DatePicker dpFraGraf, dpTilGraf, dpFromPie, dpToPie, dpFromTal, dpToTal;

    @FXML
    private LineChart<String,Number> lineChartAnalyse;

    @FXML
    private VBox vboxTilCCBAnalyse, talVboxBinary, talVboxBinaryNumbers, talVboxBinaryPercent, talVboxCatOne, talVboxCatTwo;

    @FXML
    private ToggleButton tgDays, tgMonths, tgWeeks, tgForloebKort, tgForloebLangt;

    @FXML
    private ToggleGroup forloebVisningGroup;

    @FXML
    private ListView<DreamDTO> filterListe, forloebListe, forloebValgListe;

    @FXML
    private PieChart pieChartAnalyse;

    @FXML
    private ComboBox<String> comboPieKategorier;

    @FXML
    private CheckBox lucid, praktiserer, modsat, arketypisk, praksis, mareridt, kollektiv, advarsel, holografisk;

    @FXML
    private Button btnVisGraf, btnAndOr, btnForloebVisListe, btnForloebPlus;

    @FXML
    private Spinner<Integer> daysSpinner, monthsSpinner;

    @FXML
    private Label lblForloebDream, antalDreamsLblTal, antalDreamsLblCirkel, lblAntalDrommeGraf, lblAntalDrommeForloeb, lblAntalDrommeListe;

    private boolean visLangtForloeb = false;

    @FXML
    public void initialize() {
        User user = User.getInstance();

        this.analyseService = new AnalyseService(user);
        comboPieKategorier.setItems(user.getKategoriLabels());

        // User tæller op hver gang statistikken skal regnes om. En sync kan sende hundredvis af
        // beskeder i træk (én pr. hentet drøm), og en fuld genberegning pr. besked ville låse
        // brugerfladen - derfor lægges der kun én genberegning i kø ad gangen. Vagten slippes
        // FØRST i genberegningen, så ændringer der kommer ind undervejs får deres egen tur i
        // stedet for at blive tabt. compareAndSet fordi syncen tæller op fra sin egen tråd.
        AtomicBoolean genberegningPlanlagt = new AtomicBoolean(false);
        user.statsGenberegningProperty().addListener((obs, oldVal, newVal) -> {
            if (genberegningPlanlagt.compareAndSet(false, true)) {
                Platform.runLater(() -> {
                    genberegningPlanlagt.set(false);
                    analyseService.updateStats();
                    updateGuiDates();
                    loadTalData();
                    updateAntalDreamsCirkel();
                    kollektiv.setVisible(user.isVisKollektiv());
                    kollektiv.setManaged(user.isVisKollektiv());
                    advarsel.setVisible(user.isVisAdvarsel());
                    advarsel.setManaged(user.isVisAdvarsel());
                    holografisk.setVisible(user.isVisHolografisk());
                    holografisk.setManaged(user.isVisHolografisk());
                    analyseService.updateForloeb();
                });
            }
        });


        user.addFilterVbox(vboxTilCCBAnalyse);

        loadCCBs();

        kategoriLabels = user.getKategoriLabels();

        kollektiv.setVisible(user.isVisKollektiv());
        kollektiv.setManaged(user.isVisKollektiv());
        advarsel.setVisible(user.isVisAdvarsel());
        advarsel.setManaged(user.isVisAdvarsel());
        holografisk.setVisible(user.isVisHolografisk());
        holografisk.setManaged(user.isVisHolografisk());

        setGuiDates();

        setAntalDreamsLabel();
        updateAntalDreamsCirkel();

        filterListe.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                // "param" er her ListView'en selv - bind direkte til dens faktiske bredde
                // (en almindelig addListener fyrer kun ved senere ÆNDRINGER, ikke med den bredde
                // ListView'en allerede har når cellen oprettes, hvilket gav forkert - for smal
                // eller for bred - ombrydningsbredde og dermed "..." i enden af nogle drømme)
                label.maxWidthProperty().bind(Bindings.max(100, param.widthProperty().subtract(40))); // 40 px margin til scrollbar + padding
            }
            @Override
            protected void updateItem(DreamDTO dream, boolean empty) {
                super.updateItem(dream, empty);
                setGraphic(null);
                setText(null);
                if (!empty && dream != null) {
                    label.setText(dream.getVisbartIndhold());
                    setGraphic(label);
                }
            }
        });

        forloebListe.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                label.maxWidthProperty().bind(Bindings.max(100, param.widthProperty().subtract(40))); // 40 px margin til scrollbar + padding
            }
            @Override
            protected void updateItem(DreamDTO dream, boolean empty) {
                super.updateItem(dream, empty);
                setGraphic(null);
                setText(null);
                if (!empty && dream != null) {
                    label.setText(dream.getVisbartIndhold());
                    setGraphic(label);
                }
            }
        });

        forloebValgListe.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                label.maxWidthProperty().bind(Bindings.max(100, param.widthProperty().subtract(40))); // 40 px margin til scrollbar + padding
            }
            @Override
            protected void updateItem(DreamDTO dream, boolean empty) {
                super.updateItem(dream, empty);
                setGraphic(null);
                setText(null);
                if (!empty && dream != null) {
                    label.setText(visLangtForloeb ? dream.getFuldeIndhold() : dream.getMinimalIndhold());
                    setGraphic(label);
                }
            }
        });

        // Sørg for at der altid er præcis ét toggle valgt (Kort/Langt) i forløbsfanen
        forloebVisningGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true);
            }
        });

        FilterDTO data = new FilterDTO();
        data.fra = dpFraGraf.getValue();
        data.til = dpTilGraf.getValue();
        analyseService.updateFilteredDreams(data, false);
        filterListe.setItems(analyseService.getFilteredDreams());
        lblAntalDrommeListe.textProperty().bind(Bindings.size(analyseService.getFilteredDreams()).asString("Antal drømme: %d"));

        // Tal-tabben:
        loadTalData();

        // Forløb-tabben:
        setupSpinners();
        forloebListe.setItems(analyseService.getForloebDreams());
        forloebValgListe.setItems(analyseService.getForloeb());
        analyseService.updateForloeb();

        lblAntalDrommeForloeb.textProperty().bind(Bindings.size(analyseService.getForloebDreams()).asString("Antal drømme: %d"));

        updateAntalDrommeGraf(buildGrafDTO());
    }

    private void setAntalDreamsLabel() {
        int antal = analyseService.countDreams(dpFromTal.getValue(), dpToTal.getValue());
        antalDreamsLblTal.setText(formatDataForDrommeText(antal));
    }

    private void updateAntalDreamsCirkel() {
        String kategoriId = analyseService.idForKategoriNavn(comboPieKategorier.getSelectionModel().getSelectedItem());
        int antal = (kategoriId == null) ? 0 : analyseService.countDreamsForKategori(kategoriId, dpFromPie.getValue(), dpToPie.getValue());
        antalDreamsLblCirkel.setText(formatDataForDrommeText(antal));
    }

    private String formatDataForDrommeText(int antal) {
        return antal == 1 ? "Data for 1 drøm" : "Data for " + antal + " drømme";
    }

    private void updateAntalDrommeGraf(GrafDTO indat) {
        int antal = analyseService.countDreamsForGraf(indat);
        lblAntalDrommeGraf.setText(antal == 1 ? "Antal drømme: 1" : "Antal drømme: " + antal);
    }

    private GrafDTO buildGrafDTO() {
        GrafDTO data = new GrafDTO();
        data.fra = dpFraGraf.getValue();
        data.til = dpTilGraf.getValue();
        data.lucid = lucid.isSelected();
        data.praktiserer = praktiserer.isSelected();
        data.modsat = modsat.isSelected();
        data.arketypisk = arketypisk.isSelected();
        data.mareridt = mareridt.isSelected();
        data.kollektiv = kollektiv.isSelected();
        data.advarsel = advarsel.isSelected();
        data.holografisk = holografisk.isSelected();
        data.praksis = praksis.isSelected();
        return data;
    }

    private void setGuiDates() {
        for (DatePicker dp : List.of(dpFraGraf,dpFromPie,dpFromTal)) {
            dp.setValue(analyseService.getStartDate());
        }
        for (DatePicker dp : List.of(dpTilGraf,dpToPie,dpToTal)) {
            dp.setValue(LocalDate.now());
        }
    }

    private void updateGuiDates() {
        for (DatePicker dp : List.of(dpFraGraf,dpFromPie,dpFromTal)) {
            dp.setValue(analyseService.getStartDate());
        }
    }

    @FXML
    private void updateFilterList() {
        FilterDTO data = new FilterDTO();
        data.fra = dpFraGraf.getValue();
        data.til = dpTilGraf.getValue();
        data.lucid = lucid.isSelected();
        data.praktiserer = praktiserer.isSelected();
        data.modsat = modsat.isSelected();
        data.arketypisk = arketypisk.isSelected();
        data.praksis = praksis.isSelected();
        data.mareridt = mareridt.isSelected();
        data.kollektiv = kollektiv.isSelected();
        data.advarsel = advarsel.isSelected();
        data.holografisk = holografisk.isSelected();

        filterListe.getItems().clear();

        analyseService.updateFilteredDreams(data, true);
    }

    @FXML
    private void toggleANDOR() {
        if (analyseService.isAndOr()) {
            btnAndOr.setText("Filterstatus: ELLER");
            analyseService.setAndOr(false);
        } else {
            btnAndOr.setText("Filterstatus: OG");
            analyseService.setAndOr(true);
        }
    }

    @FXML
    private void onSelectKategori() {
        updateAntalDreamsCirkel();
        String kategoriId = analyseService.idForKategoriNavn(comboPieKategorier.getSelectionModel().getSelectedItem());
        if (kategoriId != null) {
            Map<String,Integer> mapData = analyseService.getDataForPieChart(kategoriId,dpFromPie.getValue(),dpToPie.getValue());
            pieData.clear();
            for (Map.Entry<String, Integer> entry : mapData.entrySet()) {
                pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            }
            pieChartAnalyse.setLegendVisible(false);
            pieChartAnalyse.setData(pieData);
            farvelægLagkagestykker(kategoriId);
        }
    }

    /**
     * Giver hvert lagkagestykke den farve symbolet selv handler om - rød for rodchakraet,
     * græsgrøn for græsgrøn. Stykkernes noder findes allerede lige efter setData(), og
     * "-fx-pie-color" er den variabel modena bygger sin gradient over, så farven falder ind
     * i appens udseende i stedet for at ligge oven på det.
     *
     * Kun Farver og Chakraer farvelægges. Alle andre diagrammer røres ikke og beholder den
     * palet de altid har haft - også selv om et enkelt dyr eller en arketype skulle kunne
     * læses som et farveord. Et symbol vi ikke kan udlede noget om, får et neutralt gråt
     * stykke: en opfundet kulør ville lyve om dataene.
     */
    private void farvelægLagkagestykker(String kategoriId) {
        boolean farvelæg = Category.harNaturligeFarver(kategoriId);

        for (PieChart.Data stykke : pieData) {
            Node node = stykke.getNode();
            if (node == null) {
                continue;
            }
            if (!farvelæg) {
                node.setStyle("");
                continue;
            }
            Color farve = Symbolfarver.forSymbol(stykke.getName());
            // Kanten er nødvendig nu hvor farverne er ægte: uden den flyder nabonuancer som
            // grøn og græsgrøn sammen, og sort forsvinder helt i den mørke baggrund.
            node.setStyle("-fx-pie-color: " + Symbolfarver.tilWeb(farve == null ? Symbolfarver.UKENDT : farve) + ";"
                    + " -fx-border-color: -fx-hovedtxt-text; -fx-border-width: 1;");
        }
    }

    private void loadCCBs() {
        for (Category c : analyseService.getCats()) {
            CheckComboBox<String> ccb = new CheckComboBox<>();
            ccb.getItems().addAll(c.getSymbolsForDisplay());
            vboxTilCCBAnalyse.getChildren().add(ccb);
            ccb.setMaxWidth(280);
            ccb.setMinWidth(280);
            ccb.setTitle(c.getName());
            ccb.setShowCheckedCount(true);
            c.addFilterCCB(ccb);
        }
    }

    @FXML
    public void loadTalData() {
        List<String> binaries = new ArrayList<>(Category.FLAGS_SYMBOLS_IN_ORDER);

        talVboxBinary.getChildren().clear();
        talVboxBinaryNumbers.getChildren().clear();
        talVboxBinaryPercent.getChildren().clear();

        // Samme nævner som kategorioverskrifterne længere nede bruger: alle drømme i
        // intervallet, ikke kun dem der har mindst ét flag. "13% af drømmene var lucide"
        // ville ellers betyde noget forskelligt de to steder på den samme skærm.
        int totalDrommeTal = analyseService.countDreams(dpFromTal.getValue(), dpToTal.getValue());

        int[] values = analyseService.getTalBinary(dpFromTal.getValue(), dpToTal.getValue());
        for (int i = 0; i < binaries.size(); i++) {
            Label lbl = new Label();
            Label vals = new Label();
            Label andel = new Label();
            lbl.setText(binaries.get(i));
            vals.setAlignment(Pos.CENTER_RIGHT);
            vals.setMaxWidth(Double.MAX_VALUE);
            vals.setText(String.valueOf(values[i]));
            andel.setAlignment(Pos.CENTER_RIGHT);
            andel.setMaxWidth(Double.MAX_VALUE);
            andel.setText(AnalyseService.formatAndel(values[i], totalDrommeTal));

            if ((!binaries.get(i).equals("Advarsel")||analyseService.usingAdvarsel()) && (!binaries.get(i).equals("Kollektiv")||analyseService.usingKollektiv())
                    && (!binaries.get(i).equals("Holografisk")||analyseService.usingHolografisk())) {
                talVboxBinary.getChildren().add(lbl);
                talVboxBinaryNumbers.getChildren().add(vals);
                talVboxBinaryPercent.getChildren().add(andel);
            }

        }

        // Og nu TableViews med labels? i talVboxCatOne, talVboxCatTwo
        talVboxCatOne.getChildren().clear();
        talVboxCatTwo.getChildren().clear();

        ArrayList<ArrayList<String>> statsForCats = analyseService.getTalCategories(dpFromTal.getValue(), dpToTal.getValue());
        ArrayList<Category> cats = analyseService.getCats();

        int counter = 0;

        for (int i = 0; i < cats.size(); i++) {
            String katNavn = cats.get(i).getName();
            int antalIKat = analyseService.countDreamsForKategori(cats.get(i).getId(), dpFromTal.getValue(), dpToTal.getValue());

            // Navnet venstrejusteret, antal + andel højrejusteret på samme linje
            Label lbl = new Label(katNavn);
            Label andelLbl = new Label(AnalyseService.formatAntalOgAndel(antalIKat, totalDrommeTal));
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox overskrift = new HBox(lbl, spacer, andelLbl);
            overskrift.setAlignment(Pos.CENTER_LEFT);
            overskrift.setMaxWidth(Double.MAX_VALUE);

            ListView<String> tv = new ListView<>();
            tv.getStyleClass().add("custom-list-view");
            tv.getItems().addAll(statsForCats.get(i));
            tv.addEventHandler(MOUSE_CLICKED, Event -> tv.getSelectionModel().clearSelection());

            if (counter < 4) {
                talVboxCatOne.getChildren().add(overskrift);
                talVboxCatOne.getChildren().add(tv);
                counter++;
            } else {
                talVboxCatTwo.getChildren().add(overskrift);
                talVboxCatTwo.getChildren().add(tv);
                counter++;
            }
        }
        // og så drømmeantallet
        setAntalDreamsLabel();

    }

    @FXML
    private void onVisGraf() {
        lineChartAnalyse.getData().clear();
        lineChartAnalyse.setAnimated(false);

        // Send datoerne med til getData-funktionen - og alt fra alle ccb og cber! Som en GrafDTO :-)
        GrafDTO data = buildGrafDTO();
        data.xakse = dayWeekOrMonth();

        lineChartAnalyse.setVisible(false);
        lineChartAnalyse.layout();

        for (XYChart.Series<String, Number> series : analyseService.getDataForLineChart(data)) {
            lineChartAnalyse.getData().add(series);
        }

        lineChartAnalyse.setCreateSymbols(false);
        lineChartAnalyse.layout();
        lineChartAnalyse.setVisible(true);

        updateAntalDrommeGraf(data);
    }

    private String dayWeekOrMonth() {
        String xAkseValg;
        xAkseValg = (tgDays.isSelected()) ? "dage" : (tgWeeks.isSelected()) ? "uger" : "måneder";
        if (xAkseValg.equals("måneder")) {
            tgMonths.setSelected(true);
        }
        return xAkseValg;
    }

    /* Forløbs-tabbens funktioner */
    private void setupSpinners() {
        daysSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 5));
        monthsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9, 1));
        daysSpinner.setEditable(false);
        monthsSpinner.setEditable(false);
    }

    @FXML
    private void plusBtnPressed() {
        if (btnForloebPlus.getText().equals("+")) {
            btnForloebPlus.setText("-");
        } else {
            btnForloebPlus.setText("+");
        }
    }

    @FXML
    private void btnVisForloebPressed() {
        if (!forloebValgListe.getSelectionModel().isEmpty()) {
            int months = (btnForloebPlus.getText().equals("+") ? monthsSpinner.getValue() : monthsSpinner.getValue()*-1);
            analyseService.refreshForloebDreams(forloebValgListe.getSelectionModel().getSelectedItem().getDato(), daysSpinner.getValue(), months);
        }

    }

    @FXML
    private void onForloebVisningChanged() {
        visLangtForloeb = tgForloebLangt.isSelected();
        forloebValgListe.refresh();
    }

    @FXML
    public void onSelectForloebDream() {
        if (forloebValgListe.getSelectionModel().getSelectedItem() != null) {
            String id = forloebValgListe.getSelectionModel().getSelectedItem().getId();
            lblForloebDream.setText(analyseService.getForloebStage(id));
        }
    }
}
