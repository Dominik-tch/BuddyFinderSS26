package at.ac.fhcampuswien.models;

import java.util.UUID;

public class User {
    private UUID id;
    private String userName;
    private String password;
    private String email;
    private String firstName;
    private String lastName;

    public User() {
        this.id = UUID.randomUUID();
    }

    public User(String userName, String password, String email, String firstName, String lastName) {
        this.id = UUID.randomUUID();
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public User(String userName, String password, String email, String firstName, String lastName, UUID id) {
        this.id = id;
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public UUID getId() {
        return id;
    }
    public String getUserName() {
        return userName;
    }
    public String getPassword() {
        return password;
    }
    public String getEmail() {
        return email;
    }
    public String getFirstName() {
        return firstName;
    }
    public String getLastName() {
        return lastName;
    }

    public void setFirstName(String firstName) {this.firstName = firstName;}
    public void setLastName(String lastName) {this.lastName = lastName;}
    public void setEmail(String email) {this.email = email;}

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        User other = (User) object;

        return java.util.Objects.equals(userName, other.userName) &&
                java.util.Objects.equals(password, other.password) &&
                java.util.Objects.equals(email, other.email) &&
                java.util.Objects.equals(firstName, other.firstName) &&
                java.util.Objects.equals(lastName, other.lastName);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userName, password, email, firstName, lastName);
    }
}