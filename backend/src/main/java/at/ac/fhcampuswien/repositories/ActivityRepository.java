package at.ac.fhcampuswien.repositories;

import at.ac.fhcampuswien.DatabaseUtil;
import at.ac.fhcampuswien.exceptions.ActivityNotFoundException;
import at.ac.fhcampuswien.exceptions.DatabaseException;
import at.ac.fhcampuswien.models.Activity;

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
        String sql = "INSERT INTO activities (id, title, owner, price, location, user_limit, description) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, activity.getId().toString());
            pstmt.setString(2, activity.getTitle());
            pstmt.setString(3, activity.getOwner());
            pstmt.setInt(4, activity.getPrice());
            pstmt.setString(5, activity.getLocation());
            pstmt.setInt(6, activity.getUserLimit());
            pstmt.setString(7, activity.getDescription());

            pstmt.executeUpdate();

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
                    return new Activity(
                            rs.getString("title"),
                            rs.getString("owner"),
                            rs.getInt("price"),
                            rs.getString("location"),
                            rs.getInt("user_limit"),
                            rs.getString("description"),
                            UUID.fromString(rs.getString("id"))
                    );
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
            user_limit = ?
            WHERE id = ?
        """;

        try (
            Connection conn = DatabaseUtil.getConnection();
            PreparedStatement pstmt =
                conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, updatedActivity.getTitle());
            pstmt.setString(2, updatedActivity.getLocation());
            pstmt.setInt(3, updatedActivity.getPrice());
            pstmt.setString(4, updatedActivity.getDescription());
            pstmt.setInt(5, updatedActivity.getUserLimit());
            pstmt.setString(6, id.toString());

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

                activities.add(activity);
            }

        } catch (SQLException e) {
            throw new DatabaseException("Error searching activities", e);
        }

        return activities;
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
}
