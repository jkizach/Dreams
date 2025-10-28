package fixit.dreams;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Window;

import javax.net.ssl.*;
import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

public class GITHUBUpdater {

    private static final String CURRENT_VERSION = "13"; // skal sættes når jeg laver en nye MSI!
    private static final String GITHUB_API_URL = "https://api.github.com/repos/jkizach/Dreams/releases/latest";
    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), "Documents", "DrømmeappenData", "update.json");

    public static void checkForUpdateIfNeeded() {
        LocalDate lastChecked = readLastCheckedDate();
        if (lastChecked == null || lastChecked.isBefore(LocalDate.now().minusDays(30))) {
            checkForUpdate();
            writeLastCheckedDate(LocalDate.now());
        }
    }


    public static String readUrl(String urlString) throws IOException, InterruptedException {
        // 1. Opret HttpClient (genbrugelig og trådsikker)
        HttpClient client = HttpClient.newBuilder()
                // Sæt denne linje til at følge alle omdirigeringer, herunder 303.
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        // 2. Byg anmodningen (HttpRequest)
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .build();

        // 3. Send anmodningen og modtag svaret som en String
        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString() // Angiver, at svaret skal læses som en String
        );

        // 4. Tjek for succesfuld statuskode (f.eks. 200)
        if (response.statusCode() == 200) {
            return response.body();
        } else {
            throw new IOException("Fejl ved hentning af URL: Statuskode " + response.statusCode());
        }
    }

    private static void checkForUpdate() {
        new Thread(() -> {
            try {

                String htmlUrl = "https://github.com/jkizach/Dreams/releases/latest";

                String downloadUrl = "https://drive.google.com/uc?export=download&id=1Go7y6yCLC_lRZ9JIKKtl3xEjhc2WnLIt";
                String latestVersion = readUrl(downloadUrl);

                System.out.println(latestVersion);
                if (!CURRENT_VERSION.equals(latestVersion)) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Opdatering tilgængelig");
                        alert.setHeaderText("Ny version: " + latestVersion);
                        alert.setContentText("Der findes en ny version. Vil du hente den?");
                        ButtonType ok = new ButtonType("Download", ButtonBar.ButtonData.OK_DONE);
                        alert.getButtonTypes().setAll(ok, ButtonType.CANCEL);


                        alert.showAndWait().ifPresent(response -> {
                            if (response == ok) {
                                try {
                                    if (Desktop.isDesktopSupported()) {
                                        Desktop.getDesktop().browse(new URI(htmlUrl));
                                    } else {
                                        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + htmlUrl);
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });
                    });
                }

            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                log("Opdateringstjek mislykkedes: " + e.getMessage() + sw);
            }
        }).start();

    }

    private static LocalDate readLastCheckedDate() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                ObjectMapper mapper = new ObjectMapper();
                Map<?, ?> data = mapper.readValue(CONFIG_PATH.toFile(), Map.class);
                String dateStr = (String) data.get("lastChecked");
                return LocalDate.parse(dateStr);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private static void writeLastCheckedDate(LocalDate date) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Files.createDirectories(CONFIG_PATH.getParent());
            mapper.writeValue(CONFIG_PATH.toFile(), Map.of("lastChecked", date.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void log(String message) {
        try {
            Files.writeString(
                    Paths.get(System.getProperty("user.home"), "drømmeapp-updater-log.txt"),
                    LocalDate.now() + " - " + message + System.lineSeparator(),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {}
    }
}

