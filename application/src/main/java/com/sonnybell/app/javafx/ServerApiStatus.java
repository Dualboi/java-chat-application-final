package com.sonnybell.app.javafx;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.json.JSONObject;

/**
 * Utility class to poll the server status to update a JavaFX label.
 * Runs asynchronously to avoid blocking the JavaFX application thread.
 * Uses the ClientSideGui thread to ensure UI updates are made on the JavaFX Application Thread.
 * This class provides a method to fetch the current usernames
 * and user amounts on the server and display them in a label.
 * It uses Java's HttpClient to make asynchronous HTTP requests
 * to the server's API endpoint.
 */
public interface ServerApiStatus {

    /**
     * Polls the server status and updates the provided label with
     * the current usernames and user amounts on the server.
     *
     * @param rightLabel The JavaFX label to update with server status.
     */
    static void pollServerStatus(Label rightLabel) {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/api/status"))
                .build();

        // Send the request asynchronously and handle the response
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenAccept(response -> {
                    JSONObject json = new JSONObject(response);
                    int totalClients = json.getInt("totalClients");
                    String clientNames = json.getString("clientNames");
                    Platform.runLater(() -> {
                        rightLabel.setText("Connected Clients: " + totalClients + "\n\n"
                                + "Connected Usernames:\n" + clientNames.replace(",", "\n"));
                    });
                })
                // Handle any exceptions that occur during the request
                .exceptionally(e -> {
                    // Optionally update the label with an error message
                    Platform.runLater(() -> rightLabel.setText("Unable to fetch server status."));
                    return null;
                });
    }
}
