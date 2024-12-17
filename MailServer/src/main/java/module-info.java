module petroni.company.mailserver {
    requires javafx.controls;
    requires javafx.fxml;
    requires json.simple;
    requires deva.company.common;
    requires petroni.company.mailclient; // Dipendenza verso il modulo comune

    opens deva.company.mailserver to javafx.fxml;
    opens deva.company.mailserver.controller to javafx.fxml;
    exports deva.company.mailserver;
    exports deva.company.mailserver.controller;
    exports deva.company.mailserver.model;
}
