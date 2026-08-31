package fixit.dreams;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.control.CheckComboBox;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

public class EditDreamController {
    private Dream dream;

    private User user;

    // Nøglet på kategoriens id, ikke dens titel. CheckComboBoxens titel er det navn brugeren
    // ser, og det navn kan ændre sig - id'et kan ikke.
    private final LinkedHashMap<String, CheckComboBox<String>> editCCBs = new LinkedHashMap<>();

    public void setDream(Dream dream) {
        this.dream = dream;
        displayDream();
    }

    @FXML
    private TextArea editDagrest, editSkrivefelt, editTolkning;

    @FXML
    private DatePicker dpEditDream;

    @FXML
    private VBox vboxEditDream = new VBox();

    @FXML
    private CheckBox lucidEdit, praktisererEdit, modsatEdit, arketypiskEdit, praksisEdit, mareridtEdit, kollektivEdit, advarselEdit, holografiskEdit;

    @FXML
    public void initialize() {
        user = User.getInstance();
        loadCCBs();

        // De tre valgfri cber:
        kollektivEdit.setVisible(user.isVisKollektiv());
        kollektivEdit.setManaged(user.isVisKollektiv());
        advarselEdit.setVisible(user.isVisAdvarsel());
        advarselEdit.setManaged(user.isVisAdvarsel());
        holografiskEdit.setVisible(user.isVisHolografisk());
        holografiskEdit.setManaged(user.isVisHolografisk());
    }

    private void loadCCBs() {
        for (Category c : user.getUiCategories()) {
            CheckComboBox<String> ccb = new CheckComboBox<>();
            ccb.getItems().addAll(c.getSymbolsForDisplay());
            vboxEditDream.getChildren().add(ccb);
            ccb.setMaxWidth(280);
            ccb.setMinWidth(280);
            ccb.setTitle(c.getName());
            ccb.setShowCheckedCount(true);
            editCCBs.put(c.getId(), ccb);
        }
    }

    private void displayDream() {
        for (CategoryDTO cdto : dream.getCategories()) {
            CheckComboBox<String> ccb = editCCBs.get(cdto.id);
            if (ccb == null || cdto.symbols == null) {
                continue; // fx "Kvaliteter", der ikke har en CheckComboBox i UI'et
            }
            for (String symbol : cdto.symbols) {
                ccb.getCheckModel().check(symbol);
            }
        }
        lucidEdit.setSelected(dream.hasFlag("Lucid"));
        praktisererEdit.setSelected(dream.hasFlag("Praktiserer"));
        modsatEdit.setSelected(dream.hasFlag("Modsatkønnet"));
        arketypiskEdit.setSelected(dream.hasFlag("Arketypisk"));
        mareridtEdit.setSelected(dream.hasFlag("Mareridt"));
        kollektivEdit.setSelected(dream.hasFlag("Kollektiv"));
        advarselEdit.setSelected(dream.hasFlag("Advarsel"));
        holografiskEdit.setSelected(dream.hasFlag("Holografisk"));
        praksisEdit.setSelected(dream.hasFlag("Om praksis"));
        editDagrest.setText(dream.getDagrest());
        editTolkning.setText(dream.getTolkning());
        editSkrivefelt.setText(dream.getIndhold());
        dpEditDream.setValue(dream.getDato());
    }

    @FXML
    private void saveDream(ActionEvent event) {
        if (dpEditDream.getValue() == null) {
            return;
        }
        // Gemmer valgene fra checkcomboboxene
        for (Map.Entry<String, CheckComboBox<String>> post : editCCBs.entrySet()) {
            String kategoriId = post.getKey();
            TreeSet<String> nyeSymboler = new TreeSet<>(post.getValue().getCheckModel().getCheckedItems());

            CategoryDTO cdto = dream.getCategories().stream()
                    .filter(c -> kategoriId.equals(c.id))
                    .findFirst()
                    .orElse(null);

            if (cdto == null) {
                cdto = new CategoryDTO();
                cdto.id = kategoriId;
                dream.addCategoryDTO(cdto);
            }
            cdto.symbols = nyeSymboler;
        }


        dream.setDagrest(editDagrest.getText());
        dream.setTolkning(editTolkning.getText());
        dream.setIndhold(editSkrivefelt.getText());
        dream.setDato(dpEditDream.getValue());

        dream.setCategory(Category.buildFlagsCategoryDTO(
                lucidEdit.isSelected(), praktisererEdit.isSelected(), modsatEdit.isSelected(), arketypiskEdit.isSelected(),
                praksisEdit.isSelected(), mareridtEdit.isSelected(), advarselEdit.isSelected(), kollektivEdit.isSelected(),
                holografiskEdit.isSelected()
        ));
        dream.touch();

        user.addDream(dream);

        user.setDreamEdited(dream.getId());
        closePopup(event);
    }

    @FXML
    private void closePopup(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close(); // Lukker popup-vinduet
    }
}
