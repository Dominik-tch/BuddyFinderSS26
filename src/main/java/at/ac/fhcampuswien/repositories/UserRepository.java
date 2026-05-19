package at.ac.fhcampuswien.repositories;

import at.ac.fhcampuswien.DatabaseUtil;
import at.ac.fhcampuswien.exceptions.DatabaseException;
import at.ac.fhcampuswien.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class UserRepository {

    public void add(User user) {
        String sql = "INSERT INTO users (id, username, password, email, first_name, last_name) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getId().toString());
            pstmt.setString(2, user.getUserName());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getFirstName());
            pstmt.setString(6, user.getLastName());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DatabaseException("Error adding user to database", e);
        }
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            UUID.fromString(rs.getString("id"))
                    );
                }
            }
            return null;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DatabaseException("Error fetching user by username", e);
        }
    }

    public User findById(UUID id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            UUID.fromString(rs.getString("id"))
                    );
                }
            }
            return null;

        } catch (SQLException e) {
            e.printStackTrace();
            throw new DatabaseException("Error fetching user by id", e);
        }
    }
}