package at.ac.fhcampuswien.controllers;

import at.ac.fhcampuswien.ApiUtils;
import at.ac.fhcampuswien.exceptions.DatabaseException;
import at.ac.fhcampuswien.models.Session;
import at.ac.fhcampuswien.models.User;
import at.ac.fhcampuswien.repositories.SessionRepository;
import at.ac.fhcampuswien.repositories.UserRepository;
import at.ac.fhcampuswien.services.SessionService;
import at.ac.fhcampuswien.services.UserService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
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
                default -> {
                    // Path not found
                    String response = "{ \"error\": \"Path not found\" }";
                    ApiUtils.sendResponse(exchange, 404, response);
                }
            }
        } catch (IllegalArgumentException e) {
            ApiUtils.sendResponse(exchange, 400, "{\"error\": \"" + e.getMessage() + "\"}");
        } catch (JsonSyntaxException e) {
            ApiUtils.sendResponse(exchange, 400, "{\"error\": \"Malformed JSON syntax in request body.\"}");
        } catch (IllegalStateException e) {
            ApiUtils.sendResponse(exchange, 409, "{\"error\": \"" + e.getMessage() + "\"}");
        } catch (DatabaseException | SQLException e) {
            ApiUtils.sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            ApiUtils.sendResponse(exchange, 500, "{\"error\": \"An unexpected error occurred.\"}");
        }
    }

    private void handleBaseRequest(String method, HttpExchange exchange) throws IOException {
        switch (method) {
            case "GET" -> {
                String response = "{ \"message\": \"Base endpoint in /api/users/!\" }";
                ApiUtils.sendResponse(exchange, 200, response);
            }
            default -> {
                String response = "{ \"error\": \"Method not allowed\" }";
                ApiUtils.sendResponse(exchange, 405, response);
            }
        }
    }

    private void handleRegisterRequest(String method, HttpExchange exchange) throws IOException {
        // Handle POST for /api/users/register
        switch (method) {
            case "POST" -> {
                InputStream is = exchange.getRequestBody();
                User user = getUserFromHttpInputStream(is);

                userService.registerUser(user);

                String response = "{ \"message\": \"User registered successfully\" }";
                ApiUtils.sendResponse(exchange, 201, response);
            }
            default -> {
                String response = "{ \"error\": \"Method not allowed\" }";
                ApiUtils.sendResponse(exchange, 405, response);
            }
        }
    }

    private void handleLoginRequest(String method, HttpExchange exchange) throws IOException, SQLException {
        // Handle POST for /api/users/login
        switch (method) {
            case "POST" -> {
                InputStream is = exchange.getRequestBody();
                User loginAttempt = getUserFromHttpInputStream(is);

                // Try to authtificate user
                User authenticatedUser = userService.authenticateUser(loginAttempt.getUserName(), loginAttempt.getPassword());

                if (authenticatedUser != null) {
                    //login succesfull -> create session
                    Session newSession = sessionService.createSessionForUser(authenticatedUser.getId().toString());

                    String response = "{ \"message\": \"Login successful\", \"sessionID\": \"" + newSession.getSessionId() + "\" }";
                    ApiUtils.sendResponse(exchange, 200, response);
                } else {
                    // wrong password or username
                    String response = "{ \"error\": \"Invalid username or password\" }";
                    ApiUtils.sendResponse(exchange, 401, response);
                }
            }
            default -> {
                String response = "{ \"error\": \"Method not allowed\" }";
                ApiUtils.sendResponse(exchange, 405, response);
            }
        }
    }

    private void handleLogoutRequest(String method, HttpExchange exchange) throws IOException, SQLException {
        // Handle POST for /api/users/logout
        switch (method) {
            case "POST" -> {
                // Header "Authorization: Bearer <token>"
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);

                    // delete session from database
                    sessionService.invalidateSession(token);

                    String response = "{ \"message\": \"Logged out successfully\" }";
                    ApiUtils.sendResponse(exchange, 200, response);
                } else {
                    String response = "{ \"error\": \"Missing or invalid Authorization header\" }";
                    ApiUtils.sendResponse(exchange, 400, response);
                }
            }
            default -> {
                String response = "{ \"error\": \"Method not allowed\" }";
                ApiUtils.sendResponse(exchange, 405, response);
            }
        }
    }

    private void handleProfileRequest(String method, HttpExchange exchange) throws IOException {
        // Handle GET for /api/users/profile
        switch (method) {
            case "GET" -> {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.startsWith("id=")) {
                    String idStr = query.substring(3);
                    try {
                        User user = userService.getUserById(java.util.UUID.fromString(idStr));
                        if (user != null) {
                            String response = "{" +
                                    "\"userName\":\"" + user.getUserName() + "\"," +
                                    "\"firstName\":\"" + (user.getFirstName() != null ? user.getFirstName() : "-") + "\"," +
                                    "\"lastName\":\"" + (user.getLastName() != null ? user.getLastName() : "-") + "\"," +
                                    "\"email\":\"" + (user.getEmail() != null ? user.getEmail() : "-") + "\"" +
                                    "}";
                            ApiUtils.sendResponse(exchange, 200, response);
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                ApiUtils.sendResponse(exchange, 404, "{\"error\": \"User not found\"}");
            }
            default -> {
                String response = "{ \"error\": \"Method not allowed\" }";
                ApiUtils.sendResponse(exchange, 405, response);
            }
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
                    String response = String.format(
                            "{\"userName\":\"%s\", \"email\":\"%s\", \"firstName\":\"%s\", \"lastName\":\"%s\"}",
                            user.getUserName(),
                            user.getEmail() != null ? user.getEmail() : "",
                            user.getFirstName() != null ? user.getFirstName() : "",
                            user.getLastName() != null ? user.getLastName() : ""
                    );

                    ApiUtils.sendResponse(exchange, 200, response);
                }
                case "PUT" -> {
                    InputStream is = exchange.getRequestBody();
                    User updatedData = getUserFromHttpInputStream(is);

                    User existingUser = userService.getUserById(userId);

                    existingUser.setEmail(updatedData.getEmail());
                    existingUser.setFirstName(updatedData.getFirstName());
                    existingUser.setLastName(updatedData.getLastName());

                    userService.updateUser(existingUser);

                    ApiUtils.sendResponse(exchange, 200, "{ \"message\": \"Profile updated successfully\" }");
                }
                default -> {
                    ApiUtils.sendResponse(exchange, 405, "{ \"error\": \"Method not allowed\" }");
                }
            }
        } catch (SecurityException e) {
            ApiUtils.sendResponse(exchange, 401, "{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            ApiUtils.sendResponse(exchange, 500, "{\"error\": \"An error occurred while processing the profile.\"}");
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