package at.ac.fhcampuswien.controllers;

import at.ac.fhcampuswien.ApiUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HelloController implements HttpHandler {
    private final String BASE = "/api/hello/";
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Get the HTTP method (GET, POST, etc.)
        String method = exchange.getRequestMethod();

        // Get the requested URI path (e.g. /api/hello/greet)
        String path = exchange.getRequestURI().getPath();

        // Route based on the path
        switch (path) {
            case BASE -> handleBaseRequest(method, exchange);
            case BASE + "greet" -> handleGreetRequest(method, exchange);
            case BASE + "info" -> handleInfoRequest(method, exchange);
            default -> {
                // Path not found
                String response = "{ \"error\": \"Path not found\" }";
                ApiUtils.sendResponse(exchange, 404, response);
            }
        }
    }

    private void handleBaseRequest(String method, HttpExchange exchange) throws IOException {
        switch (method) {
            case "GET" -> {
                String response = "{ \"message\": \"Base endpoint in /api/hello/!\" }";
                ApiUtils.sendResponse(exchange, 200, response);
            }
            default -> {
                String response = "{ \"error\": \"Method not allowed\" }";
                ApiUtils.sendResponse(exchange, 405, response);
            }
        }
    }

    private void handleGreetRequest(String method, HttpExchange exchange) throws IOException {
        // Handle GET and POST for /api/hello/greet
        switch (method) {
            case "GET" -> {
                String response = "{ \"message\": \"Hello, friend!\" }";
                ApiUtils.sendResponse(exchange, 200, response);
            }
            case "POST" -> {
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String response = "{ \"received\": " + requestBody + " }";
                ApiUtils.sendResponse(exchange, 200, response);
            }
            default -> {
                String response = "{ \"error\": \"Method not allowed\" }";
                ApiUtils.sendResponse(exchange, 405, response);
            }
        }
    }

    private void handleInfoRequest(String method, HttpExchange exchange) throws IOException {
        // Handle GET for /api/hello/info
        switch (method) {
            case "GET" -> {
                String response = "{ \"info\": \"This is the Hello API\" }";
                ApiUtils.sendResponse(exchange, 200, response);
            }
            default -> {
                String response = "{ \"error\": \"Method not allowed\" }";
                ApiUtils.sendResponse(exchange, 405, response);
            }
        }
    }
}
