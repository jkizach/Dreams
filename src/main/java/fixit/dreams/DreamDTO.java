package fixit.dreams;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDate;

public class DreamDTO {
    private String id;
    private StringProperty indhold;
    private LocalDate dato;
    private StringProperty dagrest;

    public DreamDTO(String id, String indhold, String dagrest, LocalDate dato) {
        this.id = id;
        this.indhold = new SimpleStringProperty(indhold);
        this.dagrest = new SimpleStringProperty(dagrest);
        this.dato = dato;
    }

    public String getId() {
        return id;
    }

    public String getDagrest() {
        return dagrest.get();
    }

    public String getIndhold() {
        return indhold.get();
    }

    public void setIndhold(String newIndhold) {
        this.indhold.set(newIndhold);
    }

    public void setDagrest(String dagrest) {
        this.dagrest.set(dagrest);
    }

    public LocalDate getDato() {
        return dato;
    }

    public void setDato(LocalDate dato) {
        this.dato = dato;
    }

    public StringProperty indholdProperty() {
        return indhold;
    }

    public String getVisbartIndhold() {
        String dg = (!dagrest.get().isEmpty()) ? "\nDAGREST: " + dagrest.get() + "\n": "\n";
        return ("-------------------------------\n" + dato + "\n" + indhold.get() + dg);
    }
}

