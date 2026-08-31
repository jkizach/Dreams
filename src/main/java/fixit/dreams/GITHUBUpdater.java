package fixit.dreams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

// Opdateringstjek mod GitHub-releases, med download og opstart af installeren.
//
// Kaldet gik tidligere altid galt i den installerede app og faldt tilbage på et C#-program,
// app/updater.exe. Årsagen var ikke Java, men at jlink-imaget manglede SunEC-provideren; se
// "requires jdk.crypto.ec" i module-info.java. Fallbacken er fjernet, fordi Java-kaldet nu
// virker direkte fra MSI'en - og alt herunder er derfor også ren Java uden en anden binær.
//
// Hvorfor vi henter filen selv i stedet for bare at åbne release-siden: både Windows og macOS
// hænger deres advarsel op på et flag, som kun BROWSERE sætter - Mark-of-the-Web
// (Zone.Identifier) henholdsvis com.apple.quarantine. En fil hentet med HttpClient får ingen
// af delene, og så udebliver både SmartScreen-dialogen og Gatekeepers "kan ikke verificeres".
// Det er samme mekanisme som xattr -cr-tricket, bare på forkant. Prisen er, at vi selv skal
// stå inde for hvad vi kører: derfor verificeres GitHubs sha256 ALTID før filen røres, og
// mangler det felt, henter vi slet ikke, men sender brugeren til siden i stedet.
public class GITHUBUpdater {

    private static final String CURRENT_VERSION = "v2.0"; // skal sættes når jeg laver en nye MSI!
    private static final String GITHUB_API_URL = "https://api.github.com/repos/jkizach/Dreams/releases/latest";
    private static final Path CONFIG_PATH = AppPaths.APP_DATA_PATH.resolve("update.json");

    // Selve API-kaldet er nogle få kilobyte og må gerne give hurtigt op. Downloaden må ikke
    // arve den grænse - installeren er 70-100 MB og kan sagtens tage minutter på en dårlig linje.
    private static final Duration API_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(30);

    record Asset(String navn, String url, String sha256, long størrelse) {}
    private record Release(String tag, String htmlUrl, Asset asset) {}

    public static void checkForUpdateIfNeeded() {
        LocalDate lastChecked = readLastCheckedDate();
        if (lastChecked == null || lastChecked.isBefore(LocalDate.now().minusDays(30))) {
            checkForUpdate();
            writeLastCheckedDate(LocalDate.now());
        }
    }

    private static void checkForUpdate() {
        Thread updaterThread = new Thread(() -> {
            Release release = hentSenesteRelease();

            if (release == null || !erNyereEnd(release.tag(), CURRENT_VERSION)) {
                return;
            }
            Platform.runLater(() -> spørgOmOpdatering(release));
        });
        updaterThread.setDaemon(true);
        updaterThread.start();
    }

