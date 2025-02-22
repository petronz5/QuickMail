package deva.company.mailclient1.util;


import java.io.Serializable;
import java.util.ArrayList;

public class Response implements Serializable {
    private boolean success;
    private String message;
    private long idEmail;
    private ArrayList<SerializableEmail> inbox;
    private ArrayList<SerializableEmail> sent;
    private ArrayList<SerializableEmail> trashed;
    private ArrayList<String> usersList;


    public Response(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    public Response(boolean success, String message, ArrayList<String> usersList, boolean isUserList) {
        this.success = success;
        this.message = message;
        this.usersList = usersList;
    }


    public Response(boolean success, String message, long idEmail) {
        this.success = success;
        this.message = message;
        this.idEmail = idEmail;
    }

    public Response(boolean success, String message, ArrayList<SerializableEmail> inbox, ArrayList<SerializableEmail> sent, ArrayList<SerializableEmail> trashed) {
        this.success = success;
        this.message = message;
        this.inbox = inbox;
        this.sent = sent;
        this.trashed = trashed;
    }

    public Response(boolean success, String message, ArrayList<SerializableEmail> inbox) {
        this.success = success;
        this.message = message;
        this.inbox = inbox;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    public void setIdEmail(long idEmail) { this.idEmail = idEmail; }
    public long getIdEmail() { return idEmail; }

    public ArrayList<SerializableEmail> getInbox() {
        return inbox;
    }

    public void setInbox(ArrayList<SerializableEmail> inbox) {
        this.inbox = inbox;
    }

    public ArrayList<SerializableEmail> getSent() {
        return sent;
    }

    public void setSent(ArrayList<SerializableEmail> sent) {
        this.sent = sent;
    }

    public ArrayList<SerializableEmail> getTrashed() {
        return trashed;
    }

    public ArrayList<String> getUsersList() {
        return usersList;
    }

    public void setTrashed(ArrayList<SerializableEmail> trashed) {
        this.trashed = trashed;
    }

    @Override
    public String toString() {
        return "Response{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", inbox=" + inbox +
                ", sent=" + sent +
                ", trashed=" + trashed +
                '}';
    }
}