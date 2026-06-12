package at.ac.fhcampuswien.models;

import java.util.List;
import java.util.UUID;

public class Activity {
    private final UUID id;
    private String title;
    private String owner;
    private int price;
    private String location;
    private int userLimit;
    private String description;
    private int currentParticipants;
    private List<UserPreview> participants;

    private String latitude;
    private String longitude;
    private String weather;

    public Activity() {
        this.id = UUID.randomUUID();
    }

    public Activity(String title, String owner, int price, String location, int userLimit, String description) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.owner = owner;
        this.price = price;
        this.location = location;
        this.userLimit = userLimit;
        this.description = description;
    }

    public Activity(String title, String owner, int price, String location, int userLimit, String description, UUID id) {
        this.id = id;
        this.title = title;
        this.owner = owner;
        this.price = price;
        this.location = location;
        this.userLimit = userLimit;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }
    public String getTitle() {
        return title;
    }
    public String getOwner() {
        return owner;
    }
    public int getPrice() {
        return price;
    }
    public String getLocation() {
        return location;
    }
    public int getUserLimit() {
        return userLimit;
    }
    public String getDescription() {return description;}
    public String getWeather() {return weather;}
    public String getLongitude() {
        return longitude;
    }
    public String getLatitude() {
        return latitude;
    }
    public int getCurrentParticipants() {
        return currentParticipants;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
    public void setParticipants(List<UserPreview> participants) {
        this.participants = participants;
    }
    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }
    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }
    public void setWeather(String weather) {this.weather = weather;}
    public void setCurrentParticipants(int currentParticipants) {
        this.currentParticipants = currentParticipants;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        Activity other = (Activity) object;

        return price == other.price &&
                userLimit == other.userLimit &&
                java.util.Objects.equals(title, other.title) &&
                java.util.Objects.equals(owner, other.owner) &&
                java.util.Objects.equals(location, other.location) &&
                java.util.Objects.equals(description, other.description) &&
                java.util.Objects.equals(latitude, other.latitude) &&
                java.util.Objects.equals(longitude, other.longitude);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(title, owner, price, location, userLimit, description, latitude, longitude);
    }
}