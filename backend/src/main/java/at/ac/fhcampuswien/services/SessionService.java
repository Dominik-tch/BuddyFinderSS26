package at.ac.fhcampuswien.services;

import at.ac.fhcampuswien.models.Session;
import at.ac.fhcampuswien.repositories.SessionRepository;

import java.sql.SQLException;

public class SessionService {
    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {this.sessionRepository = sessionRepository;}

    public Session createSessionForUser(String userId) throws SQLException {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        Session newSession = new Session(userId);
        sessionRepository.createSession(newSession);
        return newSession;
    }

    public String validateTokenAndGetUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Session activeSession = sessionRepository.getSessionById(token);
        if (activeSession != null) {
            return activeSession.getUserId();
        }
        return null;
    }

    public void invalidateSession(String token) throws SQLException {
        if (token != null && !token.isBlank()) {
            sessionRepository.deleteSession(token);
        }
    }
}