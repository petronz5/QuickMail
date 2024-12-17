package deva.company.mailserver.controller;

// Import di tutte le classi necessarie


import deva.company.mailclient1.util.Request;
import deva.company.mailclient1.util.Response;
import deva.company.mailclient1.util.SerializableEmail;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import deva.company.mailserver.model.Server;
import deva.company.mailserver.model.User;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MailServerController {

    private static final int THREAD_NUMBER = 6; //Costante per numero massimo di thread

    private ServerSocket serverSocket;  // Socket del server che accetta connessioni
    private Server server;  //oggetto Server che contiene la logica dell'applicazione
    private ExecutorService fixedThreadPool;    //Un pool di thread fisso per gestire le richieste client in parallelo


    @FXML
    private TextArea logsTxt;
    @FXML
    private Button startBtn;
    @FXML
    private Button stopBtn;


    // Inizializza il controller, collegando le proprietà del testo dell'area log alla log property
    @FXML
    public void initialize() {
        this.server = new Server();
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
        logsTxt.textProperty().bind(server.logProperty());
        logsTxt.setWrapText(true);
    }

    /***
     * Chiude un eventuale precedente ServerSocket e ne crea uno nuovo sulla porta 9000
     * Carica gli utenti e il database delle email
     * Crea un pool di thread
     * Avvia un nuovo thread che in un loop accetta connessioni da parte dei client (accept()),
     * e per ogni connessione crea un ServerRunnable eseguito dal pool di thread.
     */
    @FXML
    public void startServer() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // Chiude qualsiasi istanza precedente
            }

            serverSocket = new ServerSocket(9000); // Riavvia il ServerSocket
            server.loadUsers();     // Carica gli utenti da login.json
            server.readFromDatabase(); // Carica i dati necessari dal database

            server.setRunning(true);

        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        fixedThreadPool = Executors.newFixedThreadPool(THREAD_NUMBER);  // Crea un nuovo pool di thread

        new Thread(() -> {
            while (server.getRunning()) {
                try {
                    Socket requestSocket = serverSocket.accept();
                    //server.addRequest("Connection opened with " + requestSocket.getInetAddress().getHostName() + "\n\n");
                    ServerRunnable sr = new ServerRunnable(requestSocket);
                    fixedThreadPool.execute(sr);
                } catch (SocketException s) {
                    System.out.println("Server socket closed, stopping server");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();

        startBtn.setDisable(true);
        stopBtn.setDisable(false);
        server.addRequest("Server started\n\n");
    }

    /***
     * Chiude il serversocket e il pool di thread
     */
    @FXML
    public void stopServer() {
        System.out.println("Stop server");
        server.handleServerStatus(false);

        // Arresta il pool di thread se attivo
        if (fixedThreadPool != null && !fixedThreadPool.isShutdown()) {
            fixedThreadPool.shutdownNow();
        }

        // Chiudi il serverSocket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                System.out.println("Exception during server stop: " + ex.getMessage());
            }
        }

        startBtn.setDisable(false);
        stopBtn.setDisable(true);
        server.addRequest("Server stopped\n\n");
    }

    // Classe interna 'ServerRunnable' che implementa 'Runnable' per gestire le richieste client
    class ServerRunnable implements Runnable {
        Socket sockets;
        ObjectInputStream objectInputStream;
        ObjectOutputStream objectOutputStream;

        boolean isInitialized = false;

        public ServerRunnable(Socket sockets) {
            this.sockets = sockets;
            try {
                objectOutputStream = new ObjectOutputStream(sockets.getOutputStream());
                objectOutputStream.flush(); // Questo aiuta a sincronizzare il flusso
                // Poi inizializzo l'input
                objectInputStream = new ObjectInputStream(sockets.getInputStream());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        /**
         * Tento di leggere un oggetto 'Request' dallo stream di input e gestisco i tipi di richieste
         * tramite uno switch (per ogni richiesta, loggo l'azione e chiama il metodo appropriato)
         * Loggo il completamento della richiesta e gestisco le eccezioni, chiudo gli stream di I/O e il socket
         * nel blocco finally.
         */
        @Override
        public void run() {
            String emailAddress = null;
            if (objectInputStream == null || objectOutputStream == null) return;
            try {
                Request request = (Request) objectInputStream.readObject();
                Response response;
                switch (request.getRequest()) {
                    case "login" -> {
                        String emailLogin = request.getEmailAddress();
                        if(!emailLogin.endsWith("@unito.it")) {
                            Response resp = new Response(false, "Invalid email address - wrong domain");
                            server.addRequest("Errore: Dominio non corretto per l'email '" + emailLogin + "' durante il login.\n\n");
                            objectOutputStream.writeObject(resp);
                            objectOutputStream.flush();
                            break;
                        }

                        boolean authenticated = server.authenticateUser(emailLogin);
                        if (authenticated) {
                            Response resp = new Response(true, "Login successful");
                            server.addRequest("Utente '" + emailLogin + "' ha fatto l'accesso.\n\n");
                            objectOutputStream.writeObject(resp);
                            objectOutputStream.flush();
                            emailAddress = emailLogin;
                        } else {
                            Response resp = new Response(false, "User not found");
                            server.addRequest("Errore: Utente '" + emailLogin + "' non trovato durante il login.\n\n");
                            objectOutputStream.writeObject(resp);
                            objectOutputStream.flush();
                        }
                    }
                    case "logout" -> {
                        String logoutUser = request.getEmailAddress();
                        server.addRequest("Utente '" + logoutUser + "' è uscito dall'applicazione.\n\n");
                        Response resp = new Response(true, "Logout successful");
                        objectOutputStream.writeObject(resp);
                        objectOutputStream.flush();
                    }
                    case "getAll" -> {
                        //server.addRequest("Client " + request.getEmailAddress() + " is requesting all emails\n\n");
                        response = sendAllEmails(request.getEmailAddress());
                        objectOutputStream.writeObject(response);
                        objectOutputStream.flush();
                    }
                    case "update" -> {
                        //server.addRequest("Client " + request.getEmailAddress() + " is updating emails\n\n");
                        response = updateClient(request);
                        objectOutputStream.writeObject(response);
                        objectOutputStream.flush();
                    }
                    case "submit" -> {
                        //server.addRequest("Client " + request.getEmailAddress() + " is sending an email to " + request.getEmail().getReceiver() + "\n\n");
                        response = sendEmail(request);
                        objectOutputStream.writeObject(response);
                        objectOutputStream.flush();
                    }
                    case "delete" -> {
                        response = deleteEmail(request);
                        objectOutputStream.writeObject(response);
                        objectOutputStream.flush();
                    }
                    case "setRead" -> {
                        response = setEmailRead(request);
                        objectOutputStream.writeObject(response);
                        objectOutputStream.flush();
                    }
                    case "getUsers" -> {
                        // Recupera tutti gli utenti dal server
                        ArrayList<String> allUsers = new ArrayList<>();
                        for (User u : server.getUsers()) {
                            allUsers.add(u.getEmailAddress());
                        }
                        Response resp = new Response(true, "usersList", allUsers, true);
                        objectOutputStream.writeObject(resp);
                        objectOutputStream.flush();
                    }

                }


            }catch (EOFException e) {
                System.out.println("Client disconnected.");
            }
            catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                try {
                    if (objectOutputStream != null) objectOutputStream.close();
                    if (objectInputStream != null) objectInputStream.close();
                    if (sockets != null && !sockets.isClosed()) sockets.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        //Gestisco le richieste di aggiornamento del client
        private Response updateClient(Request request) {
            long lastInbox = request.getLastInbox();
            User user = server.getUser(request.getEmailAddress());

            ArrayList<SerializableEmail> newInbox;

            synchronized (user) { // Sincronizzazione sull'utente per garantire coerenza durante la lettura
                boolean isLast = server.checkLastInbox(user, lastInbox);

                if (!isLast) {
                    newInbox = server.getNewInbox(user, lastInbox);
                    return new Response(true, "update", newInbox);
                } else {
                    return new Response(true, "update");
                }
            }
            // Il client B ha richiesto un aggiornamento della casella postale
            //server.addRequest("");
        }



        //Metodo setEmailRead che gestisce le richieste di impostazione di un'email come letta
        private Response setEmailRead(Request request) {
            try {
                SerializableEmail email = request.getEmail();
                if (email == null) {
                    String errorMessage = "Email is null";
                    server.addRequest("Error: " + errorMessage + " in setEmailRead for " + request.getEmailAddress() + "\n\n");
                    return new Response(false, errorMessage);
                }

                User user = server.getUser(request.getEmailAddress());

                boolean found = false;

                synchronized (user) {
                    for (SerializableEmail inboxEmail : user.getInbox()) {
                        if (inboxEmail.getIdEmail() == email.getIdEmail()) {
                            inboxEmail.setRead(true);
                            found = true;
                            break;
                        }
                    }
                }

                if (found) {
                    server.addRequest("L'utente '" + request.getEmailAddress() + "' ha letto l'email con oggetto '" + email.getSubject() + "'.\n\n");
                } else {
                    String errorMessage = "Email non trovata nella casella di posta";
                    server.addRequest("Errore: " + errorMessage + " per " + request.getEmailAddress() + "\n\n");
                    return new Response(false, errorMessage);
                }

                synchronized (server) {
                    server.saveToDatabase();
                }

                return new Response(true, "Email set as read");
            } catch (Exception e) {
                e.printStackTrace();
                server.addRequest("Internal server error in setEmailRead for " + request.getEmailAddress() + "\n\n");
                return new Response(false, "Internal server error");
            }
        }



        // Gestisco le richieste di invio di TUTTE le email di un utente
        public Response sendAllEmails(String emailAddress) {
            Response response = null;
            User user = server.getUser(emailAddress);

            if (user != null) {
                synchronized (user) {
                    response = new Response(
                            true,
                            "getAll",
                            user.getInbox(),
                            user.getSent(),
                            user.getTrashed()
                    );
                }
            } else {
                response = new Response(
                        false,
                        "User not found, try again"
                );
            }
            return response;
        }



        // Gestisco le richieste con synchronized di invio email nel metodo sendEmail(Request request)
        private Response sendEmail(Request request) {
            SerializableEmail email = (SerializableEmail) request.getEmail();
            long newIdSent = 0;

            String[] usersEmails = email.getReceiver().split(",");
            ArrayList<User> receivers = new ArrayList<>();

            for (String userEmail : usersEmails) {
                String trimmedEmail = userEmail.trim();
                if (!trimmedEmail.endsWith("@unito.it")) {
                    String errorMessage = "Il mittente '" + email.getSender() + "' ha inviato una mail ad un indirizzo con un dominio diverso da @unito.it: " + trimmedEmail;
                    server.addRequest(errorMessage + "\n\n");
                    return new Response(false, "Invalid domain for recipient: " + trimmedEmail);
                }

                User usr = server.getUser(trimmedEmail);
                if (usr == null) {
                    String errorMessage = "Il mittente '" + email.getSender() + "' ha inviato una mail ad un indirizzo inesistente: " + trimmedEmail;
                    server.addRequest(errorMessage + "\n\n");
                    return new Response(false, "Account not found: " + trimmedEmail);
                }
                receivers.add(usr);
            }

            // Sincronizza sull'utente mittente
            User sender = server.getUser(email.getSender());
            synchronized (sender) {
                newIdSent = createNewId(sender.getSent());
                sender.getSent().add(email);
            }

            // Sincronizza sugli utenti destinatari
            for (User usr : receivers) {
                synchronized (usr) {
                    email.setIdEmail(createNewId(usr.getInbox()));
                    usr.getInbox().add(email);
                }
            }

            // Sincronizza il salvataggio su database
            synchronized (server) {
                try {
                    server.saveToDatabase();
                    server.readFromDatabase();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            String recipients = String.join(", ", usersEmails);
            String emailType = request.getEmailType();

            if (emailType == null || emailType.isEmpty() || emailType.equals("new")) {
                // Email normale
                server.addRequest("Il mittente '" + email.getSender() + "' ha inviato una nuova email a '" + recipients + "'.\n\n");
            } else if (emailType.equals("reply")) {
                server.addRequest("L'utente '" + email.getSender() + "' ha risposto alla mail con oggetto '" + email.getSubject() + "' a '" + recipients + "'.\n\n");
            } else if (emailType.equals("replyAll")) {
                server.addRequest("L'utente '" + email.getSender() + "' ha risposto a tutti alla mail con oggetto '" + email.getSubject() + "' a '" + recipients + "'.\n\n");
            } else if (emailType.equals("forward")) {
                server.addRequest("L'utente '" + email.getSender() + "' ha inoltrato la mail con oggetto '" + email.getSubject() + "' a '" + recipients + "'.\n\n");
            }

            return new Response(true, "Email successfully sent", newIdSent);
        }


        // Gestisco le richieste di eliminazione email
        private Response deleteEmail(Request request) {
            try {
                SerializableEmail emailToDelete = (SerializableEmail) request.getEmail();
                User user = server.getUser(request.getEmailAddress());
                String section = request.getSection();
                long idTrashed = 0;
                String message = "";

                synchronized (user) {
                    // Rimozione dell'email dalla sezione appropriata
                    switch (section) {
                        case "inbox":
                            user.getInbox().removeIf(email -> email.getIdEmail() == emailToDelete.getIdEmail());
                            break;
                        case "sent":
                            user.getSent().removeIf(email -> email.getIdEmail() == emailToDelete.getIdEmail());
                            break;
                        case "trashed":
                            user.getTrashed().removeIf(email -> email.getIdEmail() == emailToDelete.getIdEmail());
                            message = "Email successfully deleted from trashed";
                            break;
                    }

                    // Se l'email non è già nel cestino, aggiungila
                    if (!section.equals("trashed")) {
                        idTrashed = createNewId(user.getTrashed());
                        emailToDelete.setIdEmail(idTrashed);
                        user.getTrashed().add(emailToDelete);
                        message = "Email added to trashed";
                    }
                }

                // Sincronizza il salvataggio su database
                synchronized (server) {
                    server.saveToDatabase();
                    server.readFromDatabase();
                }

                if (!section.equals("trashed")) {
                    return new Response(true, message, idTrashed);
                } else {
                    return new Response(true, message);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                return new Response(false, "Internal server error. Please try later");
            }
        }


        // Genero un nuovo ID univoco per email
        private long createNewId(ArrayList<SerializableEmail> list) {
            long id = 0;
            for (SerializableEmail email : list) {
                id = Math.max(email.getIdEmail(), id);
            }
            id++;
            return id;
        }

    }
}