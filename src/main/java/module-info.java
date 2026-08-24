module fixit.dreams {
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires java.desktop;
    requires java.net.http;


    opens fixit.dreams to javafx.fxml;
    exports fixit.dreams;

    // SyncController (fx:controller) lever i fixit.dreams selv, ikke her - fixit.dreams.sync
    // eksporteres udelukkende så Jackson kan reflektere over SyncDTO's public felter.
    exports fixit.dreams.sync;
}