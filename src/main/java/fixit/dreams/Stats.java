package fixit.dreams;

import javafx.scene.chart.XYChart;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;

public class Stats {
    protected User user;
    private TreeMap<String,StatsDO> categoryStats;

    private Map<String, Integer> lucidStats;
    private Map<String, Integer> praktisererStats;
    private Map<String, Integer> modsatStats;
    private Map<String, Integer> kollektivStats;
    private Map<String, Integer> arketypiskStats;
    private Map<String, Integer> praksisStats;
    private Map<String, Integer> mareridtStats;
    private Map<String, Integer> advarselStats;

    ArrayList<Map<String, Integer>> binaryStats;

    private LocalDate firstDream;

    private Map<Integer,String> monthTranslator;

    public Stats() {
        this.user = User.getInstance();
        this.categoryStats = new TreeMap<>();

        this.lucidStats = new HashMap<>();
        this.praktisererStats = new HashMap<>();
        this.modsatStats = new HashMap<>();
        this.kollektivStats = new HashMap<>();
        this.arketypiskStats = new HashMap<>();
        this.praksisStats = new HashMap<>();
        this.mareridtStats = new HashMap<>();
        this.advarselStats = new HashMap<>();

        this.binaryStats = new ArrayList<>();
        binaryStats.add(lucidStats);
        binaryStats.add(praktisererStats);
        binaryStats.add(modsatStats);
        binaryStats.add(arketypiskStats);
        binaryStats.add(praksisStats);
        binaryStats.add(mareridtStats);
        binaryStats.add(advarselStats);
        binaryStats.add(kollektivStats);

        this.monthTranslator = new HashMap<>();
        calculateStats();
        setupTranslators();
    }

    public void calculateStats() {
        clearAll();

        for (Category c : user.getCategories()) {
            categoryStats.putIfAbsent(c.getName(), new StatsDO(c.getName()));
        }
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
        // her skal der i stedet loopes gennem listen af CategoryDTO i hver drøm... men hvad så med stats?
        for (CategoryDTO cat : dream.getCategories()) {
            //categoryStats.putIfAbsent(cat.name, new StatsDO(cat.name)); // jeg tror at den her skal flyttes til CalculateStats og skal bruge user.Cats!
            categoryStats.get(cat.name).updateStatsDO(key,cat);
        }

        lucidStats.put(key, lucidStats.getOrDefault(key, 0) + (dream.getLucid() ? 1 : 0));
        praktisererStats.put(key, praktisererStats.getOrDefault(key, 0) + (dream.getPraktiserer() ? 1: 0));
        modsatStats.put(key, modsatStats.getOrDefault(key, 0) + (dream.getModsat() ? 1 : 0));
        kollektivStats.put(key, kollektivStats.getOrDefault(key, 0) + (dream.getKollektiv() ? 1 : 0));
        arketypiskStats.put(key, arketypiskStats.getOrDefault(key, 0) + (dream.getArketypisk() ? 1 : 0));
        praksisStats.put(key, praksisStats.getOrDefault(key, 0) + (dream.getOmpraksis() ? 1 : 0));
        mareridtStats.put(key, mareridtStats.getOrDefault(key, 0) + (dream.getMareridt() ? 1 : 0));
        advarselStats.put(key, advarselStats.getOrDefault(key, 0) + (dream.getAdvarsel() ? 1 : 0));
    }

    public TreeMap<String, Integer> getStatsPerDag(TreeMap<String, TreeMap<String, Integer>> statsMap, LocalDate date) {
        return statsMap.getOrDefault(String.valueOf(date), new TreeMap<>());
    }

    public TreeMap<String, Integer> getStatsPerUge(TreeMap<String, TreeMap<String, Integer>> statsMap, LocalDate date) {
        String weekKey = "" + date.getYear() + date.get(WeekFields.ISO.weekOfYear());
        return statsMap.getOrDefault(weekKey, new TreeMap<>());
    }

    public TreeMap<String, Integer> getStatsPerM(TreeMap<String, TreeMap<String, Integer>> statsMap, LocalDate date) {
        return statsMap.getOrDefault(String.valueOf(YearMonth.from(date)), new TreeMap<>());
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
        this.categoryStats.clear();
        this.lucidStats.clear();
        this.praktisererStats.clear();
        this.modsatStats.clear();
        this.kollektivStats.clear();
        this.arketypiskStats.clear();
        this.praksisStats.clear();
        this.mareridtStats.clear();
        this.advarselStats.clear();
    }


    public XYChart.Series<String, Number> makeXY(TreeMap<String, TreeMap<String, Integer>> statsMap, String symbol, LocalDate fra, LocalDate til, String xakse) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(symbol);
        switch (xakse) {
            case "dage":
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM");
                while (!fra.isAfter(til)) {
                    TreeMap<String, Integer> temp = getStatsPerDag(statsMap, fra);
                    String label = fra.format(formatter);
                    series.getData().add(new XYChart.Data<>(label, temp.getOrDefault(symbol, 0)));
                    fra = fra.plusDays(1);
                }
                break;
            case "uger":
                while (fra.get(WeekFields.ISO.weekOfYear()) <= til.get(WeekFields.ISO.weekOfYear())) {
                    TreeMap<String, Integer> temp = getStatsPerUge(statsMap, fra);
                    String ugeLabel = fra.get(WeekFields.ISO.weekOfYear()) + "\n" + fra.getYear();
                    series.getData().add(new XYChart.Data<>(ugeLabel, temp.getOrDefault(symbol, 0)));
                    fra = fra.plusWeeks(1);
                }
                break;
            case "måneder":
                while (fra.getMonthValue() <= til.getMonthValue()) {
                    TreeMap<String, Integer> temp = getStatsPerM(statsMap, fra);
                    series.getData().add(new XYChart.Data<>(monthTranslator.get(fra.getMonthValue()) + "\n" + fra.getYear(), temp.getOrDefault(symbol, 0)));
                    fra = fra.plusMonths(1);
                }
                break;
        }
        return series;
    }

    public XYChart.Series<String, Number> makeBoolXY(Map<String, Integer> statsMap, LocalDate fra, LocalDate til, String xakse, String name) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(name); // skal sættes til havdenten navnet jo er!
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

    public int[] getTalBinary(LocalDate fra, LocalDate til) {
        int[] out = new int[8];
        while (!fra.isAfter(til)) {
            for (int i = 0; i < 8; i++) {
                int value = getBoolStatsPerDag(binaryStats.get(i), fra);
                out[i] += value;
            }
            fra = fra.plusDays(1);
        }
        return out;
    }

    public ArrayList<TreeMap<String,Integer>> getTalCatStats(LocalDate fra, LocalDate til) {
        ArrayList<TreeMap<String,Integer>> testList = new ArrayList<>();

        // Nej jeg skal loope gennem StatsDO og SÅ for hver køre et dato-while-loop!

        // Kunne jeg loope gennem user.getCategories().getName() og så bruge det som key i mine statsCats?
        for (Category category : user.getCategories()) {
            TreeMap<String, Integer> totals = new TreeMap<>();
            LocalDate loopVar = fra;
            while (!loopVar.isAfter(til)) {
                TreeMap<String, Integer> tm = categoryStats.get(category.getName()).getCatStats().get(String.valueOf(loopVar));
                if (tm != null) {
                    tm.forEach((key, value) -> totals.merge(key, value, Integer::sum));
                }
                loopVar = loopVar.plusDays(1);
            }
            testList.add(totals);
        }
        return testList;
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

    public TreeMap<String, TreeMap<String, Integer>> getCategoryStats(String name) {
        return categoryStats.get(name).getCatStats();
    }
}
