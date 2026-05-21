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
}
