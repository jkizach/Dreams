package fixit.dreams;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.UUID;

public class Dream {
    private String id;

    private ArrayList<CategoryDTO> categories;
//    private TreeSet<String> arketyper = new TreeSet<>();
//    private TreeSet<String> dyr = new TreeSet<>();
//    private TreeSet<String> farver = new TreeSet<>();
//    private TreeSet<String> personer = new TreeSet<>();
//    private TreeSet<String> forloeb = new TreeSet<>();
//    private TreeSet<String> chakraer = new TreeSet<>();
//    private TreeSet<String> brugerDefineretA = new TreeSet<>();
//    private TreeSet<String> brugerDefineretB = new TreeSet<>();
//    private TreeSet<String> brugerDefineretC = new TreeSet<>();

    private String indhold;
    private String dagrest;

    private LocalDate dato;

    private Boolean lucid = false;
    private Boolean praktiserer = false;
    private Boolean modsat = false;
    private Boolean arketypisk = false;
    private Boolean ompraksis = false;
    private Boolean mareridt = false;
    private Boolean kollektiv = false;
    private Boolean advarsel = false;

    public Dream() {} //jackson

    public Dream(DreamData data) {
        this.id = UUID.randomUUID().toString();
        this.categories = new ArrayList<>(data.categories);
        this.indhold = data.indhold;
        this.dagrest = data.dagrest;
        this.dato = data.dato;
        this.lucid = data.lucid;
        this.praktiserer = data.praktiserer;
        this.modsat = data.modsat;
        this.arketypisk = data.arketypisk;
        this.ompraksis = data.ompraksis;
        this.mareridt = data.mareridt;
        this.kollektiv = data.kollektiv;
        this.advarsel = data.advarsel;
//        this.arketyper.addAll(data.arketyper);
//        this.dyr.addAll(data.dyr);
//        this.farver.addAll(data.farver);
//        this.personer.addAll(data.personer);
//        this.chakraer.addAll(data.chakraer);
//        this.forloeb.addAll(data.forloeb);
//        this.brugerDefineretA.addAll(data.brugerDefineretA);
//        this.brugerDefineretB.addAll(data.brugerDefineretB);
//        this.brugerDefineretC.addAll(data.brugerDefineretC);
    }

    public String getId() {
        return id;
    }

//    public TreeSet<String> getArketyper() {
//        return arketyper;
//    }
//
//    public TreeSet<String> getDyr() {
//        return dyr;
//    }
//
//    public TreeSet<String> getFarver() {
//        return farver;
//    }
//
//    public TreeSet<String> getPersoner() {
//        return personer;
//    }
//
//    public TreeSet<String> getBrugerDefineretA() {
//        return brugerDefineretA;
//    }
//
//    public TreeSet<String> getBrugerDefineretB() {
//        return brugerDefineretB;
//    }
//
//    public TreeSet<String> getBrugerDefineretC() {
//        return brugerDefineretC;
//    }
//
//    public TreeSet<String> getChakraer() {
//        return chakraer;
//    }

    public String getIndhold() {
        return indhold;
    }

    public String getDagrest() {
        return dagrest;
    }

    public Boolean getLucid() {
        return lucid;
    }

    public Boolean getPraktiserer() {
        return praktiserer;
    }

    public Boolean getModsat() {
        return modsat;
    }

    public Boolean getArketypisk() {
        return arketypisk;
    }

    public Boolean getOmpraksis() {
        return ompraksis;
    }

    public Boolean getMareridt() {
        return mareridt;
    }

    public LocalDate getDato() {
        return dato;
    }

    public void setIndhold(String indhold) {
        this.indhold = indhold;
    }

    public void setDagrest(String dagrest) {
        this.dagrest = dagrest;
    }

    public void setLucid(Boolean lucid) {
        this.lucid = lucid;
    }

    public void setPraktiserer(Boolean praktiserer) {
        this.praktiserer = praktiserer;
    }

    public void setModsat(Boolean modsat) {
        this.modsat = modsat;
    }

    public void setArketypisk(Boolean arketypisk) {
        this.arketypisk = arketypisk;
    }

    public void setOmpraksis(Boolean ompraksis) {
        this.ompraksis = ompraksis;
    }

    public void setMareridt(Boolean mareridt) {
        this.mareridt = mareridt;
    }

//    public void setArketyper(TreeSet<String> arketyper) {
//        this.arketyper = arketyper;
//    }
//
//    public void setDyr(TreeSet<String> dyr) {
//        this.dyr = dyr;
//    }
//
//    public void setFarver(TreeSet<String> farver) {
//        this.farver = farver;
//    }
//
//    public void setPersoner(TreeSet<String> personer) {
//        this.personer = personer;
//    }
//
//    public void setChakraer(TreeSet<String> chakraer) {
//        this.chakraer = chakraer;
//    }
//
//    public void setBrugerDefineretA(TreeSet<String> brugerDefineretA) {
//        this.brugerDefineretA = brugerDefineretA;
//    }
//
//    public void setBrugerDefineretB(TreeSet<String> brugerDefineretB) {
//        this.brugerDefineretB = brugerDefineretB;
//    }
//
//    public void setBrugerDefineretC(TreeSet<String> brugerDefineretC) {
//        this.brugerDefineretC = brugerDefineretC;
//    }

    public void setDato(LocalDate dato) {
        this.dato = dato;
    }

//    public TreeSet<String> getForloeb() {
//        return forloeb;
//    }

    public Boolean getKollektiv() {
        return kollektiv;
    }

    public Boolean getAdvarsel() {
        return advarsel;
    }

//    public void setForloeb(TreeSet<String> forloeb) {
//        this.forloeb = forloeb;
//    }

    public void setKollektiv(Boolean kollektiv) {
        this.kollektiv = kollektiv;
    }

    public void setAdvarsel(Boolean advarsel) {
        this.advarsel = advarsel;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<CategoryDTO> getCategories() {
        return categories;
    }
}
