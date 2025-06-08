package com.sonnybell.app.client;

import com.sonnybell.app.chatfunctions.ChatHistory;
import com.sonnybell.app.games.CapitalGame;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * ClientHandler class to manage individual client connections.
 * It handles sending and receiving messages for each connected client.
 */
public class ClientHandler implements Runnable {
    // List to keep track of all connected clients (both socket and web clients)
    private static final List<ClientHandler> CLIENT = new CopyOnWriteArrayList<>();

    // Maintain a static set of all connected handlers
    private static final Set<ClientHandler> HANDLERS = new CopyOnWriteArraySet<>();

    // Make these static since they're used in static methods
    private static final String LOG_PATTERN = "%h/MessageLog.log";
    private static final boolean APPEND_MODE = true;

    /**
     * Static variable to keep track of the total number of connected clients.
     * It is incremented when a new client connects and decremented when a client
     * disconnects. This includes both socket clients and web clients.
     */
    private static int clientTotal;

    /**
     * List to keep track of client usernames.
     * This is a synchronized list to ensure thread safety when multiple clients
     * are connected. This includes both socket clients and web clients.
     */
    private static List<String> clientNamesList = Collections.synchronizedList(new ArrayList<>());

    // Socket connected to the client
    private Socket socket;
    // BufferedReader to read messages from the client
    private BufferedReader reader;
    // BufferedWriter to send messages to the client
    private BufferedWriter writer;
    // Username of the client
    private String username;

    /**
     * Constructor to initialize the client handler with a socket.
     *
     * @param socket The socket connected to the client.
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;

        try {
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Reading username after password is validated
            this.username = reader.readLine();
            if (username == null) {
                socket.close();
                return;
            }

            // Getting chat history from the ChatHistory class
            for (String msg : ChatHistory.getMessageHistory()) {
                writer.write(msg);
                writer.newLine();
            }
            writer.write("---END_HISTORY---");
            writer.newLine();
            writer.flush();

            System.out.println("A new user has connected!");

            // Use centralized tracking for socket clients
            synchronized (ClientHandler.class) {
                // Adds the client to the total count of clients
                clientTotal++;

                // Adds the client to the list of usernames
                clientNamesList.add(username);
            }

            // Add this client to the list of connected clients
            CLIENT.add(this);
            HANDLERS.add(this);

            String message = "SERVER: " + username + " has joined the chat!";
            broadcastMessage(message);
        } catch (IOException e) {
            closeEverything();
        }
    }

    /**
     * Method to get the BufferedWriter for sending messages to the client.
     *
     * @return The BufferedWriter for sending messages to the client.
     */
    public BufferedWriter getBufferedWriter() {
        return writer;
    }

    /**
     * Method to get the socket connected to the client.
     *
     * @return The socket connected to the client.
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * Static method to add a web client to tracking.
     * Should be called when a web client connects.
     *
     * @param username The username of the connecting web client.
     */
    public static synchronized void addWebClient(String username) {
        // Adds the web client to the total count of clients
        clientTotal++;

        // Adds the web client to the list of usernames
        clientNamesList.add(username);

        System.out.println("Web user " + username + " has connected!");
    }

    /**
     * Static method to remove a web client from tracking.
     * Should be called when a web client disconnects.
     *
     * @param username The username of the disconnecting web client.
     */
    public static synchronized void removeWebClient(String username) {
        // Removes the web client from the total count of clients
        clientTotal--;

        // Removes the web client from the list of usernames
        clientNamesList.remove(username);

        System.out.println("Web user " + username + " has disconnected!");
    }

    /**
     * Static method to get the total number of connected clients.
     * This includes both socket clients and web clients.
     *
     * @return The total number of connected clients.
     */
    public static List<ClientHandler> getClientList() {
        return CLIENT;
    }

    /**
     * Default constructor for the ClientHandler class.
     * It initializes the client handler with a socket.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Method to get the total number of connected clients.
     *
     * @return The total number of connected clients.
     */
    public static int getClientTotal() {
        return clientTotal;
    }

