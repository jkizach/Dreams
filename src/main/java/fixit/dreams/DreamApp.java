package fixit.dreams;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

public class DreamApp extends Application {
    private static Scene scene;


    @Override
    public void start(Stage stage) throws IOException {
        Image icon = new Image(getClass().getResourceAsStream("/moona.png"));
        //Locale.setDefault(Locale.forLanguageTag("DK"));
        FXMLLoader fxmlLoader = new FXMLLoader(DreamApp.class.getResource("hovedmenu.fxml"));
        stage.setOnCloseRequest(event -> handleWindowClose());
        scene = new Scene(fxmlLoader.load(), 1200, 780);
        //scene.getStylesheets().add(getClass().getResource("currentTema.css").toExternalForm());
        CSSUpdater.init();

        Path cssPath = AppPaths.APP_DATA_PATH.resolve("currentTema.css");
        File cssFile = cssPath.toFile();
        scene.getStylesheets().clear();
        scene.getStylesheets().add(cssFile.toURI().toString()); // Indlæs direkte fra resources
        //scene.getStylesheets().applyCss();

        stage.setTitle("Drømmeappen 2.0");
        stage.setMinWidth(650);
        stage.setMinHeight(550);
        stage.setScene(scene);
        stage.getIcons().add(icon);
        stage.show();

        // Test af splash-screen lukning!
        SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null) {
            splash.close();
        }
    }

    private void handleWindowClose() {
        User tempo = User.getInstance();
        CSSUpdater.updateCSSVariables(tempo.getForetrukneTema().getTemaForCSSUpdater(),false);
        // Er kategorier/temaer/indstillinger hentet fra skyen i denne session, ligger den
        // nyeste udgave allerede på disken - og den her i hukommelsen er den forældede.
        // Så skal den netop IKKE gemmes hen over (se SyncService.overtagXFraSkyen).
        if (!tempo.harHentetIndstillingerFraSkyen()) {
            IOutils.saveUser(tempo);
        }
        if (!tempo.harHentetTemaerFraSkyen()) {
            IOutils.saveTemaer(tempo.getTemaer());
        }
        if (!tempo.harHentetKategorierFraSkyen()) {
            IOutils.saveCategories(tempo.getCategories());
        }
        IOutils.saveDreams(tempo.getDreams());

        // Cloud-sync ved appluk. To beslutninger, i den rækkefølge:
        //
        // 1) Er der overhovedet noget at sende? Det kan afgøres helt lokalt mod skyindekset,
        //    så en session hvor man bare har kigget, lukker uden at røre Firebase overhovedet.
        //
        // 2) Er der noget, får pushet lov at blive færdigt - men højst i tre sekunder.
        //    Tråden er stadig en dæmontråd, så grænsen er en garanti: appen lukker uanset hvad.
        //
        // Før ventede vi aldrig, og pushet blev typisk dræbt halvvejs. Det var det rigtige valg
        // dengang et push først skulle hente 800+ dokumenter ned; nu koster det én læsning og
        // er ovre på et øjeblik. Prisen for ikke at vente var, at drømme skrevet i den sidste
        // session først nåede skyen ved NÆSTE opstart - og aldrig, hvis den ikke kom.
        SyncService sync = new SyncService(tempo);
        if (sync.harUsendteÆndringer()) {
            Thread pushThread = new Thread(sync::pushOnCloseIfEnabled);
            pushThread.setDaemon(true);
            pushThread.start();
            try {
                pushThread.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static Scene getCurrentScene() {
        return scene;
    }

    public static void main(String[] args) {
        if (!SingleInstanceLock.acquireLock()) {
            System.out.println("Appen kører allerede!");
            System.exit(0);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            SingleInstanceLock.releaseLock();
        }));
        launch();
    }
}