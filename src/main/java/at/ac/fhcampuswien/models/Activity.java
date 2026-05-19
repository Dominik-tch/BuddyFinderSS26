package at.ac.fhcampuswien.models;

import java.util.UUID;

public class Activity {
    private UUID id;
    private String title;
    private String owner;
    private int price;
    private String location;
    private int userLimit;

    public Activity() {
        this.id = UUID.randomUUID();
    }

    public Activity(String title, String owner, int price, String location, int userLimit) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.owner = owner;
        this.price = price;
        this.location = location;
        this.userLimit = userLimit;
    }

    public Activity(String title, String owner, int price, String location, int userLimit, UUID id) {
        this.id = id;
        this.title = title;
        this.owner = owner;
        this.price = price;
        this.location = location;
        this.userLimit = userLimit;
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

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        Activity other = (Activity) object;

        return price == other.price &&
                userLimit == other.userLimit &&
                java.util.Objects.equals(title, other.title) &&
                java.util.Objects.equals(owner, other.owner) &&
                java.util.Objects.equals(location, other.location);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(title, owner, price, location, userLimit);
    }
}
