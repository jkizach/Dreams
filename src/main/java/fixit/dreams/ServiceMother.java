package fixit.dreams;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.ArrayList;
import java.util.List;

public class ServiceMother {
    protected User user;
    protected ObservableList<String> arketyper = FXCollections.observableArrayList();
    protected ObservableList<String> chakraer = FXCollections.observableArrayList();
    protected ObservableList<String> forloeb = FXCollections.observableArrayList();
    protected ObservableList<String> dyr = FXCollections.observableArrayList();
    protected ObservableList<String> farver = FXCollections.observableArrayList();
    protected ObservableList<String> personer = FXCollections.observableArrayList();
    protected ObservableList<String> brugerDefineretA = FXCollections.observableArrayList();
    protected ObservableList<String> brugerDefineretB = FXCollections.observableArrayList();
    protected ObservableList<String> brugerDefineretC = FXCollections.observableArrayList();
    protected ObservableList<String> kategorier = FXCollections.observableArrayList();
    protected ObservableList<String> temaer = FXCollections.observableArrayList();

    public ServiceMother(User user) {
        this.user = user;
        // hent alle kategorierne med en funktion
        refreshKategoriLists();
        // kategorierne:
        kategorier.setAll(user.getKategoriLabels());
    }

    public void populateTemaer() {
        temaer.addAll(user.getTemaer().keySet());
    }

    public ObservableList<String> getArketyperForDisplay() {
        return arketyper;
    }
    public ObservableList<String> getChakraerForDisplay() {
        return chakraer;
    }
    public ObservableList<String> getDyrForDisplay() {
        return dyr;
    }
    public ObservableList<String> getFarverForDisplay() {
        return farver;
    }
    public ObservableList<String> getForloebForDisplay() {
        return forloeb;
    }
    public ObservableList<String> getPersonerForDisplay() {
        return personer;
    }
    public ObservableList<String> getBrugerDefineretAForDisplay() {
        return brugerDefineretA;
    }
    public ObservableList<String> getBrugerDefineretBForDisplay() {
        return brugerDefineretB;
    }
    public ObservableList<String> getBrugerDefineretCForDisplay() {
        return brugerDefineretC;
    }
    public ObservableList<String> getKategorier() {
        return kategorier;
    }

    protected void refreshKategoriLists() {
        arketyper.clear();
        chakraer.clear();
        dyr.clear();
        farver.clear();
        forloeb.clear();
        personer.clear();
        brugerDefineretA.clear();
        brugerDefineretB.clear();
        brugerDefineretC.clear();
        arketyper.addAll(getArketyper());
        chakraer.addAll(getChakraer());
        dyr.addAll(getDyr());
        farver.addAll(getFarver());
        personer.addAll(getPersoner());
        forloeb.addAll(getForloeb());
        brugerDefineretA.addAll(getBrugerDefineretA());
        brugerDefineretB.addAll(getBrugerDefineretB());
        brugerDefineretC.addAll(getBrugerDefineretC());
    }

    public List<String> getArketyper() {
        List<String> outList = new ArrayList<>();
        for (String s : user.getArketyper()) {
            outList.add(s);
        }
        return outList;
    }

    public List<String> getDyr() {
        List<String> outList = new ArrayList<>();
        for (String s : user.getDyr()) {
            outList.add(s);
        }
        return outList;
    }

    public List<String> getFarver() {
        List<String> outList = new ArrayList<>();
        for (String s : user.getFarver()) {
            outList.add(s);
        }
        return outList;
    }

    public List<String> getPersoner() {
        List<String> outList = new ArrayList<>();
        for (String s : user.getPersoner()) {
            outList.add(s);
        }
        return outList;
    }

    public List<String> getChakraer() {
        List<String> outList = new ArrayList<>();
        for (String s : user.getChakraer()) {
            outList.add(s);
        }
        return outList;
    }

    public List<String> getForloeb() {
        List<String> outList = new ArrayList<>();
        for (String s : user.getForloeb()) {
            outList.add(s);
        }
        return outList;
    }

    public List<String> getBrugerDefineretA() {
        List<String> outList = new ArrayList<>();
        for (String s : user.getBrugerDefineretA()) {
            outList.add(s);
        }
        return outList;
    }
    public List<String> getBrugerDefineretB() {
        List<String> outList = new ArrayList<>();
        for (String s : user.getBrugerDefineretB()) {
            outList.add(s);
        }
        return outList;
    }
    public List<String> getBrugerDefineretC() {
        List<String> outList = new ArrayList<>();
        for (String s : user.getBrugerDefineretC()) {
            outList.add(s);
        }
        return outList;
    }
    public String getNewCategoryInternalName(String brugersNavn) {
        return user.getEtBrugerNavnTilMit(brugersNavn);
    }

    public ArrayList<Category> getCats() {
        return user.getCategories();
    }
}

