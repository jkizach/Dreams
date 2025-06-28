package fixit.dreams;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.util.Comparator;

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
        dreamDTOs.add(new DreamDTO(dream.getId(), dream.getIndhold(), dream.getDagrest(), dream.getDato())); // UUID med
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
                    d.setDato(user.getDream(id).getDato());
                    break;
                }
            }
        }
        user.setDreamEdited("tom");
    }

    public void deleteDream(String id) {
        user.deleteDream(id);
    }

    public Dream getDream(String id) {
        return user.getDream(id);
    }

    private void refreshDreamList() {
        dreamDTOs.clear();
        for (Dream dream : user.getDreams().values()) {
            dreamDTOs.add(new DreamDTO(dream.getId(), dream.getIndhold(), dream.getDagrest(), dream.getDato()));
        }
        sortDreamsByDate();
    }

    public void refreshDreamList(LocalDate fra, LocalDate til) {
        dreamDTOs.clear();
        for (Dream dream : user.getDreams().values()) {
            if (isInRange(dream.getDato(), fra, til)) {
                dreamDTOs.add(new DreamDTO(dream.getId(), dream.getIndhold(), dream.getDagrest(), dream.getDato()));
            }
        }
        sortDreamsByDate();
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
        user.addCategory(kategoriNavn);
        kategorier.clear();
        kategorier.addAll(user.getKategoriLabels());
    }

    public String renameKategori(String nytNavn, String gammeltNavn) {
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
                }
            }
        }
        user.refreshKategoriLabels();
        user.genberegnStatsPlease();
        refreshDreamList();
        return "Navn ændret!";
    }

    public Boolean okToAddNewUserDefinedCat() {
        return user.getCategories().size() <= 8;
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
                    cdto.symbols.remove(symbolet);
                    break;
                }
            }
        }
        refreshDreamList();
        user.skalStatsGenberegnes.set(true);
    }

    public void setVisAdvarsel(boolean b) {
        user.setVisAdvarsel(b);
        user.skalStatsGenberegnes.set(true);
    }

    public void setVisKollektiv(boolean b) {
        user.setVisKollektiv(b);
        user.skalStatsGenberegnes.set(true);
    }

    public boolean isVisAdvarsel() {
        return user.isVisAdvarsel();
    }

    public boolean isVisKollektiv() {
        return user.isVisKollektiv();
    }

    public LocalDate getFirstDreamDate() {
        LocalDate firstDate = LocalDate.now();
        for (Dream d : user.getDreams().values()) {
            LocalDate date = d.getDato();
            if (firstDate.isAfter(date)) {
                firstDate = date;
            }
        }
        return firstDate;
    }

    private boolean isInRange(LocalDate date, LocalDate start, LocalDate end) {
        return (date.isEqual(start) || date.isAfter(start)) && (date.isEqual(end) || date.isBefore(end));
    }
}

