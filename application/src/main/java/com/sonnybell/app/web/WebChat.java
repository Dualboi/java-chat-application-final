package com.sonnybell.app.web;

import com.sonnybell.app.chatfunctions.ChatHistory;
import com.sonnybell.app.client.ClientHandler;
import com.sonnybell.app.games.CapitalGame;
import com.sonnybell.app.server.Server;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * WebChat class to handle HTTP requests for the web chat feature.
 * It implements HttpHandler to process GET and POST requests for chat messages.
 */
public class WebChat implements HttpHandler {
    // Constant for HTTP status code 405 Method Not Allowed, used when the method is
    // not supported
    private static final int HTTP_METHOD_NOT_ALLOWED = 405;
    // Constant for HTTP status code 404 Not Found, used when the requested resource
    // is not found
    private static final int HTTP_NOT_FOUND = 404;
    // Constant for HTTP status code 200 OK, used for successful GET requests
    private static final int HTTP_OK = 200;
    // Constant for HTTP status code 204 No Content, used when a POST request is
    // successful
    private static final int HTTP_NO_CONTENT = 204;
    // Constant for unknown content length, used when the response body is empty
    private static final int UNKNOWN_CONTENT_LENGTH = -1;
    // Set to keep track of web users currently logged in via the web interface.
    // This is a thread-safe set to handle concurrent access from multiple web
    // clients.
    private static final java.util.Set<String> WEB_USERS = ConcurrentHashMap.newKeySet();

