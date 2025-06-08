package com.sonnybell.app.web;

import com.sonnybell.app.chatfunctions.RemoveUserEndpointHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WebServer class to start the HTTP server.
 * It creates a new WebServer instance and starts it.
 */
public class WebServer implements Runnable {
    private int port;
    private Instant startTime;

    public WebServer(int port) {
        this.port = port;
        this.startTime = Instant.now();
    }

    /**
     * Main method to start the web server.
     * It creates a new WebServer instance and starts it.
     *
     */
    @Override
    public void run() {
        ExecutorService threadPool = Executors.newCachedThreadPool();
        Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) -> {
            e.printStackTrace();
        });

        try {
            // Create a new HttpServer instance
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", port), 0);

            // Create a WebHandler to handle general requests (files, /api/status)
            WebHandler webHandler = new WebHandler(startTime);
            // Map general requests to the WebHandler
            server.createContext("/", webHandler); // This will handle /api/status and file serving

            // Create and map the new AdminRemoveUserHandler for the specific admin path
            RemoveUserEndpointHandler adminRemoveUserHandler = new RemoveUserEndpointHandler();
            // Map the admin remove user endpoint to the handler
            // api/admin/remove-user/
            server.createContext("/api/admin/remove-user/", adminRemoveUserHandler);
            // Map the WebChat endpoints to the WebChat handler
            // api/webchat/messages, /api/webchat/login, and /api/webchat/logout
            // These endpoints will be handled by the WebChat class
            server.createContext("/api/webchat/messages", new WebChat());
            server.createContext("/api/webchat/login", new WebChat());
            server.createContext("/api/webchat/status", new WebChat()); // Add this line
            server.createContext("/api/webchat/logout", new WebChat()); // Add this line

            // Set executor for handling the requests
            server.setExecutor(threadPool);

            // Start the server
            server.start();
            System.out.println("HTTP web server started on port " + port);
            System.out.println("Visit http://localhost:" + port + " to access the server.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
