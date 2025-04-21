package fixit.dreams;

import javafx.application.Platform;
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

public class AnalyseController {
    private AnalyseService analyseService;
    private ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
    ObservableList<String> kategoriLabels;

    @FXML
    private AnchorPane analyseRoot;

    @FXML
    private DatePicker dpFraGraf, dpTilGraf, dpFromPie, dpToPie, dpFromTal, dpToTal;

    @FXML
    private LineChart<String,Number> lineChartAnalyse;

    @FXML
    private VBox vboxTilCCBAnalyse;

    @FXML
    private ToggleButton tgDays, tgMonths, tgWeeks;

    @FXML
    private Tab tabPie, grafTab, listeTab;

    @FXML
    private ListView<DreamDTO> filterListe;

    @FXML
    private PieChart pieChartAnalyse;

    @FXML
    private ComboBox<String> comboPieKategorier;

    @FXML
    private CheckBox lucid, praktiserer, modsat, arketypisk, praksis, mareridt, kollektiv, advarsel;

    @FXML
    Button btnVisGraf, btnAndOr;

    @FXML
    public void initialize() {
        User user = User.getInstance();

        this.analyseService = new AnalyseService(user);
        comboPieKategorier.setItems(user.getKategoriLabels());

        user.skalStatsGenberegnes.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                analyseService.updateStats();
                setGuiDates();
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
                label.setMaxWidth(600); // Justér denne værdi efter behov
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

        FilterDTO data = new FilterDTO();
        data.fra = dpFraGraf.getValue();
        data.til = dpTilGraf.getValue();
        analyseService.updateFilteredDreams(data, false);
        filterListe.setItems(analyseService.getFilteredDreams());

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
        //filterListe.setItems(FXCollections.observableArrayList());
        //filterListe.setItems(analyseService.getFilteredDreams());
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

}
