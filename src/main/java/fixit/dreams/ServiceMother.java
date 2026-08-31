package fixit.dreams;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ServiceMother {
    protected User user;

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


    public ObservableList<String> getKategorier() {
        return kategorier;
    }

    protected void refreshKategoriLists() {

    }

    public ArrayList<Category> getCats() {
        return user.getUiCategories();
    }

    /** Brugerfladen vælger på navne, alt herunder arbejder på id'er. Se User.idForKategoriNavn. */
    public String idForKategoriNavn(String navn) {
        return user.idForKategoriNavn(navn);
    }

    protected boolean isInRange(LocalDate date, LocalDate start, LocalDate end) {
        return (date.isEqual(start) || date.isAfter(start)) && (date.isEqual(end) || date.isBefore(end));
    }

}

