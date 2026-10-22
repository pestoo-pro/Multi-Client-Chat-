package ma.chat.client;

import java.io.IOException;
import ma.chat.client.config.ClientConfig;
import ma.chat.client.ui.ClientApplication;

/** Resolves configuration before launching the JavaFX application. */
public final class TCPClient {
    private TCPClient() {
    }

    public static void main(String[] args) {
        ClientConfig config;
        try {
            config = ClientConfig.fromArguments(args);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Configuration error: " + e.getMessage());
            System.exit(1);
            return;
        }
        ClientApplication.launchWithConfig(config);
    }
}
