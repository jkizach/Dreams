package fixit.dreams;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class AppPaths {
    // Base folder (Documents/DrømmeappenData) - burde virke både på pc og mac
    static final Path APP_DATA_PATH = Paths.get(System.getProperty("user.home"), "Documents", "DrømmeappenData");

    // Sørg for at mappen findes før alt andet
    static {
        try {
            if (Files.notExists(APP_DATA_PATH)) {
                Files.createDirectories(APP_DATA_PATH);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private AppPaths() {}
}
