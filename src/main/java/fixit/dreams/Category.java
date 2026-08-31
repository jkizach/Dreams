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

    // Id'erne på de indbyggede kategorier der behandles særligt. De er slugs af navnene og
    // ligger fast: netop fordi id'et ikke følger med en omdøbning, beholder Chakraer sin faste
    // rækkefølge og Farver sin farvelægning selv om brugeren giver dem nye navne.
    public static final String ID_KVALITETER = Kategoriid.forIndbygget(FLAGS_CATEGORY_NAME);
    public static final String ID_CHAKRAER = Kategoriid.forIndbygget("Chakraer");
    public static final String ID_FORLOEB = Kategoriid.forIndbygget("Forløb");
    public static final String ID_FARVER = Kategoriid.forIndbygget("Farver");

    private static final List<String> ID_ER_MED_FAST_RAEKKEFOELGE =
            List.of(ID_CHAKRAER, ID_FORLOEB, ID_KVALITETER);

    private TreeSet<String> symbols;
    private ArrayList<String> customOrder;
    private boolean hasCustomOrder;
    private String id;
    private String name;
    private ArrayList<CheckComboBox<String>> ccbDream;
    private ArrayList<CheckComboBox<String>> ccbFilter;

    /**
     * Til de indbyggede kategorier og til indlæsning af kategorier fra før skema v4: id'et
     * udledes af navnet alene, så to maskiner uafhængigt når frem til det samme (se Kategoriid).
     */
    public Category(String name) {
        this(Kategoriid.forIndbygget(name), name);
    }

    public Category(String id, String name) {
        this.id = id;
        this.name = name;
        hasCustomOrder = ID_ER_MED_FAST_RAEKKEFOELGE.contains(id);
        symbols = new TreeSet<>();
        ccbDream = new ArrayList<>();
        ccbFilter = new ArrayList<>();
        customOrder = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    // De to kategorier hvor symbolerne selv ER farver, og hvor cirkeldiagrammet derfor tegnes
    // i de rigtige farver i stedet for JavaFX' palet (se AnalyseController og Symbolfarver).
    // Alle andre kategorier holdes udenfor med vilje: at et dyr eller en arketype tilfældigvis
    // kan læses som et farveord gør det ikke til en farve.
    //
    // Opslaget sker på id, ikke navn, så farvelægningen overlever at brugeren omdøber Farver.
    public static final List<String> FARVEDE_KATEGORI_ID_ER = List.of(ID_FARVER, ID_CHAKRAER);

    // Null-tjekket er ikke pedanteri: List.of() er en immutable liste, og dens contains(null)
    // kaster NullPointerException i stedet for at svare false.
    public static boolean harNaturligeFarver(String kategoriId) {
        return kategoriId != null && FARVEDE_KATEGORI_ID_ER.contains(kategoriId);
    }

    public boolean isFlagsCategory() {
        return ID_KVALITETER.equals(id);
    }

    // Bygger CategoryDTO'en for "Kvaliteter" direkte fra de 9 checkboxes' tilstand -
    // ikke via ccbDream/getccbDreamSelections(), da denne kategori bevidst IKKE har en CheckComboBox i UI'et.
    public static CategoryDTO buildFlagsCategoryDTO(boolean lucid, boolean praktiserer, boolean modsat, boolean arketypisk,
                                                      boolean ompraksis, boolean mareridt, boolean advarsel, boolean kollektiv,
                                                      boolean holografisk) {
        CategoryDTO dto = new CategoryDTO();
        dto.id = ID_KVALITETER;   // en drøms tag bærer id, ikke navn - se CategoryDTO
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
            selected.id = getId();
            selected.symbols = new TreeSet<>(combo.getCheckModel().getCheckedItems());
            break;
        }
        return selected;
    }

    public CategoryDTO getccbFilterSelections() {
        CategoryDTO selected = new CategoryDTO();
        for (CheckComboBox<String> combo : ccbFilter) {
            selected.id = getId();
            selected.symbols = new TreeSet<>(combo.getCheckModel().getCheckedItems());
            break;
        }
        return selected;
    }
}
