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

import java.io.File;
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
    protected BooleanProperty skalStatsGenberegnes = new SimpleBooleanProperty(false);
    protected StringProperty dreamEdited = new SimpleStringProperty("tom");

    private ArrayList<VBox> vboxes = new ArrayList<>();

    private User() {
        UserDTO loadedUserDTO = IOutils.loadUser();
        HashMap<String,Tema> loadedTemaer = IOutils.loadTemaer();
        ArrayList<Category> loadedCategories = IOutils.loadCategories();
        HashMap<String, Dream> loadedDreams = IOutils.loadDreams();

        if (loadedTemaer != null) {
            System.out.println("Temaer er loaded!");
        }

        if (loadedCategories != null) {
            System.out.println("Categories loaded!");
            this.categories = loadedCategories;
        } else {
            System.out.println("Categories not loaded!");

        }

        if (loadedDreams != null && !loadedDreams.isEmpty()) {
            System.out.println("Dreams loaded!");
            dreams = loadedDreams;
        }

        if (loadedUserDTO != null) {
            System.out.println("Load userDTO virkede...");
            this.kategoriLabels = FXCollections.observableArrayList();
            this.foretrukneTema = loadedUserDTO.foretrukneTema;
            this.temaer = loadedTemaer;
            this.visAdvarsel = loadedUserDTO.visAdvarsel;
            this.visKollektiv = loadedUserDTO.visKollektiv;
            for (Category cat : categories) {
                kategoriLabels.add(cat.getName());
            }

        } else {
            System.out.println("Load user virkede ikke...");
            this.kategoriLabels = FXCollections.observableArrayList();
            categories = new ArrayList<>();

            dreams = new HashMap<>();
            temaer = new HashMap<>();

            // Manuel tilføjelse af symboler:
            Category ark = new Category("Arketyper");
            ark.addSymbols(List.of("skyggen", "anima", "animus", "visdom"));
            categories.add(ark);

            Category cha = new Category("Chakraer");
            cha.addSymbols(List.of("krone", "pineal", "hals", "hjerte", "solar plexus", "hara", "rod"));
            categories.add(cha);

            Category dy = new Category("Dyr");
            dy.addSymbols(List.of("abe", "aborre", "admiral", "agerhøne", "albatros", "alligator", "allike", "anakonda", "and", "antilope", "bavian", "bison", "bjørn", "blishøne", "blåhals", "blåhval", "blåmejse", "blæksprutte", "bogfinke", "brilleslange", "bæltedyr", "bændelorm", "bæver", "bøffel", "chimpanse", "delfin", "drage", "dingo", "dinosaur", "djævlerokke", "dompap", "dovendyr", "dromedar", "dronte", "dræbersnegl", "duehøg", "dådyr", "ederfugl", "egern", "elefant", "elg", "ellekrage", "falk", "fasan", "firben", "fiskehejre", "fiskeørn", "fjeldræv", "flagermus", "flamingo", "flodhest", "flåt", "frø", "fugl", "fugleedderkop", "fuglekonge", "fårekylling", "gazelle", "gedde", "gekko", "gepard", "gibbon", "giraf", "glente", "gnaver", "gnu", "gorilla", "gransanger", "grib", "gris", "gråand", "gråegern", "gråspurv", "græshoppe", "grævling", "grønirisk", "gulirisk", "gulspurv", "gås", "gærdesanger", "gærdesmutte", "gøg", "haj", "halemajse", "hammerhaj", "hare", "havodder", "havskildpadde", "havørn", "hejre", "hest", "hingst", "hjort", "hornugle", "hugorm", "humlebi", "hund", "husskade", "hvalros", "hveps", "hvepsevåge", "hvidhaj", "hyæne", "hærfugl", "hættemåge", "høg", "høne", "igle", "ilder", "ildsalamander", "insekt", "isbjørn", "isfugl", "islænder", "jagtfalk", "jaguar", "jernspurv", "jordegern", "jærv", "kakadue", "kakerlak", "kalkun", "kamel", "kamæleon", "kanin", "kaskelot", "kat", "kejserpingvin", "kejserørn", "kernebider", "kiwi", "klapperslange", "knopsvane", "knæler", "koalabjørn", "kobra", "kolibri", "komodovaran", "kondor", "kongeørn", "koraller", "kronhjort", "kvækerfinke", "kvælerslange", "kænguru", "laks", "lam", "lama", "larve", "leguan", "lemming", "lemur", "leopard", "los", "lunde", "lækat", "lærke", "løve", "marabustork", "mariehøne", "markmus", "marsvin", "mink", "mosegris", "moskusokse", "muldvarp", "muldyr", "mus", "musvit", "musvåge", "myg", "myre", "myresluger", "måge", "mår", "møl", "natsværmer", "nattergal", "natugle", "næbdyr", "nældesommerfugl", "næseabe", "næsehorn", "næsehornsbille", "okse", "orangutang", "orm", "ozelot", "padderokke", "panda", "papegøje", "papegøjefisk", "pegasus", "pelikan", "perlehøne", "pindsvin", "piratfisk", "plankton", "prærieulv", "puddelhund", "pukkelhval", "puma", "pungrotte", "pytonslange", "rottweiler", "rovdyr", "rovfugl", "rådyr", "ræv", "rødhals", "rødkælk", "rødspætte", "sardin", "sild", "skarv", "skildpadde", "skorpion", "skovmus", "skovsnegl", "skrubtudse", "skøjteløber", "slange", "slørugle", "sneppe", "snog", "solsort", "sommerfugl", "sortspætte", "spurv", "spækhugger", "stenbider", "stenbuk", "stinkdyr", "stork", "struds", "sumpmus", "svale", "svane", "sæl", "søhest", "sølvmåge", "søløve", "søpapegøje", "søpindsvin", "tapir", "terrier", "tiger", "tornskade", "torsk", "trane", "troldand", "tukan", "turteldue", "tusindben", "tæge", "ugle", "ulv", "urokse", "vagtel", "vampyrflaggermus", "vandmand", "vaskebjørn", "vildsvin", "vipstjert", "vårhare", "vædder", "væsel", "zebra", "yak", "ælling", "æsel", "ørkenrotte", "ørred", "ål"));
            categories.add(dy);

            Category far = new Category("Farver");
            far.addSymbols(List.of("blommefarvet","blå","bordeaux","bronze","brun","dueblå","grå","græsgrøn","grøn","gul","guld","himmelblå","hvid","kastanjebrun","khaki","kobber","koksgrå","laksefarvet","lavendel","lilla","limegrøn","lyseblå","lysebrun","lysegrå","lysegrøn","lyselilla","lyserød","marineblå","mørkeblå","mørkebrun","mørkegrå","mørkelilla","mørkerød","nøddebrun","okker","olivengrøn","orange","pink","rav","rosa","rød","sort","turkis","vinrød","violet"));
            categories.add(far);

            Category forl = new Category("Forløb");
            forl.addSymbols(List.of("begyndelse", "det ukendte","tilnærmelse","møde","enhed","opdeling","adskillelse","det nye ukendte","afslutning"));
            categories.add(forl);

            Category pers = new Category("Personer");
            pers.addSymbols(List.of("Jes", "Anna", "Petra"));
            categories.add(pers);

            //testCats();

            for (Category cat : categories) {
                kategoriLabels.add(cat.getName());
            }

            addPredefinedThemes();
        }
    }

    public static synchronized User getInstance()
    {
        if (instans == null)
            instans = new User();

        return instans;
    }

    public void testCats() {
        for (Category c : categories) {
            System.out.println(c.getSymbols().first());
            System.out.println(c.getName());
        }
    }

    private void addPredefinedThemes() {
        Tema darkGreen = new Tema(Color.web("#1a1a1a"),Color.web("#62cd84"),Color.web("#262626"),Color.web("#333333"),
                Color.web("#d1d1d1"),Color.web("#1a1a1a"),Color.web("#62cd84"),Color.web("#62cd84"),"mørkt grønt", "Courier New");
        temaer.put(foretrukneTema, darkGreen);

        Tema darkBlue = new Tema(Color.web("#1a1a1a"),Color.web("#678ae6"),Color.web("#262626"),Color.web("#333333"),
                Color.web("#d1d1d1"),Color.web("#1a1a1a"),Color.web("#678ae6"),Color.web("#0b38af"),"mørkt blåt", "Consolas");
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

    public void addVbox(VBox vbox) {
        vboxes.add(vbox);
    }

    public void updateVboxes() {
        for (VBox vbox : vboxes) {
            CheckComboBox<String> ccb = new CheckComboBox<>();
            ccb.getItems().addAll(categories.getLast().getSymbols());
            vbox.getChildren().add(ccb);
            ccb.setMaxWidth(280);
            ccb.setMinWidth(280);
            ccb.setTitle(categories.getLast().getName());
            ccb.setShowCheckedCount(true);
            categories.getLast().addDreamCCB(ccb);
            categories.getLast().addDreamCCB(ccb);
        }
    }

    public void setDreamEdited(String dreamId) {
        dreamEdited.set(dreamId);
    }

    public String getDreamEdited() {
        return dreamEdited.get();
    }
}