    /**
     * Handles HTTP requests for the web chat API.
     * It supports GET and POST methods for messages, login, logout, and user
     * listing.
     *
     * @param exchange The HttpExchange object containing request and response data.
     * @throws IOException If an I/O error occurs during request handling.
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // Handle different API endpoints based on the request path
        if ("/api/webchat/messages".equals(path)) {
            // Handle the messages endpoint for both GET and POST methods
            // This endpoint allows clients to retrieve chat messages or post new messages
            if ("GET".equalsIgnoreCase(method)) {
                handleGetMessages(exchange);
            } else if ("POST".equalsIgnoreCase(method)) {
                handlePostMessage(exchange);
            } else {
                exchange.sendResponseHeaders(HTTP_METHOD_NOT_ALLOWED, UNKNOWN_CONTENT_LENGTH);
            }
            // Handle the messages endpoint for both GET and POST methods
        } else if ("/api/webchat/login".equals(path)) {
            // Handle the login endpoint for POST method only
            // This endpoint allows users to log in with a username and password
            if ("POST".equalsIgnoreCase(method)) {
                handleLogin(exchange);
            } else {
                exchange.sendResponseHeaders(HTTP_METHOD_NOT_ALLOWED, UNKNOWN_CONTENT_LENGTH);
            }
            // Handle the login endpoint for POST method only
        } else if ("/api/webchat/status".equals(path)) {
            // Handle the status check endpoint for POST method only
            if ("POST".equalsIgnoreCase(method)) {
                handleStatusCheck(exchange);
            } else {
                exchange.sendResponseHeaders(HTTP_METHOD_NOT_ALLOWED, UNKNOWN_CONTENT_LENGTH);
            }
            // Handle the status check endpoint for POST method only
        } else if ("/api/webchat/logout".equals(path)) {
            // Handle the logout endpoint for POST method only
            // This endpoint allows users to log out and removes them from the web users set
            if ("POST".equalsIgnoreCase(method)) {
                handleLogout(exchange);
            } else {
                exchange.sendResponseHeaders(HTTP_METHOD_NOT_ALLOWED, UNKNOWN_CONTENT_LENGTH);
            }
            // Handle the logout endpoint for POST method only
        } else {
            exchange.sendResponseHeaders(HTTP_NOT_FOUND, UNKNOWN_CONTENT_LENGTH);
        }
    }

    /**
     * Handles GET requests to retrieve chat messages.
     * It returns the chat history as a JSON array.
     *
     * @param exchange The HttpExchange object containing request and response data.
     * @throws IOException If an I/O error occurs during response handling.
     */
    private void handleGetMessages(HttpExchange exchange) throws IOException {
        List<String> messages = ChatHistory.getMessageHistory();
        JSONArray arr = new JSONArray();
        // Convert the chat history to a JSON array
        for (String msg : messages) {
            arr.put(msg);
        }
        // Convert the JSON array to a byte array for the response
        // and set the appropriate headers
        byte[] resp = arr.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(HTTP_OK, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }

    /**
     * Handles POST requests to send a chat message.
     * It expects a JSON body with "user" and "message" fields.
     * The message is added to the chat history and broadcasted to all connected
     * clients.
     *
     * @param exchange The HttpExchange object containing request and response data.
     * @throws IOException If an I/O error occurs during request handling.
     */
    private void handlePostMessage(HttpExchange exchange) throws IOException {
        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject payload = new JSONObject(requestBody);
        String user = payload.optString("user", "");
        String message = payload.optString("message", "");

        // Handle game command
        if (message.startsWith("/startgame")) {
            CapitalGame.startGame();
            ClientHandler.broadcastMessageToAll("GAME START command issued by: " + user);
        } else if (CapitalGame.isGameActive()) {
            boolean wasCorrect = CapitalGame.checkAnswer(user, message);
            if (wasCorrect) {
                sendNoContent(exchange);
                return;
            }
        }

        // Normal chat message from web client
        String formattedMessage = user + ": " + message;
        ClientHandler.broadcastMessageToAll(formattedMessage);

        sendNoContent(exchange);
    }

    /**
     * Handles POST requests for user login.
     * It checks the provided username and password against the server's
     * credentials.
     * If valid, it adds the user to the set of web users and returns a success
     * response.
     *
     * @param exchange The HttpExchange object containing request and response data.
     * @throws IOException If an I/O error occurs during request handling.
     */
    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject obj = new JSONObject(body);
        String username = obj.optString("username", "");
        String password = obj.optString("password", "");
        boolean valid = password.equals(Server.getServerPass()) && !username.isBlank();

        // Adds web user to the set if valid
        if (valid) {
            if (WEB_USERS.add(username)) {
                ClientHandler.addWebClient(username);

                String joinMsg = "SERVER: " + username + " has joined the chat!";
                ChatHistory.addMessageToHistory(joinMsg);
                ClientHandler.logMessage(joinMsg, "HelloUser");
                // Only broadcast, do not log again in broadcastMessageToAll
                ClientHandler.broadcastMessageToAll(joinMsg);
            }
        }

        // Prepare the response
        // with the validity of the login attempt
        JSONObject resp = new JSONObject();
        resp.put("valid", valid);
        byte[] respBytes = resp.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(HTTP_OK, respBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respBytes);
        }
    }

    /**
     * Handles POST requests to check if a user is still logged in.
     *
     * @param exchange The HttpExchange object containing request and response data.
     * @throws IOException If an I/O error occurs during request handling.
     */
    private void handleStatusCheck(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject obj = new JSONObject(body);
        String username = obj.optString("username", "");

        // Check both the local WEB_USERS set and the centralized tracking
        // This ensures that if an admin removes a user, they're properly logged out
        boolean loggedIn = WEB_USERS.contains(username) && ClientHandler.getClientNamesList().contains(username);

        JSONObject resp = new JSONObject();
        resp.put("loggedIn", loggedIn);

        byte[] respBytes = resp.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(HTTP_OK, respBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respBytes);
        }
    }

    /**
     * Handles POST requests for user logout.
     * It removes the user from the set of web users and returns a response
     * indicating success.
     *
     * @param exchange The HttpExchange object containing request and response data.
     * @throws IOException If an I/O error occurs during request handling.
     */
    private void handleLogout(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JSONObject obj = new JSONObject(body);
        String username = obj.optString("username", "");
        boolean removed = WEB_USERS.remove(username);
        if (removed) {
            ClientHandler.removeWebClient(username);

            String leaveMsg = "SERVER: " + username + " has left the chat.";
            ChatHistory.addMessageToHistory(leaveMsg);
            ClientHandler.logMessage(leaveMsg, "GoodbyeUser");
            // Only broadcast, do not log again in broadcastMessageToAll
            ClientHandler.broadcastMessageToAll(leaveMsg);
        }
        // Prepare the response indicating whether the user was removed
        JSONObject resp = new JSONObject();
        resp.put("removed", removed);
        byte[] respBytes = resp.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(HTTP_OK, respBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(respBytes);
        }
    }

    /**
     * Static method to remove a user from the WEB_USERS set.
     * This is used by the moderation system when an admin removes a web user.
     *
     * @param username The username to remove from the web users set.
     * @return true if the user was removed, false if they weren't in the set.
     */
    public static boolean removeFromWebUsers(String username) {
        return WEB_USERS.remove(username);
    }

    private void sendNoContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(HTTP_NO_CONTENT, UNKNOWN_CONTENT_LENGTH);
        exchange.close();
    }
}
