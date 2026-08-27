package fixit.dreams;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

// Viser "Om appen" og "Hjælp".
//
// Teksterne er stadig almindelige txt-filer - de skal kunne læses og rettes i en editor uden
// værktøj - men de må bære et minimum af opmærkning, som her bliver til rigtig typografi:
//
//   # tekst      overskrift
//   ## tekst     underoverskrift
//   - tekst      punkt på en liste
//   ---          vandret streg
//   tom linje    afsnitsskift
//
// Farver og font hentes fra brugerens valgte tema og sættes DIREKTE på noderne, ikke via CSS.
// Det er med vilje: currentTema.css ligger i brugerens datamappe og kopieres kun derover hvis
// den ikke findes i forvejen (CSSUpdater.copyCssIfNotExists), så nye stilregler i resource-
// filen ville aldrig nå ud til nogen der har brugt appen før. Læser man temaet direkte,
// virker det for alle med det samme - også med et hjemmelavet tema.
public class TxtController {

    private static final double BRØDSTØRRELSE = 16;
    private static final double AFSNITSLUFT = 14;
    private static final double LUFT_FØR_OVERSKRIFT = 26;

    @FXML
    private ScrollPane txtviewScroll;

    @FXML
    private VBox txtviewIndhold;

    public void setType(String type) {
        Tema tema = User.getInstance().getForetrukneTema();

        String farve = cssFarve(tema.getBaggrundD());
        String baggrund = "-fx-background-color: " + farve + ";";
        txtviewIndhold.setStyle(baggrund);
        // ScrollPane maler sin egen flade bag indholdet - uden dette ville der stå en grå
        // ramme rundt om teksten i mørke temaer.
        txtviewScroll.setStyle("-fx-background: " + farve + "; " + baggrund);

        byg(IOutils.loadOmHelpTxt(type), tema);
    }

    private void byg(String tekst, Tema tema) {
        txtviewIndhold.getChildren().clear();

        for (String rå : tekst.split("\n")) {
            String linje = rå.strip();
            if (linje.isEmpty()) {
                continue; // luften mellem afsnit laves med margener, ikke med tomme linjer
            }

            if (linje.equals("---")) {
                tilføjStreg(tema);
            } else if (linje.startsWith("## ")) {
                tilføjOverskrift(linje.substring(3), 20, tema);
            } else if (linje.startsWith("# ")) {
                tilføjOverskrift(linje.substring(2), 26, tema);
            } else if (linje.startsWith("- ")) {
                tilføjPunkt(linje.substring(2), tema);
            } else {
                tilføjAfsnit(linje, tema);
            }
        }
    }

    private void tilføjOverskrift(String tekst, double størrelse, Tema tema) {
        TextFlow flow = flow(tekst, tema.getFont(), størrelse, true, tema.getTekstC());
        // Den allerførste overskrift skal ikke skubbes ned - der er allerede luft fra polstringen.
        double over = txtviewIndhold.getChildren().isEmpty() ? 0 : LUFT_FØR_OVERSKRIFT;
        VBox.setMargin(flow, new Insets(over, 0, 10, 0));
        txtviewIndhold.getChildren().add(flow);
    }

    private void tilføjAfsnit(String tekst, Tema tema) {
        TextFlow flow = flow(tekst, tema.getFont(), BRØDSTØRRELSE, false, tema.getTekstA());
        VBox.setMargin(flow, new Insets(0, 0, AFSNITSLUFT, 0));
        txtviewIndhold.getChildren().add(flow);
    }

    // Prikken sidder i sin egen node ved siden af teksten, ikke inde i den. Så rykker anden og
    // tredje linje af et langt punkt ind under den første i stedet for ud i venstre margen.
    private void tilføjPunkt(String tekst, Tema tema) {
        Text prik = new Text("•");
        prik.setFont(Font.font(tema.getFont(), BRØDSTØRRELSE));
        prik.setFill(tema.getTekstC());

        TextFlow krop = flow(tekst, tema.getFont(), BRØDSTØRRELSE, false, tema.getTekstA());
        HBox.setHgrow(krop, Priority.ALWAYS);

        HBox punkt = new HBox(10, prik, krop);
        punkt.setPadding(new Insets(0, 0, 0, 12));
        VBox.setMargin(punkt, new Insets(0, 0, 6, 0));
        txtviewIndhold.getChildren().add(punkt);
    }

    private void tilføjStreg(Tema tema) {
        Region streg = new Region();
        streg.setMinHeight(1);
        streg.setPrefHeight(1);
        streg.setMaxHeight(1);
        streg.setStyle("-fx-background-color: " + cssFarve(tema.getKant()) + ";");
        VBox.setMargin(streg, new Insets(8, 0, AFSNITSLUFT + 8, 0));
        txtviewIndhold.getChildren().add(streg);
    }

    // Color.toString() giver "0x333333ff", og dét kan JavaFX' CSS-parser ikke læse - den
    // springer reglen over uden at fejle, så baggrunden bare bliver stående lys. Skal skrives
    // som #RRGGBB.
    private String cssFarve(Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    private TextFlow flow(String tekst, String fontnavn, double størrelse, boolean fed, Color farve) {
        Text t = new Text(tekst);
        t.setFont(fed ? Font.font(fontnavn, FontWeight.BOLD, størrelse) : Font.font(fontnavn, størrelse));
        t.setFill(farve);

        TextFlow flow = new TextFlow(t);
        flow.setLineSpacing(5); // luft mellem linjerne i samme afsnit - gør lange tekster læselige
        return flow;
    }

    @FXML
    private void closePopup(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close(); // Lukker popup-vinduet
    }
}
