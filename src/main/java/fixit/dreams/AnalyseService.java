package fixit.dreams;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.IntStream.rangeClosed;

public class AnalyseService extends ServiceMother{
    private Stats stats = new Stats();
    private boolean andOr = true;
    ObservableList<DreamDTO> filteredDreams = FXCollections.observableArrayList();
    ObservableList<DreamDTO> forloebDreams = FXCollections.observableArrayList();
    ObservableList<DreamDTO> forloeb = FXCollections.observableArrayList();


    public AnalyseService(User user) {
        super(user);
    }

    public ObservableList<DreamDTO> getForloeb() {
        return forloeb;
    }

    public void updateForloeb() {
        forloeb.clear();
        for (Dream d : user.getDreams().values()) {
            for (CategoryDTO c : d.getCategories()) {
                if (c.name.equals("Forløb") && !c.symbols.isEmpty()) {
                    DreamDTO dto = new DreamDTO(d.getId(), d.getIndhold(), d.getDagrest(), d.getTolkning(), d.getDato());
                    forloeb.add(dto);
                    break;
                }
            }
        }
        sortDreamsByDate(forloeb);
    }

    public ObservableList<DreamDTO> getForloebDreams() {
        return forloebDreams;
    }

    public void refreshForloebDreams(LocalDate startDate, int days, int targetMonthDelta) {
        forloebDreams.clear();

        LocalDate targetDate = startDate.plusMonths(targetMonthDelta);

        // Lav vindue ± precisionDays omkring den dato
        Set<LocalDate> datoer = IntStream.rangeClosed(-days, days)
                .mapToObj(targetDate::plusDays)
                .collect(Collectors.toSet());

        for (Dream d : user.getDreams().values()) {
            if (datoer.contains(d.getDato())) {
                DreamDTO dto = new DreamDTO(d.getId(), d.getIndhold(), d.getDagrest(), d.getTolkning(), d.getDato());
                forloebDreams.add(dto);
            }
        }
        sortDreamsByDate(forloebDreams);
    }


    public Map<String,Integer> getDataForPieChart(String kategoriNavn, LocalDate start, LocalDate slut) {
        // Her skal fra og til datoerne jo bruges!
        HashMap<String,Integer> outMap = new HashMap<>();

        // men med den nye kode har en dream en ArrayList<CategoryDTO> som har navn og TreeSet
        for (Dream d : user.getDreams().values()) {
            // her skal tjekkes som dato er in range!
            if (isInRange(d.getDato(), start, slut)) {
                for (CategoryDTO c : d.getCategories()) {
                    if (c.name.equals(kategoriNavn)) {
                        for (String symbol : c.symbols) {
                            outMap.merge(symbol, 1, Integer::sum);
                        }
                        break;
                    }
                }
            }

        }
        return outMap;
    }


    public ArrayList<XYChart.Series<String, Number>> getDataForLineChart(GrafDTO indat) {
        ArrayList<XYChart.Series<String, Number>> outdat = new ArrayList<>();

        // Det kan nu gøres smartere!!! Med StatsDO osv... for alle checkmodels er tilgængelige
        // i user.getCategories() -- getccbFilterSelections returnerer en CategoryDTO med et TreeMap med symboler og et navn
        for (Category c : user.getCategories()) {
            if (c.getccbFilterSelections() != null) {
                for (String symbol : c.getccbFilterSelections().symbols) {
                    outdat.add(stats.makeXY(stats.getCategoryStats(c.getName()), symbol, indat.fra, indat.til, indat.xakse));
                }
            }
        }

        if (indat.lucid) {
            outdat.add(stats.makeBoolXY(stats.getLucidStats(), indat.fra, indat.til, indat.xakse,"lucid"));
        }

        if (indat.praktiserer) {
            outdat.add(stats.makeBoolXY(stats.getPraktisererStats(), indat.fra, indat.til, indat.xakse, "praktiserer"));
        }

        if (indat.modsat) {
            outdat.add(stats.makeBoolXY(stats.getModsatStats(), indat.fra, indat.til, indat.xakse, "modsatkønnet"));
        }

        if (indat.kollektiv) {
            outdat.add(stats.makeBoolXY(stats.getKollektivStats(), indat.fra, indat.til, indat.xakse, "kollektiv"));
        }

        if (indat.arketypisk) {
            outdat.add(stats.makeBoolXY(stats.getArketypiskStats(), indat.fra, indat.til, indat.xakse, "arketypisk"));
        }

        if (indat.praksis) {
            outdat.add(stats.makeBoolXY(stats.getPraksisStats(), indat.fra, indat.til, indat.xakse, "om praksis"));
        }

        if (indat.mareridt) {
            outdat.add(stats.makeBoolXY(stats.getMareridtStats(), indat.fra, indat.til, indat.xakse, "mareridt"));
        }

        if (indat.advarsel) {
            outdat.add(stats.makeBoolXY(stats.getAdvarselStats(), indat.fra, indat.til, indat.xakse, "advarsel"));
        }

        return outdat;
    }

    private boolean isInRange(LocalDate date, LocalDate start, LocalDate end) {
        return (date.isEqual(start) || date.isAfter(start)) && (date.isEqual(end) || date.isBefore(end));
    }

    public void updateStats() {
        stats.calculateStats();
        user.statsErGenberegnet();
    }

    public LocalDate getFirstDreamDate() {
        return stats.getFirstDream();
    }

    public ObservableList<DreamDTO> getFilteredDreams() {
        return filteredDreams;
    }

