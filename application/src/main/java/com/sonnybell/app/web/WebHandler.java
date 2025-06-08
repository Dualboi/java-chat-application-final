package com.sonnybell.app.web;

import com.sonnybell.app.client.ClientHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * WebHandler class that implements HttpHandler to handle HTTP requests.
 * It loads files from the resources directory and serves them as HTTP
 * responses.
 */
public class WebHandler implements HttpHandler {
    private Instant serverStartTime;
    private final int minute = 60;

    /**
     * Constructor to initialize the WebHandler with the server start time.
     *
     * @param serverStartTime The Instant representing the server start time.
     */
    public WebHandler(Instant serverStartTime) {
        this.serverStartTime = serverStartTime;
    }

    /**
     * Gets the server start time.
     *
     * @return The Instant representing the server start time.
     */
    public int getMinute() {
        return minute;
    }

    /**
     * Loads a file from the resources directory.
     *
     * @param fileName The name of the file to load.
     * @return The content of the file as a String, or null if the file is not
     *         found.
     */
    private String loadFile(String fileName) {
        String fileContent = null;
        final int byteSize = 1024;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream != null) {
                // Use a ByteArrayOutputStream to read the InputStream into a byte array
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[byteSize];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, length);
                }
                // Convert the byte array to a string
                fileContent = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
            }
        } catch (IOException e) {
            e.printStackTrace(); // Log the error if necessary, but avoid printing stack traces to users.
        }
        return fileContent;
    }

    /**
     * Handles HTTP requests.
     * This method is called when a request is received.
     * handles html injection of java variables into the html file.
     * is a robust logic for any other html redirects within the index page.
     *
     * @param exchange The HttpExchange object containing the request and response.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        final int getOk = 200;

        // If this is the JSON endpoint
        if ("/api/status".equals(requestPath)) {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            String jsonResponse = buildStatusJson();
            byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(getOk, responseBytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(responseBytes);
            }
            return;
        }

        // Treat "/" as "/index.html"
        if ("/".equals(requestPath)) {
            requestPath = "/index.html";
        }

        // Strip leading slash and treat as file name
        String fileName = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;

        // Try to load the file
        String responseContent = loadFile(fileName);
        if (responseContent == null) {
            return;
        } else {
            if (fileName.endsWith(".html")) {
                // Calculate server uptime
                Duration uptime = Duration.between(serverStartTime, Instant.now());
                long hours = uptime.toHours();
                long minutes = uptime.toMinutes() % minute;
                long seconds = uptime.getSeconds() % minute;

                String uptimeMessage = String.format("Server uptime: %02d:%02d:%02d", hours, minutes, seconds);

                // Inject uptime message into HTML content
                responseContent = responseContent.replace("{{SERVER_UPTIME}}", uptimeMessage);

                // Accessing the total number of clients connected
                String totalClientsMs = String.format("Total clients connected: %d", ClientHandler.getClientTotal());
                // Inject total number of clients connected variable into the CurrentClients
                // page
                responseContent = responseContent.replace("{{TOTAL_CLIENTS}}", totalClientsMs);

                // Accessing the list of client names and adding line breaks
                String clientNamesListMs = String.join("<br>", ClientHandler.getClientNamesList());
                // Inject all the clients connected names into the CurrentClients page
                responseContent = responseContent.replace("{{CURRENT_USERS}}", clientNamesListMs);
            }
        }

        final int time = 200;
        // If the file is not found, return a 404 error
        byte[] responseBytes = responseContent.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(time, responseBytes.length);
        OutputStream out = exchange.getResponseBody();
        out.write(responseBytes);
        out.flush();
        out.close();
    }

    // Helper method to create JSON
    private String buildStatusJson() {
        final int getMinute = 60;
        final int getSecond = 60;
        Duration uptime = Duration.between(serverStartTime, Instant.now());
        long hours = uptime.toHours();
        long minutes = uptime.toMinutes() % getMinute;
        long seconds = uptime.getSeconds() % getSecond;

        String uptimeMessage = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        int totalClients = ClientHandler.getClientTotal();
        String clientNames = String.join(",", ClientHandler.getClientNamesList());

        return String.format(
            "{\"uptime\":\"%s\",\"totalClients\":%d,\"clientNames\":\"%s\"}",
            uptimeMessage, totalClients, clientNames
        );
    }
}
