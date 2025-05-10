package fixit.dreams;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CSSUpdater {
    private static final Path APP_DATA_PATH = Paths.get(System.getProperty("user.home"), "Documents", "Drømmeappen");
    private static final Path TEMP_PATH = APP_DATA_PATH.resolve("tempTema.css");
    private static final Path CURRENT_PATH = APP_DATA_PATH.resolve("currentTema.css");

    // Sørg for at kopiere CSS-filerne, hvis de ikke allerede findes i appens mappe
    static {
        try {
            if (Files.notExists(APP_DATA_PATH)) {
                Files.createDirectories(APP_DATA_PATH);
            }
            // Kopier "currentTema.css" og "tempTema.css" fra resources til appens mappe, hvis de ikke findes
            copyCssIfNotExists("fixit/dreams/currentTema.css", CURRENT_PATH);
            copyCssIfNotExists("fixit/dreams/tempTema.css", TEMP_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metode til at kopiere CSS-filer fra resources til appens mappe
    private static void copyCssIfNotExists(String resourcePath, Path targetPath) throws IOException {
        if (Files.notExists(targetPath)) {
            try (InputStream in = CSSUpdater.class.getResourceAsStream("/" + resourcePath)) {
                if (in == null) throw new FileNotFoundException("Resource not found: " + resourcePath);
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    // Opdater CSS-variablerne
    public static void updateCSSVariables(Map<String, String> variables, Boolean temp) {
        try {
            Path path = (temp) ? TEMP_PATH : CURRENT_PATH;
            List<String> lines = Files.readAllLines(path);

            List<String> modifiedLines = lines.stream()
                    .map(line -> {
                        for (Map.Entry<String, String> entry : variables.entrySet()) {
                            if (line.contains(entry.getKey() + ":")) {
                                if (line.contains("-fx-font-family")) {
                                    return "\t" + entry.getKey() + ": '" + entry.getValue() + "';";
                                } else {
                                    String fixedColor = toHexColor(entry.getValue()); // Sikrer korrekt format
                                    return "\t" + entry.getKey() + ": " + fixedColor + ";";
                                }
                            }
                        }
                        return line;
                    })
                    .collect(Collectors.toList());

            Files.write(path, modifiedLines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Konverterer farve til hex-format, hvis nødvendigt
    private static String toHexColor(String fxColor) {
        if (fxColor.startsWith("0x")) {
            return "#" + fxColor.substring(2, 8); // Fjerner "0x" og alfa-kanal
        }
        return fxColor; // Hvis det allerede er korrekt format
    }
}
