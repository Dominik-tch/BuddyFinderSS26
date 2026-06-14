package at.ac.fhcampuswien.controllers;

import at.ac.fhcampuswien.ApiUtils;
import at.ac.fhcampuswien.exceptions.ActivityNotFoundException;
import at.ac.fhcampuswien.exceptions.DatabaseException;
import at.ac.fhcampuswien.models.Activity;
import at.ac.fhcampuswien.repositories.ActivityRepository;
import at.ac.fhcampuswien.repositories.SessionRepository;
import at.ac.fhcampuswien.repositories.UserRepository;
import at.ac.fhcampuswien.services.ActivityService;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ActivityController implements HttpHandler {
    private final ActivityService activityService = new ActivityService(new ActivityRepository());
    private final SessionService sessionService = new SessionService(new SessionRepository());
    private final UserService userService = new UserService(new UserRepository());

    private final String BASE = "/api/activities/";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "http://localhost:8000");
        exchange.getResponseHeaders().add("Access-Control-Allow-Credentials", "true");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1); // 204 No Content
            return;
        }

        try {
            // Get the HTTP method (GET, POST, etc.)
            String method = exchange.getRequestMethod();

            // Get the requested URI path (e.g. /api/activities/getAll)
            String path = exchange.getRequestURI().getPath();

            // Normalize the path for dynamic routes before hitting the switch
            String routingPath = path;
            if (path.startsWith(BASE + "delete/")) {
                routingPath = BASE + "delete";
            } else if (path.startsWith(BASE + "join/")) {
                routingPath = BASE + "join";
            } else if (path.startsWith(BASE + "update/")) {
                routingPath = BASE + "update";
            } else if (path.startsWith(BASE + "leave/")) {
                routingPath = BASE + "leave";
            } else if (path.startsWith(BASE + "weather/")) {
                routingPath = BASE + "weather";
            }

            // Route based on the NORMALIZED path
            switch (routingPath) {
                case BASE -> handleBaseRequest(method, exchange);
                case BASE + "getAll" -> handleGetAllRequest(method, exchange);
                case BASE + "getAllOwned" -> handleGetAllOwnedRequest(method, exchange);
                case BASE + "getAllJoined" -> handleGetAllJoinedRequest(method, exchange);
                case BASE + "add" -> handleAddRequest(method, exchange);
                case BASE + "delete" -> handleDeleteRequest(method, exchange);
                case BASE + "join" -> handljoinRequest(method, exchange);
                case BASE + "search" -> handleSearchRequest(method, exchange);
                case BASE + "update" -> handleUpdateRequest(method, exchange);
                case BASE + "leave" -> handleLeaveRequest(method, exchange);
                case BASE + "weather" -> handlePatchWeatherRequest(method, exchange);
                default -> ResponseFormatter.send(exchange, 404, Map.of("error", "Path not found"));
            }
        } catch (ActivityNotFoundException e) {
            ResponseFormatter.send(exchange, 404, Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            ResponseFormatter.send(exchange, 401, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            ResponseFormatter.send(exchange, 400, Map.of("error", e.getMessage()));
        } catch (JsonSyntaxException e) {
            ResponseFormatter.send(exchange, 400, Map.of("error", "Malformed JSON syntax in request body."));
        } catch (IllegalStateException e) {
            ResponseFormatter.send(exchange, 409, Map.of("error", e.getMessage()));
        } catch (DatabaseException e) {
            ResponseFormatter.send(exchange, 500, Map.of("error", e.getMessage()));
            e.getCause().printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            ResponseFormatter.send(exchange, 500, Map.of("error", "An unexpected error occurred."));
        }
    }

    private void handleBaseRequest(String method, HttpExchange exchange) throws IOException {
        switch (method) {
            case "GET" -> ResponseFormatter.send(exchange, 200, Map.of("message", "Base endpoint in /api/activities/!"));
            default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
        }
    }

    private void handleGetAllOwnedRequest(String method, HttpExchange exchange) throws IOException {
        switch (method) {
            case "GET" -> {
                String userId = getAuthenticatedUserId(exchange);
                String userName = userService.getUserById(UUID.fromString(userId)).getUserName();
                List<Activity> activities = activityService.getAllOwnedActivities(userName);
                ResponseFormatter.send(exchange, 200, activities);
            }
            default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
        }
    }

    private void handleGetAllJoinedRequest(String method, HttpExchange exchange) throws IOException {
        switch (method) {
            case "GET" -> {
                String userId = getAuthenticatedUserId(exchange);
                List<Activity> activities = activityService.getAllJoinedActivities(userId);
                ResponseFormatter.send(exchange, 200, activities);
            }
            default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
        }
    }

    private void handleGetAllRequest(String method, HttpExchange exchange) throws IOException {
        switch (method) {
            case "GET" -> {
                List<Activity> activities = activityService.getAllActivities();
                ResponseFormatter.send(exchange, 200, activities);
            }
            default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
        }
    }

    private void handleAddRequest(String method, HttpExchange exchange) throws IOException {
        switch (method) {
            case "POST" -> {
                String userId = getAuthenticatedUserId(exchange);
                InputStream is = exchange.getRequestBody();
                Activity activity = getActivityFromHttpInputStream(is);

                if (activity.getUserLimit() <= 1) {
                    throw new IllegalArgumentException("The Limit must be at least 2");
                }

                UUID userUUID = UUID.fromString(userId);
                activity.setOwner(userService.getUserById(userUUID).getUserName());

                // use the apis for location, weather, and translation
                fetchAndSetCoordinates(activity);
                fetchAndSetWeather(activity);
                fetchAndSetTranslation(activity);

                // Check if activity already exists
                if (activityService.exists(activity)) {
                    throw new IllegalStateException("Activity already exists");
                }

                activityService.addActivity(activity, userUUID);
                ResponseFormatter.send(exchange, 201, Map.of("message", "Activity added successfully"));
            }
            default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
        }
    }

    private void handleDeleteRequest(String method, HttpExchange exchange) throws IOException {
        switch (method) {
            case "DELETE" -> {
                getAuthenticatedUserId(exchange);
                String path = exchange.getRequestURI().getPath();
                String[] segments = path.split("/");
                String idString = segments[segments.length - 1];
                UUID id = UUID.fromString(idString);

                activityService.deleteActivity(id);
                ResponseFormatter.send(exchange, 200, Map.of("message", "Activity deleted successfully"));
            }
            default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
        }
    }

    private void handljoinRequest(String method, HttpExchange exchange) throws IOException {
        switch (method) {
            case "POST" -> {
                String userId = getAuthenticatedUserId(exchange);
                String path = exchange.getRequestURI().getPath();
                String[] segments = path.split("/");
                String idString = segments[segments.length - 1];

                UUID activityId = UUID.fromString(idString);
                activityService.joinActivity(UUID.fromString(userId), activityId);
                ResponseFormatter.send(exchange, 200, Map.of("message", "Joined activity successfully"));
            }
            default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
        }
    }

    private void handleSearchRequest(String method, HttpExchange exchange) throws IOException {
        switch (method) {
            case "GET" -> {
                String query = exchange.getRequestURI().getQuery();
                String title = null;
                String location = null;
                Integer maxPrice = null;

                if (query != null) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        String[] pair = param.split("=");
                        if (pair.length == 2) {
                            switch (pair[0]) {
                                case "title" -> title = pair[1];
                                case "location" -> location = pair[1];
                                case "maxPrice" -> maxPrice = Integer.parseInt(pair[1]);
                            }
                        }
                    }
                }

                List<Activity> activities = activityService.searchActivities(title, location, maxPrice);
                ResponseFormatter.send(exchange, 200, activities);
            }
            default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
        }
    }

    private void handleUpdateRequest(String method, HttpExchange exchange) throws IOException {
        switch (method) {
            case "PUT" -> {
                getAuthenticatedUserId(exchange);
                String path = exchange.getRequestURI().getPath();
                String[] segments = path.split("/");
                String idString = segments[segments.length - 1];

                UUID id = UUID.fromString(idString);
                InputStream is = exchange.getRequestBody();
                Activity updatedActivity = getActivityFromHttpInputStream(is);

                // use the apis for location, weather, and translation
                fetchAndSetCoordinates(updatedActivity);
                fetchAndSetWeather(updatedActivity);
                fetchAndSetTranslation(updatedActivity);

                activityService.updateActivity(id, updatedActivity);
                ResponseFormatter.send(exchange, 200, Map.of("message", "Activity updated successfully"));
            }
            default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
        }
    }

    private void handleLeaveRequest(String method, HttpExchange exchange) throws IOException {
        switch (method) {
            case "DELETE" -> {
                String userId = getAuthenticatedUserId(exchange);
                String path = exchange.getRequestURI().getPath();
                String[] segments = path.split("/");
                String idString = segments[segments.length - 1];
                UUID activityId = UUID.fromString(idString);

                activityService.leaveActivity(UUID.fromString(userId), activityId);
                ResponseFormatter.send(exchange, 200, Map.of("message", "Left activity successfully"));
            }
            default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
        }
    }

    private void handlePatchWeatherRequest(String method, HttpExchange exchange) throws IOException {
        switch (method) {
            case "PATCH" -> {
                getAuthenticatedUserId(exchange); // Validate auth

                String path = exchange.getRequestURI().getPath();
                String[] segments = path.split("/");
                String idString = segments[segments.length - 1];
                UUID id = UUID.fromString(idString);

                Activity activity = activityService.getActivityById(id);
                // get new weather info
                fetchAndSetWeather(activity);
                // Save the newly fetched weather back to the database
                activityService.updateWeather(id, activity.getWeather());

                // Respond with the new weather so the UI can update instantly
                ResponseFormatter.send(exchange, 200, Map.of(
                        "message", "Weather updated successfully",
                        "weather", activity.getWeather()
                ));
            }
            default -> ResponseFormatter.send(exchange, 405, Map.of("error", "Method not allowed"));
        }
    }

    private void fetchAndSetCoordinates(Activity activity) {
        String locationStr = activity.getLocation() != null ? activity.getLocation() : "Vienna";
        String formattedLocation = java.net.URLEncoder.encode(locationStr, java.nio.charset.StandardCharsets.UTF_8);
        String mapUrl = "https://nominatim.openstreetmap.org/search?q=" + formattedLocation + "&format=json";

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(mapUrl))
                    .header("User-Agent", "BuddyFinderApp/1.0")
                    .GET()
                    .build();
            HttpResponse<String> apiResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
            com.google.gson.JsonArray jsonArray = com.google.gson.JsonParser.parseString(apiResponse.body()).getAsJsonArray();

            if (!jsonArray.isEmpty()) {
                com.google.gson.JsonObject firstResult = jsonArray.get(0).getAsJsonObject();
                activity.setLatitude(firstResult.get("lat").getAsString());
                activity.setLongitude(firstResult.get("lon").getAsString());

                System.out.println("Coordinates successfully fetched and set for: " + locationStr);
            } else {
                System.out.println("Warning: OpenStreetMap returned no results for location: " + locationStr);
            }

        } catch (IOException | InterruptedException e) {
            System.out.println("Warning: Network error calling OpenStreetMap - " + e.getMessage());
        } catch (com.google.gson.JsonSyntaxException | NullPointerException e) {
            System.out.println("Warning: Failed to parse GPS data from OpenStreetMap - " + e.getMessage());
        }
    }

    private void fetchAndSetWeather(Activity activity) {
        if (activity.getLatitude() == null || activity.getLongitude() == null) {
            throw new IllegalArgumentException("Cannot fetch weather without coordinates");
        }

        try {
            String weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude="
                    + activity.getLatitude() + "&longitude=" + activity.getLongitude()
                    + "&current_weather=true";

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(weatherUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            com.google.gson.JsonObject jsonObject = com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();

            if (jsonObject.has("current_weather")) {
                com.google.gson.JsonObject currentWeather = jsonObject.getAsJsonObject("current_weather");

                // Extract data from the JSON
                String temperature = currentWeather.get("temperature").getAsString();
                String windSpeed = currentWeather.get("windspeed").getAsString();
                int weatherCode = currentWeather.get("weathercode").getAsInt();

                // Translate the weather code into a readable word (WMO standard)
                String condition = switch (weatherCode) {
                    case 0 -> "Clear sky";
                    case 1 -> "Mainly clear";
                    case 2 -> "Partly cloudy";
                    case 3 -> "Overcast";
                    case 45, 48 -> "Fog";
                    case 51, 53, 55 -> "Drizzle";
                    case 61 -> "Light rain";
                    case 63 -> "Moderate rain";
                    case 65 -> "Heavy rain";
                    case 66, 67 -> "Freezing rain";
                    case 71 -> "Light snow";
                    case 73 -> "Moderate snow";
                    case 75 -> "Heavy snow";
                    case 77 -> "Snow grains";
                    case 80 -> "Light rain showers";
                    case 81 -> "Moderate rain showers";
                    case 82 -> "Heavy rain showers";
                    case 85, 86 -> "Snow showers";
                    case 95 -> "Thunderstorm";
                    case 96, 99 -> "Thunderstorm with heavy hail";
                    default -> "Unknown";
                };

                // Combine everything into a comma-separated string
                String combinedWeather = temperature + " °C, Wind: " + windSpeed + " km/h, " + condition;

                // Save it to the Activity object
                activity.setWeather(combinedWeather);

                System.out.println("Weather successfully fetched: " + combinedWeather);
            } else {
                System.out.println("Warning: Open-Meteo returned unexpected format.");
            }

        } catch (IOException | InterruptedException e) {
            System.out.println("Warning: Network error calling Open-Meteo - " + e.getMessage());
        } catch (com.google.gson.JsonSyntaxException | IllegalStateException e) {
            System.out.println("Warning: Failed to parse Weather data - " + e.getMessage());
        }
    }

    private void fetchAndSetTranslation(Activity activity) {
        String originalTitle = activity.getTitle();
        if (originalTitle == null || originalTitle.isBlank()) {
            return;
        }

        try {
            String encodedTitle = java.net.URLEncoder.encode(originalTitle, java.nio.charset.StandardCharsets.UTF_8);

            String translateUrl = "https://api.mymemory.translated.net/get?q=" + encodedTitle + "&langpair=Autodetect%7Cen";
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(translateUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            com.google.gson.JsonObject jsonResponse = com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();

            if (jsonResponse.has("responseData")) {
                String translatedText = jsonResponse.getAsJsonObject("responseData").get("translatedText").getAsString();

                // Ensure it doesn't append empty string, exact same text, or the API error message
                if (translatedText != null
                        && !translatedText.trim().isEmpty()
                        && !translatedText.equalsIgnoreCase(originalTitle)
                        && !translatedText.contains("PLEASE SELECT TWO")) {

                    activity.setTitle(originalTitle + " (EN: " + translatedText + ")");
                    System.out.println("MILESTONE 9: Translation successfully fetched: " + translatedText);
                }
            }
        } catch (Exception e) {
            System.out.println("Warning: Translation API failed - " + e.getMessage());
        }
    }

    private Activity getActivityFromHttpInputStream(InputStream is) throws IOException {
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        //Check that the body is not empty
        if (body.isBlank()) {
            throw new IllegalArgumentException("Request body cannot be empty");
        }
        //Generate an activity from the JSON
        Activity activity = parseActivity(body);
        //Check if activity is valid
        if (activityService.isInvalid(activity)) {
            throw new IllegalArgumentException("Activity data is invalid/incomplete");
        }
        return activity;
    }

    private Activity parseActivity(String body) {
        Gson gson = new Gson();
        return gson.fromJson(body, Activity.class);
    }

    private String getAuthenticatedUserId(HttpExchange exchange) {
        java.util.List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        String token = null;

        if (cookies != null) {
            for (String cookieHeader : cookies) {
                String[] cookiePairs = cookieHeader.split("; ");
                for (String cookie : cookiePairs) {
                    if (cookie.startsWith("sessionToken=")) {
                        token = cookie.substring("sessionToken=".length());
                        break;
                    }
                }
            }
        }
        if (token != null) {
            String userId = sessionService.validateTokenAndGetUserId(token);
            if (userId != null) {
                return userId;
            }
        }
        throw new SecurityException("Unauthorized. Please log in.");
    }
}