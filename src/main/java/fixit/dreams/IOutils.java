package fixit.dreams;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fixit.dreams.sync.SyncDTO;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

public class IOutils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Alle filstier er relative til AppPaths.APP_DATA_PATH
    private static final Path FILE_PATH_USER = AppPaths.APP_DATA_PATH.resolve("user.json");
    private static final Path FILE_PATH_TEMA = AppPaths.APP_DATA_PATH.resolve("temaer.json");
    private static final Path FILE_PATH_CAT = AppPaths.APP_DATA_PATH.resolve("cats.json");
    private static final Path FILE_PATH_DREAM = AppPaths.APP_DATA_PATH.resolve("dreams.json");
    private static final Path FILE_PATH_SYNC = AppPaths.APP_DATA_PATH.resolve("sync.json");
    private static final Path FILE_PATH_DELETED = AppPaths.APP_DATA_PATH.resolve("deleted.json");
    private static final Path FILE_PATH_META = AppPaths.APP_DATA_PATH.resolve("meta.json");
    private static final Path FILE_PATH_MACHINE = AppPaths.APP_DATA_PATH.resolve("machine.json");

    static {
        objectMapper.registerModule(new JavaTimeModule()); // Registrér JavaTimeModule
    }

    public static void saveUser(User user) {
        saveUserDTO(new UserDTO(user)); // Konverter til DTO
    }

    // Gemmer indstillingerne direkte fra en DTO. Bruges af cloud-syncen, som skal kunne skrive
    // en udgave hentet fra skyen til disk UDEN at gå gennem den kørende User (se SyncService).
    public static void saveUserDTO(UserDTO userDTO) {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(userDTO);
            Files.writeString(FILE_PATH_USER, json);
            stampSettings(userDTO);
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
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapList);
            Files.writeString(FILE_PATH_TEMA, json);
            stampIfChanged(meta -> meta.temaer, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String,Tema> loadTemaer() {
        try {
            ArrayList<HashMap<String, String>> mapList = objectMapper.readValue(FILE_PATH_TEMA.toFile(), ArrayList.class);
            return toTemaer(mapList);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveCategories(ArrayList<Category> cats) {
        List<CategoryDTO> dtoList = toCategoryDTOs(cats);
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dtoList);
            Files.writeString(FILE_PATH_CAT, json);
            stampIfChanged(meta -> meta.categories, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Category> loadCategories() {
        try {
            List<CategoryDTO> dtoList = objectMapper.readValue(
                    FILE_PATH_CAT.toFile(),
                    new TypeReference<List<CategoryDTO>>() {}
            );
            return toCategories(dtoList);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Omregningerne mellem model og DTO deles med cloud-syncen, som sender og modtager præcis
    // de samme DTO-former som dem der ligger i cats.json og temaer.json.
    public static List<CategoryDTO> toCategoryDTOs(ArrayList<Category> cats) {
        return cats.stream().map(c -> {
            CategoryDTO dto = new CategoryDTO();
            dto.name = c.getName();
            dto.symbols = c.getSymbols();
            dto.customOrder = c.getCustomOrder();
            return dto;
        }).toList();
    }

    public static ArrayList<Category> toCategories(List<CategoryDTO> dtoList) {
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
    }

    public static HashMap<String,Tema> toTemaer(List<HashMap<String,String>> mapList) {
        HashMap<String,Tema> userTema = new HashMap<>();
        for (HashMap<String, String> tema : mapList) {
            userTema.put(tema.get("temaName"), new Tema(tema));
        }
        return userTema;
    }

    // synchronized fordi der nu er to skrivere: FX-tråden ved appluk, og sync-tråden når den
    // har hentet drømme ned fra skyen. To samtidige skrivninger af den 1,4 MB store fil ville
    // kunne efterlade den halvt overskrevet - og det er hele brugerens dagbog.
    public static synchronized void saveDreams(HashMap<String,Dream> dreams) {
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

    // Køen over drømme slettet på DENNE maskine, som endnu ikke er blevet til gravsten i skyen
    // (id -> hvornår den blev slettet). Ligger i sin egen fil, ikke i sync.json: den slettes ved
    // "log ud", og en sletning foretaget mens man var logget ud ville så gå tabt.
    //
    // Sletningstidspunktet gemmes med, frem for kun ID'et, så gravstenen kan bære det tidspunkt
    // sletningen FAKTISK skete. Ellers ville en drøm, der blev slettet her kl. 10 og redigeret på
    // en anden maskine kl. 11, blive slettet alligevel hvis denne maskine først synkroniserede
    // kl. 12 - sletningen ville fejlagtigt se nyere ud end redigeringen.
    public static LinkedHashMap<String, Instant> loadDeletedDreams() {
        try {
            File file = FILE_PATH_DELETED.toFile();
            if (!file.exists()) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(file, new TypeReference<LinkedHashMap<String, Instant>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return new LinkedHashMap<>();
        }
    }

    public static void saveDeletedDreams(Map<String, Instant> deleted) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(FILE_PATH_DELETED.toFile(), deleted);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Vores lokale billede af hvad skyen indeholder: drøm-id -> det updatedAt vi senest har
    // fået sendt derop. Det er dét, der gør en almindelig sync billig - i stedet for at hente
    // alle 800+ dokumenter ned bare for at spørge "hvad har ændret sig?", kan vi svare selv.
    //
    // Filen hedder cloudindex-<uid>.json, altså én pr. konto. Det er dét, der gør det ufarligt
    // at lade den ligge ved log ud: et indeks beskriver ÉN bestemt konto, og logger man ind på
    // en anden, findes der ingen fil for dét uid, hvorved den dyre vej tages helt af sig selv.
    // Logger man ind på samme konto igen, er indekset stadig gyldigt, og syncen koster fire
    // læsninger i stedet for 800+.
    //
    // Filen må KUN stoles på når skyens meta/state-dokument bekræfter at denne maskine også var
    // den sidste der skrev (se SyncService). Ellers kan en anden maskine have ændret noget som
    // indekset ikke kender, og så skal den dyre vej gås. Det er også dét, der dækker hullet
    // mellem log ud og log ind: har en anden maskine skrevet imens, står dens maskin-id i
    // ejerskabsdokumentet, og indekset bliver ikke brugt.
    //
    // Returnerer null - ikke et tomt kort - hvis filen mangler eller er ødelagt. Forskellen er
    // vigtig: et tomt kort betyder "skyen er tom", mens null betyder "vi ved det ikke", og de to
    // svar fører til hver sin vej gennem syncen.
    private static Path cloudIndexPath(String uid) {
        return AppPaths.APP_DATA_PATH.resolve("cloudindex-" + uid + ".json");
    }

    public static LinkedHashMap<String, Instant> loadCloudIndex(String uid) {
        if (uid == null || uid.isBlank()) {
            return null;
        }
        try {
            File file = cloudIndexPath(uid).toFile();
            if (!file.exists()) {
                return null;
            }
            return objectMapper.readValue(file, new TypeReference<LinkedHashMap<String, Instant>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveCloudIndex(String uid, Map<String, Instant> indeks) {
        if (uid == null || uid.isBlank()) {
            return;
        }
        try {
            objectMapper.writeValue(cloudIndexPath(uid).toFile(), indeks);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteCloudIndex(String uid) {
        if (uid == null || uid.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(cloudIndexPath(uid));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Maskinens eget id. Beskriver hardwaren, ikke kontoen, og bor derfor i sin egen fil frem
    // for i sync.json: ellers ville et log ud kaste det væk, et log ind lave et nyt, og
    // ejerskabsdokumentet i skyen aldrig genkende maskinen igen - hvorved den dyre vej blev
    // taget ved hvert eneste login, uanset at indekset lå lige der.
    //
    // Til gengæld skal filen IKKE følge med når man flytter ved at kopiere datamappen: to
    // maskiner med samme id ville begge tro at de skrev sidst, og springe hinandens ændringer
    // over. Se afsnittet "Ny computer" i om.txt.
    public static String loadMachineId() {
        try {
            File file = FILE_PATH_MACHINE.toFile();
            if (!file.exists()) {
                return null;
            }
            JsonNode node = objectMapper.readTree(file);
            JsonNode id = node.get("machineId");
            return (id != null && !id.isNull() && !id.asText().isBlank()) ? id.asText() : null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void saveMachineId(String machineId) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(FILE_PATH_MACHINE.toFile(), Map.of("machineId", machineId));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Ældre installationer har ét fælles cloudindex.json. Det hørte til den konto der var logget
    // ind, og navngives derfor bare om første gang. Uden det ville opgraderingen koste én
    // fuld listning af skyen - præcis dét indekset er sat i verden for at undgå.
    //
    // Kaldes kun fra syncNow med kontoens eget uid, aldrig med et vilkårligt: filen bærer ikke
    // selv rundt på hvem den hører til, så det er kalderen der skal vide det.
    public static void migrerGammeltIndeks(String uid) {
        if (uid == null || uid.isBlank()) {
            return;
        }
        Path gammel = AppPaths.APP_DATA_PATH.resolve("cloudindex.json");
        Path ny = cloudIndexPath(uid);
        try {
            if (Files.exists(gammel) && Files.notExists(ny)) {
                Files.move(gammel, ny);
            }
        } catch (IOException e) {
            e.printStackTrace(); // mislykkes den, tages den dyre vej - irriterende, ikke farligt
        }
    }

    // Tidsstemplerne for kategori-definitioner, temaer og indstillinger (se MetaDTO).
    // Returnerer ALDRIG null: mangler eller er meta.json ødelagt, betyder det bare "vi har
    // endnu ikke set noget indhold", og den bliver genskabt ved næste gem.
    public static MetaDTO loadMeta() {
        try {
            File file = FILE_PATH_META.toFile();
            if (!file.exists()) {
                return new MetaDTO();
            }
            MetaDTO meta = objectMapper.readValue(file, MetaDTO.class);
            return (meta != null) ? meta : new MetaDTO();
        } catch (IOException e) {
            e.printStackTrace();
            return new MetaDTO();
        }
    }

    public static void saveMeta(MetaDTO meta) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(FILE_PATH_META.toFile(), meta);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Stempler ét af meta.json's tre felter ud fra det JSON der lige er skrevet til disk.
    // meta.json skrives kun hvis stemplet faktisk ændrede sig - uændret indhold ved appluk
    // skal hverken bumpe et tidsstempel eller røre filen.
    private static void stampIfChanged(Function<MetaDTO, MetaDTO.Stamp> vælgStempel, String json) {
        MetaDTO meta = loadMeta();
        if (vælgStempel.apply(meta).stampIfChanged(MetaDTO.hashOf(json))) {
            saveMeta(meta);
        }
    }

    // Indstillingernes stempel må kun afhænge af brugerens EGNE valg. schemaVersion er appens
    // eget bogholderi og skrives af SchemaMigrator ved opgradering - tælles den med, ville
    // enhver fremtidig skema-bump se ud som om brugeren havde ændret sine indstillinger.
    private static void stampSettings(UserDTO userDTO) {
        ObjectNode node = objectMapper.valueToTree(userDTO);
        node.remove("schemaVersion");
        stampIfChanged(meta -> meta.settings, node.toString());
    }

    // Sletter sync.json fuldstændigt - bruges ved "log ud", rører aldrig de øvrige datafiler.
    //
    // Skyindekset blev tidligere revet med her, fordi det dengang lå i én fælles cloudindex.json:
    // lod man den blive liggende, ville en senere sync mod en ANDEN konto tro at drømmene
    // allerede var sendt derop - og så ville de aldrig blive det. Nu hedder filen
    // cloudindex-<uid>.json, så et indeks aldrig kan forveksles med en anden kontos, og det er
    // netop pointen at det får lov at blive liggende: logger man ind på samme konto igen,
    // koster den første sync fire læsninger i stedet for 800+.
    //
    // machine.json bliver også liggende. Maskinen er den samme, uanset hvem der er logget ind.
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
