package at.ac.fhcampuswien.repositories;

import at.ac.fhcampuswien.DatabaseUtil;
import at.ac.fhcampuswien.exceptions.DatabaseException;
import at.ac.fhcampuswien.models.Session;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SessionRepository {

    public void createSession(Session session) throws SQLException {
        String sql = "INSERT INTO sessions (session_id, user_id) VALUES (?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, session.getSessionId());
            pstmt.setString(2, session.getUserId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error creating session", e);
        }
    }
    public Session getSessionById(String sessionId) {
        String sql = "SELECT user_id FROM sessions WHERE session_id = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sessionId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String userId = rs.getString("user_id");
                    return new Session(sessionId, userId);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching session from database", e);
        }
    }
    public void deleteSession(String sessionId) throws SQLException {
        String sql = "DELETE FROM sessions WHERE session_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting session", e);
        }
    }
}