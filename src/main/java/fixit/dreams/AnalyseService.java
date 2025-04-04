package fixit.dreams;

import javafx.scene.chart.XYChart;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;

public class AnalyseService extends ServiceMother{
    private Stats stats = new Stats();

    public AnalyseService(User user) {
        super(user);
    }

    public Map<String,Integer> getDataForPieChart(String kategoriNavn) {
        HashMap<String,Integer> outMap = new HashMap<>();

        for (Dream d : user.getDreams().values()) {
            if (List.of("Arketyper", "Chakraer", "Dyr", "Farver", "Personer","Forløb").contains(kategoriNavn)) {
                switch (kategoriNavn) {
                    case "Arketyper":
                        for (String symbol : d.getArketyper()) {
                            outMap.merge(symbol, 1, Integer::sum);
                        }
                        break;
                    case "Chakraer":
                        for (String symbol : d.getChakraer()) {
                            outMap.merge(symbol, 1, Integer::sum);
                        }
                        break;
                    case "Dyr":
                        for (String symbol : d.getDyr()) {
                            outMap.merge(symbol, 1, Integer::sum);
                        }
                        break;
                    case "Farver":
                        for (String symbol : d.getFarver()) {
                            outMap.merge(symbol, 1, Integer::sum);
                        }
                        break;
                    case "Forløb":
                        for (String symbol : d.getForloeb()) {
                            outMap.merge(symbol, 1, Integer::sum);
                        }
                        break;
                    case "Personer":
                        for (String symbol : d.getPersoner()) {
                            outMap.merge(symbol, 1, Integer::sum);
                        }
                        break;
                }
            } else {
                String abc = getNewCategoryInternalName(kategoriNavn);
                // Ellers er det en brugerdefineret kategori:
                if (abc.endsWith("A")) {
                    for (String symbol : d.getBrugerDefineretA()) {
                        outMap.merge(symbol, 1, Integer::sum);
                    }

                } else if (abc.endsWith("B")) {
                    for (String symbol : d.getBrugerDefineretB()) {
                        outMap.merge(symbol, 1, Integer::sum);
                    }
                } else {
                    for (String symbol : d.getBrugerDefineretC()) {
                        outMap.merge(symbol, 1, Integer::sum);
                    }
                }
            }
        }
        return outMap;
    }

    public XYChart.Series<String, Number> onVisGraf(GrafDTO guiData) {
        XYChart.Series<String, Number> outSeries = new XYChart.Series<>();
        outSeries.setName("Bananer");
        TreeMap<String, Integer> data = new TreeMap<>();
        data.put("Jan 2024", 4);
        data.put("Feb 2024", 1);
        data.put("Mar 2024", 3);
        data.put("Apr 2024", 5);
        data.put("Maj 2024", 2);
        data.put("Jun 2024", 6);
        data.put("Jul 2024", 1);
        data.put("Aug 2024", 3);
        data.put("Sep 2024", 5);
        data.put("Okt 2024", 2);
        data.put("Nov 2024", 5);
        data.put("Dec 2024", 2);

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            outSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        if (user.skalStatsGenberegnes()){
            updateStats();
        }


        LocalDate current = guiData.fra;
        LocalDate end = guiData.til;
        System.out.println("Sidste ugenr: " + end.get(WeekFields.ISO.weekOfYear()) + " " + end);
        while (current.get(WeekFields.ISO.weekOfYear()) <= end.get(WeekFields.ISO.weekOfYear())) {
            System.out.println(current.get(WeekFields.ISO.weekOfYear()));

            current = current.plusWeeks(1);
        }

        LocalDate current2 = guiData.fra;
        LocalDate end2 = guiData.til;
        // brug getMonthValue() for at få månedens talværdi...
        while (current2.getMonthValue() <= end2.getMonthValue()) {

            current2 = current2.plusMonths(1);
        }

        return outSeries;
    }

