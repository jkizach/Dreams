package fixit.dreams;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fixit.dreams.sync.SyncDTO;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class IOutils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Alle filstier er relative til AppPaths.APP_DATA_PATH
    private static final Path FILE_PATH_USER = AppPaths.APP_DATA_PATH.resolve("user.json");
    private static final Path FILE_PATH_TEMA = AppPaths.APP_DATA_PATH.resolve("temaer.json");
    private static final Path FILE_PATH_CAT = AppPaths.APP_DATA_PATH.resolve("cats.json");
    private static final Path FILE_PATH_DREAM = AppPaths.APP_DATA_PATH.resolve("dreams.json");
    private static final Path FILE_PATH_SYNC = AppPaths.APP_DATA_PATH.resolve("sync.json");
    private static final String TXT_PATH_OM = "om.txt";
    private static final String TXT_PATH_HELP = "help.txt";

    static {
        objectMapper.registerModule(new JavaTimeModule()); // Registrér JavaTimeModule
    }

    public static void saveUser(User user) {
        try {
            UserDTO userDTO = new UserDTO(user); // Konverter til DTO
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(FILE_PATH_USER.toFile(), userDTO);
            System.out.println("User gemt som JSON!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static UserDTO loadUser() {
        try {
            UserDTO userDTO = objectMapper.readValue(FILE_PATH_USER.toFile(), UserDTO.class);
            System.out.println("User indlæst fra JSON!");
            System.out.println(userDTO.startFromThisDate);
            return userDTO;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveTemaer(HashMap<String,Tema> userTemaer) {
        try {
            ArrayList<HashMap<String, String>> mapList = new ArrayList<>();
            for (Tema tema : userTemaer.values()) {
                mapList.add(tema.getTemaForSaving());
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(FILE_PATH_TEMA.toFile(), mapList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String,Tema> loadTemaer() {
        try {
            ArrayList<HashMap<String, String>> mapList = objectMapper.readValue(FILE_PATH_TEMA.toFile(), ArrayList.class);
            HashMap<String,Tema> userTema = new HashMap<>();
            for (HashMap<String, String> tema : mapList) {
                userTema.put(tema.get("temaName"), new Tema(tema));
            }
            return userTema;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveCategories(ArrayList<Category> cats) {
        List<CategoryDTO> dtoList = cats.stream().map(c -> {
            CategoryDTO dto = new CategoryDTO();
            dto.name = c.getName();
            dto.symbols = c.getSymbols();
            dto.customOrder = c.getCustomOrder();
            return dto;
        }).toList();
        try {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(FILE_PATH_CAT.toFile(), dtoList);} catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Category> loadCategories() {
        try {
            List<CategoryDTO> dtoList = objectMapper.readValue(
                    FILE_PATH_CAT.toFile(),
                    new TypeReference<List<CategoryDTO>>() {}
            );
            ArrayList<Category> result = new ArrayList<>();
            for (CategoryDTO dto : dtoList) {
                Category cat = new Category(dto.name);
                cat.setSymbols(dto.symbols);
                if (cat.hasCustomOrder()) {
                    cat.setCustomOrder(dto.customOrder);
                }
                result.add(cat);
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveDreams(HashMap<String,Dream> dreams) {
        List<DreamData> saveMe = new ArrayList<>();
        for (Dream d : dreams.values()) {
            DreamData temp = new DreamData();
            temp.id = d.getId();
            temp.categories = d.getCategories();
            temp.indhold = d.getIndhold();
            temp.dagrest = d.getDagrest();
            temp.tolkning = d.getTolkning();
            temp.dato = d.getDato();
            temp.updatedAt = d.getUpdatedAt();

            saveMe.add(temp);
        }

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(FILE_PATH_DREAM.toFile(), saveMe);
            System.out.println("Dreams gemt som JSON!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String, Dream> loadDreams() {
        HashMap<String, Dream> loadedDreams = new HashMap<>();
        try {
            File file = FILE_PATH_DREAM.toFile();
            if (!file.exists()) {
                return loadedDreams; // returnér tom mappe, hvis fil ikke findes endnu
            }

            // Læs listen af DreamData-objekter fra fil
            List<DreamData> dataList = objectMapper.readValue(file, new TypeReference<List<DreamData>>() {});

            for (DreamData data : dataList) {
                Dream dream = new Dream(data);
                loadedDreams.put(dream.getId(), dream);
            }
            System.out.println("Dreams er loaded!");

        } catch (IOException e) {
            e.printStackTrace();
        }
        return loadedDreams;
    }

    public static void saveSync(SyncDTO sync) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(FILE_PATH_SYNC.toFile(), sync);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static SyncDTO loadSync() {
        try {
            return objectMapper.readValue(FILE_PATH_SYNC.toFile(), SyncDTO.class);
        } catch (IOException e) {
            return null;
        }
    }

    // Sletter sync.json fuldstændigt - bruges ved "log ud", rører aldrig de øvrige datafiler.
    public static void deleteSync() {
        try {
            Files.deleteIfExists(FILE_PATH_SYNC);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void eksporterDreamlist(List<DreamDTO> dreams, String filNavn) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filNavn))) {
            for (DreamDTO dto : dreams) {
                String txt = dto.getVisbartIndhold();
                writer.write(txt);
                writer.newLine(); // Tilføjer et linjeskift for hvert objekt
            }
            System.out.println("Data er blevet skrevet til filen: " + filNavn);
        } catch (IOException e) {
            System.err.println("Fejl ved skrivning til fil: " + e.getMessage());
        }
    }

    public static String loadOmHelpTxt(String type) {
        // Bestem hvilken fil vi skal bruge
        String resourceName = (type.equals("Om appen")) ? "om.txt" : "help.txt";
        StringBuilder content = new StringBuilder();

        // Brug getResourceAsStream til at hente filen fra resources
        try (InputStream inputStream = IOutils.class.getResourceAsStream("/" + resourceName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            // Læs linje for linje
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");  // Tilføjer hver linje + newline
            }
        } catch (IOException e) {
            System.err.println("Fejl: " + e.getMessage());
        }
        return content.toString();
    }
}