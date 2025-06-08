package com.sonnybell.app.chatfunctions;

import com.sonnybell.app.interfaces.Moderation;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * RemoveUserEndpointHandler class to handle HTTP requests for removing a user.
 * This handler processes DELETE requests to remove a user by their username.
 */
public class RemoveUserEndpointHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        String requestMethod = exchange.getRequestMethod();
        final int getOk = 200;
        final int badRequest = 400;
        final int notFound = 404;
        final int methodNotAllowed = 405;
        final int pathPartsLength = 5;
        final int expectedPathParts = 4;

        if (!"DELETE".equalsIgnoreCase(requestMethod)) {
            String responseText = "Method Not Allowed. Use DELETE for this endpoint.";
            exchange.sendResponseHeaders(methodNotAllowed, responseText.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseText.getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        // Extract username from path: /api/admin/remove-user/{username}
        // The path for this handler will be "/api/admin/remove-user/", so the username
        // is the next part.
        String[] pathParts = requestPath.split("/");
        String usernameToRemove = null;

        // Assuming the context path set in WebServer.java is "/api/admin/remove-user/"
        // and the full request path might be "/api/admin/remove-user/someuser"
        // pathParts will be ["", "api", "admin", "remove-user", "someuser"]
        if (pathParts.length == pathPartsLength) { // Check for 5 parts: "", "api", "admin", "remove-user", "username"
            usernameToRemove = pathParts[expectedPathParts];
        }

        if (usernameToRemove == null || usernameToRemove.trim().isEmpty()) {
            String responseText = "Username cannot be empty or path is malformed.";
            exchange.sendResponseHeaders(badRequest, responseText.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseText.getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        boolean removalInitiated = Moderation.removeUserDirectly(usernameToRemove);
        String responseText;
        int statusCode;

        if (removalInitiated) {
            responseText = "User removal process initiated for: " + usernameToRemove;
            statusCode = getOk;
        } else {
            responseText = "Failed to remove user: " + usernameToRemove
                    + ". User might not exist or an error occurred.";
            statusCode = notFound; // Or 500 if you can distinguish internal errors
        }

        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        byte[] responseBytes = responseText.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}
