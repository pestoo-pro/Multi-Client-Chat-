package ma.chat.server.logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Handles the username handshake and incoming lines for one client. */
public class ClientHandler implements Runnable {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private final Socket clientSocket;
    private final ChatServer server;
    private PrintWriter output;
    private String username;
    private boolean readOnly;

    public ClientHandler(Socket clientSocket, ChatServer server) {
        this.clientSocket = clientSocket;
        this.server = server;
    }

    @Override
    public void run() {
        String address = clientSocket.getRemoteSocketAddress().toString();
        try (Socket socket = clientSocket) {
            // Close the socket first: closing a writer can wait behind a blocked send.
            // Socket closure also closes both streams and unblocks their I/O.
            BufferedReader input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter writer = new PrintWriter(
                    socket.getOutputStream(), true, StandardCharsets.UTF_8);
            output = writer;
            String firstLine = input.readLine();
            if (firstLine == null || !firstLine.startsWith("USERNAME:")) {
                server.log("Invalid username handshake: " + address);
                return;
            }
            String suppliedUsername = firstLine.substring("USERNAME:".length());
            readOnly = suppliedUsername.isBlank();
            username = readOnly ? null : suppliedUsername.strip();
            if (!server.registerClient(this)) {
                return;
            }
            server.log((readOnly ? "READ-ONLY client connected" : "Welcome " + username)
                    + " (" + address + ")");

            String message;
            while ((message = input.readLine()) != null) {
                if (readOnly) {
                    sendMessage("READ-ONLY MODE: You cannot send messages.");
                    continue;
                }
                if (message.equals("allUsers")) {
                    sendMessage("Active users: " + String.join(", ", server.getActiveUsernames()));
                } else if (message.equals("bye") || message.equals("end")) {
                    break;
                } else {
                    server.broadcast("[" + LocalTime.now().format(TIME) + "] "
                            + username + ": " + message);
                }
            }
        } catch (IOException e) {
            if (server.isRunning()) {
                server.log("Client connection error (" + address + "): " + e.getMessage());
            }
        } finally {
            server.removeClient(this);
            server.log((readOnly ? "READ-ONLY client disconnected"
                    : username == null ? "Client disconnected" : username + " disconnected")
                    + " (" + address + ")");
        }
    }

    public String getUsername() {
        return username;
    }

    public void sendMessage(String message) {
        output.println(message);
        if (output.checkError()) {
            closeConnection();
        }
    }

    public void closeConnection() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            server.log("Could not close client socket: " + e.getMessage());
        }
    }
}
