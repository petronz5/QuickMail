package deva.company.mailclient1.controller.client;

import deva.company.mailclient1.model.Email;
import deva.company.mailclient1.model.User;
import deva.company.mailclient1.util.Request;
import deva.company.mailclient1.util.Response;
import deva.company.mailclient1.util.SerializableEmail;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NewEmailController {

    @FXML
    private TextArea bodyMail;

    @FXML
    private TextField txtSubject;

    @FXML
    private TextField txtTo;
    private String emailType;

    private boolean correct = false;

    public User model;
    public ClientController controller;

    /**
     * Metodo di inizializzazione del controller per la nuova email.
     *
     * @param model      L'oggetto User che rappresenta l'utente corrente.
     * @param controller Il controller principale del client.
     * @param btnClicked Il bottone che ha innescato l'azione (es. "reply", "replyAll", "forward").
     */
    public void initializeNewController(User model, ClientController controller, String btnClicked) {
        this.controller = controller;
        this.model = model;
        this.emailType = btnClicked;
        this.model.writeEmail = new Email(0, model.emailAddressProperty().get());
        handleBtnClicked(btnClicked);
        bodyMail.textProperty().bindBidirectional(this.model.writeEmail.getBody());
        txtSubject.textProperty().bindBidirectional(this.model.writeEmail.getSubject());
        txtTo.textProperty().bindBidirectional(this.model.writeEmail.getReceiver());
    }

    /**
     * Gestisce le azioni in base al bottone cliccato (reply, replyAll, forward).
     *
     * @param btn Il nome del bottone cliccato.
     */
    private void handleBtnClicked(String btn) {
        if (btn.equals("reply")) {
            handleReply();
        } else if (btn.equals("replyAll")) {
            handleReplyAll();
        } else if (btn.equals("forward")) {
            handleForward();
        }
    }

    /**
     * Prepara l'email per una risposta diretta al mittente.
     * Aggiorna subject (aggiunge "RE:") e imposta il destinatario come il mittente originale.
     */
    private void handleReply() {
        String currentSubject = model.selectedEmail.getSubject().get();
        if (!currentSubject.startsWith("RE:")) {
            model.writeEmail.setSubject("RE: " + currentSubject);
        } else {
            model.writeEmail.setSubject(currentSubject);
        }
        model.writeEmail.setReceiver(model.selectedEmail.getSender().get());
        // txtTo.setDisable(true); // Opzionale: Disabilita il campo destinatari per impedire modifiche
    }

    /**
     * Prepara l'email per una risposta a tutti.
     * Aggiorna subject (aggiunge "RE_ALL:") e imposta il destinatario come il mittente originale.
     */
    private void handleReplyAll() {
        String currentSubject = model.selectedEmail.getSubject().get();
        if (!currentSubject.startsWith("RE_ALL:")) {
            model.writeEmail.setSubject("RE_ALL: " + currentSubject);
        } else {
            model.writeEmail.setSubject(currentSubject);
        }

        String senderEmail = model.selectedEmail.getSender().get();
        String userEmail = model.emailAddressProperty().get();

        // Ottieni tutti gli utenti dal server
        ArrayList<String> allUsers = controller.getAllUsersFromServer();

        List<String> replyAllRecipients = new ArrayList<>();

        // Aggiungi il mittente originale se non è l'utente corrente
        if (!senderEmail.equalsIgnoreCase(userEmail)) {
            replyAllRecipients.add(senderEmail.trim());
        }

        // Aggiungi tutti gli altri utenti, escludendo l'utente corrente e il mittente originale
        for (String email : allUsers) {
            String trimmedEmail = email.trim();
            // Escludi l'utente attuale e il mittente dell'email originale
            if (!trimmedEmail.equalsIgnoreCase(userEmail) && !trimmedEmail.equalsIgnoreCase(senderEmail)) {
                replyAllRecipients.add(trimmedEmail);
            }
        }

        // Rimuovi eventuali duplicati
        Set<String> uniqueRecipients = new LinkedHashSet<>(replyAllRecipients);

        // Unisci gli indirizzi email separati da virgola
        String recipients = String.join(", ", uniqueRecipients);

        model.writeEmail.setReceiver(recipients);
    }


    private void handleForward() {
        String originalBody = model.selectedEmail.getBody().get();
        String separator = "\n---\n"; // Stringa separatrice

        // Imposta il corpo con il contenuto originale + separatore
        model.writeEmail.setBody(originalBody + separator);
        model.writeEmail.setSubject(model.selectedEmail.getSubject().get());
        txtTo.setDisable(false); // Abilita il campo destinatari in caso di forward

        // Imposta il comportamento del corpo del messaggio per impedire la cancellazione del testo originale
        Platform.runLater(() -> {
            bodyMail.setEditable(true);
            String initialBody = bodyMail.getText();

            // Aggiunge un listener per monitorare i cambiamenti e ripristinare il testo originale se modificato
            bodyMail.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.startsWith(initialBody)) {
                    Platform.runLater(() -> bodyMail.setText(initialBody));
                }
            });

            // Imposta il cursore alla fine del separatore per iniziare a scrivere dopo il contenuto iniziale
            bodyMail.positionCaret(initialBody.length());
        });
    }

    /**
     * Gestisce il click sul bottone "Send".
     *
     * @param event L'evento di azione.
     */
    @FXML
    public synchronized void sendBtnClick(ActionEvent event) {
        handleNewEmail("submit", event);
    }

    /**
     * Gestisce l'invio della nuova email.
     * Se tutto è corretto, invia una Request submit al server
     *
     * @param action L'azione da eseguire
     * @param event  L'evento di azione.
     */
    private void handleNewEmail(String action, ActionEvent event) {
        // Verifica se il campo 'To' è vuoto
        if (txtTo.getText().isEmpty() && action.equals("submit")) {
            Platform.runLater(() -> {
                Alert a = new Alert(Alert.AlertType.ERROR, "Field To: is empty. Add a receiver and try again.");
                a.show();
            });
            return;
        }

        if (txtSubject.getText().isEmpty()) {
            model.writeEmail.setSubject("No subject");
        }

        // Verifica la validità delle email prima di aprire la connessione
        String[] addresses = txtTo.textProperty().get().split(",");
        for (String s : addresses) {
            EmailValidationResult validationResult = validateEmail(s);
            if (validationResult != EmailValidationResult.VALID) {
                // Mostra l'errore e ritorna subito
                Platform.runLater(() -> {
                    String message;
                    if (validationResult == EmailValidationResult.INVALID_DOMAIN) {
                        message = "Errore, il dominio dell'indirizzo email non è corretto: " + s.trim();
                    } else {
                        message = "Errore, questo indirizzo email non esiste: " + s.trim();
                    }
                    Alert a = new Alert(Alert.AlertType.ERROR, message);
                    a.show();
                    txtTo.clear();
                });
                return; // Interrompi subito l'operazione
            }
        }

        try {
            // Solo se tutte le email sono valide, apri la connessione
            controller.openConnection();
            if (action.equals("submit")) {
                controller.sendEmail(new Request(action, new SerializableEmail(model.writeEmail), emailType));
            }

            // Ottieni la risposta del server
            Response res = controller.getServerResponse();
            if (res.isSuccess()) {
                txtTo.textProperty().unbind();
                txtSubject.textProperty().unbind();
                bodyMail.textProperty().unbind();
                model.writeEmail.setIdEmail(res.getIdEmail()); // Ottieni l'ID dell'email dal server
                correct = true;
            } else {
                Platform.runLater(() -> {
                    Alert a = new Alert(Alert.AlertType.ERROR, res.getMessage());
                    a.show();
                });
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            controller.closeConnection();
        }

        if (correct) {
            Platform.runLater(() -> model.sentProperty().add(model.writeEmail));
            Stage stage = (Stage) ((Node) (event.getSource())).getScene().getWindow();
            stage.close();
        }
    }

    /**
     * Gestisce la chiusura della finestra senza inviare l'email
     *
     * @param event L'evento di azione.
     */
    @FXML
    public void handleClose(ActionEvent event) {
        txtTo.textProperty().unbind();
        txtSubject.textProperty().unbind();
        bodyMail.textProperty().unbind();
        Stage stage = (Stage) ((Node) (event.getSource())).getScene().getWindow();
        stage.close();
    }

    /**
     * Enum per rappresentare i possibili risultati della validazione dell'email.
     */
    private enum EmailValidationResult {
        VALID,
        INVALID_DOMAIN,
        INVALID_ADDRESS
    }

    /**
     * Metodo aggiornato per la validazione dell'email.
     * Se non termina con @unito.it restituisce dominio invalido
     *
     * @param email L'indirizzo email da validare.
     * @return Il risultato della validazione come EmailValidationResult.
     */
    private EmailValidationResult validateEmail(String email) {
        String trimmedEmail = email.trim();
        if (!trimmedEmail.endsWith("@unito.it")) {
            return EmailValidationResult.INVALID_DOMAIN;
        }

        return EmailValidationResult.VALID;
    }
}
