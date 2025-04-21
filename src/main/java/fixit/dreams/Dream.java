package fixit.dreams;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.TreeSet;
import java.util.UUID;

public class Dream {
    private String id;

    private ArrayList<CategoryDTO> categories;
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
    }

    public String getId() {
        return id;
    }

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

    public void setDato(LocalDate dato) {
        this.dato = dato;
    }

    public Boolean getKollektiv() {
        return kollektiv;
    }

    public Boolean getAdvarsel() {
        return advarsel;
    }

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
