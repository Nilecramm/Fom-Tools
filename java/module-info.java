module org.example.fomtools {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.desktop;


    opens org.example.fomtools to javafx.fxml;
    exports org.example.fomtools;
}