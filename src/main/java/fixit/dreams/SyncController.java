package fixit.dreams;

import fixit.dreams.sync.FirebaseAuthException;
import fixit.dreams.sync.SyncDTO;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SyncController {
    private User user;
    private SyncService syncService;

    private static final DateTimeFormatter DATO_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    @FXML
    private VBox loggedOutBox, loggedInBox;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button signUpBtn, signInBtn, syncNowBtn, logoutBtn;

    @FXML
    private CheckBox syncEnabledBox;

    @FXML
    private Label loggedInEmailLbl, lastSyncedLbl, statusLbl;

    @FXML
    public void initialize() {
        user = User.getInstance();
        syncService = new SyncService(user);
        refreshView();
    }

    private void refreshView() {
        SyncDTO status = syncService.getStatus();
        boolean loggedInd = (status != null);

        loggedOutBox.setVisible(!loggedInd);
        loggedOutBox.setManaged(!loggedInd);
        loggedInBox.setVisible(loggedInd);
        loggedInBox.setManaged(loggedInd);

        if (loggedInd) {
            loggedInEmailLbl.setText("Logget ind som: " + status.email);
            syncEnabledBox.setSelected(status.syncEnabled);
            lastSyncedLbl.setText(status.lastSyncedAt != null
                    ? "Sidst synkroniseret: " + DATO_FORMAT.format(status.lastSyncedAt.atZone(ZoneId.systemDefault()))
                    : "Sidst synkroniseret: aldrig");
        }
    }

    @FXML
    private void handleSignUp() {
        runAuthAction(() -> syncService.signUp(emailField.getText(), passwordField.getText()));
    }

    @FXML
    private void handleSignIn() {
        runAuthAction(() -> syncService.signIn(emailField.getText(), passwordField.getText()));
    }

    private interface AuthAction {
        void run() throws FirebaseAuthException;
    }

    // Kører login/opret-konto-kaldet på en baggrundstråd (det er et blokerende netværkskald),
    // og bruger Platform.runLater til at opdatere UI'et bagefter - samme mønster som GITHUBUpdater.
    private void runAuthAction(AuthAction action) {
        String email = emailField.getText();
        String password = passwordField.getText();
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            statusLbl.setText("Udfyld både email og adgangskode.");
            return;
        }
        setButtonsDisabled(true);
        statusLbl.setText("Arbejder...");

        Thread thread = new Thread(() -> {
            String errorMessage = null;
            try {
                action.run();
            } catch (FirebaseAuthException e) {
                errorMessage = e.toDanishMessage();
            }
            String finalError = errorMessage;
            Platform.runLater(() -> {
                setButtonsDisabled(false);
                if (finalError != null) {
                    statusLbl.setText(finalError);
                } else {
                    passwordField.clear();
                    statusLbl.setText("");
                    refreshView();
                }
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleSyncNow() {
        runSync(false);
    }

    // confirmedMerge=true bruges kun når brugeren selv har svaret "ja" på advarslen om
    // data både lokalt og i skyen (se askAboutConflict).
    private void runSync(boolean confirmedMerge) {
        setButtonsDisabled(true);
        statusLbl.setText("Synkroniserer...");

        Thread thread = new Thread(() -> {
            try {
                syncService.syncNow(confirmedMerge);
                Platform.runLater(() -> {
                    setButtonsDisabled(false);
                    statusLbl.setText("Synkroniseret!");
                    refreshView();
                    user.genberegnStatsPlease();
                });
            } catch (SyncConflictException e) {
                Platform.runLater(() -> {
                    setButtonsDisabled(false);
                    statusLbl.setText("");
                    askAboutConflict(e);
                });
            } catch (SyncException e) {
                String message = e.getMessage();
                Platform.runLater(() -> {
                    setButtonsDisabled(false);
                    statusLbl.setText(message);
                    refreshView();
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void askAboutConflict(SyncConflictException e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Data både lokalt og i skyen");
        alert.setHeaderText("Der findes " + e.getLocalCount() + " drømme lokalt og " + e.getCloudCount() + " drømme i skyen.");
        alert.setContentText("Dette kan ske hvis to maskiner uafhængigt har fået de samme drømme, uden at have "
                + "synkroniseret sammen før - i så fald kan en automatisk sammenkøring skabe dubletter (samme "
                + "drøm to gange). Vil du sammenkøre alligevel?");
        ButtonType jaBtn = new ButtonType("Sammenkør alligevel", ButtonBar.ButtonData.OK_DONE);
        ButtonType annullerBtn = new ButtonType("Annuller", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(jaBtn, annullerBtn);

        alert.showAndWait().ifPresent(response -> {
            if (response == jaBtn) {
                runSync(true);
            } else {
                statusLbl.setText("Synkronisering annulleret.");
            }
        });
    }

    @FXML
    private void handleLogout() {
        syncService.logout();
        statusLbl.setText("");
        refreshView();
    }

    @FXML
    private void handleToggleSyncEnabled() {
        syncService.setSyncEnabled(syncEnabledBox.isSelected());
    }

    private void setButtonsDisabled(boolean disabled) {
        signUpBtn.setDisable(disabled);
        signInBtn.setDisable(disabled);
        syncNowBtn.setDisable(disabled);
        logoutBtn.setDisable(disabled);
    }

    @FXML
    private void closePopup(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
