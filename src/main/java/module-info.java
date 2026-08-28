module fixit.dreams {
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires java.desktop;
    requires java.net.http;

    // Ikke-engelske sprogdata ligger i sit eget JDK-modul. Uden dette requires tager jlink det
    // ikke med i runtime-imaget, og den installerede app falder tilbage til rod-locale: "Tue"
    // i stedet for "tirs.", og 2026-08-27 i stedet for 27.08.2026. Fra IntelliJ ses det ikke,
    // for dér køres der på en fuld JDK, som har modulet i forvejen.
    requires jdk.localedata;

    // Samme fælde: TLS' elliptiske kurver (SunEC-provideren) ligger også i sit eget JDK-modul.
    // Uden dette requires tager jlink det ikke med, og så kan den installerede app ikke lave et
    // eneste HTTPS-kald - hvert handshake dør i "handshake_failure", som appen viser brugeren
    // som "tjek din internetforbindelse". Fra IntelliJ ses det ikke, for dér køres der på en
    // fuld JDK, som har modulet i forvejen.
    requires jdk.crypto.ec;

    opens fixit.dreams to javafx.fxml;
    exports fixit.dreams;

    // SyncController (fx:controller) lever i fixit.dreams selv, ikke her - fixit.dreams.sync
    // eksporteres udelukkende så Jackson kan reflektere over SyncDTO's public felter.
    exports fixit.dreams.sync;
}
