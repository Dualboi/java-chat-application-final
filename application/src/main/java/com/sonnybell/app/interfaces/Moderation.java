package com.sonnybell.app.interfaces;

import com.sonnybell.app.client.ClientHandler;
import com.sonnybell.app.web.WebChat;

/**
 * Utility class for moderation-related functionalities.
 * Used for removing users from the chat server,
 * used by admin only.
 */
public interface Moderation {

    /**
     * Removes a user directly from the server's client list.
     * This method is intended to be used by an admin.
     *
     * @param usernameToRemove The username of the user to be removed.
     * @return true if the user was found and removal was initiated, false
     *         otherwise.
     */
    static boolean removeUserDirectly(String usernameToRemove) {
        if (usernameToRemove == null || usernameToRemove.trim().isEmpty()) {
            System.err.println("Moderation: Username to remove cannot be null or empty.");
            return false;
        }

        ClientHandler handlerToRemove = null;
        for (ClientHandler handler : ClientHandler.getClientList()) {
            if (handler.getUsername().equals(usernameToRemove)) {
                handlerToRemove = handler;
                break;
            }
        }

        if (handlerToRemove != null) {
            String message = "SERVER: " + usernameToRemove + " has been removed by an admin.";
            String tag = "Moderation";

            // Log the admin action before initiating shutdown
            ClientHandler.logMessage(message, tag);
            System.out.println(message);

            // Ask the ClientHandler to shut down itself and notify its client.
            // This will also trigger the "user has left" broadcast via closeEverything ->
            // removeClientHandler.
            handlerToRemove.initiateShutdownByAdmin();

            // Broadcast the specific admin removal message to other clients.
            for (ClientHandler client : ClientHandler.getClientList()) {
                // The list should be updated, so handlerToRemove should not be in it.
                // We send to all *other* clients.
                if (!client.getUsername().equals(usernameToRemove)) { // Check username to be sure
                    client.sendMessage(message); // Send the specific admin removal message
                }
            }
            return true;
        } else {
            // Check if it's a web client by looking at the centralized username list
            if (ClientHandler.getClientNamesList().contains(usernameToRemove)) {
                // It's a web client, remove it using the web client removal method
                ClientHandler.removeWebClient(usernameToRemove);

                // Also remove from the WebChat WEB_USERS set
                WebChat.removeFromWebUsers(usernameToRemove);

                String message = "SERVER: " + usernameToRemove + " (Web) has been removed by an admin.";
                String tag = "Moderation";

                ClientHandler.logMessage(message, tag);
                System.out.println(message);

                // Broadcast removal message to all socket clients
                ClientHandler.broadcastMessageToAll(message); // This is a static method, sends to all current socket

                return true;
            } else {
                System.out.println("Moderation: User " + usernameToRemove + " not found.");
                return false;
            }
        }
    }
}
