package fixit.dreams;

import javafx.scene.chart.XYChart;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Stats {
    protected User user;
    private Map<String,StatsDO> categoryStats;
    private Map<String, Map<String, Integer>> arketyperStats;
    private Map<String, Map<String, Integer>> chakraerStats;
    private Map<String, Map<String, Integer>> dyrStats;
    private Map<String, Map<String, Integer>> farverStats;
    private Map<String, Map<String, Integer>> forloebStats;
    private Map<String, Map<String, Integer>> personerStats;
    private Map<String, Map<String, Integer>> aStats;
    private Map<String, Map<String, Integer>> bStats;
    private Map<String, Map<String, Integer>> cStats;

    private Map<String, Integer> lucidStats;
    private Map<String, Integer> praktisererStats;
    private Map<String, Integer> modsatStats;
    private Map<String, Integer> kollektivStats;
    private Map<String, Integer> arketypiskStats;
    private Map<String, Integer> praksisStats;
    private Map<String, Integer> mareridtStats;
    private Map<String, Integer> advarselStats;

    private LocalDate firstDream;

    private Map<Integer,String> monthTranslator;

    public Stats() {
        this.user = User.getInstance();
        this.categoryStats = new HashMap<>();
        this.arketyperStats = new HashMap<>();
        this.chakraerStats = new HashMap<>();
        this.dyrStats = new HashMap<>();
        this.farverStats = new HashMap<>();
        this.forloebStats = new HashMap<>();
        this.personerStats = new HashMap<>();
        this.aStats = new HashMap<>();
        this.bStats = new HashMap<>();
        this.cStats = new HashMap<>();

        this.lucidStats = new HashMap<>();
        this.praktisererStats = new HashMap<>();
        this.modsatStats = new HashMap<>();
        this.kollektivStats = new HashMap<>();
        this.arketypiskStats = new HashMap<>();
        this.praksisStats = new HashMap<>();
        this.mareridtStats = new HashMap<>();
        this.advarselStats = new HashMap<>();

        this.monthTranslator = new HashMap<>();
        calculateStats();
        setupTranslators();
    }

    public void calculateStats() {
        clearAll();
        firstDream = LocalDate.now();
        for (Dream dream : user.getDreams().values()) {
            LocalDate date = dream.getDato();
            YearMonth monthKey = YearMonth.from(date);
            String weekKey = "" + date.getYear() + date.get(WeekFields.ISO.weekOfYear());

            // Check om earliest date skal ændres:
            if (firstDream.isAfter(date)) firstDream = date;

            // Opdater statistik for både dag, uge og måned
            updateStats(String.valueOf(date), dream);
            updateStats(weekKey, dream);
            updateStats(String.valueOf(monthKey), dream);
        }
    }

    private void setupTranslators() {
        monthTranslator.put(1,"Jan");
        monthTranslator.put(2,"Feb");
        monthTranslator.put(3,"Mar");
        monthTranslator.put(4,"Apr");
        monthTranslator.put(5,"Maj");
        monthTranslator.put(6,"Jun");
        monthTranslator.put(7,"Jul");
        monthTranslator.put(8,"Aug");
        monthTranslator.put(9,"Sep");
        monthTranslator.put(10,"Okt");
        monthTranslator.put(11,"Nov");
        monthTranslator.put(12,"Dec");
    }

    private void updateStats(String key, Dream dream) {
        arketyperStats.putIfAbsent(key, new HashMap<>());

        // her skal der i stedet loopes gennem listen af CategoryDTO i hver drøm... men hvad så med stats?
        for (CategoryDTO cat : dream.getCategories()) {
            categoryStats.putIfAbsent(cat.name, new StatsDO(cat.name));
            categoryStats.get(cat.name).updateStatsDO(key,cat);
        }


//        for (String arketype : dream.getArketyper()) {
//            arketyperStats.get(key).merge(arketype, 1, Integer::sum);
//        }
//
//        chakraerStats.putIfAbsent(key, new HashMap<>());
//        for (String chakra : dream.getChakraer()) {
//            chakraerStats.get(key).merge(chakra, 1, Integer::sum);
//        }
//
//        dyrStats.putIfAbsent(key, new HashMap<>());
//        for (String dyr : dream.getDyr()) {
//            dyrStats.get(key).merge(dyr, 1, Integer::sum);
//        }
//
//        farverStats.putIfAbsent(key, new HashMap<>());
//        for (String farve : dream.getFarver()) {
//            farverStats.get(key).merge(farve, 1, Integer::sum);
//        }
//
//        forloebStats.putIfAbsent(key, new HashMap<>());
//        for (String forloeb : dream.getForloeb()) {
//            forloebStats.get(key).merge(forloeb, 1, Integer::sum);
//        }
//
//        personerStats.putIfAbsent(key, new HashMap<>());
//        for (String personer : dream.getPersoner()) {
//            personerStats.get(key).merge(personer, 1, Integer::sum);
//        }
//
//        aStats.putIfAbsent(key, new HashMap<>());
//        for (String a : dream.getBrugerDefineretA()) {
//            aStats.get(key).merge(a, 1, Integer::sum);
//        }
//
//        bStats.putIfAbsent(key, new HashMap<>());
//        for (String b : dream.getBrugerDefineretB()) {
//            bStats.get(key).merge(b, 1, Integer::sum);
//        }
//
//        cStats.putIfAbsent(key, new HashMap<>());
//        for (String c : dream.getBrugerDefineretC()) {
//            cStats.get(key).merge(c, 1, Integer::sum);
//        }

        lucidStats.put(key, lucidStats.getOrDefault(key, 0) + (dream.getLucid() ? 1 : 0));
        praktisererStats.put(key, praktisererStats.getOrDefault(key, 0) + (dream.getPraktiserer() ? 1: 0));
        modsatStats.put(key, modsatStats.getOrDefault(key, 0) + (dream.getModsat() ? 1 : 0));
        kollektivStats.put(key, kollektivStats.getOrDefault(key, 0) + (dream.getKollektiv() ? 1 : 0));
        arketypiskStats.put(key, arketypiskStats.getOrDefault(key, 0) + (dream.getArketypisk() ? 1 : 0));
        praksisStats.put(key, praksisStats.getOrDefault(key, 0) + (dream.getOmpraksis() ? 1 : 0));
        mareridtStats.put(key, mareridtStats.getOrDefault(key, 0) + (dream.getMareridt() ? 1 : 0));
        advarselStats.put(key, advarselStats.getOrDefault(key, 0) + (dream.getAdvarsel() ? 1 : 0));
    }

    public Map<String, Integer> getStatsPerDag(Map<String, Map<String, Integer>> statsMap, LocalDate date) {
        return statsMap.getOrDefault(String.valueOf(date), new HashMap<>());
    }

    public Map<String, Integer> getStatsPerUge(Map<String, Map<String, Integer>> statsMap, LocalDate date) {
        String weekKey = "" + date.getYear() + date.get(WeekFields.ISO.weekOfYear());
        return statsMap.getOrDefault(weekKey, new HashMap<>());
    }

    public Map<String, Integer> getStatsPerM(Map<String, Map<String, Integer>> statsMap, LocalDate date) {
        return statsMap.getOrDefault(String.valueOf(YearMonth.from(date)), new HashMap<>());
    }

    public int getBoolStatsPerDag(Map<String, Integer> statsMap, LocalDate date) {
        return statsMap.getOrDefault(String.valueOf(date), 0);
    }

    public int getBoolStatsPerUge(Map<String, Integer> statsMap, LocalDate date) {
        String weekKey = "" + date.getYear() + date.get(WeekFields.ISO.weekOfYear());
        return statsMap.getOrDefault(weekKey, 0);
    }

    public int getBoolStatsPerM(Map<String, Integer> statsMap, LocalDate date) {
        return statsMap.getOrDefault(String.valueOf(YearMonth.from(date)), 0);
    }

    public LocalDate getFirstDream() {
        return firstDream;
    }

    private void clearAll() {
        this.arketyperStats.clear();
        this.chakraerStats.clear();
        this.dyrStats.clear();
        this.farverStats.clear();
        this.forloebStats.clear();
        this.personerStats.clear();
        this.aStats.clear();
        this.bStats.clear();
        this.cStats.clear();
        this.lucidStats.clear();
        this.praktisererStats.clear();
        this.modsatStats.clear();
        this.kollektivStats.clear();
        this.arketypiskStats.clear();
        this.praksisStats.clear();
        this.mareridtStats.clear();
        this.advarselStats.clear();
    }


    public XYChart.Series<String, Number> makeXY(Map<String, Map<String, Integer>> statsMap, String symbol, LocalDate fra, LocalDate til, String xakse) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(symbol);
        switch (xakse) {
            case "dage":
                while (!fra.isAfter(til)) {
                    Map<String, Integer> temp = getStatsPerDag(statsMap, fra);
                    series.getData().add(new XYChart.Data<>(""+fra.getDayOfMonth(), temp.getOrDefault(symbol, 0)));
                    fra = fra.plusDays(1);
                }
                break;
            case "uger":
                while (fra.get(WeekFields.ISO.weekOfYear()) <= til.get(WeekFields.ISO.weekOfYear())) {
                    Map<String, Integer> temp = getStatsPerUge(statsMap, fra);
                    series.getData().add(new XYChart.Data<>(""+fra.get(WeekFields.ISO.weekOfYear()), temp.getOrDefault(symbol, 0)));
                    fra = fra.plusWeeks(1);
                }
                break;
            case "måneder":
                while (fra.getMonthValue() <= til.getMonthValue()) {
                    Map<String, Integer> temp = getStatsPerM(statsMap, fra);
                    series.getData().add(new XYChart.Data<>(monthTranslator.get(fra.getMonthValue()), temp.getOrDefault(symbol, 0)));
                    fra = fra.plusMonths(1);
                }
                break;
        }
        return series;
    }

    public XYChart.Series<String, Number> makeBoolXY(Map<String, Integer> statsMap, LocalDate fra, LocalDate til, String xakse) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("lucid");
        switch (xakse) {
            case "dage":
                while (!fra.isAfter(til)) {
                    int value = getBoolStatsPerDag(statsMap, fra);
                    series.getData().add(new XYChart.Data<>(""+fra.getDayOfMonth(), value));
                    fra = fra.plusDays(1);
                }
                break;
            case "uger":
                while (fra.get(WeekFields.ISO.weekOfYear()) <= til.get(WeekFields.ISO.weekOfYear())) {
                    int value = getBoolStatsPerUge(statsMap, fra);
                    series.getData().add(new XYChart.Data<>(""+fra.get(WeekFields.ISO.weekOfYear()), value));
                    fra = fra.plusWeeks(1);
                }
                break;
            case "måneder":
                while (fra.getMonthValue() <= til.getMonthValue()) {
                    int value = getBoolStatsPerM(statsMap, fra);
                    series.getData().add(new XYChart.Data<>(monthTranslator.get(fra.getMonthValue()), value));
                    fra = fra.plusMonths(1);
                }
                break;
        }
        return series;
    }

    public Map<String, Map<String, Integer>> getArketyperStats() {
        return arketyperStats;
    }

    public Map<String, Map<String, Integer>> getChakraerStats() {
        return chakraerStats;
    }

    public Map<String, Map<String, Integer>> getDyrStats() {
        return dyrStats;
    }

    public Map<String, Map<String, Integer>> getFarverStats() {
        return farverStats;
    }

    public Map<String, Map<String, Integer>> getForloebStats() {
        return forloebStats;
    }

    public Map<String, Map<String, Integer>> getPersonerStats() {
        return personerStats;
    }

    public Map<String, Map<String, Integer>> getbStats() {
        return bStats;
    }

    public Map<String, Map<String, Integer>> getaStats() {
        return aStats;
    }

    public Map<String, Map<String, Integer>> getcStats() {
        return cStats;
    }

    public Map<String, Integer> getLucidStats() {
        return lucidStats;
    }

    public Map<String, Integer> getPraktisererStats() {
        return praktisererStats;
    }

    public Map<String, Integer> getModsatStats() {
        return modsatStats;
    }

    public Map<String, Integer> getKollektivStats() {
        return kollektivStats;
    }

    public Map<String, Integer> getArketypiskStats() {
        return arketypiskStats;
    }

    public Map<String, Integer> getPraksisStats() {
        return praksisStats;
    }

    public Map<String, Integer> getMareridtStats() {
        return mareridtStats;
    }

    public Map<String, Integer> getAdvarselStats() {
        return advarselStats;
    }

    public Map<String, Map<String, Integer>> getCategoryStats(String name) {
        return categoryStats.get(name).getCatStats();
    }
}
