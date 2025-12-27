package fixit.dreams;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import javax.net.ssl.*;
import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.Map;

public class GITHUBUpdater {

    private static final String CURRENT_VERSION = "v1.5"; // skal sættes når jeg laver en nye MSI!
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
            String latestVersion = null;
            String htmlUrl = null;

            try {
                // 1. Forsøg: direkte GitHub HTTP-kald ===
                HttpURLConnection conn = (HttpURLConnection) new URL(GITHUB_API_URL).openConnection();
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("User-Agent", "Dreams-Updater");

                int responseCode = conn.getResponseCode();
                //log("GitHub response code: " + responseCode);

                ObjectMapper mapper = new ObjectMapper();
                Map<?, ?> json = mapper.readValue(conn.getInputStream(), Map.class);

                latestVersion = (String) json.get("tag_name");
                htmlUrl = (String) json.get("html_url");

            } catch (Exception e) {
                // 2. Fallback: brug updater.exe ===
                //log("GitHub kald fejlede (" + e.getMessage() + "), prøver updater.exe...");

                try {
                    Process process = new ProcessBuilder("app/updater.exe").start();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

                    String output = reader.readLine(); // forventer fx {"version":"v1.3.2","url":"https://..."}

                    //log("Output fra updater: " + output);

                    // Parse JSON-resultatet
                    ObjectMapper mapper = new ObjectMapper();
                    Map<?, ?> json = mapper.readValue(output, Map.class);

                    latestVersion = (String) json.get("version");
                    htmlUrl = (String) json.get("url");

                } catch (Exception ex) {
                    //log("Updater.exe fejlede: " + ex.getMessage());
                }
            }

            // 3. Hvis vi har fundet en ny version, vis dialog ===
            if (latestVersion != null && htmlUrl != null && !CURRENT_VERSION.equals(latestVersion)) {
                String finalLatestVersion = latestVersion;
                String finalHtmlUrl = htmlUrl;

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Opdatering tilgængelig");
                    alert.setHeaderText("Ny version: " + finalLatestVersion);
                    alert.setContentText("Der findes en ny version. Vil du hente den?");
                    ButtonType ok = new ButtonType("Download", ButtonBar.ButtonData.OK_DONE);
                    alert.getButtonTypes().setAll(ok, ButtonType.CANCEL);

                    alert.showAndWait().ifPresent(response -> {
                        if (response == ok) {
                            try {
                                if (Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().browse(new URI(finalHtmlUrl));
                                } else {
                                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + finalHtmlUrl);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                });
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

