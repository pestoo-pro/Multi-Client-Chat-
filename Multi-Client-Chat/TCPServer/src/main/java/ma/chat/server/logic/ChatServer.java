package ma.chat.server.logic;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** JavaFX-independent networking, client tracking, and lifecycle callbacks. */
public class ChatServer {
    private final int port;
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    // Includes connections still waiting for their username handshake.
    private final Set<ClientHandler> connections = ConcurrentHashMap.newKeySet();
    private final Object lifecycleLock = new Object();
    private volatile boolean running;
    private boolean stopRequested;
    private ServerSocket serverSocket;
    private Consumer<String> logListener = message -> { };
    private Runnable usersChangedListener = () -> { };
    private Runnable statusChangedListener = () -> { };

    public ChatServer(int port) {
        this.port = port;
    }

    // Configure listeners before starting the server.
    public void setLogListener(Consumer<String> listener) {
        logListener = listener;
    }

    public void setUsersChangedListener(Runnable listener) {
        usersChangedListener = listener;
    }

    public void setStatusChangedListener(Runnable listener) {
        statusChangedListener = listener;
    }

    public boolean isRunning() {
        return running;
    }

    public void log(String message) {
        System.out.println(message);
        logListener.accept(message);
    }

    public void start() throws IOException {
        synchronized (lifecycleLock) {
            if (stopRequested) {
                return;
            }
            if (running) {
                throw new IllegalStateException("Server is already running.");
            }
            serverSocket = new ServerSocket(port);
            running = true;
        }
        try {
            log("Server Started");
            log("Listening on port " + serverSocket.getLocalPort());
            log("Waiting for clients...");
            statusChangedListener.run();
            while (running) {
                Socket socket = serverSocket.accept();
                synchronized (lifecycleLock) {
                    if (!running) {
                        socket.close();
                        break;
                    }
                    ClientHandler handler = new ClientHandler(socket, this);
                    connections.add(handler);
                    log("Client connected: " + socket.getRemoteSocketAddress());
                    Thread.startVirtualThread(handler);
                }
            }
        } catch (IOException e) {
            if (running) {
                throw e;
            }
            // Closing the listening socket during stop() intentionally ends accept().
        } finally {
            stop();
        }
    }

    public void stop() {
        synchronized (lifecycleLock) {
            if (stopRequested) {
                return;
            }
            stopRequested = true;
            running = false;
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    log("Could not close server socket: " + e.getMessage());
                }
            }
            for (ClientHandler handler : connections) {
                handler.closeConnection();
            }
        }
        statusChangedListener.run();
        log("Server Stopped");
    }

    public boolean registerClient(ClientHandler client) {
        synchronized (lifecycleLock) {
            if (!running) {
                return false;
            }
            clients.add(client);
        }
        usersChangedListener.run();
        return true;
    }

    public void removeClient(ClientHandler client) {
        connections.remove(client);
        if (clients.remove(client)) {
            usersChangedListener.run();
        }
    }

    public List<String> getActiveUsernames() {
        return clients.stream()
                .map(ClientHandler::getUsername)
                .filter(username -> username != null)
                .sorted()
                .toList();
    }

    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
}