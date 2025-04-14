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
    private TreeSet<String> arketyper;
    private TreeSet<String> dyr;
    private TreeSet<String> farver;
    private TreeSet<String> personer;
    private TreeSet<String> chakraer;
    private TreeSet<String> forloeb;
    private TreeSet<String> brugerDefineretA;
    private TreeSet<String> brugerDefineretB;
    private TreeSet<String> brugerDefineretC;

    private HashMap<String,String> BrugersNavneTilMine;

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
        if (loadedTemaer != null) {
            System.out.println("Temaer er loaded!");
        }

        if (loadedCategories != null) {
            System.out.println("Categories loaded!");
            this.categories = loadedCategories;
        } else {
            System.out.println("Categories not loaded!");
            // manuel tilføjelse af alle cats til cats...??
        }

        if (loadedUserDTO != null) {
            System.out.println("Load userDTO virkede...");
            this.arketyper = new TreeSet<>(loadedUserDTO.arketyper);
            this.dyr = new TreeSet<>(loadedUserDTO.dyr);
            this.farver = new TreeSet<>(loadedUserDTO.farver);
            this.personer = new TreeSet<>(loadedUserDTO.personer);
            this.chakraer = new TreeSet<>(loadedUserDTO.chakraer);
            this.forloeb = new TreeSet<>(loadedUserDTO.forloeb);
            this.brugerDefineretA = new TreeSet<>(loadedUserDTO.brugerDefineretA);
            this.brugerDefineretB = new TreeSet<>(loadedUserDTO.brugerDefineretB);
            this.brugerDefineretC = new TreeSet<>(loadedUserDTO.brugerDefineretC);
            this.BrugersNavneTilMine = new HashMap<>(loadedUserDTO.BrugersNavneTilMine);
            this.dreams = new HashMap<>(loadedUserDTO.dreams);
            this.kategoriLabels = FXCollections.observableArrayList(loadedUserDTO.kategoriLabels);
            this.foretrukneTema = loadedUserDTO.foretrukneTema;
            this.temaer = loadedTemaer;
            this.visAdvarsel = loadedUserDTO.visAdvarsel;
            this.visKollektiv = loadedUserDTO.visKollektiv;

        } else {
            System.out.println("Load user virkede ikke...");
            categories = new ArrayList<>();
            arketyper = new TreeSet<>();
            dyr = new TreeSet<>();
            farver = new TreeSet<>();
            personer = new TreeSet<>();
            forloeb = new TreeSet<>();
            chakraer = new TreeSet<>();
            brugerDefineretA = new TreeSet<>();
            brugerDefineretB = new TreeSet<>();
            brugerDefineretC = new TreeSet<>();
            arketyper.addAll(List.of("skyggen", "anima|us", "visdom"));
            dyr.addAll(List.of("abe", "gorilla", "slange"));
            farver.addAll(List.of("grøn", "blå", "gul"));
            personer.addAll(List.of("Jes", "Anna", "Petra"));
            chakraer.addAll(List.of("krone", "pineal", "hals", "hjerte", "solar plexus", "hara", "rod"));
            forloeb.addAll(List.of("begyndelse", "slutning"));
            dreams = new HashMap<>();
            temaer = new HashMap<>();
            BrugersNavneTilMine = new HashMap<>();

            Category ark = new Category("Arketyper");
            ark.setSymbols(arketyper);
            categories.add(ark);

            Category dy = new Category("Dyr");
            dy.setSymbols(dyr);
            categories.add(dy);

            Category far = new Category("Farver");
            far.setSymbols(farver);
            categories.add(far);

            Category pers = new Category("Personer");
            pers.setSymbols(personer);
            categories.add(pers);

            Category cha = new Category("Chakraer");
            cha.setSymbols(chakraer);
            categories.add(cha);

            Category forl = new Category("Forløb");
            forl.setSymbols(forloeb);
            categories.add(forl);

            testCats();

            kategoriLabels.add("Arketyper");
            kategoriLabels.add("Chakraer");
            kategoriLabels.add("Dyr");
            kategoriLabels.add("Farver");
            kategoriLabels.add("Forløb");
            kategoriLabels.add("Personer");

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

    public void addBrugerdefineredeKategoriNavne(){
        if (!BrugersNavneTilMine.isEmpty()) {
            kategoriLabels.addAll(BrugersNavneTilMine.keySet());
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

    public TreeSet<String> getArketyper() {
        return arketyper;
    }

    public TreeSet<String> getDyr() {
        return dyr;
    }

    public TreeSet<String> getFarver() {
        return farver;
    }

    public TreeSet<String> getPersoner() {
        return personer;
    }

    public TreeSet<String> getChakraer() {
        return chakraer;
    }

    public TreeSet<String> getForloeb() {
        return forloeb;
    }

    public TreeSet<String> getBrugerDefineretA() {
        return brugerDefineretA;
    }

    public TreeSet<String> getBrugerDefineretB() {
        return brugerDefineretB;
    }

    public TreeSet<String> getBrugerDefineretC() {
        return brugerDefineretC;
    }

    public HashMap<String, String> getBrugersNavneTilMine() {
        return BrugersNavneTilMine;
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
    public void addArketype(String nyArketype) {
        arketyper.add(nyArketype);
    }
    public void addDyr(String nytDyr) {
        dyr.add(nytDyr);
    }
    public void addFarve(String nyFarve) {
        farver.add(nyFarve);
    }
    public void addPerson(String nyPerson) {
        personer.add(nyPerson);
    }
    public void addChakra(String nytChakra) {
        chakraer.add(nytChakra);
    }
    public void addForloeb(String nytForloeb) {
        forloeb.add(nytForloeb);
    }
    public void addBrugerDefineretA(String nyA) {
        brugerDefineretA.add(nyA);
    }
    public void addBrugerDefineretB(String nyB) {
        brugerDefineretB.add(nyB);
    }
    public void addBrugerDefineretC(String nyC) {
        brugerDefineretC.add(nyC);
    }
    public void addNyBrugerdefineretKategori(String nytNavn) {
        int kategoriNummer = BrugersNavneTilMine.size();
        switch (kategoriNummer) {
            case 0:
                BrugersNavneTilMine.put(nytNavn, "brugerDefineretA");
                break;
            case 1:
                BrugersNavneTilMine.put(nytNavn, "brugerDefineretB");
                break;
            case 2:
                BrugersNavneTilMine.put(nytNavn, "brugerDefineretC");
                break;
            case 3:
                break;
        }
        kategoriLabels.add(nytNavn);
    }

    public String getEtBrugerNavnTilMit(String brugersNavn) {
        return BrugersNavneTilMine.get(brugersNavn);
    }

    public Boolean okToAddNewCategory() {
        return BrugersNavneTilMine.size() < 3;
    }

    public void removeArketype(String symbol) {
        arketyper.remove(symbol);
    }
    public void removeDyr(String symbol) {
        dyr.remove(symbol);
    }
    public void removeFarve(String symbol) {
        farver.remove(symbol);
    }
    public void removePerson(String symbol) {
        personer.remove(symbol);
    }
    public void removeChakra(String symbol) {
        chakraer.remove(symbol);
    }
    public void removeForloeb(String symbol) {
        forloeb.remove(symbol);
    }
    public void removeBrugerDefineretA(String symbol) {
        brugerDefineretA.remove(symbol);
    }
    public void removeBrugerDefineretB(String symbol) {
        brugerDefineretB.remove(symbol);
    }
    public void removeBrugerDefineretC(String symbol) {
        brugerDefineretC.remove(symbol);
    }

    public void setArketyper(TreeSet<String> arketyper) {
        this.arketyper = arketyper;
    }

    public void setDyr(TreeSet<String> dyr) {
        this.dyr = dyr;
    }

    public void setFarver(TreeSet<String> farver) {
        this.farver = farver;
    }

    public void setPersoner(TreeSet<String> personer) {
        this.personer = personer;
    }

    public void setChakraer(TreeSet<String> chakraer) {
        this.chakraer = chakraer;
    }

    public void setForloeb(TreeSet<String> forloeb) {
        this.forloeb = forloeb;
    }

    public void setBrugerDefineretA(TreeSet<String> brugerDefineretA) {
        this.brugerDefineretA = brugerDefineretA;
    }

    public void setBrugerDefineretB(TreeSet<String> brugerDefineretB) {
        this.brugerDefineretB = brugerDefineretB;
    }

    public void setBrugerDefineretC(TreeSet<String> brugerDefineretC) {
        this.brugerDefineretC = brugerDefineretC;
    }

    public void setBrugersNavneTilMine(HashMap<String, String> brugersNavneTilMine) {
        BrugersNavneTilMine = brugersNavneTilMine;
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
    }
}
