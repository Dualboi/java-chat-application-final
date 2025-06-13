package com.sonnybell.app.server;

import com.sonnybell.app.client.ClientHandler;
import com.sonnybell.app.web.WebServer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * Server class to handle incoming client connections.
 * It accepts client connections and starts a new thread for each client.
 */
public class Server {
    // Default server port is set to 6666
    private static final int WEB_PORT = 8080;
    private static int serverPort = 6666;
    private static String serverPass;
    private ServerSocket serverSocket;

    /**
     * Constructor to initialize the server with a ServerSocket.
     *
     * @param serverSocket The ServerSocket to accept client connections.
     */
    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public int getServerPort() {
        return serverPort;
    }

    public static String getServerPass() {
        return serverPass;
    }

    /**
     * Method to start the server and accept client connections.
     * It runs in a loop to continuously accept new clients.
     */
    public void startServer() {
        WebServer webServer = new WebServer(WEB_PORT);
        webServer.run();

        try {
            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();

                // Temporary input/output streams for password check
                BufferedReader tempReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter tempWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                String receivedPassword;

                while (true) {
                    receivedPassword = tempReader.readLine();

                    if (receivedPassword == null) {
                        System.out.println("Client disconnected before entering a password.");
                        socket.close();
                        break;
                    }

                    if (receivedPassword.equals(serverPass)) {
                        tempWriter.write("OK");
                        tempWriter.newLine();
                        tempWriter.flush();

                        ClientHandler clientHandler = new ClientHandler(socket);
                        Thread thread = new Thread(clientHandler);
                        thread.start();
                        break;
                    } else {
                        tempWriter.write("Incorrect password. Please try again.");
                        tempWriter.newLine();
                        tempWriter.flush();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    /**
     * Method to close the server socket.
     * It ensures that the server socket is closed properly.
     */
    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Main method to start the server.
     * It creates a new ServerSocket and starts the server.
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            // prompt user for port number
            System.out.println("Enter server port (default is 6666):");
            String portInput = scanner.nextLine();
            if (portInput != null && !portInput.trim().isEmpty()) {
                try {
                    serverPort = Integer.parseInt(portInput);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid port number. Using default port 6666.");
                }
            } else {
                System.out.println("No port entered. Using default port 6666.");
            }
            System.out.println("Create a password to secure the server:");
            String inputPass = scanner.nextLine();
            if (inputPass == null || inputPass.trim().isEmpty()) {
                System.out.println("No password entered. Exiting.");
                return;
            }
            Server.serverPass = inputPass;
            System.out.println("Server password set to: " + inputPass);
            System.out.println("Server is starting...");

            ServerSocket serverSocket = new ServerSocket(serverPort);
            Server server = new Server(serverSocket);
            server.startServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
