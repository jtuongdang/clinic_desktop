package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseConfigurationTest {
    @Test
    void resolveUsesPropertiesAsUntrimmedBaseValues() {
        Properties properties = properties(
                " jdbc:synthetic:base ", " base-user ", " base-password ", " base-driver ");

        DatabaseConfiguration.ResolvedConfiguration resolved =
                DatabaseConfiguration.resolve(properties, Map.of());

        assertEquals(" jdbc:synthetic:base ", resolved.url());
        assertEquals(" base-user ", resolved.username());
        assertEquals(" base-password ", resolved.password());
        assertEquals(" base-driver ", resolved.driver());
    }

    @Test
    void eachSplitEnvironmentValueOverridesOnlyItsMatchingProperty() {
        Properties properties = properties("base-url", "base-user", "base-password", "base-driver");
        Map<String, String> values = Map.of(
                "DB_URL", "  env-url  ",
                "DB_USERNAME", "  env-user  ",
                "DB_PASSWORD", "  env-password  ",
                "DB_DRIVER", "  env-driver  ");

        DatabaseConfiguration.ResolvedConfiguration resolved =
                DatabaseConfiguration.resolve(properties, values);

        assertEquals("env-url", resolved.url());
        assertEquals("env-user", resolved.username());
        assertEquals("env-password", resolved.password());
        assertEquals("env-driver", resolved.driver());
    }

    @Test
    void blankEnvironmentValuesKeepPropertiesAndNonblankValuesAreTrimmed() {
        Properties properties = properties(" property-url ", "property-user", "property-password", "property-driver");
        Map<String, String> values = Map.of(
                "DB_URL", " \t ",
                "DB_USERNAME", "\n",
                "DB_PASSWORD", "  override-password \t",
                "DB_DRIVER", "");

        DatabaseConfiguration.ResolvedConfiguration resolved =
                DatabaseConfiguration.resolve(properties, values);

        assertEquals(" property-url ", resolved.url());
        assertEquals("property-user", resolved.username());
        assertEquals("override-password", resolved.password());
        assertEquals("property-driver", resolved.driver());
    }

    @Test
    void nonblankFullConnectionStringIsPreservedAndPrecedesSplitValues() {
        DatabaseConfiguration.ResolvedConfiguration resolved = DatabaseConfiguration.resolve(
                properties("split-url", "split-user", "split-password", "driver"),
                Map.of("DB_CONNECTION_STRING", "  jdbc:synthetic:full;option=value  "));

        assertEquals("  jdbc:synthetic:full;option=value  ", resolved.connectionString());
        assertEquals(
                DatabaseConfiguration.ConnectionMode.FULL_CONNECTION_STRING,
                resolved.connectionTarget().mode());
        assertEquals("  jdbc:synthetic:full;option=value  ", resolved.connectionTarget().value());
    }

    @Test
    void blankFullConnectionStringDoesNotOverrideProperties() {
        DatabaseConfiguration.ResolvedConfiguration resolved = DatabaseConfiguration.resolve(
                properties("split-url", "split-user", "split-password", "driver"),
                Map.of("DB_CONNECTION_STRING", " \t "));

        assertEquals("", resolved.connectionString());
        assertEquals(DatabaseConfiguration.ConnectionMode.URL_WITH_CREDENTIALS,
                resolved.connectionTarget().mode());
    }

    @Test
    void configurationAndConnectionTargetStringRepresentationsRedactAllValues() {
        DatabaseConfiguration.ResolvedConfiguration resolved = DatabaseConfiguration.resolve(
                properties("jdbc:synthetic:url-marker", "synthetic-user-marker",
                        "synthetic-password-marker", "synthetic-driver-marker"),
                Map.of("DB_CONNECTION_STRING", "jdbc:synthetic:connection-marker;password=synthetic-secret-marker"));

        String configurationText = resolved.toString();
        String targetText = resolved.connectionTarget().toString();
        for (String marker : new String[]{
                "url-marker", "user-marker", "password-marker", "driver-marker",
                "connection-marker", "secret-marker"}) {
            org.junit.jupiter.api.Assertions.assertFalse(configurationText.contains(marker));
            org.junit.jupiter.api.Assertions.assertFalse(targetText.contains(marker));
        }
    }

    @Test
    void programmaticStringSetBeforeConfigureSurvivesLoadingAndConfigure() {
        DatabaseConfiguration configuration = configuration(
                properties("property-url", "property-user", "property-password", "driver"),
                Map.of("DB_CONNECTION_STRING", "environment-full"));

        configuration.setConnectionString("  programmatic-full  ");
        assertEquals("programmatic-full", configuration.getConfiguration().connectionString());

        configuration.configure("configured-url", "configured-user", "configured-password");

        DatabaseConfiguration.ResolvedConfiguration resolved = configuration.getConfiguration();
        assertEquals("programmatic-full", resolved.connectionString());
        assertEquals("configured-url", resolved.url());
        assertEquals("configured-user", resolved.username());
        assertEquals("configured-password", resolved.password());
    }

    @Test
    void programmaticStringSetAfterConfigureOverridesEnvironmentAndCanBeCleared() {
        DatabaseConfiguration configuration = configuration(
                properties("property-url", "property-user", "property-password", "driver"),
                Map.of("DB_CONNECTION_STRING", "environment-full"));

        configuration.configure("configured-url", "configured-user", "configured-password");
        configuration.setConnectionString("  programmatic-full  ");
        assertEquals("programmatic-full", configuration.getConfiguration().connectionString());

        configuration.setConnectionString(null);
        assertEquals("environment-full", configuration.getConfiguration().connectionString());
        assertEquals("configured-url", configuration.getConfiguration().url());
    }

    @Test
    void configureDoesNotClearProgrammaticStringAndEmptySetterRestoresEnvironmentString() {
        DatabaseConfiguration configuration = configuration(
                properties("property-url", "property-user", "property-password", "driver"),
                Map.of("DB_CONNECTION_STRING", "environment-full"));

        configuration.setConnectionString("programmatic-full");
        configuration.configure("configured-url", "configured-user", "configured-password");
        assertEquals("programmatic-full", configuration.getConfiguration().connectionString());

        configuration.setConnectionString("  ");
        assertEquals("environment-full", configuration.getConfiguration().connectionString());
    }

    @Test
    void sourcesLoadLazilyOnceAndProgrammaticStringCanBeUsedBeforeLoading() {
        AtomicInteger propertiesCalls = new AtomicInteger();
        AtomicInteger environmentCalls = new AtomicInteger();
        DatabaseConfiguration configuration = new DatabaseConfiguration(
                () -> {
                    propertiesCalls.incrementAndGet();
                    return properties("property-url", "property-user", "property-password", "driver");
                },
                () -> {
                    environmentCalls.incrementAndGet();
                    return Map.of("DB_URL", "environment-url");
                });

        configuration.setConnectionString("programmatic-full");
        assertEquals("programmatic-full", configuration.getConfiguration().connectionString());
        assertEquals(0, propertiesCalls.get());
        assertEquals(0, environmentCalls.get());

        configuration.setConnectionString("");
        assertEquals("environment-url", configuration.getConfiguration().url());
        configuration.configure("configured-url", "configured-user", "configured-password");
        configuration.getConfiguration();

        assertEquals(1, propertiesCalls.get());
        assertEquals(1, environmentCalls.get());
    }

    private static DatabaseConfiguration configuration(Properties properties, Map<String, String> environment) {
        return new DatabaseConfiguration(() -> properties, () -> environment);
    }

    private static Properties properties(String url, String username, String password, String driver) {
        Properties properties = new Properties();
        properties.setProperty("db.url", url);
        properties.setProperty("db.username", username);
        properties.setProperty("db.password", password);
        properties.setProperty("db.driver", driver);
        return properties;
    }
}
