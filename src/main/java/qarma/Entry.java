package qarma;

import java.util.Objects;

public class Entry {
    private String username;
    private String password;
    private String url;
    private String notes;

    public Entry(String username, String password, String url, String notes) {
        this.username = username;
        this.password = password;
        this.url = url;
        this.notes = notes;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

}

