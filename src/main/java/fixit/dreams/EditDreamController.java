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

import java.util.ArrayList;
import java.util.TreeSet;

public class EditDreamController {
    private Dream dream;

    private User user;

    private ArrayList<CheckComboBox<String>> editCCBs = new ArrayList<>();

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
            editCCBs.add(ccb);
        }
    }

    private void displayDream() {
        for (CategoryDTO cdto : dream.getCategories()) {
            for (CheckComboBox<String> ccb : editCCBs) {
                if (cdto.name.equals(ccb.getTitle())) {
                    for (String symbol : cdto.symbols) {
                        ccb.getCheckModel().check(symbol);
                    }
                    break;
                }
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
        for (CheckComboBox<String> ccb : editCCBs) {

            TreeSet<String> newsymbols = new TreeSet<String>(ccb.getCheckModel().getCheckedItems());
            // Her skal jeg tjekke om ccb.getTitle() overhovedet er i dream.getCategories()!! Ellers skal den tilføjes...
            boolean found = dream.getCategories().stream()
                    .anyMatch(category -> category.name.equals(ccb.getTitle()));

            if (!found) {
                CategoryDTO theNewOnenew = new CategoryDTO();
                theNewOnenew.name = ccb.getTitle();
                dream.addCategoryDTO(theNewOnenew);
            }

            for (CategoryDTO cdto : dream.getCategories()) {
                if (cdto.name.equals(ccb.getTitle())) {
                    cdto.symbols = newsymbols;
                }
            }
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
