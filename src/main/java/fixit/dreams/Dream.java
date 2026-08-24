package fixit.dreams;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

public class Dream {
    private String id;

    private ArrayList<CategoryDTO> categories;
    private String indhold;
    private String dagrest;
    private String tolkning;

    private LocalDate dato;

    public Dream() {} //jackson

    public Dream(DreamData data) {
        this.id = (data.id != null && !data.id.isBlank()) ? data.id : UUID.randomUUID().toString();
        this.categories = new ArrayList<>(data.categories);
        this.indhold = data.indhold;
        this.dagrest = data.dagrest;
        this.tolkning = (data.tolkning != null)? data.tolkning : "";
        this.dato = data.dato;
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

    public String getTolkning() {
        return tolkning;
    }

    public void setTolkning(String tolkning) {
        this.tolkning = tolkning;
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

    public void setDato(LocalDate dato) {
        this.dato = dato;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<CategoryDTO> getCategories() {
        return categories;
    }

    public void addCategoryDTO(CategoryDTO cdto) {
        categories.add(cdto);
    }

    public boolean hasFlag(String symbol) {
        for (CategoryDTO dto : categories) {
            if (Category.FLAGS_CATEGORY_NAME.equals(dto.name)) {
                return dto.symbols != null && dto.symbols.contains(symbol);
            }
        }
        return false;
    }

    public void setCategory(CategoryDTO dto) {
        categories.removeIf(existing -> existing.name.equals(dto.name));
        categories.add(dto);
    }
}
