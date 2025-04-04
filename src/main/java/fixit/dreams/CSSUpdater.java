package fixit.dreams;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CSSUpdater {
    private static final String TEMP_PATH = "src/main/resources/fixit/dreams/tempTema.css";
    private static final String CURRENT_PATH = "src/main/resources/fixit/dreams/currentTema.css";

    public static void updateCSSVariables(Map<String, String> variables, Boolean temp) {
        try {
            Path path = (temp) ? Paths.get(TEMP_PATH) : Paths.get(CURRENT_PATH);
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

    private static String toHexColor(String fxColor) {
        if (fxColor.startsWith("0x")) {
            return "#" + fxColor.substring(2, 8); // Fjerner "0x" og alfa-kanal
        }
        return fxColor; // Hvis det allerede er korrekt format
    }
}
