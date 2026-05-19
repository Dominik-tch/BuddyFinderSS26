package at.ac.fhcampuswien.controllers;

import at.ac.fhcampuswien.ApiUtils;
import at.ac.fhcampuswien.exceptions.ActivityNotFoundException;
import at.ac.fhcampuswien.exceptions.DatabaseException;
import at.ac.fhcampuswien.models.Activity;
import at.ac.fhcampuswien.repositories.ActivityRepository;
import at.ac.fhcampuswien.services.ActivityService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class ActivityController implements HttpHandler {
    private final ActivityService activityService = new ActivityService(new ActivityRepository());

    private final String BASE = "/api/activities/";


    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            // Get the HTTP method (GET, POST, etc.)
            String method = exchange.getRequestMethod();

            // Get the requested URI path (e.g. /api/activities/getAll)
            String path = exchange.getRequestURI().getPath();

            // Normalize the path for dynamic routes before hitting the switch
            String routingPath = path;
            if (path.startsWith(BASE + "delete/")) {
                routingPath = BASE + "delete";
            }

            // Route based on the NORMALIZED path
            switch (routingPath) {
                case BASE -> handleBaseRequest(method, exchange);
                case BASE + "getAll" -> handleGetAllRequest(method, exchange);
                //case BASE + "getAllOwned" -> handleGetAllOwnedRequest(method, exchange);
                case BASE + "add" -> handleAddRequest(method, exchange);
                case BASE + "delete" -> handleDeleteRequest(method, exchange);
                //case BASE + "update" -> handleUpdateRequest(method, exchange);
                //case BASE + "search" -> handleSearchRequest(method, exchange);
                default -> {
                    // Path not found
                    String response = "{ \"error\": \"Path not found\" }";
                    ApiUtils.sendResponse(exchange, 404, response);
                }
            }
        } catch (ActivityNotFoundException e) {
            ApiUtils.sendResponse(exchange, 404, "{\"error\": \"" + e.getMessage() + "\"}");
        } catch (IllegalArgumentException e) {
            ApiUtils.sendResponse(exchange, 400, "{\"error\": \"" + e.getMessage() + "\"}");
        } catch (JsonSyntaxException e) {
            ApiUtils.sendResponse(exchange, 400, "{\"error\": \"Malformed JSON syntax in request body.\"}");
        } catch (IllegalStateException e) {
            ApiUtils.sendResponse(exchange, 409, "{\"error\": \"" + e.getMessage() + "\"}");
        } catch (DatabaseException e) {
            ApiUtils.sendResponse(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            ApiUtils.sendResponse(exchange, 500, "{\"error\": \"An unexpected error occurred.\"}");
        }
    }

    private void handleBaseRequest(String method, HttpExchange exchange) throws IOException {
        switch (method) {
            case "GET" -> {
                String response = "{ \"message\": \"Base endpoint in /api/activities/!\" }";
                ApiUtils.sendResponse(exchange, 200, response);
            }
            default -> {
                String response = "{ \"error\": \"Method not allowed\" }";
                ApiUtils.sendResponse(exchange, 405, response);
            }
        }
    }

    private void handleGetAllRequest(String method, HttpExchange exchange) throws IOException {
        // Handle GET for /api/activities/getAll
        switch (method) {
            case "GET" -> {
                String response = activityToJson(activityService.getAllActivities());
                ApiUtils.sendResponse(exchange, 200, response);
            }
            default -> {
                String response = "{ \"error\": \"Method not allowed\" }";
                ApiUtils.sendResponse(exchange, 405, response);
            }
        }
    }

    private void handleAddRequest(String method, HttpExchange exchange) throws IOException {
        // Handle POST for /api/activities/add
        switch (method) {
            case "POST" -> {
                String response;
                InputStream is = exchange.getRequestBody();
                Activity activity = getActivityFromHttpInputStream(is);
                //Check if activity already exists
                if (activityService.exists(activity)) {
                    throw new IllegalStateException("Activity already exists");
                }
                activityService.addActivity(activity);
                response = "{ \"message\": \"Activity added successfully\" }";
                ApiUtils.sendResponse(exchange, 201, response);
            }
            default -> {
                String response = "{ \"error\": \"Method not allowed\" }";
                ApiUtils.sendResponse(exchange, 405, response);
            }
        }
    }

    private void handleDeleteRequest(String method, HttpExchange exchange) throws IOException {
        // Handle DELETE for /api/activities/delete/{String ID}
        switch (method) {
            case "DELETE" -> {
                String path = exchange.getRequestURI().getPath();
                String[] segments = path.split("/");

                String idString = segments[segments.length - 1];
                UUID id = UUID.fromString(idString);
                activityService.deleteActivity(id);
                String response = "{ \"message\": \"Activity deleted successfully\" }";
                ApiUtils.sendResponse(exchange, 200, response);
            }

            default -> {
                String response = "{ \"error\": \"Method not allowed\" }";
                ApiUtils.sendResponse(exchange, 405, response);
            }
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

    private String activityToJson(List<Activity> activityList) {
        //Builds a Json out of the given Activity list to be able to send it via the API
        Gson gson = new Gson();
        return gson.toJson(activityList);
    }
}
