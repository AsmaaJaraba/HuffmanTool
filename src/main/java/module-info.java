module com.huffmantool {
    // JavaFX modules we need
    requires javafx.controls;
    requires javafx.fxml;

    // Our core package needs to be accessible
    // so JavaFX can work with our classes
    opens com.huffmantool to javafx.fxml;
    exports com.huffmantool;

    // Export our core and gui packages
    exports core;
    exports gui;
}