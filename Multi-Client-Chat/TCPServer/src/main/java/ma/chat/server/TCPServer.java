package ma.chat.server;

import java.io.IOException;
import ma.chat.server.config.ServerConfig;
import ma.chat.server.ui.ServerApplication;

/** Resolves configuration before launching the JavaFX application. */
public final class TCPServer {
    private TCPServer() {
    }

    public static void main(String[] args) {
        ServerConfig config;
        try {
            config = ServerConfig.load();
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Configuration error: " + e.getMessage());
            System.exit(1);
            return;
        }
        ServerApplication.launchWithConfig(config);
    }
}