    // null hvis vi ikke kunne nå GitHub eller ikke forstod svaret. Opdateringstjek er en
    // bekvemmelighed, ikke en kernefunktion: kan vi ikke nå ud, tier vi stille i stedet for
    // at genere brugeren med en fejlbesked.
    private static Release hentSenesteRelease() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(GITHUB_API_URL))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Dreams-Updater")
                    .timeout(API_TIMEOUT)
                    .build();

            HttpResponse<InputStream> svar = klient().send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (svar.statusCode() != 200) {
                return null;
            }

            JsonNode json;
            try (InputStream in = svar.body()) {
                json = new ObjectMapper().readTree(in);
            }

            String tag = json.path("tag_name").asText(null);
            String htmlUrl = json.path("html_url").asText(null);
            if (tag == null || htmlUrl == null) {
                return null;
            }
            return new Release(tag, htmlUrl, vælgAsset(json.path("assets"), ønsketEndelse()));
        } catch (Exception e) {
            return null;
        }
    }

    // Kun STRENGT nyere versioner må tilbydes. Det er ikke pedanteri: kører man installeren
    // for 1.5 oven på 2.0, læser den gamle udgave ikke det nye format i dreams.json, og
    // drømmene er væk. En "opdatering" nedad er altså ikke en irritation, det er et datatab.
    //
    // Det var det den tidligere !equals-test ikke fangede: den så kun om tagget var et ANDET
    // end vores, ikke om det var et højere. Så snart CURRENT_VERSION var forud for det seneste
    // release - hvilket den er hver gang en ny udgave er bygget men ikke lagt op endnu -
    // tilbød appen brugeren at installere sig selv baglæns.
    //
    // Kan et af de to tags ikke læses som tal, siger vi nej. Vi ved så ikke hvad der er nyest,
    // og af de to måder at tage fejl på er den tavse den ufarlige.
    static boolean erNyereEnd(String kandidat, String nuværende) {
        int[] a = versionstal(kandidat);
        int[] b = versionstal(nuværende);
        if (a == null || b == null) {
            return false;
        }
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = (i < a.length) ? a[i] : 0; // "v2" og "v2.0" er samme version
            int y = (i < b.length) ? b[i] : 0;
            if (x != y) {
                return x > y;
            }
        }
        return false; // helt ens
    }

    // "v2.0.1" -> [2, 0, 1]. Et foranstillet v og et eventuelt suffiks efter - eller +
    // ("v2.1-rc1") pilles af først, så et prøve-release ikke ser ud som en ulæselig version.
    // null hvis der står noget vi ikke forstår.
    private static int[] versionstal(String tag) {
        if (tag == null) {
            return null;
        }
        String rest = tag.strip();
        if (rest.startsWith("v") || rest.startsWith("V")) {
            rest = rest.substring(1);
        }
        int suffiks = rest.length();
        for (int i = 0; i < rest.length(); i++) {
            if (rest.charAt(i) == '-' || rest.charAt(i) == '+') {
                suffiks = i;
                break;
            }
        }
        rest = rest.substring(0, suffiks);
        if (rest.isEmpty()) {
            return null;
        }

        String[] dele = rest.split("\\.", -1);
        int[] tal = new int[dele.length];
        for (int i = 0; i < dele.length; i++) {
            try {
                tal[i] = Integer.parseInt(dele[i]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return tal;
    }

    // Filendelsen er nok til at vælge rigtigt: et release har præcis én .msi og én .dmg.
    // Kan platformen ikke genkendes, eller mangler filen sit sha256-felt, returneres null -
    // og så falder dialogen tilbage til bare at åbne release-siden.
    //
    // Endelsen gives ind udefra frem for at blive slået op her, så valget kan testes uden at
    // skulle forfalske os.name.
    static Asset vælgAsset(JsonNode assets, String endelse) {
        if (endelse == null || assets == null || !assets.isArray()) {
            return null;
        }
        for (JsonNode a : assets) {
            String navn = a.path("name").asText("");
            if (!navn.toLowerCase(Locale.ROOT).endsWith(endelse)) {
                continue;
            }
            String url = a.path("browser_download_url").asText(null);
            String digest = a.path("digest").asText(null); // formatet er "sha256:<hex>"
            if (url == null || digest == null || !digest.startsWith("sha256:")) {
                return null;
            }
            return new Asset(navn, url, digest.substring("sha256:".length()), a.path("size").asLong(0));
        }
        return null;
    }

    private static String ønsketEndelse() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return ".msi";
        }
        if (os.contains("mac")) {
            return ".dmg";
        }
        return null;
    }

    private static boolean erWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static void spørgOmOpdatering(Release release) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Opdatering tilgængelig");
        alert.setHeaderText("Ny version: " + release.tag());

        ButtonType handling;
        if (release.asset() == null) {
            // Ukendt platform, eller et release uden en fil vi tør køre: gammel opførsel.
            alert.setContentText("Der findes en ny version. Vil du hente den?");
            handling = new ButtonType("Åbn download-siden", ButtonBar.ButtonData.OK_DONE);
        } else if (erWindows()) {
            alert.setContentText("Vil du hente og installere den nu? "
                    + "Drømmeappen lukker ned, og installationen starter af sig selv.");
            handling = new ButtonType("Hent og installér", ButtonBar.ButtonData.OK_DONE);
        } else {
            alert.setContentText("Vil du hente og installere den nu? "
                    + "Drømmeappen lukker ned og åbner den hentede fil, "
                    + "hvor du trækker den nye udgave over i Programmer.");
            handling = new ButtonType("Hent og installér", ButtonBar.ButtonData.OK_DONE);
        }

        ButtonType ikkeNu = new ButtonType("Ikke nu", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(handling, ikkeNu);

        alert.showAndWait().ifPresent(svar -> {
            if (svar != handling) {
                return;
            }
            if (release.asset() == null) {
                åbnReleaseSide(release.htmlUrl());
            } else {
                hentOgInstaller(release);
            }
        });
    }

    private static void hentOgInstaller(Release release) {
        DownloadVindue vindue = new DownloadVindue(release.asset().størrelse());
        vindue.vis();

        Thread hentetråd = new Thread(() -> {
            Path fil;
            try {
                fil = hentFil(release.asset(), vindue);
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    vindue.luk();
                    fortælOmFejl(release.htmlUrl());
                });
                return;
            }

            if (fil == null) {
                Platform.runLater(vindue::luk); // brugeren afbrød
                return;
            }

            Platform.runLater(() -> {
                vindue.luk();
                try {
                    startInstallation(fil);
                } catch (IOException e) {
                    e.printStackTrace();
                    fortælOmFejl(release.htmlUrl());
                    return;
                }
                // setOnCloseRequest fyrer ikke når appen lukker sig selv, så gemmet skal
                // kaldes eksplicit - ellers ville sessionens arbejde gå tabt i en opdatering.
                DreamApp.gemOgAfslut();
            });
        });
        hentetråd.setDaemon(true);
        hentetråd.start();
    }

    // Returnerer null hvis brugeren afbrød. Kaster hvis noget gik galt - herunder hvis
    // checksummen ikke passer, for så ved vi ikke hvad vi har hentet, og så kører vi det ikke.
    private static Path hentFil(Asset asset, DownloadVindue vindue) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(asset.url()))
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "Dreams-Updater")
                .timeout(DOWNLOAD_TIMEOUT)
                .build();

        HttpResponse<InputStream> svar = klient().send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (svar.statusCode() != 200) {
            throw new IOException("GitHub svarede " + svar.statusCode());
        }

        Path mappe = Files.createTempDirectory("dreams-update");
        Path mål = mappe.resolve(asset.navn());
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        long hentet = 0;

        try (InputStream in = svar.body(); OutputStream ud = Files.newOutputStream(mål)) {
            byte[] buffer = new byte[1 << 16];
            int n;
            while ((n = in.read(buffer)) > 0) {
                if (vindue.erAfbrudt()) {
                    ud.close();
                    Files.deleteIfExists(mål);
                    Files.deleteIfExists(mappe);
                    return null;
                }
                ud.write(buffer, 0, n);
                sha256.update(buffer, 0, n);
                hentet += n;
                vindue.opdater(hentet);
            }
        }

        String målt = HexFormat.of().formatHex(sha256.digest());
        if (!målt.equalsIgnoreCase(asset.sha256())) {
            Files.deleteIfExists(mål);
            throw new IOException("Checksummen passer ikke - filen blev ikke hentet korrekt.");
        }
        return mål;
    }

    // Windows: msiexec startes og appen lukker straks efter. Vi venter bevidst ikke og lægger
    // ingen kunstig forsinkelse ind: msiexec skal først igennem UAC og sin egen opstart, og
    // vores proces er væk længe inden den rører filer. Skulle den alligevel nå det først,
    // opdager Windows Installer selv låste filer og beder brugeren lukke appen - en dialog,
    // ikke et tab.
    //
    // macOS: en DMG kan ikke "køres". Vi monterer den med open, hvorefter Finder viser
    // app-ikonet og genvejen til Programmer, og brugeren trækker den ene over på den anden.
    // Fordi filen ikke er karantæneramt, bliver den udtrukne .app det heller ikke - og
    // Gatekeeper spørger derfor ikke.
    private static void startInstallation(Path fil) throws IOException {
        if (erWindows()) {
            new ProcessBuilder("msiexec", "/i", fil.toString()).start();
        } else {
            new ProcessBuilder("open", fil.toString()).start();
        }
    }

    private static void fortælOmFejl(String htmlUrl) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Opdatering");
        alert.setHeaderText("Kunne ikke hente opdateringen");
        alert.setContentText("Du kan hente den manuelt fra download-siden i stedet.");
        ButtonType åbn = new ButtonType("Åbn download-siden", ButtonBar.ButtonData.OK_DONE);
        ButtonType luk = new ButtonType("Luk", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(åbn, luk);
        alert.showAndWait().ifPresent(svar -> {
            if (svar == åbn) {
                åbnReleaseSide(htmlUrl);
            }
        });
    }

    private static void åbnReleaseSide(String htmlUrl) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(htmlUrl));
            } else {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + htmlUrl);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static HttpClient klient() {
        // GitHubs browser_download_url omdirigerer til objects.githubusercontent.com.
        return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    }

    // Lille fremdriftsvindue. Downloaden er 70-100 MB, så den må hverken se ud som om appen
    // er gået i stå eller være umulig at fortryde.
    private static final class DownloadVindue {
        private final long total;
        private final AtomicBoolean afbrudt = new AtomicBoolean(false);
        private final ProgressBar bjælke = new ProgressBar(0);
        private final Label tekst = new Label("Henter...");
        private final Dialog<Void> dialog = new Dialog<>();
        private volatile boolean færdig = false;

        private DownloadVindue(long total) {
            this.total = total;
            bjælke.setMaxWidth(Double.MAX_VALUE);

            VBox indhold = new VBox(10, tekst, bjælke);
            indhold.setPadding(new Insets(10));
            dialog.setTitle("Henter opdatering");
            dialog.getDialogPane().setHeaderText("Henter den nye version...");
            dialog.getDialogPane().setContent(indhold);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            dialog.setResultConverter(knap -> null);

            // Både Annuller-knappen og krydset lukker vinduet; er downloaden ikke færdig,
            // betyder en lukning at brugeren fortrød.
            dialog.setOnHidden(e -> {
                if (!færdig) {
                    afbrudt.set(true);
                }
            });
        }

        private void vis() {
            dialog.show();
        }

        private boolean erAfbrudt() {
            return afbrudt.get();
        }

        // Kaldes fra hentetråden for hver blok. Selve opdateringen af UI'et skal på FX-tråden.
        private void opdater(long hentet) {
            double andel = (total > 0) ? (double) hentet / total : ProgressBar.INDETERMINATE_PROGRESS;
            String linje = (total > 0)
                    ? String.format("%d MB af %d MB", hentet >> 20, total >> 20)
                    : String.format("%d MB hentet", hentet >> 20);
            Platform.runLater(() -> {
                bjælke.setProgress(andel);
                tekst.setText(linje);
            });
        }

        private void luk() {
            færdig = true;
            dialog.hide();
        }
    }

    private static LocalDate readLastCheckedDate() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                ObjectMapper mapper = new ObjectMapper();
                Map<?, ?> data = mapper.readValue(CONFIG_PATH.toFile(), Map.class);
                String dateStr = (String) data.get("lastChecked");
                return LocalDate.parse(dateStr);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private static void writeLastCheckedDate(LocalDate date) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Files.createDirectories(CONFIG_PATH.getParent());
            mapper.writeValue(CONFIG_PATH.toFile(), Map.of("lastChecked", date.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
