package at.ac.fhcampuswien.repositories;

import at.ac.fhcampuswien.DatabaseUtil;
import at.ac.fhcampuswien.exceptions.ActivityNotFoundException;
import at.ac.fhcampuswien.exceptions.DatabaseException;
import at.ac.fhcampuswien.models.Activity;
import at.ac.fhcampuswien.models.User;
import at.ac.fhcampuswien.models.UserPreview;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActivityRepository {
    public List<Activity> findAll() {
        List<Activity> activities = new ArrayList<>();

        String sql = "SELECT * FROM activities";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Activity activity = new Activity(
                        rs.getString("title"),
                        rs.getString("owner"),
                        rs.getInt("price"),
                        rs.getString("location"),
                        rs.getInt("user_limit"),
                        rs.getString("description"),
                        UUID.fromString(rs.getString("id"))
                );
                activity.setLatitude(rs.getString("latitude"));
                activity.setLongitude(rs.getString("longitude"));
                activity.setWeather(rs.getString("weather"));
                activity.setParticipants(getParticipants(activity.getId()));
                activity.setCurrentParticipants(countParticipants(activity.getId()));
                activities.add(activity);
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Error getting activities from database", e);
        }

        return activities;
    }

    public List<Activity> findAllOwned(String owner) {
        List<Activity> activities = new ArrayList<>();

        String sql = "SELECT * FROM activities WHERE owner = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, owner);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Activity activity = new Activity(
                            rs.getString("title"),
                            rs.getString("owner"),
                            rs.getInt("price"),
                            rs.getString("location"),
                            rs.getInt("user_limit"),
                            rs.getString("description"),
                            UUID.fromString(rs.getString("id"))
                    );
                    activity.setLatitude(rs.getString("latitude"));
                    activity.setLongitude(rs.getString("longitude"));
                    activity.setWeather(rs.getString("weather"));
                    activity.setParticipants(getParticipants(activity.getId()));
                    activity.setCurrentParticipants(countParticipants(activity.getId()));
                    activities.add(activity);
                }
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Error getting activities from database", e);
        }

        return activities;
    }

    public List<Activity> findAllJoined(String userId) {
        List<Activity> activities = new ArrayList<>();
        String sql = "SELECT a.* FROM activities a " +
                "INNER JOIN activity_participants ap ON a.id = ap.activity_id " +
                "WHERE ap.user_id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Activity activity = new Activity(
                            rs.getString("title"),
                            rs.getString("owner"),
                            rs.getInt("price"),
                            rs.getString("location"),
                            rs.getInt("user_limit"),
                            rs.getString("description"),
                            UUID.fromString(rs.getString("id"))
                    );
                    activity.setLatitude(rs.getString("latitude"));
                    activity.setLongitude(rs.getString("longitude"));
                    activity.setWeather(rs.getString("weather"));
                    activity.setParticipants(getParticipants(activity.getId()));
                    activity.setCurrentParticipants(countParticipants(activity.getId()));
                    activities.add(activity);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error getting joined activities", e);
        }

        return activities;
    }

    public void joinActivity(UUID userId, UUID activityId) {
        String sql = "INSERT INTO activity_participants (user_id, activity_id) VALUES (?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId.toString());
            pstmt.setString(2, activityId.toString());
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Error joining activity", e);
        }
    }

    public void add(Activity activity) {
        String sql = "INSERT INTO activities (id, title, owner, price, location, user_limit, description, latitude, longitude, weather) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, activity.getId().toString());
            pstmt.setString(2, activity.getTitle());
            pstmt.setString(3, activity.getOwner());
            pstmt.setInt(4, activity.getPrice());
            pstmt.setString(5, activity.getLocation());
            pstmt.setInt(6, activity.getUserLimit());
            pstmt.setString(7, activity.getDescription());
            pstmt.setString(8, activity.getLatitude());
            pstmt.setString(9, activity.getLongitude());
            pstmt.setString(10, activity.getWeather());

            pstmt.executeUpdate();

            try {
                UserRepository userRepo = new UserRepository();
                User user = userRepo.findByUsername(activity.getOwner());
                UUID userUuid = null;
                if (user != null) {
                    userUuid = user.getId();
                } else {
                    userUuid = UUID.fromString(activity.getOwner());
                }
                if (userUuid != null) {
                    joinActivity(userUuid, activity.getId());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error adding activity to database", e);
        }
    }

    public void deleteById(UUID id) {
        String sql = "DELETE FROM activities WHERE id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id.toString());
            int deletedActivities = pstmt.executeUpdate();
            if (deletedActivities == 0) {
                throw new ActivityNotFoundException("Activity not found for deletion");
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error deleting activity", e);
        }
    }

    public Activity getActivityById(UUID id) {
        String sql = "SELECT * FROM activities WHERE id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Activity activity = new Activity(
                            rs.getString("title"),
                            rs.getString("owner"),
                            rs.getInt("price"),
                            rs.getString("location"),
                            rs.getInt("user_limit"),
                            rs.getString("description"),
                            UUID.fromString(rs.getString("id"))
                    );
                    activity.setLatitude(rs.getString("latitude"));
                    activity.setLongitude(rs.getString("longitude"));
                    activity.setWeather(rs.getString("weather"));
                    return activity;
                }
            }
            return null;

        } catch (SQLException e) {
            throw new DatabaseException("Error fetching activity by id", e);
        }
    }

    public void update(UUID id, Activity updatedActivity) {

        String sql = """
            UPDATE activities
            SET
            title = ?,
            location = ?,
            price = ?,
            description = ?,
            user_limit = ?,
            latitude = ?,
            longitude = ?,
            weather = ? 
            WHERE id = ?
        """;

        try (
                Connection conn = DatabaseUtil.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, updatedActivity.getTitle());
            pstmt.setString(2, updatedActivity.getLocation());
            pstmt.setInt(3, updatedActivity.getPrice());
            pstmt.setString(4, updatedActivity.getDescription());
            pstmt.setInt(5, updatedActivity.getUserLimit());
            pstmt.setString(6, updatedActivity.getLatitude());
            pstmt.setString(7, updatedActivity.getLongitude());
            pstmt.setString(8, updatedActivity.getWeather());
            pstmt.setString(9, id.toString());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Error updating activity", e);
        }
    }

    public int countParticipants(UUID activityId) {
        String sql = "SELECT COUNT(*) FROM activity_participants WHERE activity_id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, activityId.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;

        } catch (SQLException e) {
            throw new DatabaseException("Error counting participants for activity", e);
        }
    }

    public List<Activity> search(String title, String location, Integer maxPrice) {

        List<Activity> activities = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT * FROM activities WHERE 1=1"
        );

        List<Object> parameters = new ArrayList<>();

        if (title != null && !title.isBlank()) {
            sql.append(" AND LOWER(title) LIKE LOWER(?)");
            parameters.add("%" + title + "%");
        }

        if (location != null && !location.isBlank()) {
            sql.append(" AND LOWER(location) LIKE LOWER(?)");
            parameters.add("%" + location + "%");
        }

        if (maxPrice != null) {
            sql.append(" AND price <= ?");
            parameters.add(maxPrice);
        }

        try (
            Connection conn = DatabaseUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql.toString())
        )   {

            for (int i = 0; i < parameters.size(); i++) {
                pstmt.setObject(i + 1, parameters.get(i));
            }

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {

                Activity activity = new Activity(
                        rs.getString("title"),
                        rs.getString("owner"),
                        rs.getInt("price"),
                        rs.getString("location"),
                        rs.getInt("user_limit"),
                        rs.getString("description"),
                        UUID.fromString(rs.getString("id"))
                );
                activity.setLatitude(rs.getString("latitude"));
                activity.setLongitude(rs.getString("longitude"));
                activity.setWeather(rs.getString("weather"));
                activity.setParticipants(getParticipants(activity.getId()));
                activity.setCurrentParticipants(countParticipants(activity.getId()));
                activities.add(activity);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error searching activities", e);
        }

        return activities;
    }

    public void leaveActivity(UUID userId, UUID activityId) {

        String sql = """
            DELETE FROM activity_participants
            WHERE user_id = ? AND activity_id = ?
        """;

        try (
            Connection conn = DatabaseUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, userId.toString());
            pstmt.setString(2, activityId.toString());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Error leaving activity", e);
        }
    }

    public boolean isUserJoined(UUID userId, UUID activityId) {

        String sql = """
            SELECT COUNT(*) 
            FROM activity_participants
            WHERE user_id = ? AND activity_id = ?
        """;

        try (
            Connection conn = DatabaseUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

        pstmt.setString(1, userId.toString());
        pstmt.setString(2, activityId.toString());

        ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            return rs.getInt(1) > 0;
        }

        return false;

        } catch (SQLException e) {
            throw new DatabaseException("Error checking joined activity", e);
        }
    }

    public List<UserPreview> getParticipants(UUID activityId) {

        List<UserPreview> participants = new ArrayList<>();

        String sql = """
            SELECT u.id, u.username
            FROM users u
            INNER JOIN activity_participants ap
            ON u.id = ap.user_id
            WHERE ap.activity_id = ?
        """;

        try (
            Connection conn = DatabaseUtil.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, activityId.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                UserPreview user = new UserPreview(UUID.fromString(rs.getString("id")), rs.getString("username")
            );
                participants.add(user);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error getting participants", e);
        }
        return participants;
    }
}
