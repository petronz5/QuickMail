package deva.company.mailclient1.util;

import java.io.Serializable;

public class Request implements Serializable {

    private String request;
    private long lastInbox;
    private long idEmail;
    private String emailAddress;
    private SerializableEmail email;
    private String section;
    private String emailType;

    // Solo per eliminarlo
    public Request(String request, String emailAddress, SerializableEmail email, String section) {
        this.request = request;
        this.emailAddress = emailAddress;
        this.email = email;
        this.section = section;
    }

    public Request(String request) {
        this.request = request;
    }

    // Costruttore per invio email con tipo (nuovo)
    public Request(String request, SerializableEmail email, String emailType) {
        this.request = request;
        this.email = email;
        this.emailType = emailType;
    }

    public Request(String request, String emailAddress, SerializableEmail email) {
        this.request = request;
        this.emailAddress = emailAddress;
        this.email = email;
    }

    // solo per tutte le mail
    public Request(String request, String emailAddress) {
        this.request = request;
        this.emailAddress = emailAddress;
    }

    public Request(String request, String emailAddress, long lastInbox) {
        this.request = request;
        this.emailAddress = emailAddress;
        this.lastInbox = lastInbox;
    }

    // questo per mandare le email
    public Request(String request, SerializableEmail email) {
        this.request = request;
        this.email = email;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public SerializableEmail getEmail() {
        return email;
    }

    public void setEmail(SerializableEmail email) {
        this.email = email;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public long getLastInbox() {
        return lastInbox;
    }

    public String getEmailType() {
        return emailType;
    }

    @Override
    public String toString() {
        return "Request{" +
                "request='" + request + '\'' +
                ", idEmail='" + idEmail + '\'' +
                ", email=" + email +
                '}';
    }

}