    public ArrayList<XYChart.Series<String, Number>> getDataForLineChart(GrafDTO indat) {
        ArrayList<XYChart.Series<String, Number>> outdat = new ArrayList<>();

        //XYChart.Series<String, Number> outSeries = new XYChart.Series<>(); // hm, dem skal jeg jo nok bruge flere af?

        // Loop gennem hver af GTOens kategorier, og få info fra Stats, og lav en XYseries for hvert hit:
        for (String symbol : indat.arketyper) {
            outdat.add(stats.makeXY(stats.getArketyperStats(), symbol, indat.fra, indat.til, indat.xakse));
        }

        for (String symbol : indat.chakraer) {
            outdat.add(stats.makeXY(stats.getChakraerStats(), symbol, indat.fra, indat.til, indat.xakse));
        }

        for (String symbol : indat.dyr) {
            outdat.add(stats.makeXY(stats.getDyrStats(), symbol, indat.fra, indat.til, indat.xakse));
        }

        for (String symbol : indat.farver) {
            outdat.add(stats.makeXY(stats.getFarverStats(), symbol, indat.fra, indat.til, indat.xakse));
        }

        for (String symbol : indat.forloeb) {
            outdat.add(stats.makeXY(stats.getForloebStats(), symbol, indat.fra, indat.til, indat.xakse));
        }

        for (String symbol : indat.personer) {
            outdat.add(stats.makeXY(stats.getPersonerStats(), symbol, indat.fra, indat.til, indat.xakse));
        }

        for (String symbol : indat.brugerDefineretA) {
            outdat.add(stats.makeXY(stats.getaStats(), symbol, indat.fra, indat.til, indat.xakse));
        }

        for (String symbol : indat.brugerDefineretB) {
            outdat.add(stats.makeXY(stats.getbStats(), symbol, indat.fra, indat.til, indat.xakse));
        }

        for (String symbol : indat.brugerDefineretC) {
            outdat.add(stats.makeXY(stats.getcStats(), symbol, indat.fra, indat.til, indat.xakse));
        }

        if (indat.lucid) {
            outdat.add(stats.makeBoolXY(stats.getLucidStats(), indat.fra, indat.til, indat.xakse));
        }

        if (indat.praktiserer) {
            outdat.add(stats.makeBoolXY(stats.getPraktisererStats(), indat.fra, indat.til, indat.xakse));
        }

        if (indat.modsat) {
            outdat.add(stats.makeBoolXY(stats.getModsatStats(), indat.fra, indat.til, indat.xakse));
        }

        if (indat.kollektiv) {
            outdat.add(stats.makeBoolXY(stats.getKollektivStats(), indat.fra, indat.til, indat.xakse));
        }

        if (indat.arketypisk) {
            outdat.add(stats.makeBoolXY(stats.getArketypiskStats(), indat.fra, indat.til, indat.xakse));
        }

        if (indat.praksis) {
            outdat.add(stats.makeBoolXY(stats.getPraksisStats(), indat.fra, indat.til, indat.xakse));
        }

        if (indat.mareridt) {
            outdat.add(stats.makeBoolXY(stats.getMareridtStats(), indat.fra, indat.til, indat.xakse));
        }

        if (indat.advarsel) {
            outdat.add(stats.makeBoolXY(stats.getAdvarselStats(), indat.fra, indat.til, indat.xakse));
        }

        return outdat;
    }

    private boolean isInRange(LocalDate date, LocalDate start, LocalDate end) {
        return (date.isEqual(start) || date.isAfter(start)) && (date.isEqual(end) || date.isBefore(end));
    }

    public void updateStats() {
        stats.calculateStats();
        user.statsErGenberegnet();
    }

    public LocalDate getFirstDreamDate() {
        return stats.getFirstDream();
    }

    public ArrayList<String> getFilteredDreams(LocalDate fra, LocalDate til) {
        ArrayList<String> filteredDreams = new ArrayList<>();
        for (Dream d : user.getDreams().values()) {
            if (isInRange(d.getDato(), fra, til)) {
                filteredDreams.add(d.getIndhold());
            }
        }
        return filteredDreams;
    }


}
