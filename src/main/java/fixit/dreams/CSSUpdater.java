package fixit.dreams;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CSSUpdater {
    private static final Path TEMP_PATH = AppPaths.APP_DATA_PATH.resolve("tempTema.css");
    private static final Path CURRENT_PATH = AppPaths.APP_DATA_PATH.resolve("currentTema.css");

    /**
     * Lægger CSS-filerne fra resources ud i appens mappe ved HVER opstart, og skriver så temaets
     * variabler ind i dem igen.
     *
     * Før blev filerne kun kopieret hvis de ikke fandtes, og så nåede nye stilregler i en ny
     * version aldrig ud til nogen der havde brugt appen før - kun farver og font, som
     * updateCSSVariables skriver ind. Filerne kan altid genskabes af skabelonen plus temaet, så
     * der går intet tabt ved at overskrive dem: brugerens temaer ligger i temaer.json, ikke her.
     * Kun en hånd-rettelse direkte i filen i datamappen forsvinder, og det er ikke en understøttet
     * måde at bruge appen på.
     *
     * @param variabler det foretrukne temas variabler (Tema.getTemaForCSSUpdater), eller null -
     *                  så står skabelonens egne farver, ligesom ved allerførste opstart.
     */
    public static void init(Map<String, String> variabler) {
        try {
            copyCss("fixit/dreams/currentTema.css", CURRENT_PATH);
            copyCss("fixit/dreams/tempTema.css", TEMP_PATH);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (variabler != null) {
            updateCSSVariables(variabler, false);
            updateCSSVariables(variabler, true);
        }
    }

    // Metode til at kopiere CSS-filer fra resources til appens mappe
    private static void copyCss(String resourcePath, Path targetPath) throws IOException {
        try (InputStream in = CSSUpdater.class.getResourceAsStream("/" + resourcePath)) {
            if (in == null) throw new FileNotFoundException("Resource not found: " + resourcePath);
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
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
                                    return "\t" + entry.getKey() + ": '" + Fonte.tilgaengelig(entry.getValue()) + "';";
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
