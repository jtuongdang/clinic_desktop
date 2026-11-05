package vn.clinicmanagement.infrastructure.persistence;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

final class DatabaseConfiguration {
    private final Supplier<Properties> propertiesSource;
    private final Supplier<Map<String, String>> environmentSource;
    private ResolvedConfiguration loadedConfiguration;
    private boolean loaded;
    private String programmaticConnectionString = "";

    DatabaseConfiguration(
            Supplier<Properties> propertiesSource,
            Supplier<Map<String, String>> environmentSource) {
        this.propertiesSource = Objects.requireNonNull(propertiesSource);
        this.environmentSource = Objects.requireNonNull(environmentSource);
    }

    static ResolvedConfiguration resolve(Properties properties, Map<String, String> environment) {
        Objects.requireNonNull(properties);
        Objects.requireNonNull(environment);

        String environmentConnectionString = environment.get("DB_CONNECTION_STRING");
        return new ResolvedConfiguration(
                override(properties.getProperty("db.url"), environment.get("DB_URL")),
                override(properties.getProperty("db.username"), environment.get("DB_USERNAME")),
                override(properties.getProperty("db.password"), environment.get("DB_PASSWORD")),
                override(properties.getProperty("db.driver"), environment.get("DB_DRIVER")),
                nonblank(environmentConnectionString)
                        ? environmentConnectionString
                        : "");
    }

    synchronized ResolvedConfiguration getConfiguration() {
        if (!programmaticConnectionString.isEmpty()) {
            if (loaded) {
                return withConnectionString(loadedConfiguration, programmaticConnectionString);
            }
            return new ResolvedConfiguration(null, null, null, null, programmaticConnectionString);
        }

        loadIfNeeded();
        return loadedConfiguration;
    }

    synchronized void setConnectionString(String connectionString) {
        programmaticConnectionString = connectionString == null ? "" : connectionString.trim();
    }

    synchronized void configure(String url, String username, String password) {
        loadIfNeeded();
        loadedConfiguration = new ResolvedConfiguration(
                url,
                username,
                password,
                loadedConfiguration.driver(),
                loadedConfiguration.connectionString());
    }

    private void loadIfNeeded() {
        if (!loaded) {
            loadedConfiguration = resolve(propertiesSource.get(), environmentSource.get());
            loaded = true;
        }
    }

    private static ResolvedConfiguration withConnectionString(
            ResolvedConfiguration configuration, String connectionString) {
        return new ResolvedConfiguration(
                configuration.url(),
                configuration.username(),
                configuration.password(),
                configuration.driver(),
                connectionString);
    }

    private static String override(String propertyValue, String environmentValue) {
        return nonblank(environmentValue) ? environmentValue.trim() : propertyValue;
    }

    private static boolean nonblank(String value) {
        return value != null && !value.isBlank();
    }

    record ResolvedConfiguration(
            String url,
            String username,
            String password,
            String driver,
            String connectionString) {
        @Override
        public String toString() {
            return "ResolvedConfiguration[hasUrl=" + (url != null && !url.isEmpty())
                    + ", hasUsername=" + (username != null && !username.isEmpty())
                    + ", hasPassword=" + (password != null && !password.isEmpty())
                    + ", hasDriver=" + (driver != null && !driver.isEmpty())
                    + ", hasConnectionString=" + (connectionString != null && !connectionString.isEmpty()) + "]";
        }

        ConnectionTarget connectionTarget() {
            if (connectionString != null && !connectionString.isEmpty()) {
                return new ConnectionTarget(ConnectionMode.FULL_CONNECTION_STRING, connectionString, null, null);
            }
            if (url != null && !url.isEmpty()) {
                if (username != null && !username.isEmpty()) {
                    return new ConnectionTarget(ConnectionMode.URL_WITH_CREDENTIALS, url, username, password);
                }
                return new ConnectionTarget(ConnectionMode.URL_ONLY, url, null, null);
            }
            return new ConnectionTarget(ConnectionMode.UNCONFIGURED, null, null, null);
        }
    }

    record ConnectionTarget(ConnectionMode mode, String value, String username, String password) {
        @Override
        public String toString() {
            return "ConnectionTarget[mode=" + mode
                    + ", hasValue=" + (value != null && !value.isEmpty())
                    + ", hasUsername=" + (username != null && !username.isEmpty())
                    + ", hasPassword=" + (password != null && !password.isEmpty()) + "]";
        }
    }

    enum ConnectionMode {
        FULL_CONNECTION_STRING,
        URL_WITH_CREDENTIALS,
        URL_ONLY,
        UNCONFIGURED
    }
}
