package deva.company.mailclient1.model;

import deva.company.mailclient1.util.SerializableEmail;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Email implements Serializable {

    private long idEmail;
    private final StringProperty sender;
    private final StringProperty receiver;
    private final StringProperty subject;
    private final StringProperty body;
    private LocalDateTime dateTime;
    private boolean isRead;


    public Email(long idEmail, String sender, String receiver, String subject, String body, LocalDateTime dateTime, boolean isRead) {
        this.idEmail = idEmail;
        this.sender = new SimpleStringProperty(sender);
        this.receiver = new SimpleStringProperty(receiver);
        this.subject = new SimpleStringProperty(subject);
        this.body = new SimpleStringProperty(body);
        this.dateTime = dateTime;
        this.isRead = isRead;
    }

    public Email(long idEmail, String sender) {
        this.idEmail = idEmail;
        this.sender = new SimpleStringProperty(sender);
        this.receiver = new SimpleStringProperty("");
        this.subject = new SimpleStringProperty("");
        this.body = new SimpleStringProperty("");
        this.dateTime = LocalDateTime.now();
        this.isRead = false;
    }

    public Email(SerializableEmail email) {
        this.idEmail = email.getIdEmail();
        this.sender = new SimpleStringProperty(email.getSender());
        this.receiver = new SimpleStringProperty(email.getReceiver());
        this.subject = new SimpleStringProperty(email.getSubject());
        this.body = new SimpleStringProperty(email.getBody());
        this.dateTime = email.getDateTime();
        this.isRead = email.isRead();
    }

    public long getIdEmail() {
        return idEmail;
    }

    public void setIdEmail(long idEmail) {
        this.idEmail = idEmail;
    }

    public StringProperty getSender() {
        return sender;
    }

    public synchronized void setSender(String sender) {
        this.sender.set(sender);
    }

    public StringProperty getReceiver() {
        return receiver;
    }

    public synchronized void setReceiver(String receiver) {
        this.receiver.set(receiver);
    }

    public StringProperty getSubject() { return subject; }

    public synchronized void setSubject(String subject) {
        this.subject.set(subject);
    }

    public StringProperty getBody() {
        return body;
    }

    public synchronized void setBody(String body) {
        this.body.set(body);
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public synchronized void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public boolean isRead() {
        return isRead;
    }

    public synchronized void setRead(boolean read) {
        isRead = read;
    }

    @Override
    public String toString() {
        return subject.get();

    }

}