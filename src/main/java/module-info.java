module fixit.dreams {
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires java.desktop;


    opens fixit.dreams to javafx.fxml;
    exports fixit.dreams;
}