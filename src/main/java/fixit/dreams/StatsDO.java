package fixit.dreams;

import java.util.HashMap;
import java.util.Map;

public class StatsDO {
    private String name;
    private Map<String, Map<String, Integer>> catStats;

    public StatsDO(String name) {
        this.name = name;
        catStats = new HashMap<>();
    }

    public void updateStatsDO(String datekey, CategoryDTO cat) {
        catStats.putIfAbsent(datekey, new HashMap<>());
        for (String symbol : cat.symbols) {
            catStats.get(datekey).merge(symbol, 1, Integer::sum);
        }
    }

    public Map<String, Map<String, Integer>> getCatStats() {
        return catStats;
    }

    // Så updataStats har som parametre en String-key (datoen) og en drøm - ud fra det smider den data ind



}
