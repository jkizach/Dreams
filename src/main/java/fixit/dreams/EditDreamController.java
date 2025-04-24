package fixit.dreams;

import javafx.beans.property.SimpleStringProperty;
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
    private TextArea editDagrest, editSkrivefelt;

    @FXML
    private DatePicker dpEditDream;

    @FXML
    private VBox vboxEditDream = new VBox();

    @FXML
    private CheckBox lucidEdit, praktisererEdit, modsatEdit, arketypiskEdit, praksisEdit, mareridtEdit, kollektivEdit, advarselEdit;

    @FXML
    public void initialize() {
        System.out.println("Initialized...");
        user = User.getInstance();
        loadCCBs();

        // De to valgfri cber:
        kollektivEdit.setVisible(user.isVisKollektiv());
        advarselEdit.setVisible(user.isVisAdvarsel());


        //displayDream();
    }

    private void loadCCBs() {
        for (Category c : user.getCategories()) {
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
        lucidEdit.setSelected(dream.getLucid());
        praktisererEdit.setSelected(dream.getPraktiserer());
        modsatEdit.setSelected(dream.getModsat());
        arketypiskEdit.setSelected(dream.getArketypisk());
        mareridtEdit.setSelected(dream.getMareridt());
        kollektivEdit.setSelected(dream.getKollektiv());
        advarselEdit.setSelected(dream.getAdvarsel());
        praksisEdit.setSelected(dream.getOmpraksis());

        editDagrest.setText(dream.getDagrest());
        editSkrivefelt.setText(dream.getIndhold());


        dpEditDream.setValue(dream.getDato());
    }

    @FXML
    private void saveDream(ActionEvent event) {
        // Gemmer valgene fra checkcomboboxene
        for (CheckComboBox<String> ccb : editCCBs) {
            if (!ccb.getCheckModel().getCheckedItems().isEmpty()) {
                TreeSet<String> newsymbols = new TreeSet<String>(ccb.getCheckModel().getCheckedItems());

                // Her skal jeg tjekke om ccb.getTitle() overhovedet er i dream.getCategories()!! Ellers skal den tilføjes..
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
                        break;
                    }
                }
            }
        }

        dream.setDagrest(editDagrest.getText());
        dream.setIndhold(editSkrivefelt.getText());

        dream.setLucid(lucidEdit.isSelected());
        dream.setMareridt(mareridtEdit.isSelected());
        dream.setKollektiv(kollektivEdit.isSelected());
        dream.setAdvarsel(advarselEdit.isSelected());
        dream.setOmpraksis(arketypiskEdit.isSelected());
        dream.setPraktiserer(praktisererEdit.isSelected());
        dream.setArketypisk(arketypiskEdit.isSelected());
        dream.setModsat(modsatEdit.isSelected());

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
