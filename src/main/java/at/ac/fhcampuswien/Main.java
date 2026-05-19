package at.ac.fhcampuswien;

import at.ac.fhcampuswien.controllers.HelloController;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    private final static int SERVER_PORT = 8080;

    public static void main(String[] args) throws IOException {
        // Create an HTTP server listening on defined port
        HttpServer server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);

        // Register controllers and their handlers - REST endpoints
        registerController(server, "/api/hello", new HelloController());

        // Start the server
        server.setExecutor(null);
        server.start();
        System.out.printf("Server is running on http://localhost:%d", SERVER_PORT);
    }

    private static void registerController(HttpServer server, String path, HttpHandler handler) {
        HttpContext context = server.createContext(path, handler);
        // Optionally add more configurations to context if needed
    }
}