package fixit.dreams;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gennemgangen af hvad Symbolgenkendelse fandt. Intet flueben bliver sat før brugeren har
 * sagt god for det her - det er hele grunden til at dialogen findes.
 *
 * De sikre fund står som afkrydsningsfelter, alle sat fra start: de er fundet præcist i
 * teksten, så det normale svar er ja. De tvetydige står som radioknapper med "Ingen af dem"
 * valgt fra start: dér ved appen netop ikke hvad der er rigtigt, og tavshed skal give det
 * uskadelige svar. Undtagelsen er Tvetydighed.forvalgt: siger teksten kun "Anna", og registret
 * har både "Anna" og "Anna Fasting", står "Anna" valgt fra start.
 *
 * Kvaliteterne (Lucid, Praktiserer...) står ligesom de sikre fund med flueben fra start, med
 * det stykke tekst der udløste dem. Kalderen sætter så de binære afkrydsningsfelter.
 *
 * Nye navne - navne i teksten som ikke står i Personer endnu - står som afkrydsningsfelter UDEN
 * flueben. Godkendes et, opretter kalderen det som nyt symbol; se HovedmenuController.
 */
final class Symbolforslagsdialog {

    private Symbolforslagsdialog() {}

    /**
     * Viser dialogen og venter. Returnerer de symboler brugeren godkendte, eller tom hvis der
     * blev annulleret. Forskellen betyder noget: et nyt navn uden flueben er et nej, som
     * kalderen husker (se HovedmenuController) - men kun når brugeren faktisk trykkede
     * "Sæt flueben". Et annulleret vindue har ikke sagt noget som helst.
     *
     * @param kategoriNavne kategori-id til det navn brugeren ser. Symbolgenkendelse arbejder på
     *                      id'er, for et navn kan brugeren nå at ændre (se CategoryDTO).
     */
    static Optional<List<Symbolgenkendelse.Fund>> vis(Symbolgenkendelse.Resultat forslag,
                                                      Map<String, String> kategoriNavne,
                                                      Window ejer) {
        return byg(forslag, kategoriNavne, ejer).showAndWait();
    }

