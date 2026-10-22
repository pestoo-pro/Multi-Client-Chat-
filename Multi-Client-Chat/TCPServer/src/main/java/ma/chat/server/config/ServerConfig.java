package ma.chat.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Validated listening port, loaded before JavaFX starts. */
public record ServerConfig(int port) {
    public ServerConfig {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("server.port must be between 1 and 65535");
        }
    }

    public static ServerConfig load() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = ServerConfig.class.getResourceAsStream(
                "/config/application.properties")) {
            if (input == null) {
                throw new IOException("Missing configuration resource: /config/application.properties");
            }
            properties.load(input);
        }
        String value = properties.getProperty("server.port");
        if (value == null) {
            throw new IllegalArgumentException("Missing configuration property: server.port");
        }
        int port;
        try {
            port = Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid server.port: " + value);
        }
        return new ServerConfig(port);
    }
}
