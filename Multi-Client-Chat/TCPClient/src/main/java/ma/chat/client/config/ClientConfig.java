package ma.chat.client.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Validated connection settings, resolved before JavaFX starts. */
public record ClientConfig(String host, int port) {
    public ClientConfig {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("server.host must not be blank");
        }
        host = host.strip();
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("server.port must be between 1 and 65535");
        }
    }

    public static ClientConfig fromArguments(String[] args) throws IOException {
        if (args.length == 0) {
            return load();
        }
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: TCPClient [<ServerIPAddress> <PortNumber>] (provide both or neither)");
        }
        return new ClientConfig(args[0], parsePort(args[1]));
    }

    public static ClientConfig load() throws IOException {
        Properties properties = new Properties();
        try (InputStream input = ClientConfig.class.getResourceAsStream(
                "/config/application.properties")) {
            if (input == null) {
                throw new IOException("Missing configuration resource: /config/application.properties");
            }
            properties.load(input);
        }
        String host = required(properties, "server.host");
        String port = required(properties, "server.port");
        return new ClientConfig(host, parsePort(port));
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing configuration property: " + key);
        }
        return value;
    }

    private static int parsePort(String value) {
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid server.port: " + value);
        }
    }
}
