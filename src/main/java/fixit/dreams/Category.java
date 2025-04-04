package fixit.dreams;

import javafx.scene.control.ComboBox;
import org.controlsfx.control.CheckComboBox;

import java.util.ArrayList;
import java.util.TreeSet;

public class Category {
    private TreeSet<String> symbols;
    private String name;
    private ArrayList<ComboBox<String>> combos;
    private ArrayList<CheckComboBox<String>> checkcombos;

    public Category(String name) {
        this.name = name;
        symbols = new TreeSet<>();
        checkcombos = new ArrayList<>();
        combos = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setSymbols(TreeSet<String> symbols) {
        this.symbols = symbols;
    }

    public TreeSet<String> getSymbols() {
        return symbols;
    }

    public void addSymbol(String symbol) {
        symbols.add(symbol);
    }

    public void addCombo(ComboBox<String> combo) {
        combos.add(combo);
    }

    public void addCheckCombo(CheckComboBox<String> check) {
        checkcombos.add(check);
    }

    public void updateAllCombos() {
        for (CheckComboBox<String> combo : checkcombos) {
            combo.getCheckModel().clearChecks();
            combo.getItems().clear();
            combo.getItems().addAll(symbols);
            combo.setShowCheckedCount(true);
        }
        for (ComboBox<String> combo : combos) {
            combo.getSelectionModel().clearSelection();
            combo.getItems().clear();
            combo.getItems().addAll(symbols);
        }
    }
}
