package fixit.dreams;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.controlsfx.control.CheckComboBox;

import java.time.LocalDate;
import java.util.*;

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
    private VBox vboxTilCCBAnalyse, talVboxBinary, talVboxBinaryNumbers, talVboxCatOne, talVboxCatTwo;

    @FXML
    private ToggleButton tgDays, tgMonths, tgWeeks;

    @FXML
    private Tab tabPie, grafTab, listeTab;

    @FXML
    private ListView<DreamDTO> filterListe, forloebListe, forloebValgListe;

    @FXML
    private PieChart pieChartAnalyse;

    @FXML
    private ComboBox<String> comboPieKategorier;

    @FXML
    private CheckBox lucid, praktiserer, modsat, arketypisk, praksis, mareridt, kollektiv, advarsel;

    @FXML
    private Button btnVisGraf, btnAndOr, btnForloebVisListe, btnForloebPlus;

    @FXML
    private Spinner<Integer> daysSpinner, monthsSpinner;

    @FXML
    private Label lblForloebDream;

    @FXML
    public void initialize() {
        User user = User.getInstance();

        this.analyseService = new AnalyseService(user);
        comboPieKategorier.setItems(user.getKategoriLabels());

        user.skalStatsGenberegnes.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                analyseService.updateStats();
                setGuiDates();
                loadTalData();
                analyseService.updateForloeb();
            }
        });


        user.addVbox(vboxTilCCBAnalyse);

        loadCCBs();

        kategoriLabels = user.getKategoriLabels();

        kollektiv.setVisible(user.isVisKollektiv());
        advarsel.setVisible(user.isVisAdvarsel());

        setGuiDates();

        filterListe.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
            private final Label label = new Label();
            {
                label.setWrapText(true);
                //label.setMaxWidth(600); // Justér denne værdi efter behov
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
                //label.setMaxWidth(550); // Justér denne værdi efter behov
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
                label.setMaxWidth(280); // Justér denne værdi efter behov
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
                setGraphic(null);
                setText(null);
                if (!empty && dream != null) {
                    label.setText(dream.getMinimalIndhold());
                    setGraphic(label);
                }
            }
        });

        FilterDTO data = new FilterDTO();
        data.fra = dpFraGraf.getValue();
        data.til = dpTilGraf.getValue();
        analyseService.updateFilteredDreams(data, false);
        filterListe.setItems(analyseService.getFilteredDreams());

        // Tal-tabben:
        loadTalData();

        // Forløb-tabben:
        setupSpinners();
        forloebListe.setItems(analyseService.getForloebDreams());
        forloebValgListe.setItems(analyseService.getForloeb());
        analyseService.updateForloeb();

    }

    private void setGuiDates() {
        for (DatePicker dp : List.of(dpFraGraf,dpFromPie,dpFromTal)) {
            dp.setValue(analyseService.getFirstDreamDate());
        }
        for (DatePicker dp : List.of(dpTilGraf,dpToPie,dpToTal)) {
            dp.setValue(LocalDate.now());
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
        if (comboPieKategorier.getSelectionModel().getSelectedItem() != null) {
            Map<String,Integer> mapData = analyseService.getDataForPieChart(comboPieKategorier.getSelectionModel().getSelectedItem(),dpFromPie.getValue(),dpToPie.getValue());
            pieData.clear();
            for (Map.Entry<String, Integer> entry : mapData.entrySet()) {
                pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            }
            pieChartAnalyse.setLegendVisible(false);
            pieChartAnalyse.setData(pieData);
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
        List<String> binaries = new ArrayList<>(List.of("Lucid", "Praktiserer", "Modsatkønnet", "Arketypisk", "Om praksis", "Mareridt","Advarsel","Kollektiv"));

        talVboxBinary.getChildren().clear();
        talVboxBinaryNumbers.getChildren().clear();

        int[] values = analyseService.getTalBinary(dpFromTal.getValue(), dpToTal.getValue());
        for (int i = 0; i < binaries.size(); i++) {
            Label lbl = new Label();
            Label vals = new Label();
            lbl.setText(binaries.get(i));
            vals.setText(String.valueOf(values[i]));

            if ((!binaries.get(i).equals("Advarsel")||analyseService.usingAdvarsel()) && (!binaries.get(i).equals("Kollektiv")||analyseService.usingKollektiv())) {
                talVboxBinary.getChildren().add(lbl);
                talVboxBinaryNumbers.getChildren().add(vals);
            }

        }

        // Og nu TableViews med labels? i talVboxCatOne, talVboxCatTwo
        talVboxCatOne.getChildren().clear();
        talVboxCatTwo.getChildren().clear();

        ArrayList<ArrayList<String>> statsForCats = analyseService.getTalCategories(dpFromTal.getValue(), dpToTal.getValue());
        ArrayList<Category> cats = analyseService.getCats();

        int counter = 0;

        for (int i = 0; i < cats.size(); i++) {
            Label lbl = new Label();
            lbl.setText(cats.get(i).getName());
            ListView<String> tv = new ListView<>();
            tv.getStyleClass().add("custom-list-view");
            tv.getItems().addAll(statsForCats.get(i));
            tv.addEventHandler(MOUSE_CLICKED, Event -> tv.getSelectionModel().clearSelection());

            if (counter < 4) {
                talVboxCatOne.getChildren().add(lbl);
                talVboxCatOne.getChildren().add(tv);
                counter++;
            } else {
                talVboxCatTwo.getChildren().add(lbl);
                talVboxCatTwo.getChildren().add(tv);
                counter++;
            }
        }
    }

    @FXML
    private void onVisGraf() {
        lineChartAnalyse.getData().clear();
        lineChartAnalyse.setAnimated(false);

        // Send datoerne med til getData-funktionen - og alt fra alle ccb og cber! Som en GrafDTO :-)
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
        data.praksis = praksis.isSelected();
        data.xakse = dayWeekOrMonth();

        lineChartAnalyse.setVisible(false);
        lineChartAnalyse.layout();

        for (XYChart.Series<String, Number> series : analyseService.getDataForLineChart(data)) {
            lineChartAnalyse.getData().add(series);
        }

        lineChartAnalyse.setCreateSymbols(false);
        lineChartAnalyse.layout();
        lineChartAnalyse.setVisible(true);
    }

    private String dayWeekOrMonth() {
        String xAkseValg;
        xAkseValg = (tgDays.isSelected()) ? "dage" : (tgWeeks.isSelected()) ? "uger" : "måneder";
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
    public void onSelectForloebDream() {
        String id = forloebValgListe.getSelectionModel().getSelectedItem().getId();
        lblForloebDream.setText(analyseService.getForloebStage(id));
    }
}
