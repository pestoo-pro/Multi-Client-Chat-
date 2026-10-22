package ma.chat.client.ui;

import java.io.IOException;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import ma.chat.client.logic.ChatClient;

/** Coordinates the view through ChatClient, without socket or stream code. */
public class ClientController {
    private final ChatClient client;
    private final ClientApplication view;
    private boolean online;
    private boolean busy;
    private boolean readOnly;
    private volatile boolean closed;

    public ClientController(String host, int port, ClientApplication view) {
        this.client = new ChatClient(host, port);
        this.view = view;
        client.setMessageListener(message -> Platform.runLater(() -> {
            if (!closed) {
                view.messagesArea.appendText(message + "\n");
            }
        }));
    }

    public void connect() {
        if (closed || online || busy) {
            return;
        }
        String username = view.usernameField.getText().strip();
        readOnly = username.isBlank();
        busy = true;
        view.connectionError.setText("Connecting...");
        view.connectButton.setDisable(true);
        view.usernameField.setDisable(true);
        Thread.startVirtualThread(() -> {
            try {
                client.connect();
                if (closed) {
                    client.disconnect();
                    return;
                }
                client.sendUsername(username);
                client.startReceiving();
                Platform.runLater(() -> {
                    if (closed) {
                        return;
                    }
                    online = true;
                    busy = false;
                    view.identityLabel.setText(readOnly ? "READ-ONLY" : username);
                    view.readOnlyNotice.setVisible(readOnly);
                    view.readOnlyNotice.setManaged(readOnly);
                    view.showChat(readOnly ? "READ-ONLY" : username);
                    updateControls();
                    if (!readOnly) {
                        view.messageField.requestFocus();
                    }
                });
                client.awaitDisconnection();
                Platform.runLater(() -> {
                    if (!closed) {
                        online = false;
                        busy = false;
                        updateControls();
                        view.messagesArea.appendText("Disconnected from server.\n");
                    }
                });
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                disconnectQuietly();
                Platform.runLater(() -> {
                    if (!closed) {
                        busy = false;
                        view.connectionError.setText("Could not connect: " + e.getMessage());
                        view.connectButton.setDisable(false);
                        view.usernameField.setDisable(false);
                    }
                });
            }
        });
    }

    public void sendMessage() {
        if (closed || !online || readOnly || busy) {
            return;
        }
        String message = view.messageField.getText();
        if (!message.isEmpty()) {
            send(message, false);
        }
    }

    public void leave() {
        if (closed || !online || busy) {
            return;
        }
        if (readOnly) {
            busy = true;
            updateControls();
            Thread.startVirtualThread(this::disconnectQuietly);
        } else {
            send("bye", true);
        }
    }

    private void send(String message, boolean leaving) {
        busy = true;
        updateControls();
        Thread.startVirtualThread(() -> {
            try {
                client.sendMessage(message);
                Platform.runLater(() -> {
                    if (!closed) {
                        if (!leaving) {
                            view.messageField.clear();
                        }
                        busy = leaving;
                        updateControls();
                        if (!leaving && online) {
                            view.messageField.requestFocus();
                        }
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    if (!closed) {
                        view.messagesArea.appendText("Send failed: " + e.getMessage() + "\n");
                    }
                });
                disconnectQuietly();
            }
        });
    }

    private void updateControls() {
        view.statusLabel.setText(online ? "Online" : "Offline");
        view.statusIndicator.setFill(online ? Color.LIMEGREEN : Color.GRAY);
        view.messageField.setDisable(!online || readOnly || busy);
        view.sendButton.setDisable(!online || readOnly || busy);
        view.leaveButton.setDisable(!online || busy);
    }

    private void disconnectQuietly() {
        try {
            client.disconnect();
        } catch (IOException e) {
            Platform.runLater(() -> {
                if (!closed) {
                    view.messagesArea.appendText("Disconnect failed: " + e.getMessage() + "\n");
                }
            });
        }
    }

    public void shutdown() {
        if (!closed) {
            closed = true;
            // Socket closure is immediate and also unblocks the receiving thread.
            disconnectQuietly();
        }
    }
}
