package at.ac.fhcampuswien;

import com.fasterxml.jackson.dataformat.xml.XmlMapper; // NEW IMPORT
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import at.ac.fhcampuswien.ApiUtils;

import java.io.IOException;

public class ResponseFormatter {
    private static final Gson GSON = new Gson();
    private static final XmlMapper XML_MAPPER = new XmlMapper();

    public static void send(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String acceptHeader = exchange.getRequestHeaders().getFirst("Accept");

        // Determine if the client explicitly wants XML
        boolean isXml = acceptHeader != null && (acceptHeader.contains("application/xml") || acceptHeader.contains("text/xml"));

        String responseBody;
        if (isXml) {
            exchange.getResponseHeaders().set("Content-Type", "application/xml");
            responseBody = toXml(data);
        } else {
            // Default behavior for your existing frontend (JSON)
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            responseBody = GSON.toJson(data);
        }

        // Pass the serialized string to existing ApiUtils
        ApiUtils.sendResponse(exchange, statusCode, responseBody);
    }

    private static String toXml(Object data) {
        try {
            // Converts the Java Object (Activity, User, Map, etc.) to an XML string
            return XML_MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "<error>Failed to serialize to XML</error>";
        }
    }
}