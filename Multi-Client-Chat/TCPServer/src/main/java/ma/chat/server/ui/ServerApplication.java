package ma.chat.server.ui;

import ma.chat.server.config.ServerConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

/** Builds the server dashboard; all user colors belong to this view. */
public class ServerApplication extends Application {
    // JavaFX creates the Application instance; supply validated settings before launch.
    private static ServerConfig configuration;

    public static void launchWithConfig(ServerConfig config) {
        configuration = config;
        Application.launch(ServerApplication.class);
    }

    final Label statusLabel = new Label("Stopped");
    final Circle statusIndicator = new Circle(6, Color.GRAY);
    final TextArea logArea = new TextArea();
    final ListView<String> usersList = new ListView<>();
    private final Map<String, Color> userColors = new HashMap<>();
    private ServerController controller;

    @Override
    public void start(Stage stage) {
        int port = configuration.port();
        Label title = new Label("Chat Server");
        title.getStyleClass().add("screen-title");
        statusLabel.setId("server-status");
        statusIndicator.setId("server-indicator");
        Label portLabel = new Label("Port: " + port);
        portLabel.setId("server-port");
        logArea.setId("server-log");
        logArea.setEditable(false);
        logArea.setWrapText(true);
        usersList.setId("connected-users");
        usersList.setPlaceholder(new Label("No named users connected"));
        usersList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                setText(empty ? null : username);
                setTextFill(Color.BLACK);
                setBackground(empty || username == null ? null : new Background(
                        new BackgroundFill(userColors.get(username),
                                new CornerRadii(4), new Insets(2))));
            }
        });

        GridPane root = new GridPane();
        root.setId("server-dashboard");
        root.setPadding(new Insets(16));
        root.setHgap(16);
        root.setVgap(12);
        ColumnConstraints logsColumn = new ColumnConstraints();
        logsColumn.setPercentWidth(70);
        ColumnConstraints usersColumn = new ColumnConstraints();
        usersColumn.setPercentWidth(30);
        root.getColumnConstraints().addAll(logsColumn, usersColumn);
        root.add(title, 0, 0, 2, 1);
        HBox statusRow = new HBox(10, statusIndicator, statusLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusLabel.getStyleClass().add("status-text");
        root.add(statusRow, 0, 1);
        root.add(portLabel, 1, 1);
        Label activityTitle = new Label("Server activity");
        Label usersTitle = new Label("Connected named users");
        activityTitle.getStyleClass().add("section-title");
        usersTitle.getStyleClass().add("section-title");
        root.add(activityTitle, 0, 2);
        root.add(usersTitle, 1, 2);
        root.add(logArea, 0, 3);
        root.add(usersList, 1, 3);
        GridPane.setVgrow(logArea, Priority.ALWAYS);
        GridPane.setVgrow(usersList, Priority.ALWAYS);

        controller = new ServerController(port, this);
        stage.setTitle("Chat Server - Port " + port);
        Scene scene = new Scene(root, 840, 520);
        scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(640);
        stage.setMinHeight(400);
        stage.setOnCloseRequest(event -> controller.shutdown());
        stage.show();
        controller.start();
    }

    void updateUsers(List<String> usernames) {
        userColors.keySet().retainAll(usernames);
        for (String username : usernames) {
            userColors.computeIfAbsent(username, name -> randomReadableColor());
        }
        usersList.getItems().setAll(usernames);
    }

    private Color randomReadableColor() {
        Color candidate = Color.WHITE;
        for (int attempt = 0; attempt < 40; attempt++) {
            candidate = Color.hsb(ThreadLocalRandom.current().nextDouble(360), 0.28, 0.96);
            double hue = candidate.getHue();
            boolean distinct = userColors.values().stream().allMatch(existing -> {
                double distance = Math.abs(existing.getHue() - hue);
                return Math.min(distance, 360 - distance) >= 35;
            });
            if (distinct) {
                break;
            }
        }
        return candidate;
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }
}
