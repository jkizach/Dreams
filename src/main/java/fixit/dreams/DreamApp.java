package fixit.dreams;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class DreamApp extends Application {
    private static Scene scene;


    @Override
    public void start(Stage stage) throws IOException {
        Image icon = new Image(getClass().getResourceAsStream("/moona.png"));
        //Locale.setDefault(Locale.forLanguageTag("DK"));
        FXMLLoader fxmlLoader = new FXMLLoader(DreamApp.class.getResource("hovedmenu.fxml"));
        stage.setOnCloseRequest(event -> handleWindowClose());
        scene = new Scene(fxmlLoader.load(), 1000, 800);
        //scene.getStylesheets().add(getClass().getResource("currentTema.css").toExternalForm());
        CSSUpdater.init();

        Path cssPath = Paths.get(System.getProperty("user.home"), "Documents", "DrømmeappenData", "currentTema.css");
        File cssFile = cssPath.toFile();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(cssFile.toURI().toString()); // Indlæs direkte fra resources
        //scene.getStylesheets().applyCss();

        stage.setTitle("Drømmeappen 1.3.1");
        stage.setMinWidth(650);
        stage.setMinHeight(550);
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