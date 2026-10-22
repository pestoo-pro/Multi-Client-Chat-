package ma.chat.client.logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

/** Line-based networking with callbacks independent of any UI framework. */
public class ChatClient {
    private final String host;
    private final int port;
    private volatile Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Thread receiver;
    private volatile Consumer<String> messageListener = message -> { };

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setMessageListener(Consumer<String> messageListener) {
        this.messageListener = Objects.requireNonNull(messageListener);
    }

    public void connect() throws IOException {
        if ((socket != null && !socket.isClosed())
                || (receiver != null && receiver.isAlive())) {
            throw new IOException("Client already has an active connection.");
        }
        socket = new Socket(host, port);
        try {
            input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            output = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        } catch (IOException e) {
            socket.close();
            throw e;
        }
        System.out.println("Connected to server: " + socket.getRemoteSocketAddress());
    }

    public void sendUsername(String username) throws IOException {
        if (username == null) {
            throw new IllegalArgumentException("Username must not be null; use an empty string for read-only mode.");
        }
        sendMessage("USERNAME:" + username.strip());
    }

    public void sendMessage(String message) throws IOException {
        if (message == null || message.contains("\n") || message.contains("\r")) {
            throw new IllegalArgumentException("Send one text line at a time.");
        }
        if (socket == null || socket.isClosed()) {
            throw new IOException("Client is not connected.");
        }
        output.println(message);
        if (output.checkError()) {
            throw new IOException("Could not send to the server.");
        }
    }

    /** Listener callbacks run on the receiving virtual thread. */
    public void startReceiving() throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Connection closed before starting the receiver.");
        }
        if (receiver != null && receiver.isAlive()) {
            throw new IllegalStateException("Receiver is already running.");
        }
        Socket connection = socket;
        BufferedReader reader = input;
        receiver = Thread.startVirtualThread(() -> {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    messageListener.accept(message);
                }
            } catch (IOException e) {
                if (!connection.isClosed()) {
                    messageListener.accept("Connection error: " + e.getMessage());
                }
            } finally {
                try {
                    connection.close();
                } catch (IOException e) {
                    messageListener.accept("Could not close connection: " + e.getMessage());
                }
            }
        });
    }

    /** Waits until the receiver finishes, including when the server closes the socket. */
    public void awaitDisconnection() throws InterruptedException {
        if (receiver != null) {
            receiver.join();
        }
    }

    public void disconnect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            // Closing the socket also unblocks the receiver and closes its streams.
            socket.close();
            System.out.println("Disconnected from server.");
        }
    }
}
