package fixit.dreams;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Map;

public class GITHUBUpdater {

    private static final String CURRENT_VERSION = "v1.3"; // skal sættes når jeg laver en nye MSI!
    private static final String GITHUB_API_URL = "https://api.github.com/repos/jkizach/Dreams/releases/latest";
    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.home"), "Documents", "DrømmeappenData", "update.json");

    public static void checkForUpdateIfNeeded() {
        LocalDate lastChecked = readLastCheckedDate();
        if (lastChecked == null || lastChecked.isBefore(LocalDate.now().minusDays(30))) {
            checkForUpdate();
            writeLastCheckedDate(LocalDate.now());
        }
    }

    private static void checkForUpdate() {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(GITHUB_API_URL).openConnection();
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("User-Agent", "Dreams-Updater");

                int responseCode = conn.getResponseCode();
                log("GitHub response code: " + responseCode);

                ObjectMapper mapper = new ObjectMapper();
                Map<?, ?> json = mapper.readValue(conn.getInputStream(), Map.class);

                String latestVersion = (String) json.get("tag_name");
                String htmlUrl = (String) json.get("html_url");

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
                log("Opdateringstjek mislykkedes: " + e.getMessage());
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

