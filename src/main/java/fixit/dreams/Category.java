package fixit.dreams;

import javafx.scene.control.ComboBox;
import org.controlsfx.control.CheckComboBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

public class Category {
    public static final String FLAGS_CATEGORY_NAME = "Kvaliteter";
    public static final List<String> FLAGS_SYMBOLS_IN_ORDER = List.of(
            "Lucid", "Praktiserer", "Modsatkønnet", "Arketypisk", "Om praksis", "Mareridt", "Advarsel", "Kollektiv", "Holografisk");

    private TreeSet<String> symbols;
    private ArrayList<String> customOrder;
    private boolean hasCustomOrder;
    private String name;
    private ArrayList<CheckComboBox<String>> ccbDream;
    private ArrayList<CheckComboBox<String>> ccbFilter;

    public Category(String name) {
        this.name = name;
        hasCustomOrder = (List.of("Chakraer", "Forløb", FLAGS_CATEGORY_NAME).contains(name));
        symbols = new TreeSet<>();
        ccbDream = new ArrayList<>();
        ccbFilter = new ArrayList<>();
        customOrder = new ArrayList<>();
    }

    // De to kategorier hvor symbolerne selv ER farver, og hvor cirkeldiagrammet derfor tegnes
    // i de rigtige farver i stedet for JavaFX' palet (se AnalyseController og Symbolfarver).
    // Alle andre kategorier holdes udenfor med vilje: at et dyr eller en arketype tilfældigvis
    // kan læses som et farveord gør det ikke til en farve.
    public static final List<String> FARVEDE_KATEGORIER = List.of("Farver", "Chakraer");

    // Null-tjekket er ikke pedanteri: List.of() er en immutable liste, og dens contains(null)
    // kaster NullPointerException i stedet for at svare false.
    public static boolean harNaturligeFarver(String kategoriNavn) {
        return kategoriNavn != null && FARVEDE_KATEGORIER.contains(kategoriNavn);
    }

    public boolean isFlagsCategory() {
        return FLAGS_CATEGORY_NAME.equals(name);
    }

    // Bygger CategoryDTO'en for "Kvaliteter" direkte fra de 9 checkboxes' tilstand -
    // ikke via ccbDream/getccbDreamSelections(), da denne kategori bevidst IKKE har en CheckComboBox i UI'et.
    public static CategoryDTO buildFlagsCategoryDTO(boolean lucid, boolean praktiserer, boolean modsat, boolean arketypisk,
                                                      boolean ompraksis, boolean mareridt, boolean advarsel, boolean kollektiv,
                                                      boolean holografisk) {
        CategoryDTO dto = new CategoryDTO();
        dto.name = FLAGS_CATEGORY_NAME;
        dto.symbols = new TreeSet<>();
        if (lucid) dto.symbols.add("Lucid");
        if (praktiserer) dto.symbols.add("Praktiserer");
        if (modsat) dto.symbols.add("Modsatkønnet");
        if (arketypisk) dto.symbols.add("Arketypisk");
        if (ompraksis) dto.symbols.add("Om praksis");
        if (mareridt) dto.symbols.add("Mareridt");
        if (advarsel) dto.symbols.add("Advarsel");
        if (kollektiv) dto.symbols.add("Kollektiv");
        if (holografisk) dto.symbols.add("Holografisk");
        dto.customOrder = new ArrayList<>(FLAGS_SYMBOLS_IN_ORDER);
        return dto;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSymbols(TreeSet<String> symbols) {
        this.symbols = symbols;
    }

    public void setCustomOrder(ArrayList<String> customOrder) {
        this.customOrder = customOrder;
    }

    public TreeSet<String> getSymbols() {
        return symbols;
    }

    public ArrayList<String> getCustomOrder() {
        return customOrder;
    }

    public Collection<String> getSymbolsForDisplay() {
        if (hasCustomOrder) {
            return customOrder;
        } else {
            return symbols;
        }
    }

    public boolean hasCustomOrder() {
        return hasCustomOrder;
    }

    public void addSymbol(String symbol) {
        if (symbols.add(symbol) && hasCustomOrder) {
            customOrder.add(symbol);
        }
    }

    public void addSymbols(List<String> symbols) {
        for (String symbol : symbols) {
            addSymbol(symbol);
        }
        updateAllCCBs();
    }

    public void removeSymbol(String symbol) {
        symbols.remove(symbol);
        if (hasCustomOrder) customOrder.remove(symbol);
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
            combo.setTitle(this.getName());
            combo.getCheckModel().clearChecks();
            combo.getItems().clear();
            combo.getItems().addAll(getSymbolsForDisplay());
            combo.setShowCheckedCount(true);
        }
        for (CheckComboBox<String> combo : ccbFilter) {
            combo.setTitle(this.getName());
            combo.getCheckModel().clearChecks();
            combo.getItems().clear();
            combo.getItems().addAll(getSymbolsForDisplay());
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
