package com.sonnybell.app;

import com.sonnybell.app.client.Client;
import com.sonnybell.app.javafx.ClientSideGUI;
import com.sonnybell.app.server.Server;

/**
 * Main class to start the application.
 * Usage: java -jar your-app.jar <server|client|GUI>
 */
public final class Main {

    // Prevent instantiation
    private Main() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Main method to start the application based on the provided argument.
     * It can start the server, client, or GUI.
     *
     * @param args Command line arguments: "server", "client", or "GUI"
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -jar your-app.jar <server|client|GUI>");
            System.exit(1);
        }

        String mode = args[0];

        // Check the mode and start the corresponding component
        if ("server".equalsIgnoreCase(mode)) {
            System.out.println("Starting the server...");
            Server.main(args);
        } else if ("client".equalsIgnoreCase(mode)) {
            System.out.println("Starting the client...");
            Client.main(args);
        } else if ("gui".equalsIgnoreCase(mode)) {
            System.out.println("Starting the GUI...");
            ClientSideGUI.main(args); // Replace with your JavaFX main class
        } else {
            System.out.println("Invalid argument. Use 'server', 'client', or 'GUI'.");
            System.exit(1);
        }
    }
}
