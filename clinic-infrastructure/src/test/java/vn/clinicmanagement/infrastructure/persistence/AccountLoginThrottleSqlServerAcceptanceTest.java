package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.auth.AuthenticateUserCommand;
import vn.clinicmanagement.application.auth.AuthenticateUserHandler;
import vn.clinicmanagement.application.auth.AuthenticatedUser;
import vn.clinicmanagement.application.auth.PasswordHasher;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.infrastructure.persistence.identity.NhanVienRepositoryImpl;
import vn.clinicmanagement.infrastructure.persistence.system.JdbcAuditRepositoryImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountLoginThrottleSqlServerAcceptanceTest {
    private static final Pattern DATABASE_NAME = Pattern.compile("clinic_test_[A-Za-z0-9_]+");
    private static final Pattern DATABASE_OPTION = Pattern.compile("(?i);\\s*(?:databaseName|database)\\s*=[^;]*");
    private static final Pattern EMBEDDED_CREDENTIAL = Pattern.compile("(?i);\\s*(?:user|password)\\s*=");
    private static final String INVALID_MESSAGE = "Tên đăng nhập hoặc mật khẩu không đúng.";

    @Test
    void serializesConcurrentAttemptsAndRollsBackThrottleWhenAuditFails() throws Exception {
        Assumptions.assumeTrue("true".equals(System.getProperty("clinic.sqlserver.lockout.enabled")),
                "Set clinic.sqlserver.lockout.enabled=true to opt in");
        String databaseName = System.getProperty("clinic.sqlserver.test.database");
        if (databaseName == null || !DATABASE_NAME.matcher(databaseName).matches()) {
            throw new IllegalArgumentException(
                    "clinic.sqlserver.test.database must match clinic_test_[A-Za-z0-9_]+");
        }

        Properties properties = loadLocalProperties();
        String baseUrl = requiredProperty(properties, "db.url").trim();
        String username = requiredProperty(properties, "db.username").trim();
        String credential = requiredProperty(properties, "db.password");
        DatabaseConnection.setConnectionString("");
        DatabaseConnection.configure(selectDatabase(baseUrl, databaseName), username, credential);

        try (Connection connection = DatabaseConnection.getConnection()) {
            assertEquals(databaseName, queryString(connection, "SELECT DB_NAME()"),
                    "Refusing to connect unless the exact clinic_test_* database is selected");
        }
        assertTrue(DatabaseMigrationRunner.runMigrations(),
                "Apply only forward migrations to the explicitly selected synthetic database");
        try (Connection connection = DatabaseConnection.getConnection()) {
            assertEquals(1, queryInt(connection,
                    "SELECT COUNT(*) FROM __schema_migrations WHERE script_name = "
                            + "'V11__Add_Account_Login_Throttle.sql' AND success = 1"),
                    "Apply migrations through V11 to the explicitly selected synthetic test database first");
        }

        String suffix = UUID.randomUUID().toString().replace("-", "");
        int employeeId = 0;
        int rollbackEmployeeId = 0;
        try {
            String usernameForTest = "throttle_" + suffix;
            employeeId = insertSyntheticEmployee(usernameForTest);
            runConcurrentWrongPasswords(usernameForTest);
            ThrottleRow afterConcurrentFailures = readThrottle(employeeId);
            assertEquals(5, afterConcurrentFailures.failureCount());
            assertEquals(30, afterConcurrentFailures.delaySeconds());
            assertTrue(afterConcurrentFailures.nextAllowedAt() != null);

            Result<AuthenticatedUser> activeWaitResult = createHandler(false).handle(
                    new AuthenticateUserCommand(usernameForTest, "synthetic-wrong".toCharArray()));
            assertGenericFailure(activeWaitResult);
            assertEquals(afterConcurrentFailures, readThrottle(employeeId),
                    "An active wait must not mutate persisted throttle timestamps or levels");
            assertEquals(8, countAuditReason(employeeId, AuditActionCode.AUTH_LOGIN_FAILED.name(),
                            AuditReasonCode.AUTH_THROTTLED.name()),
                    "Seven concurrent delayed attempts plus one active-wait request must be audited");
            assertEquals(5, countAuditReason(employeeId, AuditActionCode.AUTH_LOGIN_FAILED.name(),
                            AuditReasonCode.AUTH_INVALID.name()),
                    "Exactly the first five concurrent wrong-password verifications may advance state");

            String rollbackUsername = "rollback_" + suffix;
            rollbackEmployeeId = insertSyntheticEmployee(rollbackUsername);
            Result<AuthenticatedUser> failedAuditResult = createHandler(true).handle(
                    new AuthenticateUserCommand(rollbackUsername, "synthetic-wrong".toCharArray()));
            assertTrue(failedAuditResult.isFailure());
            assertEquals("AUTH_UNAVAILABLE", failedAuditResult.getError().orElseThrow().code());
            ThrottleRow rollbackState = readThrottle(rollbackEmployeeId);
            assertEquals(0, rollbackState.failureCount(),
                    "A failed required audit must roll back the throttle increment");
            assertEquals(0, rollbackState.delaySeconds());
            assertEquals(0, countAudit(rollbackEmployeeId, AuditActionCode.AUTH_LOGIN_FAILED.name()));
        } finally {
            if (employeeId > 0) softDeleteSyntheticEmployee(employeeId);
            if (rollbackEmployeeId > 0) softDeleteSyntheticEmployee(rollbackEmployeeId);
        }
    }

    private static void runConcurrentWrongPasswords(String username) throws Exception {
        int clientCount = 12;
        ExecutorService clients = Executors.newFixedThreadPool(clientCount);
        CountDownLatch ready = new CountDownLatch(clientCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Result<AuthenticatedUser>>> results = new ArrayList<>();
        try {
            for (int i = 0; i < clientCount; i++) {
                results.add(clients.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start latch timed out.");
                    return createHandler(false).handle(
                            new AuthenticateUserCommand(username, "synthetic-wrong".toCharArray()));
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS), "All synthetic clients must be ready");
            start.countDown();
            for (Future<Result<AuthenticatedUser>> result : results) {
                assertGenericFailure(result.get(30, TimeUnit.SECONDS));
            }
        } finally {
            start.countDown();
            clients.shutdownNow();
            assertTrue(clients.awaitTermination(10, TimeUnit.SECONDS), "Synthetic clients must terminate");
        }
    }

    private static AuthenticateUserHandler createHandler(boolean failAudit) {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = new JdbcTransactionManager(unitOfWork);
        JdbcConnectionProvider connections = new JdbcConnectionProvider(transactions);
        NhanVienRepositoryImpl employees = new NhanVienRepositoryImpl(connections, unitOfWork);
        PasswordHasher alwaysWrong = (password, passwordHash) -> false;
        AuditWritePort auditPort = failAudit ? new FailingAuditPort()
                : new JdbcAuditRepositoryImpl(connections, unitOfWork, DatabaseConnection::getConnection);
        return new AuthenticateUserHandler(
                employees, employees, alwaysWrong, transactions, auditPort, Clock.systemUTC());
    }

    private static int insertSyntheticEmployee(String username) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO dbo.NhanVien (maNhanVien, ho, ten, username, passwordHash, hoatDong) "
                             + "OUTPUT INSERTED.id VALUES (?, 'Synthetic', 'Throttle Test', ?, 'synthetic-only-hash', 1)")) {
            statement.setString(1, "THR-" + username);
            statement.setString(2, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Synthetic employee insert returned no ID.");
                return result.getInt(1);
            }
        }
    }

    private static ThrottleRow readThrottle(int employeeId) throws SQLException {
        String sql = "SELECT loginFailureWindowStartedAt, loginFailureCount, loginLastFailureAt, "
                + "loginDelaySeconds, loginNextAllowedAt FROM dbo.NhanVien WHERE id=?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, employeeId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Synthetic employee row was not preserved.");
                return new ThrottleRow(
                        result.getTimestamp(1),
                        result.getInt(2),
                        result.getTimestamp(3),
                        result.getInt(4),
                        result.getTimestamp(5));
            }
        }
    }

    private static int countAudit(int employeeId, String action) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM dbo.NhatKyHeThong WHERE maNhanVien=? AND hanhDong=?")) {
            statement.setInt(1, employeeId);
            statement.setString(2, action);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Audit count returned no result.");
                return result.getInt(1);
            }
        }
    }

    private static int countAuditReason(int employeeId, String action, String reason) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM dbo.NhatKyHeThong WHERE maNhanVien=? AND hanhDong=? AND maLyDo=?")) {
            statement.setInt(1, employeeId);
            statement.setString(2, action);
            statement.setString(3, reason);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Audit reason count returned no result.");
                return result.getInt(1);
            }
        }
    }

    private static void softDeleteSyntheticEmployee(int employeeId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE dbo.NhanVien SET isDeleted=1, deletedAt=SYSUTCDATETIME() "
                             + "WHERE id=? AND maNhanVien LIKE 'THR-%' AND isDeleted=0")) {
            statement.setInt(1, employeeId);
            statement.executeUpdate();
        }
    }

    private static int queryInt(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) throw new SQLException("Synthetic SQL query returned no result.");
            return result.getInt(1);
        }
    }

    private static String queryString(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) throw new SQLException("Synthetic SQL query returned no result.");
            return result.getString(1);
        }
    }

    private static void assertGenericFailure(Result<AuthenticatedUser> result) {
        assertTrue(result.isFailure());
        assertEquals("AUTH_INVALID", result.getError().orElseThrow().code());
        assertEquals(INVALID_MESSAGE, result.getError().orElseThrow().message());
        assertFalse(result.getError().orElseThrow().message().contains("throttle"));
    }

    private static Properties loadLocalProperties() {
        Path configPath = Path.of("src", "main", "resources", "db.properties");
        Properties properties = new Properties();
        try (var input = Files.newInputStream(configPath)) {
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new AssertionError(
                    "Configure clinic-infrastructure/src/main/resources/db.properties for the opt-in clinic_test_* database");
        }
    }

    private static String requiredProperty(Properties properties, String name) {
        String value = properties.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("The ignored local SQL Server configuration is incomplete");
        }
        return value;
    }

    private static String selectDatabase(String baseUrl, String databaseName) {
        if (!baseUrl.toLowerCase(java.util.Locale.ROOT).startsWith("jdbc:sqlserver:")
                || EMBEDDED_CREDENTIAL.matcher(baseUrl).find()) {
            throw new IllegalArgumentException("db.url must be a SQL Server URL without embedded credentials");
        }
        return DATABASE_OPTION.matcher(baseUrl).replaceAll("").replaceFirst(";+$", "")
                + ";databaseName=" + databaseName;
    }

    private record ThrottleRow(
            Timestamp windowStartedAt,
            int failureCount,
            Timestamp lastFailureAt,
            int delaySeconds,
            Timestamp nextAllowedAt) {}

    private static final class FailingAuditPort implements AuditWritePort {
        @Override public void appendInCurrentTransaction(SystemAuditEntry entry) {
            throw new IllegalStateException("Synthetic required-audit failure.");
        }
        @Override public void appendIndependently(SystemAuditEntry entry) {
            throw new IllegalStateException("Synthetic required-audit failure.");
        }
        @Override public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {
            throw new UnsupportedOperationException();
        }
        @Override public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {
            throw new UnsupportedOperationException();
        }
    }
}
