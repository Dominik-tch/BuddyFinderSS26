package at.ac.fhcampuswien.models;

import java.util.UUID;

public class UserPreview {

    private UUID id;
    private String userName;

    public UserPreview(UUID id, String userName) {
        this.id = id;
        this.userName = userName;
    }

    public UUID getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }
}