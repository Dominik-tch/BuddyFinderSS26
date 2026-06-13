package at.ac.fhcampuswien.controllers;

import at.ac.fhcampuswien.exceptions.DatabaseException;
import at.ac.fhcampuswien.models.Session;
import at.ac.fhcampuswien.models.User;
import at.ac.fhcampuswien.repositories.SessionRepository;
import at.ac.fhcampuswien.repositories.UserRepository;
import at.ac.fhcampuswien.services.SessionService;
import at.ac.fhcampuswien.services.UserService;
import at.ac.fhcampuswien.ResponseFormatter;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public class UserController implements HttpHandler {
    private final UserService userService = new UserService(new UserRepository());
    private final SessionService sessionService = new SessionService(new SessionRepository());

    private final String BASE = "/api/users/";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1); // 204 No Content
            return;
        }

        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            // Route based on the path
            switch (path) {
                case BASE -> handleBaseRequest(method, exchange);
                case BASE + "register" -> handleRegisterRequest(method, exchange);
                case BASE + "login" -> handleLoginRequest(method, exchange);
                case BASE + "logout" -> handleLogoutRequest(method, exchange);
                case BASE + "profile" -> handleProfileRequest(method, exchange);
                case BASE + "editProfile" -> handleEditProfileRequest(method, exchange);
                case BASE + "deleteAccount" -> handleDeleteAccountRequest(method, exchange);
                default -> ResponseFormatter.send(exchange, 404, Map.of("error", "Path not found"));
            }
        } catch (IllegalArgumentException e) {
            ResponseFormatter.send(exchange, 400, Map.of("error", e.getMessage()));
        } catch (JsonSyntaxException e) {
            ResponseFormatter.send(exchange, 400, Map.of("error", "Malformed JSON syntax in request body."));
        } catch (IllegalStateException e) {
            ResponseFormatter.send(exchange, 409, Map.of("error", e.getMessage()));
        } catch (DatabaseException | SQLException e) {
            ResponseFormatter.send(exchange, 500, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            ResponseFormatter.send(exchange, 500, Map.of("error", "An unexpected error occurred."));
        }
    }

    private void handleBaseRequest(String method, HttpExchange exchange) throws IOException {
        switch (method) {
            case "GET" -> ResponseFormatter.send(exchange, 200, Map.of("message", "Base endpoint in /api/users/!"));
            default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
        }
    }

    private void handleRegisterRequest(String method, HttpExchange exchange) throws IOException {
        switch (method) {
            case "POST" -> {
                InputStream is = exchange.getRequestBody();
                User user = getUserFromHttpInputStream(is);

                userService.registerUser(user);

                ResponseFormatter.send(exchange, 201, Map.of("message", "User registered successfully"));
            }
            default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
        }
    }

    private void handleLoginRequest(String method, HttpExchange exchange) throws IOException, SQLException {
        switch (method) {
            case "POST" -> {
                InputStream is = exchange.getRequestBody();
                User loginAttempt = getUserFromHttpInputStream(is);

                // Try to authenticate user
                User authenticatedUser = userService.authenticateUser(loginAttempt.getUserName(), loginAttempt.getPassword());

                if (authenticatedUser != null) {
                    // login successful -> create session
                    Session newSession = sessionService.createSessionForUser(authenticatedUser.getId().toString());

                    ResponseFormatter.send(exchange, 200, Map.of(
                            "message", "Login successful",
                            "sessionID", newSession.getSessionId()
                    ));
                } else {
                    // wrong password or username
                    ResponseFormatter.send(exchange, 401, Map.of("error", "Invalid username or password"));
                }
            }
            default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
        }
    }

    private void handleLogoutRequest(String method, HttpExchange exchange) throws IOException, SQLException {
        switch (method) {
            case "POST" -> {
                // Header "Authorization: Bearer <token>"
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);

                    // delete session from database
                    sessionService.invalidateSession(token);

                    ResponseFormatter.send(exchange, 200, Map.of("message", "Logged out successfully"));
                } else {
                    ResponseFormatter.send(exchange, 400, Map.of("error", "Missing or invalid Authorization header"));
                }
            }
            default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
        }
    }

    private void handleProfileRequest(String method, HttpExchange exchange) throws IOException {
        switch (method) {
            case "GET" -> {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("id=")) {
                    String idStr = query.substring(3);
                    try {
                        User user = userService.getUserById(java.util.UUID.fromString(idStr));
                        if (user != null) {
                            Map<String, String> safeProfile = Map.of(
                                    "userName", user.getUserName(),
                                    "firstName", user.getFirstName() != null ? user.getFirstName() : "-",
                                    "lastName", user.getLastName() != null ? user.getLastName() : "-",
                                    "email", user.getEmail() != null ? user.getEmail() : "-"
                            );
                            ResponseFormatter.send(exchange, 200, safeProfile);
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                ResponseFormatter.send(exchange, 404, Map.of("error", "User not found"));
            }
            default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
        }
    }

    private void handleEditProfileRequest(String method, HttpExchange exchange) throws IOException {
        try {
            // Verify token and get the user's ID
            String userIdString = getAuthenticatedUserId(exchange);
            UUID userId = UUID.fromString(userIdString);

            switch (method) {
                case "GET" -> {
                    User user = userService.getUserById(userId);

                    // Build JSON manually to ensure we NEVER send the password hash to the frontend
                    Map<String, String> safeProfile = Map.of(
                            "userName", user.getUserName(),
                            "email", user.getEmail() != null ? user.getEmail() : "",
                            "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                            "lastName", user.getLastName() != null ? user.getLastName() : ""
                    );

                    ResponseFormatter.send(exchange, 200, safeProfile);
                }
                case "PUT" -> {
                    InputStream is = exchange.getRequestBody();
                    User updatedData = getUserFromHttpInputStream(is);

                    User existingUser = userService.getUserById(userId);

                    existingUser.setEmail(updatedData.getEmail());
                    existingUser.setFirstName(updatedData.getFirstName());
                    existingUser.setLastName(updatedData.getLastName());

                    userService.updateUser(existingUser);

                    ResponseFormatter.send(exchange, 200, Map.of("message", "Profile updated successfully"));
                }
                default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
            }
        } catch (SecurityException e) {
            ResponseFormatter.send(exchange, 401, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            ResponseFormatter.send(exchange, 500, Map.of("error", "An error occurred while processing the profile."));
        }
    }

    private void handleDeleteAccountRequest(String method, HttpExchange exchange) throws IOException {
        try {
            // Verify token and get the user's ID
            String userIdString = getAuthenticatedUserId(exchange);
            UUID userId = UUID.fromString(userIdString);

            switch (method) {
                case "DELETE" -> {
                    // Delete the user from the database
                    userService.deleteUser(userId);

                    // Invalidate the current session
                    String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        sessionService.invalidateSession(token);
                    }

                    ResponseFormatter.send(exchange, 200, Map.of("message", "Account deleted successfully."));
                }
                default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
            }
        } catch (SecurityException e) {
            ResponseFormatter.send(exchange, 401, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            ResponseFormatter.send(exchange, 500, Map.of("error", "An error occurred while deleting the account."));
        }
    }

    private User getUserFromHttpInputStream(InputStream is) throws IOException {
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        if (body.isBlank()) {
            throw new IllegalArgumentException("Request body cannot be empty");
        }
        return parseUser(body);
    }

    private User parseUser(String body) {
        Gson gson = new Gson();
        return gson.fromJson(body, User.class);
    }

    private String getAuthenticatedUserId(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String userId = sessionService.validateTokenAndGetUserId(token);

            if (userId != null) {
                return userId;
            }
        }
        throw new SecurityException("Unauthorized. Please log in.");
    }
}