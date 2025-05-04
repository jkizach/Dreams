package fixit.dreams;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.util.*;

public class IOutils {
    private static final String FILE_PATH = "user.json";
    private static final String FILE_PATH_TEMA = "temaer.json";
    private static final String FILE_PATH_CAT = "cats.json";
    private static final String FILE_PATH_DREAM = "dreams.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TXT_PATH_OM = "om.txt";
    private static final String TXT_PATH_HELP = "help.txt";


    static {
        objectMapper.registerModule(new JavaTimeModule()); // Registrér JavaTimeModule
    }

    public static void saveUser(User user) {
        try {
            UserDTO userDTO = new UserDTO(user); // Konverter til DTO
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH), userDTO);
            System.out.println("User gemt som JSON!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static UserDTO loadUser() {
        try {
            UserDTO userDTO = objectMapper.readValue(new File(FILE_PATH), UserDTO.class);
            System.out.println("User indlæst fra JSON!");
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
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH_TEMA), mapList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String,Tema> loadTemaer() {
        try {
            ArrayList<HashMap<String, String>> mapList = objectMapper.readValue(new File(FILE_PATH_TEMA), ArrayList.class);
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
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH_CAT), dtoList);} catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Category> loadCategories() {
        try {
            List<CategoryDTO> dtoList = objectMapper.readValue(
                    new File(FILE_PATH_CAT),
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
            temp.categories = d.getCategories();
            temp.indhold = d.getIndhold();
            temp.dagrest = d.getDagrest();
            temp.dato = d.getDato();
            temp.lucid = d.getLucid();
            temp.praktiserer = d.getPraktiserer();
            temp.modsat = d.getModsat();
            temp.arketypisk = d.getArketypisk();
            temp.ompraksis = d.getOmpraksis();
            temp.mareridt = d.getMareridt();
            temp.kollektiv = d.getKollektiv();
            temp.advarsel = d.getAdvarsel();

            saveMe.add(temp);
        }

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE_PATH_DREAM), saveMe);
            System.out.println("Dreams gemt som JSON!");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String, Dream> loadDreams() {
        HashMap<String, Dream> loadedDreams = new HashMap<>();
        try {
            File file = new File(FILE_PATH_DREAM);
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
        File file = new File((type.equals("Om appen")) ? TXT_PATH_OM : TXT_PATH_HELP);
        StringBuilder content = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
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



/*private static final String FILE_PATH = System.getProperty("user.home") + "/user.json";
Brug noget i den her stil til at finde en path til "home" - det skulle virke på mac,pc og linux!
*/