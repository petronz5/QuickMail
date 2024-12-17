package deva.company.mailclient1.controller.client;


import deva.company.mailclient1.controller.LoginController;
import deva.company.mailclient1.model.Email;
import deva.company.mailclient1.model.User;
import deva.company.mailclient1.util.Request;
import deva.company.mailclient1.util.Response;
import deva.company.mailclient1.util.SerializableEmail;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientController {

    private Socket socket;
    private ScheduledExecutorService scheduledExecutorService;
    private boolean serverAvailable = false;

    ObjectInputStream objectInputStream;
    ObjectOutputStream objectOutputStream;


    @FXML
    private Label lblFrom;
    @FXML
    private Label lblTo;
    @FXML
    private Label lblUsername;
    @FXML
    private Label lblSubject;
    @FXML
    private Label lblConnectionStatus;
    @FXML
    private TextArea txtEmail;
    @FXML
    private Button newEmailBtn;
    @FXML
    private Button btnReply;
    @FXML
    private Button btnReplyAll;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnForward;
    @FXML
    private ListView<Email> listEmails;
    @FXML
    private Button inboxBtn;
    @FXML
    private Button sentBtn;
    @FXML
    private Button trashedBtn;
    @FXML
    private Text inboxCount;
    @FXML
    private Text sentCount;
    @FXML
    private Text trashedCount;
    @FXML
    private Text txtNewInbox;
    @FXML
    private AnchorPane rootPane; // il root della tua interfaccia

    private Alert connectionAlert;
    public User model;
    protected Stage newStage;

    private String sectionName = "inbox";
    private boolean alertShown = false;
    private long alertClosedTime = 0;
    private static final long ALERT_COOLDOWN = 8000;

    /***
     * Inizializza il client per l'utente specificato.
     * Imposta il modello per l'utente, collega le proprietà dell'interfaccia grafica e
     * carica tutte le email al server
     * @param usr
     */
    public void initializeClient(String usr) {
        if (this.model != null) {
            throw new IllegalStateException("Model can only be initialized once");
        }
        listEmails.setCellFactory(param -> new ListCell<Email>() {
            @Override
            protected void updateItem(Email email, boolean empty) {
                super.updateItem(email, empty);
                if (empty || email == null) {
                    setText(null);
                } else {
                    if (sectionName.equals("inbox")) {
                        String text;
                        if (!email.isRead()) {
                            text = email.toString() + " - NEW";
                        } else {
                            text = email.toString();
                        }
                        setText(text);
                    } else {
                        setText(email.toString());
                    }
                }
            }
        });

        model = new User(usr);
        lblUsername.textProperty().bind(model.emailAddressProperty());
        sentCount.textProperty().bind(model.sentCounterProperty());
        inboxCount.textProperty().bind(model.inboxCounterProperty());
        trashedCount.textProperty().bind(model.trashedCounterProperty());
        model.emptyEmail = null;

        boolean res = allFromServer();
        if (res) {
            bindList(model.inboxProperty());
            sectionName = "inbox";
            model.selectedEmail = model.inboxProperty().get(0);
            updateDetailView(model.selectedEmail);
            inboxBtn.setStyle(" -fx-background-color: #CAC9D2;\n" + " -fx-background-radius: 5px;");
        } else {
            updateDetailView(model.emptyEmail);
            showAlert("Informazione", "No received emails to display at the moment.");
        }
        handleTimerLoadEmails(true);

    }

    /***
     * Ordine le email per data e seleziona la prima email
     * @param list
     */
    private void bindList(ListProperty<Email> list) {
        // Crea una SortedList che osserva direttamente la ListProperty del modello
        SortedList<Email> sortedList = new SortedList<>(list);
        sortedList.setComparator(Comparator.comparing(Email::getDateTime).reversed());

        // Imposta la SortedList come items della ListView
        listEmails.setItems(sortedList);

        // Debug per verificare l'ordine delle email
        System.out.println("Ordine delle email visualizzate:");
        for (Email email : sortedList) {
            System.out.println(email.getSubject() + " - " + email.getDateTime());
        }

        listEmails.getSelectionModel().selectFirst();
        listEmails.setOnMouseClicked(this::showSelectedEmail);
    }


    /***
     * Mostra la "inbox" dell'utente
     * @param event
     */
    @FXML
    private void showInbox(ActionEvent event) {
        if (model.inboxProperty().size() > 0) {
            sectionName = "inbox";
            bindList(model.inboxProperty());
            model.selectedEmail = model.inboxProperty().get(0);
            updateDetailView(model.selectedEmail);
            activeBtnHandle(sectionName);
        }
    }

    /***
     * Mostra la "setn" dell'utente
     * @param event
     */
    @FXML
    private void showSent(ActionEvent event) {
        if (model.sentProperty().size() > 0) {
            sectionName = "sent";
            bindList(model.sentProperty());
            model.selectedEmail = model.sentProperty().get(0);
            updateDetailView(model.selectedEmail);
            activeBtnHandle("sent");
        }
    }

    /***
     * Mostra il "trashed" dell'utente
     * @param event
     */
    @FXML
    private void showTrashed(ActionEvent event) {
        if (model.trashedProperty().size() > 0) {
            sectionName = "trashed";
            model.selectedEmail = model.trashedProperty().get(0);
            bindList(model.trashedProperty());
            updateDetailView(model.selectedEmail);
            activeBtnHandle(sectionName);
        }
    }


    private void activeBtnHandle(String section) {
        switch (section) {
            case "inbox" -> {
                inboxBtn.setStyle(" -fx-background-color: #CAC9D2;\n" +
                        " -fx-background-radius: 5px;");
                sentBtn.setStyle(null);
                trashedBtn.setStyle(null);
            }
            case "sent" -> {
                sentBtn.setStyle(" -fx-background-color: #CAC9D2;\n" +
                        " -fx-background-radius: 5px;");
                inboxBtn.setStyle(null);
                trashedBtn.setStyle(null);
            }
            case "trashed" -> {
                trashedBtn.setStyle(" -fx-background-color: #CAC9D2;\n" +
                        " -fx-background-radius: 5px;");
                inboxBtn.setStyle(null);
                sentBtn.setStyle(null);
            }
        }
    }

    /***
     * Gestisce il click sul bottone "Write new email"
     * Controlla se il server è disponibile, in caso contrario mostra un errore
     * @param event
     */
    @FXML
    public void handleNewEmail(ActionEvent event) {
        try {
            if (!serverAvailable) {
                // Mostra alert di errore in inglese
                Alert a = new Alert(Alert.AlertType.ERROR, "Server is offline, you can't send new emails.");
                a.show();
                return;
            }
            handleNewStage("new");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /***
     * Gestisce il click sul bottone "Reply"
     * @param event
     */
    @FXML
    void onReplyBtnClick(ActionEvent event) {
        try {
            handleNewStage("reply");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /***
     * Gestisce il click sul bottone "Reply All"
     * @param event
     */
    @FXML
    void onReplyAllBtnClick(ActionEvent event) {
        try {
            handleNewStage("replyAll");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @FXML
    void onForwardBtnClick(ActionEvent event) {
        try {
            handleNewStage("forward");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /***
     * Gestisce l'apertura di una nuova finestra per la creazione o risposta di email
     * inizializza il New EmailController
     * @param btnClicked
     * @throws IOException
     */
    private void handleNewStage(String btnClicked) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/deva/company/mailclient1/newEmail.fxml"));
        Parent root = loader.load();
        NewEmailController newController = loader.getController();
        newController.initializeNewController(model, this, btnClicked); //passing controller and model
        newStage = new Stage();
        Scene scene = new Scene(root);

        // Permetti il ridimensionamento
        newStage.setMinWidth(400); // Larghezza minima
        newStage.setMinHeight(300); // Altezza minima

        newStage.setScene(scene);
        newStage.setTitle("New Email");
        newStage.show();
    }


    /***
     * Invia una richiesta "delete" al server e aggiorna la UI
     * @param event
     */
    @FXML
    public void onDeleteBtnClick(ActionEvent event) {
        try {
            openConnection();
            SerializableEmail email = new SerializableEmail(model.selectedEmail);
            Request request = new Request("delete", model.emailAddressProperty().get(), email, sectionName);
            sendEmail(request);
            Response response = getServerResponse();
            if (response.isSuccess()) {
                Platform.runLater(() -> {
                    switch (sectionName) {
                        case "inbox" -> {
                            model.deleteInbox(model.selectedEmail);
                            model.selectedEmail.setIdEmail(response.getIdEmail());
                            model.trashedProperty().add(model.selectedEmail);
                            if (model.inboxProperty().size() > 0) {
                                model.selectedEmail = model.inboxProperty().get(0);
                            } else {
                                model.selectedEmail = null;
                            }
                            updateDetailView(model.selectedEmail);
                        }
                        case "sent" -> {
                            model.deleteSent(model.selectedEmail);
                            model.selectedEmail.setIdEmail(response.getIdEmail());
                            model.trashedProperty().add(model.selectedEmail);
                            if (model.sentProperty().size() > 0) {
                                model.selectedEmail = model.sentProperty().get(0);
                            } else {
                                model.selectedEmail = null;
                            }
                            updateDetailView(model.selectedEmail);
                        }
                        case "trashed" -> {
                            model.deleteTrashed(model.selectedEmail);
                            if (model.trashedProperty().size() > 0) {
                                model.selectedEmail = model.trashedProperty().get(0);
                            } else {
                                model.selectedEmail = null;
                            }
                            updateDetailView(model.selectedEmail);
                        }
                    }

                    if (model.sentProperty().isEmpty() && model.trashedProperty().isEmpty() && model.inboxProperty().isEmpty()) {
                        updateDetailView(model.emptyEmail);
                    }

                    // Aggiorna i contatori
                    model.setInboxCounter(model.inboxProperty().size());
                    model.setSentCounter(model.sentProperty().size());
                    model.setTrashedCounter(model.trashedProperty().size());
                });

            } else {
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, response.getMessage());
                    a.show();
                });
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    /***
     * Gestisce il logout dell'utente
     * invia la richiesta "logout" e chiude la finesta del client
     * @param event
     */
    public void handleLogout(ActionEvent event) throws IOException {
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdownNow();
        }

        handleTimerLoadEmails(false);

        // Invio richiesta di logout al server
        openConnection();
        Request request = new Request("logout", model.emailAddressProperty().get());
        sendEmail(request);
        try {
            Response response = getServerResponse();
            if(response.isSuccess()) {
                // Ok logout
            } else {
                // mostra alert di errore
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            closeConnection();
        }

        Stage currentStage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        currentStage.close();

        Platform.runLater(() -> {
            try {
                LoginController.showLoginStage();
            } catch (IOException e) {
                showAlert("Error", "An error occurred during logout.");
            }
        });
    }


    /***
     * Controlla se ci sono email non lette nella inbox del modello
     * @return
     */
    private boolean checkUnreadMails() {
        boolean unread = false;
        for (Email e: model.inboxProperty()) {
            if (!e.isRead()) {
                unread = true;
                break;
            }
        }
        return unread;
    }


    /***
     * Tenta di autenticare l'utente presso il server.
     * Invia una richiesta "login" e verifica la risposta
     * @param email
     * @return
     */
    public boolean authenticateUser(String email) {
        boolean isAuthenticated = false;
        try {
            openConnection();
            if (!serverAvailable) {
                // Server non disponibile
                System.out.println("Server Non acceso");
                return false;
            }
            Request request = new Request("login", email);
            sendEmail(request);
            Response response = getServerResponse();
            if (response.isSuccess()) {
                isAuthenticated = true;
            } else {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR, response.getMessage());
                    alert.show();
                });
            }
        } catch (IOException | ClassNotFoundException e) {
            //e.printStackTrace();
            System.out.println("Server Non acceso");
        } finally {
            closeConnection();
        }
        return isAuthenticated;
    }


    /***
     * Aggiorna la vista delle email (email non lette)
     * @param mouseEvent
     */
    @FXML
    void showSelectedEmail(MouseEvent mouseEvent) {
        Email email = listEmails.getSelectionModel().getSelectedItem();
        model.selectedEmail = email;
        updateDetailView(email);

        /*  case inbox => check if read or not   */
        if (sectionName.equals("inbox")) {
            if (!model.selectedEmail.isRead()) {
                model.selectedEmail.setRead(true);

                // Creiamo un SerializableEmail dall'email selezionata
                SerializableEmail sEmail = new SerializableEmail(model.selectedEmail);

                // Creiamo una richiesta passando il SerializableEmail
                Request request = new Request(
                        "setRead",
                        model.emailAddressProperty().get(),
                        sEmail
                );

                openConnection();
                sendEmail(request);
                try {
                    Response response = getServerResponse();
                    System.out.println(response);
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                } finally {
                    closeConnection();
                }

                // Aggiorniamo la ListView per riflettere il cambiamento
                listEmails.refresh();
            }
            if (!checkUnreadMails()) {
                Platform.runLater(() -> txtNewInbox.setVisible(false));
            }
        }
    }


    /***
     * Aggiorna la vista dettagliata (From, To, Subject, Body)
     * @param email
     */
    public void updateDetailView(Email email) {
        if(email != null) {
            lblFrom.setText(email.getSender().get());
            lblTo.setText(email.getReceiver().get());
            lblSubject.setText(email.getSubject().get());
            txtEmail.setText(email.getBody().get());
        }
    }


    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            long currentTime = System.currentTimeMillis();
            if (!alertShown && (currentTime - alertClosedTime > ALERT_COOLDOWN)) {
                alertShown = true;
                connectionAlert = new Alert(Alert.AlertType.INFORMATION);
                connectionAlert.setTitle(title);
                connectionAlert.setHeaderText(null);
                connectionAlert.setContentText(message);
                connectionAlert.setOnCloseRequest(event -> {
                    alertClosedTime = System.currentTimeMillis();
                    alertShown = false; // Permette di mostrare un nuovo alert dopo il cooldown
                });
                connectionAlert.show();
            }
        });
    }

    /***
     * In caso di perdita di connessione col server, tenta di riconnettersi
     * Se non riesce mostra un messaggio di errore
     */
    protected synchronized void attemptReconnect() {
        closeConnection();
        int attempts = 0;
        int maxAttempts = 1000; // Limita a 1000 tentativi
        while (attempts < maxAttempts) {
            try {
                openConnection();
                if (serverAvailable) {
                    break; // Esci dal ciclo se la connessione è riuscita
                }
            } catch (Exception e) {
               //e.printStackTrace();
            }
            attempts++;
            try {
                Thread.sleep(2000); // Attendi 2 secondi prima di ritentare
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
        }
        if (!serverAvailable) {
            Platform.runLater(() -> {
                if (!alertShown) {
                    alertShown = true;
                    showAlert("Connection Error", "Unable to reconnect after several attempts.");
                }
            });
        }
    }

    /***
     * Apre una connessione socket al server.
     * In caso di successi, abilita i pulsanti
     * In caso di fallimento, disabilita i pulsanti e mostra un alert di errore
     */
    protected synchronized void openConnection() {
        try {
            System.out.println("Attempting to connect...");
            socket = new Socket();
            socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), 9000), 2000);
            System.out.println("Connected to server");

            // Abilita tutti i pulsanti dopo la connessione
            Platform.runLater(() -> {
                disableAllButtons(false);  // Abilita i pulsanti
            });
            updateConnectionStatus("Server Acceso", "green");

            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            serverAvailable = true;
            alertShown = false;
        } catch (IOException e) {
            serverAvailable = false;
            // Disabilita i bottoni quando la connessione fallisce
            Platform.runLater(() -> {
                disableAllButtons(true);    // Disabilita i pulsanti
                rootPane.setStyle("-fx-background-color: rgba(255, 255, 255, 0.5);");  // UI biancastra
                showAlert("Connection Lost", "Failed to connect to the mail server. Retrying...");
            });
            updateConnectionStatus("Server Spento", "red");
            lblConnectionStatus.setVisible(true);
        }
    }

    /***
     * Chiude la connessione socket con il server
     */
    protected synchronized void closeConnection() {
        try {
            if (objectOutputStream != null) {
                objectOutputStream.close();
                objectOutputStream = null;
            }
            if (objectInputStream != null) {
                objectInputStream.close();
                objectInputStream = null;
            }
            if (socket != null && !socket.isClosed()) {
                try{
                    socket.close();
                } catch (IOException e){
                    System.err.println("Errore durante la chiusura del socket: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    /***
     * Invia un oggetto Request al server attraverso l'output stream
     * @param req
     */
    protected synchronized void sendEmail(Request req) {
        if (objectOutputStream == null) return;
        try {
            objectOutputStream.writeObject(req);
            objectOutputStream.flush();
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    protected synchronized Response getServerResponse() throws IOException, ClassNotFoundException {
        if (objectInputStream == null) {
            throw new IOException("Input stream is null");
        }
        return (Response) objectInputStream.readObject();
    }

    /***
     * Richiede al server tutte le email e le carica del modello
     * Aggiorna la UI
     * @return
     */
    protected synchronized boolean allFromServer() {
        boolean response = false;
        try {
            Request request;
            int newInbox = 0;
            request = new Request(
                    "getAll",
                    model.emailAddressProperty().get()
            );

            openConnection();
            sendEmail(request);
            Response res = getServerResponse();
            if (res.isSuccess()) {
                ArrayList<Email> inbox = new ArrayList<>();
                ArrayList<Email> sent = new ArrayList<>();
                ArrayList<Email> trashed = new ArrayList<>();
                if (!res.getInbox().isEmpty()) {
                    response = true;
                    ArrayList<Email> inboxagg = new ArrayList<>();
                    for (SerializableEmail sEmail : res.getInbox()) {
                        Email e = new Email(sEmail);
                        inboxagg.add(e);
                    }
                    for (Email e : inboxagg) {
                        if (!e.isRead()) { // Se l'email non è stata letta, incrementa il contatore
                            newInbox++;
                        }
                        model.inboxProperty().add(e); // Aggiungi l'email alla lista delle email del client
                    }
                }

                if (!res.getSent().isEmpty()) {
                    for (SerializableEmail sEmail : res.getSent()) {
                        Email e = new Email(sEmail);
                        sent.add(e);
                    }
                    for (Email e : sent) {
                        model.sentProperty().add(e);
                    }
                }
                if (!res.getTrashed().isEmpty()) {
                    for (SerializableEmail sEmail : res.getTrashed()) {
                        Email e = new Email(sEmail);
                        trashed.add(e);
                    }
                    for (Email e : trashed) {
                        model.trashedProperty().add(e);
                    }
                }
                final int newMail = newInbox;
                Platform.runLater(() -> {
                    model.setTrashedCounter(model.trashedProperty().size());
                    model.setSentCounter(model.sentProperty().size());
                    model.setInboxCounter(model.inboxProperty().size());
                    newMailHandler(newMail);
                });
                serverAvailable = true;
            } else {
                Alert a = new Alert(Alert.AlertType.ERROR, res.getMessage());
                a.show();
            }
        } catch (Exception ex) {
            //ex.printStackTrace();
            serverAvailable = false;
            Platform.runLater(() -> showAlert("Connection Lost", "Failed to connect to the mail server. Retrying..."));
        } finally {
            closeConnection();
        }
        return response;
    }

    /***
     * Richiede al server le email nuove,
     * Se ci sono email nuove, le aggiunge all'inbox
     */
    protected synchronized void updateFromServer() {
        if (!serverAvailable) {
            return;
        }
        try {
            Request request;
            int newInbox = 0;
            long lastInbox = model.inboxProperty().isEmpty() ? 0 : model.inboxProperty().get(model.inboxProperty().size() - 1).getIdEmail();

            request = new Request(
                    "update",
                    model.emailAddressProperty().get(),
                    lastInbox
            );
            openConnection();  // Tenta la connessione prima di inviare l'email

            if (serverAvailable) {  // Verifica se la connessione è effettivamente stabilita
                sendEmail(request);
                Response res = getServerResponse();
                if (res.isSuccess()) {
                    ArrayList<Email> inbox = new ArrayList<>();
                    if (res.getInbox() != null) { /* there are new emails */
                        for (SerializableEmail sEmail : res.getInbox()) {
                            newInbox = newInbox + 1;
                            Email e = new Email(sEmail);
                            inbox.add(e);
                        }
                        for (Email e : inbox) {
                            Platform.runLater(() -> model.inboxProperty().add(e));
                        }
                    }
                    final int newMail = newInbox;
                    Platform.runLater(() -> {
                        model.setTrashedCounter(model.trashedProperty().size());
                        model.setSentCounter(model.sentProperty().size());
                        model.setInboxCounter(model.inboxProperty().size());
                        newMailHandler(newMail);
                    });
                    serverAvailable = true;
                } else {
                    Alert a = new Alert(Alert.AlertType.ERROR, res.getMessage());
                    a.show();
                }
            } else {
                throw new IOException("Server is not available");
            }
        } catch (Exception ex) {
            serverAvailable = false;
            Platform.runLater(() -> showAlert("Connection Lost", "Failed to connect to the mail server."));
            attemptReconnect();
        } finally {
            closeConnection();
        }
    }

    /**
     * Mostra un indicatore NEW se sono arrivate nuove email nella inbox
     * @param newInbox
     */
    private void newMailHandler(int newInbox) {
        if (newInbox >= 1 && !txtNewInbox.isVisible()) {
            txtNewInbox.setVisible(true);
        }
    }

    public void handleTimerLoadEmails(boolean open) {
        if (open && scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newScheduledThreadPool(1);
            scheduledExecutorService.scheduleAtFixedRate(new emailDownload(), 5, 5, TimeUnit.SECONDS);
            System.out.println("ok schedule timer");
        } else if (!open && scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
        }
    }

    private void updateConnectionStatus(String status, String color) {
        Platform.runLater(() -> {
            lblConnectionStatus.setText(status);
            lblConnectionStatus.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 16px;");
            lblConnectionStatus.setVisible(true);  // Assicurarsi che sia visibile

            AnchorPane.setLeftAnchor(lblConnectionStatus, 60.0);

            // Se il messaggio è "Server Acceso", programmo la sua sparizione dopo 5 secondi
            if ("Server Acceso".equals(status)) {
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.schedule(() -> Platform.runLater(() -> lblConnectionStatus.setText("")), 5, TimeUnit.SECONDS);
                scheduler.shutdown();
            }
        });
    }


    private void disableAllButtons(boolean disable) {
        btnReply.setDisable(disable);
        btnReplyAll.setDisable(disable);
        btnForward.setDisable(disable);
        btnDelete.setDisable(disable);
        inboxBtn.setDisable(disable);
        sentBtn.setDisable(disable);
        trashedBtn.setDisable(disable);
        newEmailBtn.setDisable(disable);
    }

    public ArrayList<String> getAllUsersFromServer() {
        ArrayList<String> allUsers = new ArrayList<>();
        try {
            openConnection();
            Request req = new Request("getUsers");
            sendEmail(req);
            Response resp = getServerResponse();
            if (resp.isSuccess()) {
                allUsers = resp.getUsersList(); // recupera la lista di utenti dalla response
            } else {
                // gestisci errore se necessario
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
        return allUsers;
    }


    /**
     * Classe interna Runnable che chiama UpdateFromServer()
     * Viene utilizzata dal timer per aggiornare le email periodicamente
     */
    class emailDownload implements Runnable {
        public emailDownload() {}
        @Override
        public void run() {
            updateFromServer();
        }
    }

}