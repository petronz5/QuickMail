module deva.company.common {
    requires javafx.controls;
    requires javafx.fxml;

    opens deva.company.common to javafx.fxml;
    //exports deva.company.common.model;
}