    public static List<String> getClientNamesList() {
        return clientNamesList;
    }

    /**
     * Method to send messages to all connected clients.
     * It runs in a separate thread to continuously read messages.
     * Contains an if statement to check if the message is "quit" to exit the loop.
     * It also handles IOException when the client disconnects.
     */
    @Override
    public void run() {
        String message;
        while (socket.isConnected()) {
            try {
                message = reader.readLine();
                if (message == null) {
                    break;
                }

                // Skip logging empty or whitespace-only messages
                if (message.trim().isEmpty()) {
                    continue;
                }

                // Check for quit command - don't log it
                if ("quit".equalsIgnoreCase(message.trim())) {
                    break;
                }

                // Parse the message to extract the actual content after "username: "
                String actualMessage = message;
                String prefix = username + ": ";
                if (message.startsWith(prefix)) {
                    actualMessage = message.substring(prefix.length());
                }

                // Check for game commands on the actual message content
                if (actualMessage.startsWith("/")) {
                    handleGameCommands(actualMessage);
                    continue;
                }

                // Check if it's an answer to the current question
                if (CapitalGame.isGameActive()) {
                    boolean wasCorrectAnswer = CapitalGame.checkAnswer(username, actualMessage);
                    if (wasCorrectAnswer) {
                        // Don't broadcast the message if it was a correct answer
                        // The game will handle the announcement
                        continue;
                    }
                }

                // Regular chat message - only log if it's not empty/whitespace
                broadcastMessage(message); // Broadcast the original formatted message

            } catch (IOException e) {
                closeEverything();
                break;
            }
        }
        // Ensure resources are closed and user is removed after loop exits
        closeEverything();
    }

    /**
     * Handle game-related commands.
     */
    private void handleGameCommands(String command) {
        switch (command.toLowerCase()) {
            case "/startgame":
                CapitalGame.startGame();
                break;
            case "/stopgame":
                CapitalGame.stopGame();
                break;
            case "/scores":
                CapitalGame.showScores();
                break;
            case "/gamestatus":
                String status = CapitalGame.getGameStatus();
                // Send status only to the user who requested it
                sendMessage("GAME: " + status);
                break;
            case "/help":
                sendMessage("GAME: Available commands:");
                sendMessage("GAME: /startgame - Start a new capital game");
                sendMessage("GAME: /stopgame - Stop the current game");
                sendMessage("GAME: /scores - Show current scores");
                sendMessage("GAME: /gamestatus - Check game status");
                sendMessage("GAME: /help - Show this help message");
                break;
            default:
                sendMessage("GAME: Unknown command '" + command + "'. Type /help for available commands.");
        }
    }

    /**
     * Send a message to this specific client only.
     *
     * @param message The message to send.
     */
    public void sendMessage(String message) {
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }

    /**
     * Static method to broadcast a message to ALL connected clients.
     * This is used for game messages and server announcements.
     *
     * @param message The message to be sent to all clients.
     */
    public static void broadcastMessageToAll(String message) {
        boolean isJoinOrLeave = message.startsWith("SERVER: ")
                &&
                (message.contains("has joined the chat") || message.contains("has left the chat"));

        // Decide on a tag to label this message
        // This is used to categorize the message for logging purposes
        String tag;
        if (message.startsWith("GAME:")
                ||
                message.startsWith("QUESTION:")
                ||
                message.startsWith("CORRECT!")
                ||
                message.startsWith("TIME'S UP!")
                ||
                message.startsWith("CAPITAL GAME STARTED!")
                ||
                message.startsWith("GAME OVER!")
                ||
                message.startsWith("CURRENT SCORES:")) {
            tag = "GameMessages";
        } else if (message.startsWith("SERVER: ") && message.contains("has been removed by an admin")) {
            tag = "Moderation";
        } else if (message.contains(":") && !isJoinOrLeave) {
            tag = "UserChats";
        } else if (isJoinOrLeave) {
            tag = "HelloUser";
        } else {
            tag = "Server";
        }

        // Only log and add to history if NOT already handled (system events are handled
        // in WebChat)
        if (!isJoinOrLeave) {
            logMessage(message, tag);
            ChatHistory.addMessageToHistory(message);
        }

        for (ClientHandler handler : CLIENT) {
            try {
                handler.writer.write(message);
                handler.writer.newLine();
                handler.writer.flush();
            } catch (IOException e) {
                handler.closeEverything();
            }
        }
    }