    /**
     * Bygger dialogen uden at vise den. Skilt fra vis() fordi showAndWait() blokerer: sådan her
     * kan selve opbygningen - grupperingen, radioknapperne og resultatomregneren - efterprøves
     * uden at noget skal klikkes.
     */
    static Dialog<List<Symbolgenkendelse.Fund>> byg(Symbolgenkendelse.Resultat forslag,
                                                    Map<String, String> kategoriNavne,
                                                    Window ejer) {
        Dialog<List<Symbolgenkendelse.Fund>> dialog = new Dialog<>();
        dialog.setTitle("Symboler i teksten");
        dialog.setHeaderText(overskrift(forslag));
        if (ejer != null) {
            dialog.initOwner(ejer);
        }

        VBox indhold = new VBox(10);
        indhold.setStyle("-fx-padding: 10;");

        LinkedHashMap<CheckBox, Symbolgenkendelse.Fund> afkrydsninger = new LinkedHashMap<>();
        List<ToggleGroup> grupper = new ArrayList<>();

        if (!forslag.sikre().isEmpty()) {
            indhold.getChildren().add(fed("Fundet i teksten"));

            // sikre er sorteret på kategori-id, så ens kategorier står i forlængelse af hinanden
            // og overskriften kan sættes når id'et skifter.
            String forrigeKategori = null;
            for (Symbolgenkendelse.Fund fund : forslag.sikre()) {
                if (!fund.kategoriId().equals(forrigeKategori)) {
                    Label kategori = new Label(navn(kategoriNavne, fund.kategoriId()));
                    kategori.setStyle("-fx-opacity: 0.7;");
                    VBox.setMargin(kategori, new javafx.geometry.Insets(5, 0, 0, 0));
                    indhold.getChildren().add(kategori);
                    forrigeKategori = fund.kategoriId();
                }
                CheckBox felt = new CheckBox(fund.symbol());
                felt.setSelected(true);
                VBox.setMargin(felt, new javafx.geometry.Insets(0, 0, 0, 15));
                afkrydsninger.put(felt, fund);
                indhold.getChildren().add(felt);
            }
        }

        if (!forslag.kvaliteter().isEmpty()) {
            if (!forslag.sikre().isEmpty()) {
                indhold.getChildren().add(new Separator());
            }
            indhold.getChildren().add(fed("Kvaliteter"));
            for (Symbolgenkendelse.Fund fund : forslag.kvaliteter()) {
                // Med flueben fra start som de sikre symboler - Kvalitetsgenkendelse kræver både
                // "jeg" og intet forbehold. Stedet i teksten står med, så et forkert træf kan ses.
                CheckBox felt = new CheckBox(fund.symbol() + "  (“" + fund.fundetSom() + "”)");
                felt.setSelected(true);
                felt.setWrapText(true);
                VBox.setMargin(felt, new javafx.geometry.Insets(0, 0, 0, 15));
                afkrydsninger.put(felt, fund);
                indhold.getChildren().add(felt);
            }
        }

        if (!forslag.tvetydige().isEmpty()) {
            if (!forslag.sikre().isEmpty() || !forslag.kvaliteter().isEmpty()) {
                indhold.getChildren().add(new Separator());
            }
            indhold.getChildren().add(fed("Her kan appen ikke vide hvad du mener"));

            for (Symbolgenkendelse.Tvetydighed tvivl : forslag.tvetydige()) {
                Label spoergsmaal = new Label("Teksten siger “" + tvivl.ord() + "” - hvad menes der?");
                spoergsmaal.setWrapText(true);
                VBox.setMargin(spoergsmaal, new javafx.geometry.Insets(5, 0, 0, 0));
                indhold.getChildren().add(spoergsmaal);

                ToggleGroup gruppe = new ToggleGroup();
                grupper.add(gruppe);
                for (Symbolgenkendelse.Fund kandidat : tvivl.kandidater()) {
                    RadioButton valg = new RadioButton(
                            kandidat.symbol() + "  (" + navn(kategoriNavne, kandidat.kategoriId()) + ")");
                    valg.setToggleGroup(gruppe);
                    valg.setUserData(kandidat);
                    valg.setSelected(kandidat.equals(tvivl.forvalgt()));
                    VBox.setMargin(valg, new javafx.geometry.Insets(0, 0, 0, 15));
                    indhold.getChildren().add(valg);
                }
                RadioButton ingen = new RadioButton("Ingen af dem");
                ingen.setToggleGroup(gruppe);
                ingen.setSelected(tvivl.forvalgt() == null); // tavshed må aldrig sætte et gæt
                VBox.setMargin(ingen, new javafx.geometry.Insets(0, 0, 0, 15));
                indhold.getChildren().add(ingen);
            }
        }

        if (!forslag.nyeNavne().isEmpty()) {
            if (!forslag.sikre().isEmpty() || !forslag.kvaliteter().isEmpty() || !forslag.tvetydige().isEmpty()) {
                indhold.getChildren().add(new Separator());
            }
            indhold.getChildren().add(fed("Nye navne"));
            String kategori = navn(kategoriNavne, forslag.nyeNavne().get(0).kategoriId());
            Label forklaring = new Label("Står ikke i " + kategori + " endnu. Sæt flueben ved dem "
                    + "du vil oprette som nye symboler dér - dem uden flueben foreslås ikke igen.");
            forklaring.setWrapText(true);
            forklaring.setStyle("-fx-opacity: 0.7;");
            indhold.getChildren().add(forklaring);

            for (Symbolgenkendelse.Fund fund : forslag.nyeNavne()) {
                CheckBox felt = new CheckBox(fund.symbol());
                // Uden flueben fra start, modsat de sikre fund: et nyt symbol bliver liggende i
                // registret og dukker op i hver eneste drøm fremover, og "Paris" eller "Gud" ser
                // ud som et navn uden at være en person.
                felt.setSelected(false);
                VBox.setMargin(felt, new javafx.geometry.Insets(0, 0, 0, 15));
                afkrydsninger.put(felt, fund);
                indhold.getChildren().add(felt);
            }
        }

        // En lang drøm kan sagtens give tredive fund, og dialogen må ikke blive højere end skærmen.
        ScrollPane rulle = new ScrollPane(indhold);
        rulle.setFitToWidth(true);
        rulle.setPrefViewportHeight(400);
        rulle.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        dialog.getDialogPane().setContent(rulle);
        dialog.getDialogPane().setPrefWidth(460);

        ButtonType saetBtn = new ButtonType("Sæt flueben", ButtonBar.ButtonData.OK_DONE);
        ButtonType annullerBtn = new ButtonType("Annuller", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(saetBtn, annullerBtn);

        klaedPaa(dialog);

        dialog.setResultConverter(knap -> {
            if (knap != saetBtn) {
                return null; // showAndWait() giver så Optional.empty()
            }
            List<Symbolgenkendelse.Fund> valgte = new ArrayList<>();
            for (Map.Entry<CheckBox, Symbolgenkendelse.Fund> post : afkrydsninger.entrySet()) {
                if (post.getKey().isSelected()) {
                    valgte.add(post.getValue());
                }
            }
            for (ToggleGroup gruppe : grupper) {
                Toggle valgt = gruppe.getSelectedToggle();
                if (valgt != null && valgt.getUserData() instanceof Symbolgenkendelse.Fund fund) {
                    valgte.add(fund);
                }
            }
            return valgte;
        });

        return dialog;
    }

    private static String overskrift(Symbolgenkendelse.Resultat forslag) {
        // En kvalitet er et symbol i Kvaliteter, og tælles med som et fundet symbol.
        int sikre = forslag.sikre().size() + forslag.kvaliteter().size();
        int tvivl = forslag.tvetydige().size();
        int nye = forslag.nyeNavne().size();
        if (tvivl == 0 && nye == 0) {
            return sikre == 1 ? "Ét symbol fundet i drømmeteksten."
                    : sikre + " symboler fundet i drømmeteksten.";
        }
        if (sikre == 0 && nye == 0) {
            return tvivl == 1 ? "Ét ord i teksten kan betyde flere symboler."
                    : tvivl + " ord i teksten kan betyde flere symboler.";
        }
        if (sikre == 0 && tvivl == 0) {
            return nye == 1 ? "Ét nyt navn i drømmeteksten."
                    : nye + " nye navne i drømmeteksten.";
        }
        List<String> dele = new ArrayList<>();
        if (sikre > 0) {
            dele.add(sikre == 1 ? "ét symbol fundet" : sikre + " symboler fundet");
        }
        if (tvivl > 0) {
            dele.add(tvivl == 1 ? "ét ord appen ikke kan afgøre" : tvivl + " ord appen ikke kan afgøre");
        }
        if (nye > 0) {
            dele.add(nye == 1 ? "ét nyt navn" : nye + " nye navne");
        }
        String samlet = dele.size() == 2
                ? dele.get(0) + " og " + dele.get(1)
                : dele.get(0) + ", " + dele.get(1) + " og " + dele.get(2);
        return Character.toUpperCase(samlet.charAt(0)) + samlet.substring(1) + ".";
    }

    /**
     * Temaet på dialogen. Ingen Alert i appen er temasat i dag, så uden det her ville vinduet
     * stå med modenas hvide baggrund midt i et mørkt tema. Stilarket lægges på DialogPane'et,
     * som er det nærmeste denne slags vindue har til en scene vi selv ejer.
     */
    private static void klaedPaa(Dialog<?> dialog) {
        try {
            Path cssPath = AppPaths.APP_DATA_PATH.resolve("currentTema.css");
            dialog.getDialogPane().getStylesheets().add(cssPath.toFile().toURI().toString());
            // Overskrift, scrollbar og radioknapper - se kommentaren øverst i filen.
            dialog.getDialogPane().getStylesheets().add(
                    Symbolforslagsdialog.class.getResource("symbolforslag.css").toExternalForm());
        } catch (RuntimeException e) {
            // Et manglende stilark må ikke koste brugeren dialogen - så står den bare umalet.
            e.printStackTrace();
        }
        try {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(Symbolforslagsdialog.class.getResourceAsStream("/moona.png")));
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    private static Label fed(String tekst) {
        Label label = new Label(tekst);
        label.setStyle("-fx-font-weight: bold;");
        label.setWrapText(true);
        return label;
    }

    private static String navn(Map<String, String> kategoriNavne, String kategoriId) {
        String navn = kategoriNavne.get(kategoriId);
        return (navn != null && !navn.isBlank()) ? navn : kategoriId;
    }
}
