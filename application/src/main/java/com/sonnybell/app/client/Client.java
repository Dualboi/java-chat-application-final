package com.sonnybell.app.client;

import com.sonnybell.app.interfaces.MessageListener;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Client class to handle sending and receiving messages from the server.
 * It connects to the server, sends messages, and listens for incoming messages.
 */
public class Client {
    private static int serverPort = 6666;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String username;
    private MessageListener messageListener;

    /**
     * Constructor to initialize the client with a socket and username.
     *
     * @param socket   The socket connected to the server.
     * @param username The username of the client.
     */
    public Client(Socket socket, String username) {
        try {
            this.socket = socket;
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;

            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }

    /**
     * Method to set the server port.
     * This allows the client to connect to a different port if needed.
     *
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * Interface to listen for incoming messages.
     * It can be implemented by other classes to handle messages.
     */
    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    /**
     * Client GUI method to send messages to the server.
     * It reads user input from the console and sends it to the server.
     * It also handles the "quit" command to exit the chat.
     * The quit command is sent directly to the server without waiting for a new
     * line.
     * This allows the client to exit gracefully.
     * the quit command works with the same logic in the ClientHandler class to act
     * as handshake
     * and close the connection.
     */
    public void sendMessage(String messageToSend) {
        try {
            if ("quit".equalsIgnoreCase(messageToSend)) {
                writer.write("quit");
            } else {
                writer.write(username + ": " + messageToSend);
            }
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to send message: " + e.getMessage());
            e.printStackTrace();
            closeEverything();
        }
    }

    /**
     * Method to send messages from the console.
     * It reads user input from the console and sends it to the server.
     * It also handles the "quit" command to exit the chat.
     */
    public void sendMessageFromConsole() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (socket.isConnected()) {
                String messageToSend = scanner.nextLine();
                if (messageToSend.isEmpty()) {
                    continue;
                }
                sendMessage(messageToSend);
                if ("quit".equalsIgnoreCase(messageToSend)) {
                    break;
                }
            }
        }
    }

    /**
     * Method to listen for incoming messages from the server.
     * It runs in a separate thread to continuously read messages.
     */
    public void listenForMessages() {
        Thread listenerThread = new Thread(() -> {
            String msgFromServer;
            try {
                while ((msgFromServer = reader.readLine()) != null) {
                    // Check if server sent a quit command
                    if ("quit".equalsIgnoreCase(msgFromServer.trim())) {
                        System.out.println("[INFO] Server has requested client to quit. Disconnecting...");
                        closeEverything();
                        System.exit(0); // Force application exit
                        break;
                    }

                    if (messageListener != null) {
                        messageListener.onMessageReceived(msgFromServer);
                    } else {
                        System.out.println(msgFromServer);
                    }
                }
                System.out.println("[DEBUG] Server closed the connection (readLine returned null)");
            } catch (IOException e) {
                System.out.println("[ERROR] Error reading from server: " + e.getMessage());
                e.printStackTrace();
            } finally {
                System.out.println("[DEBUG] Listener thread finally block");
                closeEverything();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Add a method to read initial history.
     * It reads the history sent by the server until a special line
     * "---END_HISTORY---".
     */
    public void readInitialHistory() throws IOException {
        // Assume the server sends a special line "---END_HISTORY---" after the history
        String line;
        while ((line = reader.readLine()) != null) {
            if ("---END_HISTORY---".equals(line)) {
                break;
            }
            if (messageListener != null) {
                messageListener.onMessageReceived(line);
            } else {
                System.out.println(line);
            }
        }
    }

    /**
     * Method to close all resources when done.
     * It closes the socket, reader, and writer.
     */
    public void closeEverything() {
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
     * Main method to start the client.
     * It connects to the server and starts listening for messages.
     * It also handles user input for the password and username.
     * The password is sent to the server for validation.
     * If the password is correct, the client proceeds to get the username.
     * The client will keep prompting for the password until a valid one is entered.
     * If no password is entered, the client exits.
     * The client will also exit if the server is not reachable.
     * The client will print an error message if the server is not reachable.
     *
     */
    public static void main(String[] args) {
        final int defaultPort = 6666;
        try (Scanner scanner = new Scanner(System.in)) {
            // prompt user for port number
            System.out.println("Enter server port (default is " + defaultPort + "):");
            String portInput = scanner.nextLine();
            if (portInput != null && !portInput.trim().isEmpty()) {
                try {
                    serverPort = Integer.parseInt(portInput);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid port number. Using default port " + defaultPort + ".");
                    serverPort = defaultPort;
                }
            } else {
                serverPort = defaultPort;
            }

            Socket socket = new Socket("localhost", serverPort);
            BufferedWriter tempWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader tempReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String serverResponse;

            while (true) {
                System.out.println("Enter server password:");
                String clientInputPassword = scanner.nextLine();

                // exit client if no password is entered
                // this is to prevent the client from hanging if the server is not
                if (clientInputPassword == null || clientInputPassword.trim().isEmpty()) {
                    System.out.println("No password entered. Exiting.");
                    socket.close();
                    return;
                }

                // Send the password to the server
                tempWriter.write(clientInputPassword);
                tempWriter.newLine();
                tempWriter.flush();

                // Wait for server response
                serverResponse = tempReader.readLine();

                // Check if the server response is "OK"
                // The "OK" response acts as a simple handshake protocol
                // to verify that the password is correct.
                // If the password is correct, the server will respond with "OK"
                if ("OK".equals(serverResponse)) {
                    break; // Password is correct, exit the loop
                } else {
                    System.out.println("Incorrect password. Please try again.");
                }
            }

            // Password is valid, proceed to get username
            System.out.print("Enter your username: ");
            String username = scanner.nextLine();
            System.out.println("Welcome to the chat application!");

            // Send username to the server
            tempWriter.write(username);
            tempWriter.newLine();
            tempWriter.flush();

            // Close the temporary writer and reader
            Client client = new Client(socket, username);
            client.readInitialHistory();
            client.listenForMessages();
            client.sendMessageFromConsole(); // CLI uses this

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
