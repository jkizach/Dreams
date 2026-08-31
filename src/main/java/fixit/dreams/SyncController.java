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
                    // Hentede drømme er på plads i den kørende User, men listen i hovedmenuen
                    // bygges først om når dette vindue lukkes (se handleOpenSyncPopup) - derfor
                    // siger beskeden det højt. Kategorier, temaer og indstillinger skrives
                    // derimod kun til disk: de sidder for dybt i brugerfladen til at kunne
                    // skiftes ud midt i en session, og kræver en genstart (se
                    // SyncService.overtagKategorierFraSkyen).
                    statusLbl.setText(user.harHentetMetaFraSkyen()
                            ? "Synkroniseret! Drømmene vises når du lukker vinduet - kategorier og temaer efter en genstart."
                            : "Synkroniseret! Drømmene vises når du lukker vinduet.");
                    refreshView();
                    user.genberegnStatsPlease();
                });
            } catch (SyncVersionException e) {
                // Skal fanges FØR SyncException, som den arver fra. Og den fortjener en dialog
                // frem for en linje i statuslabelen: intet blev synkroniseret, og sådan bliver
                // det ved indtil maskinen er opdateret.
                String message = e.getMessage();
                Platform.runLater(() -> {
                    setButtonsDisabled(false);
                    statusLbl.setText("Synkronisering standset - skyen er nyere end denne udgave.");
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Synkronisering standset");
                    alert.setHeaderText("Skyen er nyere end denne udgave af appen.");
                    alert.setContentText(message);
                    alert.showAndWait();
                    refreshView();
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
        alert.setTitle("Mulige dubletter");
        alert.setHeaderText(e.getDuplicateCount() + " af dine " + e.getLocalCount()
                + " lokale drømme ser ud til allerede at ligge i skyen.");
        alert.setContentText("De har samme dato og indhold som drømme i skyen, men et andet ID - det sker hvis "
                + "to maskiner uafhængigt har fået de samme drømme uden at have synkroniseret sammen før. "
                + "Kører du sammen alligevel, bliver de gemt som dubletter (samme drøm to gange). "
                + "Vil du sammenkøre alligevel?");
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
