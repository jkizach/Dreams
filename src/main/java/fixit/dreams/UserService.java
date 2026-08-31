package fixit.dreams;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

public class UserService extends ServiceMother {
    private Tema tempTema;
    private Tema currentTema;
    private ObservableList<DreamDTO> dreamDTOs = FXCollections.observableArrayList();

    public UserService(User user) {
        super(user);
        // Initialiser dreamDTOs
        refreshDreamList();

        populateTemaer();
        currentTema = getTema();
    }

    public void addDream(DreamData dreamData) {
        Dream dream = new Dream(dreamData);
        user.addDream(dream);
        dreamDTOs.add(new DreamDTO(dream.getId(), dream.getIndhold(), dream.getDagrest(), dream.getTolkning(), dream.getDato())); // UUID med
        sortDreamsByDate();
    }

    public ObservableList<DreamDTO> getDreamsForDisplay() {
        sortDreamsByDate();
        return dreamDTOs;
    }

    public void updateDreamDTO() {
        String id = user.getDreamEdited();
        if (!id.equals("tom")) {
            for (DreamDTO d : dreamDTOs) {
                if (d.getId().equals(id)) {
                    d.setIndhold(user.getDream(id).getIndhold());
                    d.setDagrest(user.getDream(id).getDagrest());
                    d.setTolkning(user.getDream(id).getTolkning());
                    d.setDato(user.getDream(id).getDato());
                    break;
                }
            }
        }
        user.setDreamEdited("tom");
    }

    public void deleteDream(String id) {
        user.deleteDream(id);

        // Sletningen skrives til disk MED DET SAMME - ikke først ved appluk som drømmene selv.
        // Gik appen ned inden da, ville skyen aldrig få besked, og drømmen ville blive hentet
        // ned igen ved næste synkronisering. Det er præcis den genopstandelse gravstenene findes for.
        LinkedHashMap<String, Instant> slettede = IOutils.loadDeletedDreams();
        slettede.put(id, Instant.now());
        IOutils.saveDeletedDreams(slettede);
    }

    public Dream getDream(String id) {
        return user.getDream(id);
    }

    private void refreshDreamList() {
        refreshDreamList(null, null); // uden datoer: hele listen
    }

    // Listen bygges færdig ved siden af og lægges ind i ét hug. Forskellen fra et clear()
    // efterfulgt af add() er ikke kosmetisk pedanteri: dreamDTOs ER den liste ListView'et
    // viser, så en tømning kan ses. Efter et pull fra skyen rev listen sig selv ned og op
    // igen for øjnene af brugeren, og "Antal drømme" gik et øjeblik i nul. setAll giver
    // ListView'et én udskiftning i stedet.
    public void refreshDreamList(LocalDate fra, LocalDate til) {
        List<DreamDTO> nye = new ArrayList<>();
        for (Dream dream : user.getDreams().values()) {
            if (fra == null || til == null || isInRange(dream.getDato(), fra, til)) {
                nye.add(new DreamDTO(dream.getId(), dream.getIndhold(), dream.getDagrest(), dream.getTolkning(), dream.getDato()));
            }
        }
        nye.sort(Comparator.comparing(DreamDTO::getDato).reversed()); // Nyeste først
        dreamDTOs.setAll(nye);
    }

    private void sortDreamsByDate() {
        FXCollections.sort(dreamDTOs, Comparator.comparing(DreamDTO::getDato).reversed()); // Nyeste først
    }

    public Tema getTema() {
        return(user.getForetrukneTema());
    }

    public Tema getTema(String temaNavn) {
        return (user.getTemaer().get(temaNavn));
    }

    public ObservableList<String> getTemaerForDisplay() {
        return temaer;
    }

    public String getTemaNavn() {
        return (user.getForetrukneTemaNavn());
    }

    public Tema getCurrentTema() {
        return currentTema;
    }

    public Boolean checkTemaErEns(TemaDTO data) {
        if (!data.baggrundA.equals(currentTema.getBaggrundA())||
                !data.baggrundB.equals(currentTema.getBaggrundB())||
                !data.baggrundC.equals(currentTema.getBaggrundC())||
                !data.baggrundD.equals(currentTema.getBaggrundD())||
                !data.tekstA.equals(currentTema.getTekstA())||
                !data.tekstB.equals(currentTema.getTekstB())||
                !data.tekstC.equals(currentTema.getTekstC())||
                !data.kant.equals(currentTema.getKant())||
                !data.font.equals(currentTema.getFont())) {
            return false;
        };
        return true;
    }

    public void setTempTema(TemaDTO tempTema) {
        CSSUpdater.updateCSSVariables(tempTema.getTemaForCSSUpdater(),true);
    }
    public void setCurrentTema(Tema nytTema) {
        CSSUpdater.updateCSSVariables(nytTema.getTemaForCSSUpdater(),false);
    }

