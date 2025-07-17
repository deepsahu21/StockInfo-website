/**
 * Defines the Stock Info App module.
 */
module stockinfo.app {
    requires transitive java.logging;
    requires transitive java.net.http;
    requires transitive javafx.controls;
    requires transitive javafx.fxml;
    requires transitive javafx.web;
    requires transitive com.google.gson;

    opens cs1302.api;
}
