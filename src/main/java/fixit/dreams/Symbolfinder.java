package fixit.dreams;

import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/**
 * "Find symboler i drømmen": gennemgår drømmeteksten for symboler brugeren allerede har i
 * registret, og sætter fluebenene på det hun godkender. Intet sker uden om dialogen - se
 * Symbolforslagsdialog.
 *
 * Bruges af både Ny drøm og Rediger drøm, så de to knapper ikke kan nå at opføre sig
 * forskelligt. Det eneste der er forskelligt, er hvor fluebenene står: Ny drøms bokse er
 * registreret i Category (afkrydsIDreamCCBs), Rediger drøm har sine egne. Derfor tager find()
 * imod en afkrydsning i stedet for selv at kende kontrollerne.
 */
final class Symbolfinder {

    private Symbolfinder() {}

    /**
     * @param kvalitetsfelter de binære flueben der kan foreslås, nøglet på Kvalitetsgenkendelse's
     *                        navne. Kun synlige og ikke allerede afkrydsede bliver foreslået -
     *                        Holografisk kan være slået fra under Indstillinger.
     * @param afkryds         sætter fluebenet ved et symbol i vinduets boks for kategorien. Et
     *                        nyoprettet navn står måske ikke i boksen endnu; det må den klare.
     */
    static void find(String tekst, Map<String, CheckBox> kvalitetsfelter, Window ejer,
                     BiConsumer<Category, String> afkryds) {
        if (tekst == null || tekst.isBlank()) {
            visBesked(ejer, "Der er ingen drømmetekst at kigge i endnu.", null);
            return;
        }
        User user = User.getInstance();

        // Kategori-id til symboler, i kategoriernes egen rækkefølge. Tags springes over af
        // samme grund som i loadCCBs: de har intet CheckComboBox at sætte et flueben i.
        LinkedHashMap<String, Collection<String>> symbolerPrKategori = new LinkedHashMap<>();
        LinkedHashMap<String, String> kategoriNavne = new LinkedHashMap<>();
        LinkedHashMap<String, Category> kategoriPrId = new LinkedHashMap<>();
        for (Category c : user.getUiCategories()) {
            if (Tag.ID.equals(c.getId())) {
                continue;
            }
            symbolerPrKategori.put(c.getId(), c.getSymbols());
            kategoriNavne.put(c.getId(), c.getName());
            kategoriPrId.put(c.getId(), c);
        }

        List<String> tilladteKvaliteter = new ArrayList<>();
        kvalitetsfelter.forEach((navn, felt) -> {
            if (felt.isVisible() && !felt.isSelected()) {
                tilladteKvaliteter.add(navn);
            }
        });

        Symbolgenkendelse.Resultat forslag =
                Symbolgenkendelse.find(tekst, symbolerPrKategori, Category.ID_PERSONER, user.getIkkeNavne())
                        .medKvaliteter(Kvalitetsgenkendelse.find(tekst, tilladteKvaliteter,
                                droemmerensKoen(user)));
        if (forslag.erTom()) {
            visBesked(ejer, "Ingen af dine symboler blev fundet i teksten.",
                    "Symbolerne findes som de står i dine kategorier, også bøjet - \"hunden\" "
                            + "finder hund. Står ordet slet ikke i teksten, skal symbolet sættes i hånden.");
            return;
        }

        Optional<List<Symbolgenkendelse.Fund>> svar = Symbolforslagsdialog.vis(forslag, kategoriNavne, ejer);
        if (svar.isEmpty()) {
            return; // annulleret - intet sat, og intet navn afvist
        }
        List<Symbolgenkendelse.Fund> godkendte = svar.get();

        // Nye navne brugeren lod stå uden flueben, er et nej: de skal ikke foreslås igen.
        List<String> afviste = new ArrayList<>();
        for (Symbolgenkendelse.Fund fund : forslag.nyeNavne()) {
            if (!godkendte.contains(fund)) {
                afviste.add(Symbolgenkendelse.ikkeNavnNoegle(fund.symbol()));
            }
        }
        afvisNavne(user, afviste);

        // Godkendte nye navne oprettes først, så de står i boksen når fluebenet skal sættes.
        // Symbolerne gemmes sammen med resten af kategorierne ved appluk, som når man tilføjer
        // et symbol under Indstillinger.
        LinkedHashMap<Category, List<String>> nyeSymboler = new LinkedHashMap<>();
        for (Symbolgenkendelse.Fund fund : godkendte) {
            Category kategori = kategoriPrId.get(fund.kategoriId());
            if (kategori != null && !kategori.getSymbols().contains(fund.symbol())) {
                nyeSymboler.computeIfAbsent(kategori, k -> new ArrayList<>()).add(fund.symbol());
            }
        }
        nyeSymboler.forEach(Category::addSymbolsBevarFlueben);

        for (Symbolgenkendelse.Fund fund : godkendte) {
            if (Category.ID_KVALITETER.equals(fund.kategoriId())) {
                CheckBox felt = kvalitetsfelter.get(fund.symbol());
                if (felt != null) {
                    felt.setSelected(true);
                }
                continue;
            }
            Category kategori = kategoriPrId.get(fund.kategoriId());
            if (kategori != null) {
                afkryds.accept(kategori, fund.symbol());
            }
        }
    }

    /** Husker navnene som ikke-personer og gemmer straks, hvis mindst ét var nyt. */
    static void afvisNavne(User user, Collection<String> navne) {
        if (user.tilføjIkkeNavne(navne)) {
            IOutils.gemIkkeNavne(user);
        }
    }

    // Drømmerens køn, udledt af drømmene med flueben i Modsatkønnet - se Kvalitetsgenkendelse.
    private static Kvalitetsgenkendelse.Koen droemmerensKoen(User user) {
        List<String> tekster = new ArrayList<>();
        for (Dream d : user.getDreams().values()) {
            if (d.hasFlag(Kvalitetsgenkendelse.MODSATKOENNET)) {
                tekster.add(d.getIndhold());
            }
        }
        return Kvalitetsgenkendelse.udledDroemmerensKoen(tekster);
    }

    // Med ejer, så beskeden står oven på Rediger drøm, der selv er modalt.
    private static void visBesked(Window ejer, String overskrift, String uddybning) {
        Alert besked = new Alert(Alert.AlertType.INFORMATION);
        besked.initOwner(ejer);
        besked.setTitle("Symboler i teksten");
        besked.setHeaderText(overskrift);
        besked.setContentText(uddybning);
        besked.showAndWait();
    }
}
