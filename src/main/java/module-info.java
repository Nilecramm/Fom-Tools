module org.example.fomtools {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.desktop;


    opens com.nilecramm.fomtools to javafx.fxml;
    exports com.nilecramm.fomtools;
}