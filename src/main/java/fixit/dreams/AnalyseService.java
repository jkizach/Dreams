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

        // men med den nye kode har en dream en ArrayList<CategoryDTO> som har navn og TreeSet
        for (Dream d : user.getDreams().values()) {
            for (CategoryDTO c : d.getCategories()) {
                if (c.name.equals(kategoriNavn)) {
                    for (String symbol : c.symbols) {
                        outMap.merge(symbol, 1, Integer::sum);
                    }
                }
            }
        }
        return outMap;
    }


    public ArrayList<XYChart.Series<String, Number>> getDataForLineChart(GrafDTO indat) {
        ArrayList<XYChart.Series<String, Number>> outdat = new ArrayList<>();

        // Det kan nu gøres smartere!!! Med StatsDO osv... for alle checkmodels er tilgængelige
        // i user.getCategories() -- getccbFilterSelections returnerer en CategoryDTO med et TreeMap med symboler og et navn
        for (Category c : user.getCategories()) {
            if (c.getccbFilterSelections() != null) {
                for (String symbol : c.getccbFilterSelections().symbols) {
                    outdat.add(stats.makeXY(stats.getCategoryStats(c.getName()), symbol, indat.fra, indat.til, indat.xakse));
                }
            }
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
