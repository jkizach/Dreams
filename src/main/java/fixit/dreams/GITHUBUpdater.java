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

    private static final String CURRENT_VERSION = "v2.0"; // skal sættes når jeg laver en nye MSI!
    private static final String GITHUB_API_URL = "https://api.github.com/repos/jkizach/Dreams/releases/latest";
    private static final Path CONFIG_PATH = AppPaths.APP_DATA_PATH.resolve("update.json");

    public static void checkForUpdateIfNeeded() {
        LocalDate lastChecked = readLastCheckedDate();
        if (lastChecked == null || lastChecked.isBefore(LocalDate.now().minusDays(30))) {
            checkForUpdate();
            writeLastCheckedDate(LocalDate.now());
        }
    }

    private static void checkForUpdate() {
        Thread updaterThread = new Thread(() -> {
            String latestVersion = null;
            String htmlUrl = null;

            // Kaldet gik tidligere altid galt i den installerede app og faldt tilbage på et
            // C#-program, app/updater.exe. Årsagen var ikke Java, men at jlink-imaget manglede
            // SunEC-provideren; se "requires jdk.crypto.ec" i module-info.java. Fallbacken er
            // fjernet, fordi Java-kaldet nu virker direkte fra MSI'en.
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(GITHUB_API_URL).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("User-Agent", "Dreams-Updater");

                ObjectMapper mapper = new ObjectMapper();
                Map<?, ?> json = mapper.readValue(conn.getInputStream(), Map.class);

                latestVersion = (String) json.get("tag_name");
                htmlUrl = (String) json.get("html_url");

            } catch (Exception e) {
                // Opdateringstjek er en bekvemmelighed, ikke en kernefunktion: kan vi ikke nå
                // GitHub, tier vi stille i stedet for at genere brugeren med en fejlbesked.
            }

            // Hvis vi har fundet en ny version, vis dialog ===
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
        });
        updaterThread.setDaemon(true);
        updaterThread.start();
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
}
