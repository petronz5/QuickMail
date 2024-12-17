module petroni.company.mailclient {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;
    //requires deva.company.common; // Dipendenza verso il modulo comune

    opens deva.company.mailclient1 to javafx.fxml, javafx.graphics;
    opens deva.company.mailclient1.controller to javafx.fxml;
    opens deva.company.mailclient1.controller.client to javafx.fxml;
    exports deva.company.mailclient1.controller;
    exports deva.company.mailclient1.controller.client;
    exports deva.company.mailclient1.model;
    exports deva.company.mailclient1.util;


}