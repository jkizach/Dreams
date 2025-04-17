package fixit.dreams;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

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
    private boolean statsSkalGenberegnes = true;

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

        if (loadedDreams != null) {
            System.out.println("Dreams loaded!");
            dreams = loadedDreams;
        }

        if (loadedUserDTO != null) {
            System.out.println("Load userDTO virkede...");
            this.kategoriLabels = FXCollections.observableArrayList(loadedUserDTO.kategoriLabels);
            this.foretrukneTema = loadedUserDTO.foretrukneTema;
            this.temaer = loadedTemaer;
            this.visAdvarsel = loadedUserDTO.visAdvarsel;
            this.visKollektiv = loadedUserDTO.visKollektiv;

        } else {
            System.out.println("Load user virkede ikke...");
            categories = new ArrayList<>();

            dreams = new HashMap<>();
            temaer = new HashMap<>();

            // Manuel tilføjelse af symboler:
            Category ark = new Category("Arketyper");
            ark.addSymbols(List.of("skyggen", "anima|us", "visdom"));
            categories.add(ark);

            Category dy = new Category("Dyr");
            dy.addSymbols(List.of("abe", "gorilla", "slange"));
            categories.add(dy);

            Category far = new Category("Farver");
            far.addSymbols(List.of("grøn", "blå", "gul"));
            categories.add(far);

            Category pers = new Category("Personer");
            pers.addSymbols(List.of("Jes", "Anna", "Petra"));
            categories.add(pers);

            Category cha = new Category("Chakraer");
            cha.addSymbols(List.of("krone", "pineal", "hals", "hjerte", "solar plexus", "hara", "rod"));
            categories.add(cha);

            Category forl = new Category("Forløb");
            forl.addSymbols(List.of("begyndelse", "slutning"));
            categories.add(forl);

            testCats();

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
                Color.web("#d1d1d1"),Color.web("#1a1a1a"),Color.web("#678ae6"),Color.web("#0b38af"),"mørkt blåt", "Lucida Bright");
        temaer.put("mørkt blåt", darkBlue);
    }

    public void addDream(Dream d) {
        dreams.put(d.getId(),d);
        statsSkalGenberegnes = true;
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
        statsSkalGenberegnes = false;
    }

    public boolean skalStatsGenberegnes() {
        return statsSkalGenberegnes;
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
        kategoriLabels.add(c.getName());
    }
}
