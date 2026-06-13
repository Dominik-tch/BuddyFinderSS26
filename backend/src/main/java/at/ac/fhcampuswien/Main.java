package at.ac.fhcampuswien;

import at.ac.fhcampuswien.controllers.ActivityController;
import at.ac.fhcampuswien.controllers.UserController;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    private final static int SERVER_PORT = 8081;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);

        registerController(server, "/api/activities", new ActivityController());
        registerController(server, "/api/users", new UserController());

        DatabaseUtil.initializeDatabase();

        server.setExecutor(null);
        server.start();
        System.out.printf("Server is running on http://localhost:%d", SERVER_PORT);
    }

    private static void registerController(HttpServer server, String path, HttpHandler handler) {
        HttpContext context = server.createContext(path, handler);
    }
}