    public void setForetrukneTema(String temaNavn) {
        user.setForetrukneTema(temaNavn);
        currentTema = getTema();
    }

    public void addNytTema(TemaDTO data, String temaNavn) {
        Tema nytTema = new Tema(data, temaNavn);
        user.addNytTema(nytTema);
        setForetrukneTema(temaNavn);
        temaer.add(temaNavn);
    }

    public void addNytSymbol(String kategorien, String symbolet) {
        // Hvordan håndterer jeg bruger-definerede kategorier??? Sådan her:
        for (Category c : user.getCategories()) {
            if (c.getName().equals(kategorien)) {
                // Med stort hvis person, ellers to-lower?
                if (kategorien.equals("Personer")) {
                    symbolet = symbolet.substring(0, 1).toUpperCase() + symbolet.substring(1);
                } else {
                    symbolet = symbolet.toLowerCase();
                }
                c.addSymbol(symbolet);
                c.updateAllCCBs();
                break;
            }
        }
        refreshKategoriLists();
    }

    public void addNyKategori(String kategoriNavn) {
        char first = kategoriNavn.charAt(0);
        if (Character.isLowerCase(first)) {
            kategoriNavn = kategoriNavn.substring(0, 1).toUpperCase() + kategoriNavn.substring(1);
        }
        for (Category c : user.getCategories()) {
            if (c.getName().equals(kategoriNavn)) {
                return;
            }
        }
        user.addCategory(kategoriNavn);
        kategorier.clear();
        kategorier.addAll(user.getKategoriLabels());
    }

    public String renameKategori(String nytNavn, String gammeltNavn) {
        if (gammeltNavn.equals("Forløb")) {
            return "Kan ikke omdøbes!";
        }
        char first = nytNavn.charAt(0);
        if (Character.isLowerCase(first)) {
            nytNavn = nytNavn.substring(0, 1).toUpperCase() + nytNavn.substring(1);
        }
        for (Category c : user.getCategories()) {
            if (c.getName().equals(nytNavn)) {
                return "Navnet findes allerede!";
            }
        }
        for (Category c : user.getCategories()) {
            if (c.getName().equals(gammeltNavn)) {
                c.setName(nytNavn);
                c.updateAllCCBs();
            }
        }
        // Loop gennem alle drømmene og ændr kategorinavnet!
        for (Dream d : user.getDreams().values()) {
            for (CategoryDTO dto : d.getCategories()) {
                if (dto.name.equals(gammeltNavn)) {
                    dto.name = nytNavn;
                    d.touch();
                }
            }
        }
        user.refreshKategoriLabels();
        user.genberegnStatsPlease();
        refreshDreamList();
        return "Navn ændret!";
    }

    public Boolean okToAddNewUserDefinedCat() {
        return user.getUiCategories().size() <= 8;
    }

    public void fjernSymbol(String kategorien, String symbolet) {
        for (Category c : user.getCategories()) {
            if (c.getName().equals(kategorien)) {
                if (c.getSymbols().contains(symbolet)) {
                    c.removeSymbol(symbolet);
                    c.updateAllCCBs();
                    break;
                }
            }
        }
        // Fjern symbolet fra alle drømme!
        for (Dream d : user.getDreams().values()) {
            for (CategoryDTO cdto : d.getCategories()) {
                if (cdto.name.equals(kategorien)) {
                    if (cdto.symbols.remove(symbolet)) {
                        d.touch();
                    }
                    break;
                }
            }
        }
        refreshDreamList();
        user.genberegnStatsPlease();
    }

    public void setVisAdvarsel(boolean b) {
        user.setVisAdvarsel(b);
        user.genberegnStatsPlease();
    }

    public void setVisKollektiv(boolean b) {
        user.setVisKollektiv(b);
        user.genberegnStatsPlease();
    }

    public void setVisHolografisk(boolean b) {
        user.setVisHolografisk(b);
        user.genberegnStatsPlease();
    }

    public boolean isVisAdvarsel() {
        return user.isVisAdvarsel();
    }

    public boolean isVisKollektiv() {
        return user.isVisKollektiv();
    }

    public boolean isVisHolografisk() {
        return user.isVisHolografisk();
    }

    public LocalDate getStartDate() {
        return user.getStartFromThisDate();
    }

    public LocalDate getFirstDreamDate() {
        return user.getFirstDreamDate();
    }

    public boolean harDrømme() {
        return !user.getDreams().isEmpty();
    }

    public void setStartDate(LocalDate startDate) {
        user.setStartFromThisDate(startDate);
        user.genberegnStatsPlease();
    }
}