    private void sortDreamsByDate(ObservableList<DreamDTO> sortme) {
        FXCollections.sort(sortme, Comparator.comparing(DreamDTO::getDato).reversed()); // Nyeste først
    }

    public void updateFilteredDreams(FilterDTO data, boolean useData) {
        filteredDreams.clear();

        if (!useData) {
            for (Dream d : user.getDreams().values()) {
                if (isInRange(d.getDato(), data.fra, data.til)) {
                    DreamDTO dto = new DreamDTO(d.getId(), d.getIndhold(), d.getDagrest(), d.getTolkning(),d.getDato());
                    filteredDreams.add(dto);
                }
            }
        } else {
            // Named loop - fordi hvis bare ét symbol ikke er der ved OG-logik, så dur drømmen ikke
            outer:
            for (Dream d : user.getDreams().values()) {
                if (isInRange(d.getDato(), data.fra, data.til)) {
                    // Check om det er AND|OR logik der skal bruges:
                    if (andOr) {
                        // Check om drømmen matcher ift. Valget i CCBerne:

                        for (Category c : user.getCategories()) {
                            if (c.getccbFilterSelections() != null) {
                                for (String symbol : c.getccbFilterSelections().symbols) {
                                    // Hvis alle matcher så go on!
                                    for (CategoryDTO dto : d.getCategories()) {
                                        if (dto.name.equals(c.getName())) {
                                            if (!dto.symbols.contains(symbol)) {
                                                continue outer;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Check om drømmen matcher ift. checkboxes
                        if (d.getAdvarsel() == data.advarsel && d.getArketypisk() == data.arketypisk && d.getMareridt() == data.mareridt &&
                        d.getKollektiv() == data.kollektiv && d.getModsat() == data.modsat && d.getLucid() == data.lucid &&
                        d.getPraktiserer() == data.praktiserer && d.getOmpraksis() == data.praksis) {
                            DreamDTO dto = new DreamDTO(d.getId(), d.getIndhold(), d.getDagrest(), d.getTolkning(), d.getDato());
                            filteredDreams.add(dto);
                        }

                    } else {
                        // nu er det en ELLER-logik, så nu skal d tilføjes bare én kvalitet matcher:
                        // CBer først:
                        if ((data.lucid && d.getLucid())|(data.praksis && d.getOmpraksis())|(data.advarsel && d.getAdvarsel())|(data.arketypisk && d.getArketypisk())|
                        (data.kollektiv && d.getKollektiv())|(data.modsat && d.getModsat())|(data.mareridt && d.getMareridt())|(data.praktiserer && d.getPraktiserer())) {
                            DreamDTO dto = new DreamDTO(d.getId(), d.getIndhold(), d.getDagrest(), d.getTolkning(), d.getDato());
                            filteredDreams.add(dto);
                            continue;
                        }
                        // Check om drømmen matcher ift. Valget i CCBerne og her er match = tilføj!
                        for (Category c : user.getCategories()) {
                            if (c.getccbFilterSelections() != null) {
                                for (String symbol : c.getccbFilterSelections().symbols) {
                                    // Hvis alle matcher så go on!
                                    for (CategoryDTO dto : d.getCategories()) {
                                        if (dto.name.equals(c.getName())) {
                                            if (dto.symbols.contains(symbol)) {
                                                DreamDTO addme = new DreamDTO(d.getId(), d.getIndhold(), d.getDagrest(), d.getTolkning(), d.getDato());
                                                filteredDreams.add(addme);
                                                continue outer;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        sortDreamsByDate(filteredDreams);
    }

    public boolean isAndOr() {
        return andOr;
    }

    public void setAndOr(boolean andOr) {
        this.andOr = andOr;
    }

    public boolean usingKollektiv() {
        return user.isVisKollektiv();
    }

    public boolean usingAdvarsel() {
        return user.isVisAdvarsel();
    }

    public int[] getTalBinary(LocalDate fra, LocalDate til) {
        //stats.getTalCatStats(fra,til);
        return(stats.getTalBinary(fra, til));
    }

    public ArrayList<ArrayList<String>> getTalCategories(LocalDate fra, LocalDate til) {
        ArrayList<ArrayList<String>> outDat = new ArrayList<>();
        ArrayList<TreeMap<String,Integer>> totalStats = stats.getTalCatStats(fra,til);

        // Kan en ny kat være null her?
        for (TreeMap<String,Integer> data : totalStats) {
            ArrayList<String> temp = new ArrayList<>();

            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                temp.add(String.format("%-22s %3d", entry.getKey(), entry.getValue()));
            }

            // Sorter temp efter tallet...
            temp.sort((a, b) -> {
                int valueA = extractNumber(a);
                int valueB = extractNumber(b);
                return Integer.compare(valueB, valueA); // Omvendt orden
            });

            outDat.add(temp);
        }
        return outDat;
    }

    private static int extractNumber(String input) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }
        throw new IllegalArgumentException("Intet tal fundet i strengen: " + input);
    }

    public String getForloebStage(String id) {
        String out = "";
        for (CategoryDTO c : user.getDream(id).getCategories()) {
            if (c.name.equals("Forløb")) {
                for (String symbol : c.symbols) {
                    out = out + symbol;
                }
            }
        }
        return out;
    }

    public int countDreams(LocalDate fra, LocalDate til) {
        int counts = 0;
        for (Dream d : user.getDreams().values()) {
            if (isInRange(d.getDato(), fra, til)) {
                counts++;
            }
        }
        return counts;
    }

}