    /**
     * Method to broadcast a message to all connected clients except the sender.
     * This is used for regular chat messages.
     *
     * @param message The message to be sent.
     */
    public void broadcastMessage(String message) {
        // Decide on a tag to label this message
        String tag;
        if (message.contains("has joined the chat!")) {
            tag = "HelloUser";
        } else if (message.contains("has left the chat.")) {
            tag = "GoodbyeUser";
        } else {
            tag = "UserChats";
        }

        ChatHistory.addMessageToHistory(message);
        logMessage(message, tag);

        for (ClientHandler client : CLIENT) {
            try {
                if (!client.username.equals(this.username)) {
                    client.writer.write(message);
                    client.writer.newLine();
                    client.writer.flush();
                }
            } catch (IOException e) {
                client.closeEverything();
            }
        }
    }

    /**
     * Method to remove the client handler from the list of connected clients.
     * and broadcast a message indicating the client has left.
     */
    public void removeClientHandler() {
        // Removes the client from the server
        CLIENT.remove(this);
        HANDLERS.remove(this);

        // Use centralized tracking for socket clients
        synchronized (ClientHandler.class) {
            // Removes the client from the total count of clients
            clientTotal--;
            // Removes the client from the list of usernames
            clientNamesList.remove(username);
        }

        String message = "SERVER: " + username + " has left the chat.";
        broadcastMessage(message);
    }

    /**
     * Method to close all resources associated with the client.
     */
    public void closeEverything() {
        removeClientHandler();
        try {
            if (reader != null) {
                reader.close();
            }

            if (writer != null) {
                writer.close();
            }

            if (socket != null) {
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Static method to log messages to a file.
     *
     * @param message The message to be logged.
     * @param tag     The tag to associate with the message.
     */
    public static void logMessage(String message, String tag) {
        String projectDir = System.getProperty("user.dir");
        if (projectDir == null) {
            System.err.println("Could not resolve project directory.");
            return;
        }

        String filePath = LOG_PATTERN.replace("%h", projectDir);
        File file = new File(filePath);

        if (!file.exists()) {
            try {
                boolean created = file.createNewFile();
                if (created) {
                    System.out.println("Log file created.");
                }
            } catch (IOException e) {
                System.err.println("Failed to create log file.");
                e.printStackTrace();
                return;
            }
        }

        try (FileWriter fw = new FileWriter(filePath, APPEND_MODE)) {
            String timestamped = "[" + new java.util.Date() + "] [" + tag + "] " + message;
            fw.write(timestamped + System.lineSeparator());
        } catch (IOException e) {
            System.err.println("Failed to write to log file.");
            e.printStackTrace();
        }
    }

    /**
     * Initiates a shutdown sequence for this client handler, typically triggered by
     * an admin.
     * Sends a "quit" command to the client and then closes all server-side
     * resources for this client.
     */
    public void initiateShutdownByAdmin() {
        try {
            if (socket != null && !socket.isClosed() && writer != null) {
                writer.write("quit"); // Send quit command to the client
                writer.newLine();
                writer.flush();
                // Log that admin initiated quit, if desired
                // logMessage("Admin initiated quit for user: " + username, "Moderation");
            }
        } catch (IOException e) {
            System.err.println("ClientHandler: Error sending 'quit' message during admin removal for " + username + ": "
                    + e.getMessage());
            // Continue with closing everything, as the client might be unresponsive
        } finally {
            // Proceed to close everything on the server side for this client
            // This will also handle removing the client from lists and broadcasting their
            // departure
            closeEverything();
        }
    }
}
