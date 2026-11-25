package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoSoDuLieuDiCuChayTest {

    @Test
    void cliMapsMigrationSuccessToZeroAndFailureToNonZero() {
        assertEquals(0, CoSoDuLieuDiCuChay.runMigrationsForCli(() -> true, System.err));
        assertEquals(1, CoSoDuLieuDiCuChay.runMigrationsForCli(() -> false, System.err));
    }

    @Test
    void cliExceptionReturnsNonZeroAndPrintsOnlySafeDiagnostic() {
        ByteArrayOutputStream capturedError = new ByteArrayOutputStream();
        String secretUrl = "jdbc:sqlserver://synthetic-host;databaseName=synthetic-db";
        String secretUsername = "synthetic-user";
        String secretPassword = "synthetic-password";
        RuntimeException failure = new RuntimeException(
                "synthetic SQLException: " + secretUrl + " " + secretUsername + " " + secretPassword);

        int status;
        try (PrintStream errorOutput = new PrintStream(capturedError, true, StandardCharsets.UTF_8)) {
            status = CoSoDuLieuDiCuChay.runMigrationsForCli(() -> {
                throw failure;
            }, errorOutput);
        }

        String diagnostic = capturedError.toString(StandardCharsets.UTF_8);
        assertEquals(1, status);
        assertEquals(" LỖI MIGRATION: Không thể hoàn tất quá trình migration." + System.lineSeparator(), diagnostic);
        assertFalse(diagnostic.contains(secretUrl));
        assertFalse(diagnostic.contains(secretUsername));
        assertFalse(diagnostic.contains(secretPassword));
        assertFalse(diagnostic.contains("SQLException"));
        assertFalse(diagnostic.contains("RuntimeException"));
        assertFalse(diagnostic.contains("at "));
    }

    @Test
    void executesV7BatchesThroughFakeJdbcWithoutOpeningDatabaseConnection() throws IOException, SQLException {
        String scriptPath = "migrations/V7__Harden_Permission_Matrix_Defaults.sql";
        String sql;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(scriptPath)) {
            if (input == null) throw new AssertionError("Missing migration script: " + scriptPath);
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        List<String> executedBatches = new ArrayList<>();
        Statement fakeStatement = (Statement) Proxy.newProxyInstance(
                Statement.class.getClassLoader(), new Class<?>[]{Statement.class}, (proxy, method, args) -> {
                    if ("execute".equals(method.getName())) {
                        executedBatches.add((String) args[0]);
                        return false;
                    }
                    if ("close".equals(method.getName())) return null;
                    throw new UnsupportedOperationException("Unexpected Statement method: " + method.getName());
                });
        Connection fakeConnection = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                    if ("createStatement".equals(method.getName())) return fakeStatement;
                    throw new UnsupportedOperationException("Unexpected Connection method: " + method.getName());
                });

        CoSoDuLieuDiCuChay.executeSqlScript(fakeConnection, sql);

        assertEquals(2, executedBatches.size());
        assertTrue(executedBatches.get(0).contains("THROW 51070"));
        assertTrue(executedBatches.get(0).contains("THROW 51071"));
        assertTrue(executedBatches.get(1).contains("CREATE UNIQUE INDEX"));
    }

    @Test
    void failedBatchAfterEarlierBatchRollsBackWithoutTrackingOrCommit() {
        SQLException migrationFailure = new SQLException("synthetic batch failure");
        MigrationJdbcFake jdbc = new MigrationJdbcFake(migrationFailure, null, null);

        SQLException thrown = assertThrows(SQLException.class, () -> CoSoDuLieuDiCuChay.executeMigration(
                jdbc.connection(), "V-test__synthetic.sql",
                "CREATE TABLE SyntheticRoom (id INT);\nGO\nINSERT INTO SyntheticRoom VALUES (1);", 12L));

        assertSame(migrationFailure, thrown);
        assertEquals(List.of("CREATE TABLE SyntheticRoom (id INT);"), jdbc.executedBatches);
        assertEquals(0, jdbc.trackingWrites);
        assertEquals(0, jdbc.commits);
        assertEquals(1, jdbc.rollbacks);
    }

    @Test
    void trackingWriteFailureRollsBackAndDoesNotCommit() {
        SQLException trackingFailure = new SQLException("synthetic tracking write failure");
        MigrationJdbcFake jdbc = new MigrationJdbcFake(null, trackingFailure, null);

        SQLException thrown = assertThrows(SQLException.class, () -> CoSoDuLieuDiCuChay.executeMigration(
                jdbc.connection(), "V-test__synthetic.sql", "CREATE TABLE SyntheticRoom (id INT);", 12L));

        assertSame(trackingFailure, thrown);
        assertEquals(1, jdbc.trackingWrites);
        assertEquals(0, jdbc.commits);
        assertEquals(1, jdbc.rollbacks);
    }

    @Test
    void rollbackFailureIsSuppressedOnOriginalMigrationFailure() {
        SQLException migrationFailure = new SQLException("synthetic batch failure");
        SQLException rollbackFailure = new SQLException("synthetic rollback failure");
        MigrationJdbcFake jdbc = new MigrationJdbcFake(migrationFailure, null, rollbackFailure);

        SQLException thrown = assertThrows(SQLException.class, () -> CoSoDuLieuDiCuChay.executeMigration(
                jdbc.connection(), "V-test__synthetic.sql",
                "CREATE TABLE SyntheticRoom (id INT);\nGO\nINSERT INTO SyntheticRoom VALUES (1);", 12L));

        assertSame(migrationFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(rollbackFailure, thrown.getSuppressed()[0]);
        assertEquals(0, jdbc.commits);
        assertEquals(1, jdbc.rollbacks);
    }

    @Test
    void executeSqlScriptExecutesSplitNonEmptyBatchesInOrderWithoutDatabaseConnection() throws SQLException {
        List<String> executedBatches = new ArrayList<>();
        AtomicBoolean statementClosed = new AtomicBoolean(false);

        Statement fakeStatement = (Statement) Proxy.newProxyInstance(
                Statement.class.getClassLoader(),
                new Class<?>[]{Statement.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("execute".equals(name) && args != null && args.length == 1 && args[0] instanceof String sql) {
                        executedBatches.add(sql);
                        return false;
                    }
                    if ("close".equals(name)) {
                        statementClosed.set(true);
                        return null;
                    }
                    throw new UnsupportedOperationException("Unexpected Statement method: " + name);
                }
        );

        Connection fakeConnection = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("createStatement".equals(name)) {
                        return fakeStatement;
                    }
                    throw new UnsupportedOperationException("Unexpected Connection method: " + name);
                }
        );

        String syntheticScript = "\n"
                + "GO\n"
                + "CREATE TABLE SyntheticRoom (id INT PRIMARY KEY);\n"
                + "  go \r\n"
                + "\n"
                + "  Go\n"
                + "INSERT INTO SyntheticRoom (id) VALUES (101);\n"
                + "   gO   \n"
                + "UPDATE SyntheticRoom SET id = 102 WHERE id = 101;\n"
                + "GO\n";

        CoSoDuLieuDiCuChay.executeSqlScript(fakeConnection, syntheticScript);

        assertEquals(
                List.of(
                        "CREATE TABLE SyntheticRoom (id INT PRIMARY KEY);",
                        "INSERT INTO SyntheticRoom (id) VALUES (101);",
                        "UPDATE SyntheticRoom SET id = 102 WHERE id = 101;"
                ),
                executedBatches
        );
        assertTrue(statementClosed.get());
    }

    private static final class MigrationJdbcFake {
        private final SQLException batchFailure;
        private final SQLException trackingFailure;
        private final SQLException rollbackFailure;
        private final List<String> executedBatches = new ArrayList<>();
        private int trackingWrites;
        private int commits;
        private int rollbacks;

        private MigrationJdbcFake(SQLException batchFailure, SQLException trackingFailure,
                                  SQLException rollbackFailure) {
            this.batchFailure = batchFailure;
            this.trackingFailure = trackingFailure;
            this.rollbackFailure = rollbackFailure;
        }

        private Connection connection() {
            Statement statement = (Statement) Proxy.newProxyInstance(
                    Statement.class.getClassLoader(), new Class<?>[]{Statement.class}, (proxy, method, args) -> {
                        if ("execute".equals(method.getName())) {
                            String sql = (String) args[0];
                            if (batchFailure != null && executedBatches.size() == 1) {
                                throw batchFailure;
                            }
                            executedBatches.add(sql);
                            return false;
                        }
                        if ("close".equals(method.getName())) return null;
                        throw new UnsupportedOperationException("Unexpected Statement method: " + method.getName());
                    });
            PreparedStatement trackingStatement = (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        if ("executeUpdate".equals(method.getName())) {
                            trackingWrites++;
                            if (trackingFailure != null) throw trackingFailure;
                            return 1;
                        }
                        if ("setString".equals(method.getName()) || "setLong".equals(method.getName())
                                || "close".equals(method.getName())) return null;
                        throw new UnsupportedOperationException(
                                "Unexpected PreparedStatement method: " + method.getName());
                    });
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "createStatement": return statement;
                            case "prepareStatement": return trackingStatement;
                            case "commit": commits++; return null;
                            case "rollback":
                                rollbacks++;
                                if (rollbackFailure != null) throw rollbackFailure;
                                return null;
                            default: throw new UnsupportedOperationException(
                                    "Unexpected Connection method: " + method.getName());
                        }
                    });
        }
    }
}
