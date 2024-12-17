package deva.company.mailclient1.controller;

import deva.company.mailclient1.controller.client.ClientController;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class LoginController {

    @FXML
    private TextField emailField;
    @FXML
    public ListView<String> accSelector;
    @FXML
    public Button loginBtn;

    // Aggiunta di una variabile statica per memorizzare l'istanza della finestra di login
    private static Stage loginStage = null;

    @FXML
    public void initialize() {

    }

    @FXML
    public void login(ActionEvent e) throws IOException {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Per favore, inserisci il tuo indirizzo email.");
            alert.show();
            return;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/deva/company/mailclient1/clientView.fxml"));
        Scene scene = new Scene(loader.load());

        Stage newStage = new Stage();
        newStage.setTitle("Quick Mail - " + email);
        newStage.setScene(scene);

        ClientController clientController = loader.getController();

        // Tenta di autenticare l'utente
        boolean isAuthenticated = clientController.authenticateUser(email);

        if (isAuthenticated) {
            clientController.initializeClient(email);
            newStage.setOnCloseRequest((windowEvent -> clientController.handleTimerLoadEmails(false)));
            newStage.show();

            // Chiudi la finestra di login
            ((Stage) ((Button) e.getSource()).getScene().getWindow()).close();
        }
    }

    public static void showLoginStage() throws IOException {
        if (loginStage != null && loginStage.isShowing()) {
            loginStage.toFront(); // Porta la finestra esistente in primo piano
        } else {
            FXMLLoader loader = new FXMLLoader(LoginController.class.getResource("/deva/company/mailclient1/login.fxml"));
            Scene scene = new Scene(loader.load());

            loginStage = new Stage();
            loginStage.setTitle("Quick Mail - Login");
            loginStage.setScene(scene);

            // Imposta l'azione quando la finestra viene chiusa
            loginStage.setOnCloseRequest((WindowEvent event) -> loginStage = null);

            // Mostra la finestra di login e salva lo stage nella variabile statica
            loginStage.show();
        }
    }

}
