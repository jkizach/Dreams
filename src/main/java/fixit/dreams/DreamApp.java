package fixit.dreams;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class DreamApp extends Application {
    private static Scene scene;


    @Override
    public void start(Stage stage) throws IOException {
        Image icon = new Image(getClass().getResourceAsStream("/moona.png"));
        FXMLLoader fxmlLoader = new FXMLLoader(DreamApp.class.getResource("hovedmenu.fxml"));
        stage.setOnCloseRequest(event -> handleWindowClose());
        scene = new Scene(fxmlLoader.load(), 1000, 800);
        scene.getStylesheets().add(getClass().getResource("currentTema.css").toExternalForm());
        stage.setTitle("Drømmeappen 0.9");
        stage.setScene(scene);
        stage.getIcons().add(icon);
        stage.show();
    }

    private void handleWindowClose() {
        User tempo = User.getInstance();
        CSSUpdater.updateCSSVariables(tempo.getForetrukneTema().getTemaForCSSUpdater(),false);
        IOutils.saveUser(tempo);
        IOutils.saveTemaer(tempo.getTemaer());
        IOutils.saveCategories(tempo.getCategories());
        IOutils.saveDreams(tempo.getDreams());
    }

    public static Scene getCurrentScene() {
        return scene;
    }

    public static void main(String[] args) {
        launch();
    }
}