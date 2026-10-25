package fixit.dreams;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Røde bølgestreger under stavefejl i en almindelig TextArea, og forslag ved højreklik.
 * Kontrollerne står i FXML (TextArea og et tomt, musegennemsigtigt Pane oven på det i en
 * StackPane); opførslen ligger her, som med Tagfelt.
 *
 * TextArea kan ikke selv style enkelte ord, så stregerne tegnes i laget ovenpå. Placeringen
 * kommer fra TextAreaSkin.getCharacterBounds(), som er offentligt API og giver tegnets plads i
 * TextArea'ens egne koordinater med scroll og ombrydning regnet med. RichTextFX kunne have gjort
 * det hele, men den og dens fire afhængigheder er automatiske moduler, og dem tager jlink ikke.
 *
 * Selve tjekket kører på en baggrundstråd ~300 ms efter man holder op med at skrive. Ordet man
 * står og skriver på (markøren lige efter det) markeres ikke, før man er gået videre.
 */
class Stavefelt {

    private static final Duration PAUSE = Duration.millis(300);
    private static final int MAKS_FORSLAG = 5;

    /** Én tråd til alle Hunspell-opslag, så tjek og forslag aldrig står i kø bag FX-tråden. */
    private static final ExecutorService TRAAD = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "stavekontrol");
        t.setDaemon(true);
        return t;
    });

    private final TextArea felt;
    private final Pane lag;
    private final PauseTransition ventetid = new PauseTransition(PAUSE);
    private final ContextMenu menu = new ContextMenu();
    /**
     * Feltets egen menu fra før stavekontrollen, til højreklik der ikke rammer en stavefejl. Ny
     * drøm har en tom menu (= ingen menu); Rediger drøm har null, dvs. JavaFX' Klip/Kopiér/Sæt ind.
     */
    private final ContextMenu tidligereMenu;

    private Stavekontrol stavekontrol;
    /** "Ignorér" gælder kun til appen lukkes. */
    private final Set<String> ignorerede = new HashSet<>();

    private List<Stavekontrol.Fejl> fejl = List.of();
    /** Tæller op ved hver tekstændring, så et forældet tjek fra baggrunden kan kendes og smides væk. */
    private long version = 0;
    private boolean skalTegnes = false;
    private final Runnable efterLayout = this::tegnHvisNoedvendigt;

    private Color stregfarve = Color.RED;
    private Region fulgtBaggrund;

    Stavefelt(TextArea felt, Pane lag) {
        this.felt = felt;
        this.lag = lag;

        lag.setMouseTransparent(true);
        lag.setManaged(true);
        // Ellers vokser laget (og dermed StackPane'en) med stregerne.
        lag.setPrefSize(0, 0);
        lag.setMinSize(0, 0);

        menu.getStyleClass().add("stavemenu");
        tidligereMenu = felt.getContextMenu();
        felt.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, this::vedHoejreklik);

        ventetid.setOnFinished(e -> tjek());
        felt.textProperty().addListener((obs, gammel, ny) -> {
            version++;
            fejl = Stavekontrol.forskyd(fejl, gammel == null ? "" : gammel, ny == null ? "" : ny);
            tegnSenere();
            ventetid.playFromStart();
        });
        felt.scrollTopProperty().addListener((obs, o, n) -> tegnSenere());
        felt.scrollLeftProperty().addListener((obs, o, n) -> tegnSenere());
        felt.widthProperty().addListener((obs, o, n) -> tegnSenere());
        felt.heightProperty().addListener((obs, o, n) -> tegnSenere());
        felt.caretPositionProperty().addListener((obs, o, n) -> tegnSenere());
        felt.focusedProperty().addListener((obs, o, n) -> tegnSenere());

        // Skin'et (og dermed .content, der bærer temaets baggrund) findes først når feltet vises.
        felt.skinProperty().addListener((obs, o, n) -> foelgBaggrund());
        foelgBaggrund();

        // Tegnes efter layout, ellers er ombrydningen ikke regnet om endnu og stregerne står
        // hvor ordene var før.
        felt.sceneProperty().addListener((obs, gammel, ny) -> {
            if (ny != null) {
                ny.addPostLayoutPulseListener(efterLayout);
            }
            if (gammel != null) {
                gammel.removePostLayoutPulseListener(efterLayout);
            }
        });
        Scene scene = felt.getScene();
        if (scene != null) {
            scene.addPostLayoutPulseListener(efterLayout);
        }

        Stavekontrol.indlaes().thenAcceptAsync(s -> {
            stavekontrol = s;
            tjek();
        }, Platform::runLater).exceptionally(t -> {
            t.printStackTrace(); // uden ordbog er feltet bare et almindeligt tekstfelt
            return null;
        });
    }

    /**
     * Lytter på tekstfeltets faktiske baggrund (temaets -fx-textfelt-background på .content), så
     * stregfarven følger med når temaet skiftes eller forhåndsvises - uden at Stavefelt skal
     * kende til temaer.
     */
    private void foelgBaggrund() {
        if (felt.getSkin() == null || !(felt.lookup(".content") instanceof Region indhold)
                || indhold == fulgtBaggrund) {
            return;
        }
        fulgtBaggrund = indhold;
        indhold.backgroundProperty().addListener((obs, o, n) -> opdaterStregfarve());
        opdaterStregfarve();
    }

    private void opdaterStregfarve() {
        Background bg = fulgtBaggrund.getBackground();
        Color ny = Color.RED;
        if (bg != null && !bg.getFills().isEmpty() && bg.getFills().get(0).getFill() instanceof Color farve) {
            ny = stregfarve(farve);
        }
        if (!ny.equals(stregfarve)) {
            stregfarve = ny;
            tegnSenere();
        }
    }

    /** Lys og mørk rød til baggrunde hvor ren rød forsvinder (mørkerød hhv. lyserød). */
    private static final List<Color> ROEDE = List.of(Color.RED, Color.web("#ff6b6b"), Color.web("#b00000"));

    /**
     * Stregen er rød - det er det alle kender som "stavefejl" - så længe den kan ses. Først når
     * ingen af de røde har kontrast 3:1 til baggrunden (WCAG's krav til grafik), bliver den sort
     * eller hvid, alt efter hvad der står tydeligst. Det sker kun på baggrunde der selv er røde.
     */
    static Color stregfarve(Color baggrund) {
        for (Color roed : ROEDE) {
            if (kontrast(roed, baggrund) >= 3) {
                return roed;
            }
        }
        return kontrast(Color.BLACK, baggrund) >= kontrast(Color.WHITE, baggrund) ? Color.BLACK : Color.WHITE;
    }

    /** WCAG-kontrastforhold mellem to farver, fra 1 (ens) til 21 (sort/hvid). */
    static double kontrast(Color a, Color b) {
        double la = luminans(a), lb = luminans(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminans(Color c) {
        return 0.2126 * lineaer(c.getRed()) + 0.7152 * lineaer(c.getGreen()) + 0.0722 * lineaer(c.getBlue());
    }

    private static double lineaer(double kanal) {
        return kanal <= 0.03928 ? kanal / 12.92 : Math.pow((kanal + 0.055) / 1.055, 2.4);
    }

    /** Tjekker hele teksten forfra, fx efter at symbolerne har ændret sig. */
    void tjek() {
        if (stavekontrol == null) {
            return;
        }
        String tekst = felt.getText() == null ? "" : felt.getText();
        long tjekketVersion = version;
        // Egne ord og symboler læses her på FX-tråden; kun et øjebliksbillede sendes videre.
        // Egne ord hentes fra User hver gang, så ord hentet fra skyen tæller med det samme.
        User user = User.getInstance();
        Set<String> godkendte = new HashSet<>(user.getEgneOrd());
        godkendte.addAll(ignorerede);
        godkendte.addAll(Stavekontrol.ordFraSymboler(user.getCategories()));
        Stavekontrol s = stavekontrol;
        TRAAD.execute(() -> {
            List<Stavekontrol.Fejl> fundne = s.find(tekst, godkendte);
            Platform.runLater(() -> {
                if (version == tjekketVersion) {
                    fejl = fundne;
                    tegnSenere();
                }
            });
        });
    }

    private void tegnSenere() {
        skalTegnes = true;
        // Sørger for at der kommer en layout-puls, også når intet andet har ændret sig.
        felt.requestLayout();
    }

    private void tegnHvisNoedvendigt() {
        if (skalTegnes) {
            skalTegnes = false;
            tegn();
        }
    }

    private void tegn() {
        lag.getChildren().clear();
        if (!(felt.getSkin() instanceof TextAreaSkin skin) || fejl.isEmpty()) {
            return;
        }
        Node viewport = felt.lookup(".viewport");
        Bounds synligt = viewport != null
                ? lag.sceneToLocal(viewport.localToScene(viewport.getLayoutBounds()))
                : lag.sceneToLocal(felt.localToScene(felt.getLayoutBounds()));
        if (synligt == null) {
            return;
        }
        lag.setClip(new Rectangle(synligt.getMinX(), synligt.getMinY(), synligt.getWidth(), synligt.getHeight()));

        int markoer = felt.getCaretPosition();
        int laengde = felt.getLength();
        for (Stavekontrol.Fejl f : fejl) {
            if (f.slut() > laengde) {
                continue; // forældet - næste tjek retter det
            }
            if (felt.isFocused() && markoer == f.slut()) {
                continue; // ordet man står og skriver på
            }
            tegnStreg(skin, f, synligt);
        }
    }

    /** Én bølgestreg pr. linje ordet står på - et ord kan være brudt over to linjer. */
    private void tegnStreg(TextAreaSkin skin, Stavekontrol.Fejl f, Bounds synligt) {
        double venstre = Double.NaN, hoejre = 0, bund = 0, linjeTop = Double.NaN;
        for (int i = f.start(); i < f.slut(); i++) {
            Rectangle2D tegn = skin.getCharacterBounds(i);
            if (tegn == null) {
                return;
            }
            if (Double.isNaN(linjeTop) || Math.abs(tegn.getMinY() - linjeTop) > 1) {
                if (!Double.isNaN(venstre)) {
                    boelge(venstre, hoejre, bund, synligt);
                }
                linjeTop = tegn.getMinY();
                venstre = tegn.getMinX();
                bund = tegn.getMaxY();
            }
            hoejre = tegn.getMaxX();
        }
        if (!Double.isNaN(venstre)) {
            boelge(venstre, hoejre, bund, synligt);
        }
    }

    private void boelge(double venstre, double hoejre, double bund, Bounds synligt) {
        Point2D fra = lag.sceneToLocal(felt.localToScene(venstre, bund - 3));
        double bredde = hoejre - venstre;
        if (fra.getY() < synligt.getMinY() || fra.getY() > synligt.getMaxY() + 4) {
            return;
        }
        Polyline streg = new Polyline();
        for (double x = 0; x <= bredde; x += 2) {
            streg.getPoints().addAll(fra.getX() + x, fra.getY() + ((int) (x / 2) % 2 == 0 ? 0 : 1.5));
        }
        streg.setStroke(stregfarve);
        streg.setStrokeWidth(1);
        streg.setManaged(false);
        lag.getChildren().add(streg);
    }

    private void vedHoejreklik(ContextMenuEvent e) {
        Stavekontrol.Fejl ramt = null;
        if (felt.getSkin() instanceof TextAreaSkin skin) {
            for (Stavekontrol.Fejl f : fejl) {
                if (f.slut() > felt.getLength()) {
                    continue;
                }
                boolean truffet = e.isKeyboardTrigger()
                        ? felt.getCaretPosition() >= f.start() && felt.getCaretPosition() <= f.slut()
                        : rammer(skin, f, e.getX(), e.getY());
                if (truffet) {
                    ramt = f;
                    break;
                }
            }
        }
        if (ramt == null || stavekontrol == null) {
            // Som før stavekontrollen: feltets egen menu får lov. Er den null, viser TextArea'ens
            // behavior sin standardmenu; en tom menu betyder at der bevidst ikke skal være nogen.
            if (tidligereMenu != null && tidligereMenu.getItems().isEmpty()) {
                e.consume();
            }
            return;
        }
        // Forslagene vises herfra, og hændelsen standses i filteret, så hverken Control eller
        // behavior når at vise feltets egen menu oveni. Feltets contextMenu røres aldrig, så
        // dets egen menu (eller mangel på samme) er uændret alle andre steder.
        if (tidligereMenu != null) {
            tidligereMenu.hide();
        }
        byggMenu(ramt);
        menu.show(felt, e.getScreenX(), e.getScreenY());
        e.consume();
    }

    /**
     * Ligger punktet (TextArea-koordinater) på et af ordets tegn? Det tjekkes mod
     * getCharacterBounds - samme tal som stregerne tegnes efter - og ikke med skin.getIndex(),
     * for den regner i indholdets koordinater uden scroll: så snart der var scrollet, ramte et
     * højreklik et helt andet sted i teksten, og der kom ingen menu.
     */
    private static boolean rammer(TextAreaSkin skin, Stavekontrol.Fejl f, double x, double y) {
        for (int i = f.start(); i < f.slut(); i++) {
            Rectangle2D tegn = skin.getCharacterBounds(i);
            if (tegn != null && tegn.contains(x, y)) {
                return true;
            }
        }
        return false;
    }

    private void byggMenu(Stavekontrol.Fejl f) {
        menu.getItems().clear();
        // Op til ~¼ sekund for et ord Hunspell ikke kan gætte; det er til at leve med ved et højreklik.
        List<String> forslag = stavekontrol.forslag(f.ord());
        if (forslag.isEmpty()) {
            MenuItem ingen = new MenuItem("Ingen forslag");
            ingen.setDisable(true);
            menu.getItems().add(ingen);
        }
        for (String ord : forslag.subList(0, Math.min(MAKS_FORSLAG, forslag.size()))) {
            MenuItem punkt = new MenuItem(ord);
            punkt.setOnAction(a -> erstat(f, ord));
            menu.getItems().add(punkt);
        }
        MenuItem tilfoej = new MenuItem("Tilføj \"" + f.ord() + "\" til ordbogen");
        tilfoej.setOnAction(a -> tilfoejTilOrdbog(f.ord()));
        MenuItem ignorer = new MenuItem("Ignorér");
        ignorer.setOnAction(a -> {
            ignorerede.add(f.ord().toLowerCase(Locale.ROOT));
            fjernOgTjek(f.ord());
        });
        menu.getItems().addAll(new SeparatorMenuItem(), tilfoej, ignorer);
    }

    private void erstat(Stavekontrol.Fejl f, String ord) {
        // Kun hvis ordet stadig står der - ellers rettede man noget andet end man troede.
        if (f.slut() <= felt.getLength() && felt.getText(f.start(), f.slut()).equals(f.ord())) {
            felt.replaceText(f.start(), f.slut(), ord);
            felt.positionCaret(f.start() + ord.length());
        }
    }

    private void tilfoejTilOrdbog(String ord) {
        // I user.json, så ordet følger med i syncen til de andre maskiner (se UserDTO.egneOrd).
        User user = User.getInstance();
        if (user.tilføjEgetOrd(ord)) {
            IOutils.gemEgneOrd(user);
        }
        fjernOgTjek(ord);
    }

    /** Fjerner stregen med det samme i stedet for at vente på næste tjek. */
    private void fjernOgTjek(String ord) {
        List<Stavekontrol.Fejl> tilbage = new ArrayList<>();
        for (Stavekontrol.Fejl f : fejl) {
            if (!f.ord().equalsIgnoreCase(ord)) {
                tilbage.add(f);
            }
        }
        fejl = tilbage;
        tegnSenere();
        tjek();
    }
}
