package fixit.dreams;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.controlsfx.control.CheckComboBox;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

public class EditDreamController {

    // Åbner redigeringsvinduet for én drøm, og gør det ÉT sted. Der er nu to veje ind -
    // drømmelisten i hovedmenuen og forløbsfanen i analysen - og lå koden i hver sin
    // controller, ville de to kunne komme til at opføre sig forskelligt (andet tema, anden
    // modalitet, andet ikon) uden at nogen opdagede det. Vinduet er modalt og venter, så
    // kalderen ved at redigeringen er ovre når kaldet vender tilbage - det er dér listerne
    // skal bygges om.
    static void aabnRedigering(Dream dream) {
        try {
            FXMLLoader loader = new FXMLLoader(EditDreamController.class.getResource("editDream-view.fxml"));
            Parent root = loader.load();

            EditDreamController controller = loader.getController();
            controller.setDream(dream);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setScene(new Scene(root));

            Path cssPath = AppPaths.APP_DATA_PATH.resolve("currentTema.css");
            root.getStylesheets().clear();
            root.getStylesheets().add(cssPath.toFile().toURI().toString());
            root.applyCss();

            popupStage.setTitle("Rediger drøm");
            popupStage.getIcons().add(new Image(EditDreamController.class.getResourceAsStream("/moona.png")));

            popupStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
    private TextField tfTagEdit;

    @FXML
    private FlowPane tagChipsEdit;

    /** Samme klasse som på Ny drøm, så de to felter ikke kan nå at opføre sig forskelligt. */
    private Tagfelt tagfelt;

    @FXML
    private CheckBox lucidEdit, praktisererEdit, modsatEdit, arketypiskEdit, praksisEdit, mareridtEdit, kollektivEdit, advarselEdit, holografiskEdit;

    @FXML
    public void initialize() {
        user = User.getInstance();
        loadCCBs();
        tagfelt = new Tagfelt(tfTagEdit, tagChipsEdit, user.getTagkategori());

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
            if (Tag.ID.equals(c.getId())) {
                continue; // tags har intet CheckComboBox - de står i tagfeltet under fanen Tags
            }
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
        tagfelt.vis(user.tagsPaaDroem(dream));
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

        // Samme upsert-primitiv som flagene bruger lige nedenfor: tag-DTO'en har altid både id
        // og et symbolsæt, så den kan lægges ind uden at tjekke for null.
        dream.setCategory(tagfelt.somCategoryDTO());

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
