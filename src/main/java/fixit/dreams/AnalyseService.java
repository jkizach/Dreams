package fixit.dreams;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;

public class AnalyseService extends ServiceMother{
    private Stats stats = new Stats();
    private boolean andOr = true;
    ObservableList<DreamDTO> filteredDreams = FXCollections.observableArrayList();

    public AnalyseService(User user) {
        super(user);
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
            outdat.add(stats.makeBoolXY(stats.getLucidStats(), indat.fra, indat.til, indat.xakse));
        }

        if (indat.praktiserer) {
            outdat.add(stats.makeBoolXY(stats.getPraktisererStats(), indat.fra, indat.til, indat.xakse));
        }

        if (indat.modsat) {
            outdat.add(stats.makeBoolXY(stats.getModsatStats(), indat.fra, indat.til, indat.xakse));
        }

        if (indat.kollektiv) {
            outdat.add(stats.makeBoolXY(stats.getKollektivStats(), indat.fra, indat.til, indat.xakse));
        }

        if (indat.arketypisk) {
            outdat.add(stats.makeBoolXY(stats.getArketypiskStats(), indat.fra, indat.til, indat.xakse));
        }

        if (indat.praksis) {
            outdat.add(stats.makeBoolXY(stats.getPraksisStats(), indat.fra, indat.til, indat.xakse));
        }

        if (indat.mareridt) {
            outdat.add(stats.makeBoolXY(stats.getMareridtStats(), indat.fra, indat.til, indat.xakse));
        }

        if (indat.advarsel) {
            outdat.add(stats.makeBoolXY(stats.getAdvarselStats(), indat.fra, indat.til, indat.xakse));
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

    public void updateFilteredDreams(FilterDTO data, boolean useData) {
        filteredDreams.clear();

        if (!useData) {
            for (Dream d : user.getDreams().values()) {
                if (isInRange(d.getDato(), data.fra, data.til)) {
                    DreamDTO dto = new DreamDTO(d.getId(), d.getIndhold(), d.getDagrest(), d.getDato());
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
                            DreamDTO dto = new DreamDTO(d.getId(), d.getIndhold(), d.getDagrest(), d.getDato());
                            filteredDreams.add(dto);
                        }

                    } else {
                        // nu er det en ELLER-logik, så nu skal d tilføjes bare én kvalitet matcher:
                        // CBer først:
                        if ((data.lucid && d.getLucid())|(data.praksis && d.getOmpraksis())|(data.advarsel && d.getAdvarsel())|(data.arketypisk && d.getArketypisk())|
                        (data.kollektiv && d.getKollektiv())|(data.modsat && d.getModsat())|(data.mareridt && d.getMareridt())|(data.praktiserer && d.getPraktiserer())) {
                            DreamDTO dto = new DreamDTO(d.getId(), d.getIndhold(), d.getDagrest(), d.getDato());
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
                                                DreamDTO addme = new DreamDTO(d.getId(), d.getIndhold(), d.getDagrest(), d.getDato());
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
    }

    public boolean isAndOr() {
        return andOr;
    }

    public void setAndOr(boolean andOr) {
        this.andOr = andOr;
    }
}
