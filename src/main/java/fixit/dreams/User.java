package fixit.dreams;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.controlsfx.control.CheckComboBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

class User {
    private static User instans = null;
    private ArrayList<Category> categories;
    private HashMap<String,Dream> dreams;
    private ObservableList<String> kategoriLabels = FXCollections.observableArrayList();
    private String foretrukneTema = "mørkt grønt";
    private HashMap<String,Tema> temaer;
    private boolean visAdvarsel = false;
    private boolean visKollektiv = false;
    private LocalDate startFromThisDate = null;
    protected BooleanProperty skalStatsGenberegnes = new SimpleBooleanProperty(false);
    protected StringProperty dreamEdited = new SimpleStringProperty("tom");

    private ArrayList<VBox> dreamVboxes = new ArrayList<>();
    private ArrayList<VBox> filterVboxes = new ArrayList<>();

    private User() {
        SchemaMigrator.migrateIfNeeded();

        UserDTO loadedUserDTO = IOutils.loadUser();
        HashMap<String,Tema> loadedTemaer = IOutils.loadTemaer();
        ArrayList<Category> loadedCategories = IOutils.loadCategories();
        HashMap<String, Dream> loadedDreams = IOutils.loadDreams();
        this.kategoriLabels = FXCollections.observableArrayList();

        if (loadedTemaer != null) {
            System.out.println("Temaer er loaded!");
            this.temaer = loadedTemaer;
        } else {
            temaer = new HashMap<>();
            addPredefinedThemes();
        }

        if (loadedCategories != null) {
            System.out.println("Categories loaded!");
            this.categories = loadedCategories;
            for (Category cat : getUiCategories()) {
                kategoriLabels.add(cat.getName());
            }
        } else {
            categories = new ArrayList<>();
            addDefaultCategories();
            System.out.println("Categories not loaded!");
        }

        if (loadedDreams != null && !loadedDreams.isEmpty()) {
            System.out.println("Dreams loaded!");
            dreams = loadedDreams;
        } else {
            dreams = new HashMap<>();
        }

        if (loadedUserDTO != null) {
            System.out.println("Load userDTO virkede...");
            this.foretrukneTema = loadedUserDTO.foretrukneTema;
            this.visAdvarsel = loadedUserDTO.visAdvarsel;
            this.visKollektiv = loadedUserDTO.visKollektiv;
            this.startFromThisDate = (loadedUserDTO.startFromThisDate != null) ? loadedUserDTO.startFromThisDate : getFirstDreamDate();

        } else {
            System.out.println("Ingen user loaded...");
            this.startFromThisDate = getFirstDreamDate();
         }
    }

    public LocalDate getFirstDreamDate() {
        LocalDate earliest = LocalDate.now();
        for (Dream d : dreams.values()) {
            if (earliest.isAfter(d.getDato())) {
                earliest = d.getDato();
            }
        }
        return earliest;
    }

    public static synchronized User getInstance()
    {
        if (instans == null)
            instans = new User();

        return instans;
    }

    // test-only seam; package-private, usynlig uden for fixit.dreams
    static synchronized void resetForTests() {
        instans = null;
    }


    private void addPredefinedThemes() {
        Tema darkGreen = new Tema(Color.web("#1a1a1a"),Color.web("#62cd84"),Color.web("#262626"),Color.web("#333333"),
                Color.web("#d1d1d1"),Color.web("#1a1a1a"),Color.web("#62cd84"),Color.web("#62cd84"),"mørkt grønt", "Courier New");
        temaer.put(foretrukneTema, darkGreen);

        Tema darkBlue = new Tema(Color.web("#1a1a1a"),Color.web("#678ae6"),Color.web("#262626"),Color.web("#333333"),
                Color.web("#d1d1d1"),Color.web("#1a1a1a"),Color.web("#678ae6"),Color.web("#0b38af"),"mørkt blåt", "Source Code Pro");
        temaer.put("mørkt blåt", darkBlue);
    }

    public void addDream(Dream d) {
        dreams.put(d.getId(),d);
        skalStatsGenberegnes.set(true);
    }

    public void deleteDream(String id) {
        dreams.remove(id);
        skalStatsGenberegnes.set(true);
    }

    public Dream getDream(String id) {
        return dreams.get(id);
    }

    public HashMap<String,Dream> getDreams() {
        return dreams;
    }

    public ObservableList<String> getKategoriLabels() {
        return kategoriLabels;
    }

    public void refreshKategoriLabels() {
        kategoriLabels.clear();
        for (Category cat : getUiCategories()) {
            kategoriLabels.add(cat.getName());
        }
    }

    public ArrayList<Category> getUiCategories() {
        ArrayList<Category> result = new ArrayList<>();
        for (Category c : categories) {
            if (!c.isFlagsCategory()) {
                result.add(c);
            }
        }
        return result;
    }

    public String getForetrukneTemaNavn() {
        return foretrukneTema;
    }

    public void setForetrukneTema(String foretrukneTema) {
        this.foretrukneTema = foretrukneTema;
    }

    public HashMap<String, Tema> getTemaer() {
        return temaer;
    }

    public Tema getForetrukneTema() {
        return temaer.get(foretrukneTema);
    }

    public void addNytTema(Tema nytTema) {
        temaer.put(nytTema.getTemaName(),nytTema);
    }

    public void setDreams(HashMap<String, Dream> dreams) {
        this.dreams = dreams;
    }

