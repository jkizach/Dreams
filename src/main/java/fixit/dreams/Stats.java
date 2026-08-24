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

    private LocalDate firstDream;

    private Map<Integer,String> monthTranslator;

    public Stats() {
        this.user = User.getInstance();
        this.categoryStats = new TreeMap<>();

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
            String weekKey = date.get(WeekFields.ISO.weekBasedYear()) + "-" + date.get(WeekFields.ISO.weekOfWeekBasedYear());

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
        // loopes gennem listen af CategoryDTO i hver drøm - inkl. "Kvaliteter" (de tidligere binære flag)
        for (CategoryDTO cat : dream.getCategories()) {
            categoryStats.get(cat.name).updateStatsDO(key,cat);
        }
    }

    public TreeMap<String, Integer> getStatsPerDag(TreeMap<String, TreeMap<String, Integer>> statsMap, LocalDate date) {
        return statsMap.getOrDefault(String.valueOf(date), new TreeMap<>());
    }

    public TreeMap<String, Integer> getStatsPerUge(TreeMap<String, TreeMap<String, Integer>> statsMap, LocalDate date) {
        String weekKey = date.get(WeekFields.ISO.weekBasedYear()) + "-" + date.get(WeekFields.ISO.weekOfWeekBasedYear());
        return statsMap.getOrDefault(weekKey, new TreeMap<>());
    }

    public TreeMap<String, Integer> getStatsPerM(TreeMap<String, TreeMap<String, Integer>> statsMap, LocalDate date) {
        return statsMap.getOrDefault(String.valueOf(YearMonth.from(date)), new TreeMap<>());
    }

    public int getBoolStatsPerDag(Map<String, Integer> statsMap, LocalDate date) {
        return statsMap.getOrDefault(String.valueOf(date), 0);
    }

    public int getBoolStatsPerUge(Map<String, Integer> statsMap, LocalDate date) {
        String weekKey = date.get(WeekFields.ISO.weekBasedYear()) + "-" + date.get(WeekFields.ISO.weekOfWeekBasedYear());
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
                while (!fra.isAfter(til)) {
                    TreeMap<String, Integer> temp = getStatsPerUge(statsMap, fra);
                    String ugeLabel = fra.get(WeekFields.ISO.weekOfYear()) + "\n" + fra.getYear();
                    series.getData().add(new XYChart.Data<>(ugeLabel, temp.getOrDefault(symbol, 0)));
                    fra = fra.plusWeeks(1);
                }
                break;
            case "måneder":
                while (!fra.isAfter(til)) {
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
        series.setName(name); // skal sættes til hvadenten navnet jo er!
        switch (xakse) {
            case "dage":
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM");
                while (!fra.isAfter(til)) {
                    int value = getBoolStatsPerDag(statsMap, fra);
                    String label = fra.format(formatter);
                    series.getData().add(new XYChart.Data<>(label, value));
                    fra = fra.plusDays(1);
                }
                break;
            case "uger":
                while (!fra.isAfter(til)) {
                    int value = getBoolStatsPerUge(statsMap, fra);
                    String ugeLabel = fra.get(WeekFields.ISO.weekOfYear()) + "\n" + fra.getYear();
                    series.getData().add(new XYChart.Data<>(ugeLabel, value));
                    fra = fra.plusWeeks(1);
                }
                break;
            case "måneder":
                while (!fra.isAfter(til)) {
                    int value = getBoolStatsPerM(statsMap, fra);
                    series.getData().add(new XYChart.Data<>(monthTranslator.get(fra.getMonthValue()) + "\n" + fra.getYear(), value));
                    fra = fra.plusMonths(1);
                }
                break;
        }
        return series;
    }

    public int[] getTalBinary(LocalDate fra, LocalDate til) {
        List<String> symbols = Category.FLAGS_SYMBOLS_IN_ORDER;
        List<Map<String, Integer>> statsPerSymbol = new ArrayList<>();
        for (String symbol : symbols) {
            statsPerSymbol.add(getFlagStats(symbol));
        }

        int[] out = new int[symbols.size()];
        LocalDate loopVar = fra;
        while (!loopVar.isAfter(til)) {
            for (int i = 0; i < symbols.size(); i++) {
                out[i] += getBoolStatsPerDag(statsPerSymbol.get(i), loopVar);
            }
            loopVar = loopVar.plusDays(1);
        }
        return out;
    }

    public ArrayList<TreeMap<String,Integer>> getTalCatStats(LocalDate fra, LocalDate til) {
        ArrayList<TreeMap<String,Integer>> testList = new ArrayList<>();

        // Nej jeg skal loope gennem StatsDO og SÅ for hver køre et dato-while-loop!

        // Kunne jeg loope gennem user.getCategories().getName() og så bruge det som key i mine statsCats?
        for (Category category : user.getUiCategories()) {
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


    public Map<String, Integer> getFlagStats(String symbol) {
        Map<String, Integer> out = new HashMap<>();
        StatsDO kvaliteter = categoryStats.get(Category.FLAGS_CATEGORY_NAME);
        if (kvaliteter != null) {
            for (Map.Entry<String, TreeMap<String, Integer>> entry : kvaliteter.getCatStats().entrySet()) {
                Integer value = entry.getValue().get(symbol);
                if (value != null) {
                    out.put(entry.getKey(), value);
                }
            }
        }
        return out;
    }

    public TreeMap<String, TreeMap<String, Integer>> getCategoryStats(String name) {
        return categoryStats.get(name).getCatStats();
    }
}
