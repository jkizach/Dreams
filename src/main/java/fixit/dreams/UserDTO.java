package fixit.dreams;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class UserDTO {
    public TreeSet<String> arketyper;
    public TreeSet<String> dyr;
    public TreeSet<String> farver;
    public TreeSet<String> personer;
    public TreeSet<String> chakraer;
    public TreeSet<String> forloeb;
    public TreeSet<String> brugerDefineretA;
    public TreeSet<String> brugerDefineretB;
    public TreeSet<String> brugerDefineretC;

    public HashMap<String,String> BrugersNavneTilMine;

    public HashMap<String,Dream> dreams;

    public ArrayList<String> kategoriLabels;
    public String foretrukneTema;

    public boolean visAdvarsel;
    public boolean visKollektiv;


    public UserDTO() {}

    public UserDTO(User user) {
        this.arketyper = new TreeSet<>(user.getArketyper());
        this.dyr = new TreeSet<>(user.getDyr());
        this.farver = new TreeSet<>(user.getFarver());
        this.personer = new TreeSet<>(user.getPersoner());
        this.chakraer = new TreeSet<>(user.getChakraer());
        this.forloeb = new TreeSet<>(user.getForloeb());
        this.brugerDefineretA = new TreeSet<>(user.getBrugerDefineretA());
        this.brugerDefineretB = new TreeSet<>(user.getBrugerDefineretB());
        this.brugerDefineretC = new TreeSet<>(user.getBrugerDefineretC());
        this.BrugersNavneTilMine = new HashMap<>(user.getBrugersNavneTilMine());
        this.dreams = new HashMap<>(user.getDreams());
        this.kategoriLabels = new ArrayList<>(user.getKategoriLabels());
        this.foretrukneTema = user.getForetrukneTemaNavn();
        this.visAdvarsel = user.isVisAdvarsel();
        this.visKollektiv = user.isVisKollektiv();

    }

    public User toUser() {
        User user = User.getInstance();
        user.setArketyper(this.arketyper);
        user.setDyr(this.dyr);
        user.setFarver(this.farver);
        user.setPersoner(this.personer);
        user.setChakraer(this.chakraer);
        user.setForloeb(this.forloeb);
        user.setBrugerDefineretA(this.brugerDefineretA);
        user.setBrugerDefineretB(this.brugerDefineretB);
        user.setBrugerDefineretC(this.brugerDefineretC);
        user.setBrugersNavneTilMine(this.BrugersNavneTilMine);
        user.setDreams(this.dreams);
        user.setKategoriLabels(this.kategoriLabels);
        user.setForetrukneTema(this.foretrukneTema);
        user.setVisAdvarsel(this.visAdvarsel);
        user.setVisKollektiv(this.visKollektiv);
        return user;
    }

}