    public void setKategoriLabels(ArrayList<String> kategoriLabels) {
        this.kategoriLabels.addAll(kategoriLabels);
    }

    public void setTemaer(HashMap<String, Tema> temaer) {
        this.temaer = temaer;
    }

    public boolean isVisAdvarsel() {
        return visAdvarsel;
    }

    public void setVisAdvarsel(boolean visAdvarsel) {
        this.visAdvarsel = visAdvarsel;
    }

    public boolean isVisKollektiv() {
        return visKollektiv;
    }

    public void setVisKollektiv(boolean visKollektiv) {
        this.visKollektiv = visKollektiv;
    }

    public void statsErGenberegnet() {
        skalStatsGenberegnes.set(false);
    }

    public void genberegnStatsPlease() {skalStatsGenberegnes.set(true);}

    public boolean skalStatsGenberegnes() {
        return skalStatsGenberegnes.get();
    }

    public ArrayList<Category> getCategories() {
        return categories;
    }

    public void setCategories(ArrayList<Category> categories) {
        this.categories = categories;
    }

    public void addCategory(String name) {
        Category c = new Category(name);
        categories.add(c);
        updateVboxes();
        kategoriLabels.add(c.getName());
        skalStatsGenberegnes.set(true);
    }

    public void addDreamVbox(VBox vbox) {
        dreamVboxes.add(vbox);
    }

    public void addFilterVbox(VBox vbox) {
        filterVboxes.add(vbox);
    }

    public void updateVboxes() {
        for (VBox vbox : dreamVboxes) {
            CheckComboBox<String> ccb = newCCBFor(categories.getLast());
            vbox.getChildren().add(ccb);
            categories.getLast().addDreamCCB(ccb);
        }
        for (VBox vbox : filterVboxes) {
            CheckComboBox<String> ccb = newCCBFor(categories.getLast());
            vbox.getChildren().add(ccb);
            categories.getLast().addFilterCCB(ccb);
        }
    }

    private CheckComboBox<String> newCCBFor(Category category) {
        CheckComboBox<String> ccb = new CheckComboBox<>();
        ccb.getItems().addAll(category.getSymbols());
        ccb.setMaxWidth(Double.MAX_VALUE);
        ccb.setMinWidth(280);
        ccb.setTitle(category.getName());
        ccb.setShowCheckedCount(true);
        return ccb;
    }

    public void setDreamEdited(String dreamId) {
        dreamEdited.set(dreamId);
    }

    public String getDreamEdited() {
        return dreamEdited.get();
    }

    private void addDefaultCategories() {
        Category ark = new Category("Arketyper");
        ark.addSymbols(List.of("skyggen", "anima", "animus", "visdom", "jeget"));
        categories.add(ark);

        Category cha = new Category("Chakraer");
        cha.addSymbols(List.of("krone", "pineal", "hals", "hjerte", "solar plexus", "hara", "rod"));
        categories.add(cha);

        Category dy = new Category("Dyr");
        dy.addSymbols(List.of("abe", "anakonda", "and", "bjørn", "blæksprutte", "bæver", "delfin", "drage", "dinosaur", "dådyr", "egern", "elefant", "elg", "falk", "firben", "flagermus", "flamingo", "flodhest", "frø", "fugl", "gepard", "giraf", "gnaver", "gorilla", "gris", "grævling", "haj", "hare", "hest", "hjort", "hugorm", "humlebi", "hund", "hveps", "høg", "høne", "insekt", "isbjørn", "kamel", "kamæleon", "kanin", "kat", "komodovaran", "kænguru", "lam", "lama", "leopard", "løve", "muldvarp", "mus", "myre", "næsehorn", "okse", "orangutang", "orm", "panda", "puma", "rovdyr", "rovfugl", "rådyr", "ræv", "skildpadde", "skorpion", "skrubtudse", "slange", "snog", "sommerfugl", "sæl", "søhest", "tiger", "ugle", "ulv", "vildsvin", "vædder", "zebra", "yak", "æsel"));
        categories.add(dy);

        Category far = new Category("Farver");
        far.addSymbols(List.of("blå","brun","dueblå","grå","græsgrøn","grøn","gul","guld","himmelblå","hvid","koksgrå","laksefarvet","lavendel","lilla","limegrøn","lyseblå","lysebrun","lysegrå","lysegrøn","lyselilla","lyserød","mørkeblå","mørkebrun","mørkegrå","mørkelilla","mørkerød","olivengrøn","orange","pink","rosa","rød","sort","turkis","violet"));
        categories.add(far);

        Category forl = new Category("Forløb");
        forl.addSymbols(List.of("begyndelse", "det ukendte","tilnærmelse","møde","enhed","opdeling","adskillelse","det nye ukendte","afslutning"));
        categories.add(forl);

        Category pers = new Category("Personer");
        pers.addSymbols(List.of());
        categories.add(pers);

        Category kva = new Category(Category.FLAGS_CATEGORY_NAME);
        kva.setSymbols(new TreeSet<>(Category.FLAGS_SYMBOLS_IN_ORDER));
        kva.setCustomOrder(new ArrayList<>(Category.FLAGS_SYMBOLS_IN_ORDER));
        categories.add(kva);

        for (Category cat : getUiCategories()) {
            kategoriLabels.add(cat.getName());
        }
    }

    public LocalDate getStartFromThisDate() {
        return startFromThisDate;
    }

    public void setStartFromThisDate(LocalDate startFromThisDate) {
        this.startFromThisDate = startFromThisDate;
    }
}
