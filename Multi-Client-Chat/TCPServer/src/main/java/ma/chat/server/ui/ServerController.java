package ma.chat.server.ui;

import java.io.IOException;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import ma.chat.server.logic.ChatServer;

/** Bridges networking callbacks to the JavaFX dashboard. */
public class ServerController {
    private final ChatServer server;
    private final ServerApplication view;
    private boolean closed;
    private boolean started;

    public ServerController(int port, ServerApplication view) {
        this.server = new ChatServer(port);
        this.view = view;
        server.setLogListener(message -> Platform.runLater(() -> {
            if (!closed) {
                view.logArea.appendText(message + "\n");
            }
        }));
        server.setUsersChangedListener(() -> Platform.runLater(() -> {
            if (!closed) {
                // Read current data here so queued callbacks cannot restore an old list.
                view.updateUsers(server.getActiveUsernames());
            }
        }));
        server.setStatusChangedListener(() -> Platform.runLater(this::updateStatus));
    }

    public void start() {
        if (started || closed) {
            return;
        }
        started = true;
        Thread.startVirtualThread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                server.log("Server error: " + e.getMessage());
            } finally {
                Platform.runLater(this::updateStatus);
            }
        });
    }

    private void updateStatus() {
        if (!closed) {
            boolean running = server.isRunning();
            view.statusLabel.setText(running ? "Running" : "Stopped");
            view.statusIndicator.setFill(running ? Color.LIMEGREEN : Color.GRAY);
        }
    }

    public void shutdown() {
        if (!closed) {
            closed = true;
            server.stop();
        }
    }
}
