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
    private CategoryAxis lcCategoryAxis;

    @FXML
    private ToggleButton tgDays, tgMonths, tgWeeks;

    @FXML
    private Tab tabPie, grafTab, listeTab;

    @FXML
    private ListView<String> filterListe;

    @FXML
    private PieChart pieChartAnalyse;

    @FXML
    private ComboBox<String> comboPieKategorier;

    @FXML
    private CheckBox lucid, praktiserer, modsat, arketypisk, praksis, mareridt, kollektiv, advarsel;

    @FXML
    Button btnVisGraf;

    @FXML
    public void initialize() {
        User user = User.getInstance();

        this.analyseService = new AnalyseService(user);
        comboPieKategorier.setItems(user.getKategoriLabels());

        loadCCBs();
//        arketyper = new CheckComboBox<>(analyseService.getArketyperForDisplay());
//        chakraer = new CheckComboBox<>(analyseService.getChakraerForDisplay());
//        dyr = new CheckComboBox<>(analyseService.getDyrForDisplay());
//        farver = new CheckComboBox<>(analyseService.getFarverForDisplay());
//        forloeb = new CheckComboBox<>(analyseService.getForloebForDisplay());
//        personer = new CheckComboBox<>(analyseService.getPersonerForDisplay());
//        userDefinedA = new CheckComboBox<>(analyseService.getBrugerDefineretAForDisplay());
//        userDefinedB = new CheckComboBox<>(analyseService.getBrugerDefineretBForDisplay());
//        userDefinedC = new CheckComboBox<>(analyseService.getBrugerDefineretCForDisplay());

//        List<CheckComboBox<String>> checkComboBoxes = Arrays.asList(
//                arketyper, chakraer, dyr, farver, forloeb, personer, userDefinedA, userDefinedB, userDefinedC
//        );

        kategoriLabels = user.getKategoriLabels();

//        for (int i = 0; i < kategoriLabels.size(); i++) {
//            setupCCB(checkComboBoxes.get(i), kategoriLabels.get(i));
//        }

        kollektiv.setVisible(user.isVisKollektiv());
        advarsel.setVisible(user.isVisAdvarsel());

        dpFraGraf.setValue(analyseService.getFirstDreamDate());
        dpTilGraf.setValue(LocalDate.now());

        dpFromPie.setValue(analyseService.getFirstDreamDate());
        dpToPie.setValue(LocalDate.now());

        dpFromTal.setValue(analyseService.getFirstDreamDate());
        dpToTal.setValue(LocalDate.now());

        filterListe.getItems().addAll(analyseService.getFilteredDreams(dpFraGraf.getValue(), dpTilGraf.getValue()));

    }

    @FXML
    private void onSelectKategori() {
        if (comboPieKategorier.getSelectionModel().getSelectedItem() != null) {
            Map<String,Integer> mapData = analyseService.getDataForPieChart(comboPieKategorier.getSelectionModel().getSelectedItem());
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
            ccb.getItems().addAll(c.getSymbols());
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

        // Send datoerne med til getData-funktionen - og alt fra alle ccb og cber! Som en GrafDTO :-)
        GrafDTO data = new GrafDTO();
        data.fra = dpFraGraf.getValue();
        data.til = dpTilGraf.getValue();
//        data.arketyper = arketyper.getCheckModel().getCheckedItems();
//        data.chakraer = chakraer.getCheckModel().getCheckedItems();
//        data.dyr = dyr.getCheckModel().getCheckedItems();
//        data.farver = farver.getCheckModel().getCheckedItems();
//        data.forloeb = forloeb.getCheckModel().getCheckedItems();
//        data.personer = personer.getCheckModel().getCheckedItems();
//        data.brugerDefineretA = userDefinedA.getCheckModel().getCheckedItems();
//        data.brugerDefineretB = userDefinedB.getCheckModel().getCheckedItems();
//        data.brugerDefineretC = userDefinedC.getCheckModel().getCheckedItems();
        data.lucid = lucid.isSelected();
        data.praktiserer = praktiserer.isSelected();
        data.modsat = modsat.isSelected();
        data.arketypisk = arketypisk.isSelected();
        data.mareridt = mareridt.isSelected();
        data.kollektiv = kollektiv.isSelected();
        data.advarsel = advarsel.isSelected();
        data.praksis = praksis.isSelected();
        data.xakse = dayWeekOrMonth();

        //lineChartAnalyse.getData().add(analyseService.onVisGraf(data));

        lineChartAnalyse.setVisible(false);
//        lineChartAnalyse.getData().clear();
//        lineChartAnalyse.layout();

        for (XYChart.Series<String, Number> series : analyseService.getDataForLineChart(data)) {
            lineChartAnalyse.getData().add(series);
        }

        //analyseService.getDataForLineChart(data);
        lineChartAnalyse.setCreateSymbols(false);

        lineChartAnalyse.layout();


        //lineChartAnalyse.layout(); // uundgåeligt! Ellers vises første graf som ragelse...
        lineChartAnalyse.setVisible(true);
        //lcCategoryAxis.setVisible(true);

    }

    private void setupCCB(CheckComboBox<String> ccb, String title) {
        vboxTilCCBAnalyse.getChildren().add(ccb);
        ccb.setMaxWidth(280);
        ccb.setMinWidth(280);
        ccb.setTitle(title);
        ccb.setShowCheckedCount(true);
    }

    private String dayWeekOrMonth() {
        String xAkseValg;
        xAkseValg = (tgDays.isSelected()) ? "dage" : (tgWeeks.isSelected()) ? "uger" : "måneder";
        return xAkseValg;
    }

}
