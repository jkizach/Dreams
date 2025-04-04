package fixit.dreams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class IOutils {
    private static final String FILE_PATH = "user.json";
    private static final String FILE_PATH_TEMA = "temaer.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
}
/*private static final String FILE_PATH = System.getProperty("user.home") + "/user.json";
Brug noget i den her stil til at finde en path til "home" - det skulle virke på mac,pc og linux!!!
*/