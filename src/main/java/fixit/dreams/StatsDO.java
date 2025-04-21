package fixit.dreams;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class StatsDO {
    private String name;
    private TreeMap<String, TreeMap<String, Integer>> catStats;

    public StatsDO(String name) {
        this.name = name;
        catStats = new TreeMap<>();
    }

    public void updateStatsDO(String datekey, CategoryDTO cat) {
        catStats.putIfAbsent(datekey, new TreeMap<>());
        for (String symbol : cat.symbols) {
            catStats.get(datekey).merge(symbol, 1, Integer::sum);
        }
    }

    public TreeMap<String, TreeMap<String, Integer>> getCatStats() {
        return catStats;
    }

    // Så updataStats har som parametre en String-key (datoen) og en drøm - ud fra det smider den data ind

}
