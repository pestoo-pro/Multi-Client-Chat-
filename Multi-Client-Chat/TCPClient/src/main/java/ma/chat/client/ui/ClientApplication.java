package ma.chat.client.ui;

import ma.chat.client.config.ClientConfig;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/** Builds the client views and manages the JavaFX application lifecycle. */
public class ClientApplication extends Application {
    // JavaFX creates the Application instance; supply validated settings before launch.
    private static ClientConfig configuration;

    public static void launchWithConfig(ClientConfig config) {
        configuration = config;
        Application.launch(ClientApplication.class);
    }

    final TextField usernameField = new TextField();
    final Button connectButton = new Button("CONNECT");
    final Label connectionError = new Label();
    final Label identityLabel = new Label();
    final Label statusLabel = new Label("Offline");
    final Circle statusIndicator = new Circle(6, Color.GRAY);
    final Label readOnlyNotice = new Label(
            "READ-ONLY MODE: You can receive messages but cannot send.");
    final TextArea messagesArea = new TextArea();
    final TextField messageField = new TextField();
    final Button sendButton = new Button("SEND");
    final Button leaveButton = new Button("LEAVE");
    private final GridPane loginPane = new GridPane();
    private final GridPane chatPane = new GridPane();
    private ClientController controller;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        String host = configuration.host();
        int port = configuration.port();
        controller = new ClientController(host, port, this);

        usernameField.setId("username-field");
        usernameField.setPromptText("Username (optional)");
        connectButton.setId("connect-button");
        connectionError.setId("connection-error");
        connectionError.setWrapText(true);
        connectionError.setMaxWidth(520);
        loginPane.setId("identity-screen");
        loginPane.setPadding(new Insets(24));
        loginPane.setHgap(12);
        loginPane.setVgap(12);
        Label title = new Label("Multi-Client Chat");
        title.getStyleClass().add("screen-title");
        loginPane.add(title, 0, 0, 2, 1);
        loginPane.add(new Label("Username"), 0, 1);
        loginPane.add(usernameField, 1, 1);
        Label helper = new Label("Leave blank for READ-ONLY MODE");
        helper.getStyleClass().add("helper-text");
        helper.setWrapText(true);
        loginPane.add(helper, 1, 2);
        loginPane.add(connectButton, 1, 3);
        loginPane.add(connectionError, 0, 4, 2, 1);
        GridPane.setHgrow(usernameField, Priority.ALWAYS);

        identityLabel.setId("identity-label");
        statusLabel.setId("status-label");
        statusIndicator.setId("status-indicator");
        readOnlyNotice.setId("read-only-notice");
        readOnlyNotice.setWrapText(true);
        messagesArea.setId("messages-area");
        messagesArea.setEditable(false);
        messagesArea.setWrapText(true);
        messageField.setId("message-field");
        messageField.setPromptText("Message or command: allUsers, bye, end");
        sendButton.setId("send-button");
        leaveButton.setId("leave-button");
        chatPane.setId("chat-screen");
        chatPane.setPadding(new Insets(16));
        chatPane.setHgap(10);
        chatPane.setVgap(12);
        HBox identityRow = new HBox(10, identityLabel, statusIndicator, statusLabel);
        identityRow.setAlignment(Pos.CENTER_LEFT);
        chatPane.add(identityRow, 0, 0, 3, 1);
        chatPane.add(readOnlyNotice, 0, 1, 3, 1);
        chatPane.add(messagesArea, 0, 2, 3, 1);
        chatPane.add(messageField, 0, 3);
        chatPane.add(sendButton, 1, 3);
        chatPane.add(leaveButton, 2, 3);
        GridPane.setHgrow(messageField, Priority.ALWAYS);
        GridPane.setHgrow(messagesArea, Priority.ALWAYS);
        GridPane.setVgrow(messagesArea, Priority.ALWAYS);
        chatPane.setVisible(false);
        chatPane.setManaged(false);

        connectButton.setOnAction(event -> controller.connect());
        usernameField.setOnAction(event -> controller.connect());
        sendButton.setOnAction(event -> controller.sendMessage());
        messageField.setOnAction(event -> controller.sendMessage());
        leaveButton.setOnAction(event -> controller.leave());
        stage.setOnCloseRequest(event -> controller.shutdown());
        stage.setTitle("Multi-Client Chat - " + host + ":" + port);
        usernameField.getStyleClass().add("input-field");
        messageField.getStyleClass().add("input-field");
        connectButton.getStyleClass().add("primary-button");
        sendButton.getStyleClass().add("primary-button");
        leaveButton.getStyleClass().add("danger-button");
        connectionError.getStyleClass().add("error-text");
        statusLabel.getStyleClass().add("status-text");
        readOnlyNotice.getStyleClass().add("read-only-notice");
        Scene scene = new Scene(new StackPane(loginPane, chatPane), 680, 460);
        scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(580);
        stage.setMinHeight(380);
        stage.show();
        usernameField.requestFocus();
    }

    void showChat(String identity) {
        loginPane.setVisible(false);
        loginPane.setManaged(false);
        chatPane.setVisible(true);
        chatPane.setManaged(true);
        stage.setTitle("Multi-Client Chat - " + identity);
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }
}
