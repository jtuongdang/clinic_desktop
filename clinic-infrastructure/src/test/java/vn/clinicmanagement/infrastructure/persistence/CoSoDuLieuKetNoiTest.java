package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoSoDuLieuKetNoiTest {
    private static final String SYNTHETIC_URL = "jdbc:synthetic:diagnostics-test";

    @Test
    void fullConnectionStringUsesSingleArgumentConnectionOverload() throws SQLException {
        FakeConnectionFactory factory = new FakeConnectionFactory();
        CoSoDuLieuCauHinh.ConnectionTarget target = new CoSoDuLieuCauHinh.ConnectionTarget(
                CoSoDuLieuCauHinh.ConnectionMode.FULL_CONNECTION_STRING,
                "jdbc:synthetic:full",
                null,
                null);

        CoSoDuLieuKetNoi.openConnection(target, factory);

        assertEquals("jdbc:synthetic:full", factory.singleArgument);
        assertEquals(1, factory.singleArgumentCalls);
        assertEquals(0, factory.credentialCalls);
    }

    @Test
    void splitConfigurationWithUsernameUsesCredentialOverload() throws SQLException {
        FakeConnectionFactory factory = new FakeConnectionFactory();
        CoSoDuLieuCauHinh.ConnectionTarget target = new CoSoDuLieuCauHinh.ConnectionTarget(
                CoSoDuLieuCauHinh.ConnectionMode.URL_WITH_CREDENTIALS,
                "jdbc:synthetic:split",
                "synthetic-user",
                null);

        CoSoDuLieuKetNoi.openConnection(target, factory);

        assertEquals("jdbc:synthetic:split", factory.url);
        assertEquals("synthetic-user", factory.username);
        assertNull(factory.password);
        assertEquals(0, factory.singleArgumentCalls);
        assertEquals(1, factory.credentialCalls);
    }

    @Test
    void splitConfigurationWithoutUsernameUsesSingleArgumentOverload() throws SQLException {
        FakeConnectionFactory factory = new FakeConnectionFactory();
        CoSoDuLieuCauHinh.ConnectionTarget target = new CoSoDuLieuCauHinh.ConnectionTarget(
                CoSoDuLieuCauHinh.ConnectionMode.URL_ONLY,
                "jdbc:synthetic:url-only",
                null,
                null);

        CoSoDuLieuKetNoi.openConnection(target, factory);

        assertEquals("jdbc:synthetic:url-only", factory.singleArgument);
        assertEquals(1, factory.singleArgumentCalls);
        assertEquals(0, factory.credentialCalls);
    }

    @Test
    void testConnectionPrintsOnlyGenericSuccessDiagnostics() throws SQLException {
        SyntheticDriver driver = new SyntheticDriver(SYNTHETIC_URL, false);
        DriverManager.registerDriver(driver);
        try {
            CapturedOutput output = captureOutput(() -> CoSoDuLieuKetNoi.testConnection(
                    () -> DriverManager.getConnection(SYNTHETIC_URL)));

            assertTrue(output.result());
            assertTrue(output.stdout().contains("KẾT NỐI THÀNH CÔNG!"));
            assertFalse(output.stdout().contains("Synthetic Database"));
            assertFalse(output.stdout().contains("synthetic-version-secret"));
            assertFalse(output.stdout().contains("synthetic-user-secret"));
            assertTrue(output.stderr().isEmpty());
            assertEquals(1, driver.closeCount.get());
            assertEquals(0, driver.metadataAccessCount.get());
        } finally {
            DriverManager.deregisterDriver(driver);
        }
    }

    @Test
    void testConnectionHidesJdbcFailureDetails() throws SQLException {
        SyntheticDriver driver = new SyntheticDriver(SYNTHETIC_URL, true);
        DriverManager.registerDriver(driver);
        try {
            CapturedOutput output = captureOutput(() -> CoSoDuLieuKetNoi.testConnection(
                    () -> DriverManager.getConnection(SYNTHETIC_URL)));

            assertFalse(output.result());
            assertTrue(output.stderr().contains("KẾT NỐI THẤT BẠI!"));
            assertFalse(output.stdout().contains("synthetic-error-secret"));
            assertFalse(output.stdout().contains("synthetic-state-secret"));
            assertFalse(output.stderr().contains("synthetic-error-secret"));
            assertFalse(output.stderr().contains("synthetic-state-secret"));
            assertFalse(output.stderr().contains("9876"));
        } finally {
            DriverManager.deregisterDriver(driver);
        }
    }

    private static CapturedOutput captureOutput(BooleanSupplier action) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try (PrintStream capturedOut = new PrintStream(stdout, true, StandardCharsets.UTF_8);
             PrintStream capturedErr = new PrintStream(stderr, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            System.setErr(capturedErr);
            boolean result = action.getAsBoolean();
            return new CapturedOutput(result, stdout.toString(StandardCharsets.UTF_8),
                    stderr.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    @FunctionalInterface
    private interface BooleanSupplier {
        boolean getAsBoolean();
    }

    private record CapturedOutput(boolean result, String stdout, String stderr) {}

    private static final class FakeConnectionFactory implements CoSoDuLieuKetNoi.ConnectionFactory {
        private String singleArgument;
        private int singleArgumentCalls;
        private String url;
        private String username;
        private String password;
        private int credentialCalls;

        @Override
        public Connection getConnection(String connectionString) {
            singleArgument = connectionString;
            singleArgumentCalls++;
            return null;
        }

        @Override
        public Connection getConnection(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = password;
            credentialCalls++;
            return null;
        }
    }

    private static final class SyntheticDriver implements Driver {
        private final String url;
        private final boolean failOnConnect;
        private final AtomicInteger closeCount = new AtomicInteger();
        private final AtomicInteger metadataAccessCount = new AtomicInteger();
        private final Connection connection;

        private SyntheticDriver(String url, boolean failOnConnect) {
            this.url = url;
            this.failOnConnect = failOnConnect;
            DatabaseMetaData metadata = (DatabaseMetaData) java.lang.reflect.Proxy.newProxyInstance(
                    DatabaseMetaData.class.getClassLoader(), new Class<?>[]{DatabaseMetaData.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getDatabaseProductName" -> "Synthetic Database";
                        case "getDatabaseProductVersion" -> "synthetic-version-secret";
                        case "getUserName" -> "synthetic-user-secret";
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            this.connection = (Connection) java.lang.reflect.Proxy.newProxyInstance(
                    Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "isClosed" -> false;
                        case "close" -> {
                            closeCount.incrementAndGet();
                            yield null;
                        }
                        case "getMetaData" -> {
                            metadataAccessCount.incrementAndGet();
                            yield metadata;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            if (!acceptsURL(url)) return null;
            if (failOnConnect) {
                throw new SQLException("synthetic-error-secret", "synthetic-state-secret", 9876);
            }
            return connection;
        }

        @Override
        public boolean acceptsURL(String url) {
            return this.url.equals(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return java.util.logging.Logger.getGlobal();
        }
    }
}
