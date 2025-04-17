package fixit.dreams;

import javafx.scene.control.ComboBox;
import org.controlsfx.control.CheckComboBox;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class Category {
    private TreeSet<String> symbols;
    private String name;
    private ArrayList<CheckComboBox<String>> ccbDream;
    private ArrayList<CheckComboBox<String>> ccbFilter;

    public Category(String name) {
        this.name = name;
        symbols = new TreeSet<>();
        ccbDream = new ArrayList<>();
        ccbFilter = new ArrayList<>();
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

    public void addSymbols(List<String> symbols) {
        for (String symbol : symbols) {
            addSymbol(symbol);
        }
        updateAllCCBs();
    }

    public void removeSymbol(String symbol) {
        symbols.remove(symbol);
        updateAllCCBs();
    }

    public void addDreamCCB(CheckComboBox<String> ccb) {
        ccbDream.add(ccb);
    }

    public void addFilterCCB(CheckComboBox<String> ccb) {
        ccbFilter.add(ccb);
    }

    public void updateAllCCBs() {
        for (CheckComboBox<String> combo : ccbDream) {
            combo.getCheckModel().clearChecks();
            combo.getItems().clear();
            combo.getItems().addAll(symbols);
            combo.setShowCheckedCount(true);
        }
        for (CheckComboBox<String> combo : ccbFilter) {
            combo.getCheckModel().clearChecks();
            combo.getItems().clear();
            combo.getItems().addAll(symbols);
            combo.setShowCheckedCount(true);
        }
    }

    public void resetDreamCCBs() {
        for (CheckComboBox<String> combo : ccbDream) {
            combo.getCheckModel().clearChecks();
        }
    }

    public CategoryDTO getccbDreamSelections() {
        CategoryDTO selected = new CategoryDTO();
        for (CheckComboBox<String> combo : ccbDream) {
            selected.name = getName();
            selected.symbols = new TreeSet<>(combo.getCheckModel().getCheckedItems());
            break;
        }
        return selected;
    }

    public CategoryDTO getccbFilterSelections() {
        CategoryDTO selected = new CategoryDTO();
        for (CheckComboBox<String> combo : ccbFilter) {
            selected.name = getName();
            selected.symbols = new TreeSet<>(combo.getCheckModel().getCheckedItems());
            break;
        }
        return selected;
    }
}
