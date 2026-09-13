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
    private ToggleButton tgDays, tgMonths, tgWeeks, tgForloebKort, tgForloebLangt,
            tgPeriodeUge, tgPeriodeMaaned, tgPeriodeAar, tgPeriodeInterval;

    @FXML
    private ToggleGroup forloebVisningGroup, periodeGruppe;

    @FXML
    private ListView<DreamDTO> filterListe, forloebListe, forloebValgListe;

    @FXML
    private PieChart pieChartAnalyse;

    @FXML
    private ComboBox<String> comboPieKategorier;

    @FXML
    private CheckBox lucid, praktiserer, modsat, arketypisk, praksis, mareridt, kollektiv, advarsel, holografisk;

    @FXML
    private Button btnVisGraf, btnAndOr, btnForloebVisListe, btnForloebPlus, btnForloebRediger;

    @FXML
    private Spinner<Integer> daysSpinner, monthsSpinner;

    @FXML
    private Label lblForloebDream, antalDreamsLblTal, antalDreamsLblCirkel, lblAntalDrommeGraf, lblAntalDrommeForloeb, lblAntalDrommeListe, lblPeriode;

    private boolean visLangtForloeb = false;

    // Sand mens koden selv skriver i Cirkel-fanens to datovælgere.
    //
    // En DatePicker der har fået sin skin - altså enhver der sidder i et rigtigt vindue - fyrer
    // onAction når setValue() ændrer værdien. Og vi skriver i to felter efter hinanden: efter
    // det første kald står Fra på den nye måned mens Til stadig står på den gamle. Uden vagten
    // bliver vores egen halvfærdige skrivning læst som om brugeren havde rettet datoen i
    // hånden, og perioden hopper i Interval midt i sit eget opdateringsforløb - hvorefter
    // pilene begynder at flytte sig med et vilkårligt antal dage i stedet for en måned.
    //
    // Fælden er usynlig uden et vindue: en DatePicker uden skin fyrer ingenting ved setValue,
    // så en test der bare loader FXML'en ser ikke problemet.
    private boolean skriverSelv;

    // Hvad koden selv sidst skrev. Brugeren kan bekræfte den samme dato igen - Enter i feltet,
    // eller det samme dagfelt i kalenderen - og så kommer der en onAction uden at noget har
    // ændret sig. Sådan et tryk må ikke smide en valgt måned på gulvet.
    private LocalDate sidstSkrevetFra, sidstSkrevetTil;

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
                    // Var updateAntalDreamsCirkel(): tallet blev opdateret, men cirklen blev
                    // stående på de gamle data, så etiket og diagram kunne sige hver sit.
                    // onSelectKategori tæller op OG tegner om - og gør ingenting hvis der slet
                    // ikke er valgt en kategori endnu.
                    onSelectKategori();
                    kollektiv.setVisible(user.isVisKollektiv());
                    kollektiv.setManaged(user.isVisKollektiv());
                    advarsel.setVisible(user.isVisAdvarsel());
                    advarsel.setManaged(user.isVisAdvarsel());
                    holografisk.setVisible(user.isVisHolografisk());
                    holografisk.setManaged(user.isVisHolografisk());
                    genopfriskForloebsdroemme();
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

        // Cirkel-fanen begynder i Interval med de datoer setGuiDates lige har sat - første drøm
        // til i dag - så fanen ser ud og opfører sig præcis som før perioden fandtes.
        visPeriode(aktuelPeriode());

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

        // Samme regel for periodeknapperne på cirkelfanen: et tryk på den knap der allerede er
        // valgt, ville ellers slå perioden helt fra og efterlade pilene uden noget at flytte.
        periodeGruppe.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
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
        int antal = (kategoriId == null) ? 0 : analyseService.countDreamsForKategori(kategoriId, pieFra(), pieTil());
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

    // Graf- og Tal-fanernes Fra-dato er et rent filter, og den må gerne følge startdatoen: har
    // en sync hentet ældre drømme ned, skal de kunne ses uden at man selv skal rette datoen.
    //
    // Cirkel-fanens Fra-dato er ikke bare et filter mere - den ER den periode brugeren står i.
    // Står den på september, er det september etiketten og pilene regner ud fra, og en
    // genberegning (der kommer én pr. hentet drøm under en sync) ville trække den ned til den
    // første drøm og lade etiketten stå og lyve om hvad cirklen viser. I Interval opfører den
    // sig præcis som før.
    private void updateGuiDates() {
        for (DatePicker dp : List.of(dpFraGraf,dpFromTal)) {
            dp.setValue(analyseService.getStartDate());
        }
        if (valgtPeriodetype() == Periode.Type.INTERVAL) {
            skriverSelv = true;
            try {
                dpFromPie.setValue(analyseService.getStartDate());
            } finally {
                skriverSelv = false;
            }
        }
        visPeriode(aktuelPeriode());
    }

    /* ---------- Cirkel-fanens periode ---------- */

    // Datovælgerne er facit for hvad cirklen viser - men de kan stå tomme: markér teksten,
    // tryk slet, og værdien er null. ServiceMother.isInRange tager ikke imod null og ville
    // kaste en NullPointerException midt i optællingen. En tom Fra læses derfor som
    // startdatoen og en tom Til som i dag, og svaret skrives tilbage i vælgeren, så skærmen
    // ikke står og påstår noget andet end det cirklen er tegnet på.
    private LocalDate pieFra() {
        return dpFromPie.getValue() != null ? dpFromPie.getValue() : analyseService.getStartDate();
    }

    private LocalDate pieTil() {
        return dpToPie.getValue() != null ? dpToPie.getValue() : LocalDate.now();
    }

    private Periode.Type valgtPeriodetype() {
        if (tgPeriodeUge.isSelected())    return Periode.Type.UGE;
        if (tgPeriodeMaaned.isSelected()) return Periode.Type.MÅNED;
        if (tgPeriodeAar.isSelected())    return Periode.Type.ÅR;
        return Periode.Type.INTERVAL;
    }

    // Perioden gemmes ikke i et felt: den regnes ud af de to datovælgere og den knap der er
    // trykket ind, hver gang der er brug for den. En hel uge der gøres hel igen er den samme
    // uge, så det koster ingenting at spørge - og der er kun ét sted sandheden kan stå forkert.
    private Periode aktuelPeriode() {
        return new Periode(valgtPeriodetype(), pieFra(), pieTil());
    }

    // Perioden ud på skærmen: datoerne i vælgerne, teksten mellem pilene. De to skrivninger
    // holdes under vagten, så halvvejen - Fra ny, Til gammel - ikke bliver læst som at brugeren
    // har rettet noget.
    private void visPeriode(Periode periode) {
        skriverSelv = true;
        try {
            sidstSkrevetFra = periode.fra();
            sidstSkrevetTil = periode.til();
            dpFromPie.setValue(periode.fra());
            dpToPie.setValue(periode.til());
        } finally {
            skriverSelv = false;
        }
        lblPeriode.setText(periode.etiket());
    }

    // Hvilken dato den nye periode skal lægge sig omkring, når man skifter fra fx Interval til
    // Måned. Normalt Fra-datoen: står den i september, er det september man får.
    //
    // Med ét hensyn: rummer det udsnit man står i DAGS DATO, er det den måned - eller uge,
    // eller år - man står i, man vil se. Ellers ville det allerførste tryk på "Måned" lande på
    // måneden for ens allerførste drøm, fordi standardintervallet begynder dér, og man skulle
    // trykke ">" mange hundrede gange for at komme hjem igen.
    private LocalDate anker() {
        LocalDate iDag = LocalDate.now();
        LocalDate fra = pieFra();
        LocalDate til = pieTil();
        boolean rummerIDag = !iDag.isBefore(fra) && !iDag.isAfter(til);
        return rummerIDag ? iDag : fra;
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
    private void onPeriodetypeValgt() {
        Periode.Type type = valgtPeriodetype();
        // Interval rører ikke datoerne - man beholder præcis det udsnit man stod i, og pilene
        // begynder bare at flytte det med dets egen længde i stedet for en uge ad gangen.
        Periode ny = (type == Periode.Type.INTERVAL)
                ? new Periode(type, pieFra(), pieTil())
                : Periode.omkring(type, anker());
        visPeriode(ny);
        onSelectKategori();
    }

    @FXML
    private void onForrigePeriode() {
        visPeriode(aktuelPeriode().forrige());
        onSelectKategori();
    }

    @FXML
    private void onNaestePeriode() {
        visPeriode(aktuelPeriode().næste());
        onSelectKategori();
    }

    // Retter man selv en dato, er det ikke en hel uge eller måned længere. Så skal knapperne
    // ikke stå og rette den tilbage igen ved næste tryk - vi lander i Interval, hvor pilene
    // flytter vinduet med dets egen længde og dermed stadig kan bruges til noget.
    @FXML
    private void onPieDatoRettet() {
        if (skriverSelv) {
            return; // vores egen skrivning, ikke brugerens
        }
        if (Objects.equals(dpFromPie.getValue(), sidstSkrevetFra)
                && Objects.equals(dpToPie.getValue(), sidstSkrevetTil)) {
            return; // de samme datoer bekræftet igen - ingen grund til at forlade perioden
        }
        tgPeriodeInterval.setSelected(true); // setSelected fyrer ikke onAction
        visPeriode(new Periode(Periode.Type.INTERVAL, pieFra(), pieTil()));
        onSelectKategori();
    }

    @FXML
    private void onSelectKategori() {
        updateAntalDreamsCirkel();
        String kategoriId = analyseService.idForKategoriNavn(comboPieKategorier.getSelectionModel().getSelectedItem());
        if (kategoriId != null) {
            Map<String,Integer> mapData = analyseService.getDataForPieChart(kategoriId,pieFra(),pieTil());
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

        // Kategorierne deles ligeligt over de to kolonner; ved et ulige antal får venstre den ekstra.
        // Tidligere stod der fast 4 til venstre, så 10 kategorier blev til 4 og 6.
        int antalVenstre = (cats.size() + 1) / 2;

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

            if (i < antalVenstre) {
                talVboxCatOne.getChildren().add(overskrift);
                talVboxCatOne.getChildren().add(tv);
            } else {
                talVboxCatTwo.getChildren().add(overskrift);
                talVboxCatTwo.getChildren().add(tv);
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

    // Samme funktion som "Rediger drøm" i Drømmeliste-fanen, men på den drøm der er valgt i
    // søgeresultatet til højre. Deler vindue med den anden knap (EditDreamController.aabnRedigering),
    // så de to veje ikke kan komme til at opføre sig forskelligt.
    //
    // Oprydningen bagefter er ikke til pynt. Vinduet er modalt, så når kaldet vender tilbage er
    // redigeringen ovre - og listen her viser DTO'er, der er øjebliksbilleder af drømmene og
    // altså stadig bærer den gamle tekst. Forløbsdrømmene til venstre klarer sig selv: de bygges
    // om af statistiksignalet, som redigeringen har udløst (se lytteren i initialize).
    @FXML
    private void redigerForloebDream() {
        DreamDTO valgt = forloebListe.getSelectionModel().getSelectedItem();
        if (valgt == null) {
            return; // ingen drøm valgt - præcis som knappen i Drømmeliste-fanen gør det
        }
        Dream drøm = analyseService.getDream(valgt.getId());
        if (drøm == null) {
            return;
        }

        EditDreamController.aabnRedigering(drøm);

        analyseService.genopfriskForloebDreams();
        vælgIgen(valgt.getId());
    }

    // Forløbsdrømmene til venstre. Listen bygges HELT om hver gang noget ændrer sig i drømmene,
    // og da rækkerne er nye objekter, ryger markeringen med. Det var ikke til at se på listen -
    // men etiketten under den bliver stående med fasen fra den drøm der IKKE længere er valgt,
    // og så står der noget forkert på skærmen. Derfor findes valget frem igen bagefter.
    private void genopfriskForloebsdroemme() {
        DreamDTO valgt = forloebValgListe.getSelectionModel().getSelectedItem();

        analyseService.updateForloeb();

        if (valgt == null) {
            return;
        }
        for (DreamDTO dto : forloebValgListe.getItems()) {
            if (dto.getId().equals(valgt.getId())) {
                forloebValgListe.getSelectionModel().select(dto);
                onSelectForloebDream(); // fasen kan være ændret af netop den redigering
                return;
            }
        }
        // Drømmen har mistet sin fase (eller er slettet) og står der ikke længere
        lblForloebDream.setText("Ingen drøm valgt endnu");
    }

    // Listen er bygget om, så den valgte række er et NYT objekt. Uden det her ville markeringen
    // forsvinde hver gang man havde redigeret en drøm, og man skulle finde sin plads igen.
    private void vælgIgen(String dreamId) {
        for (DreamDTO dto : forloebListe.getItems()) {
            if (dto.getId().equals(dreamId)) {
                forloebListe.getSelectionModel().select(dto);
                forloebListe.scrollTo(dto);
                return;
            }
        }
    }
}
