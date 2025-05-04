package fixit.dreams;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class TxtController {
    private String type;

    public void setType(String type) {
        this.type = type;
        String text = IOutils.loadOmHelpTxt(type);
        txtviewTextArea.setText(text);
    }

    @FXML
    private TextArea txtviewTextArea;

    @FXML
    private void closePopup(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close(); // Lukker popup-vinduet
    }
}
