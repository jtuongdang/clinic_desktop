package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcTransactionManagerTest {
    private String databaseUrl;
    private JdbcTransactionManager transactionManager;

    @BeforeEach
    void createIsolatedDatabase() throws SQLException {
        databaseUrl = "jdbc:h2:mem:clinic_tx_test_" + UUID.randomUUID().toString().replace("-", "")
                + ";DB_CLOSE_DELAY=-1";
        // Override any machine-level DB settings so this test can only use its synthetic H2 database.
        DatabaseConnection.setConnectionString(databaseUrl);
        transactionManager = new JdbcTransactionManager(new JdbcUnitOfWork());

        try (Connection connection = DriverManager.getConnection(databaseUrl);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE transaction_fixture (fixture_id VARCHAR(64) PRIMARY KEY)");
        }
    }

    @Test
    void commitsSuccessfulTransaction() throws SQLException {
        Result<String> result = transactionManager.inTransaction(() -> {
            insertFixture("SYNTHETIC-COMMIT-001");
            return Result.success("committed");
        });

        assertTrue(result.isSuccess());
        assertEquals(1, fixtureCount("SYNTHETIC-COMMIT-001"));
    }

    @Test
    void rollsBackWhenOperationReturnsFailure() throws SQLException {
        Result<String> result = transactionManager.inTransaction(() -> {
            insertFixture("SYNTHETIC-FAILURE-001");
            return Result.failure("SYNTHETIC_FAILURE", "Synthetic test failure");
        });

        assertTrue(result.isFailure());
        assertEquals("SYNTHETIC_FAILURE", result.getError().orElseThrow().code());
        assertEquals(0, fixtureCount("SYNTHETIC-FAILURE-001"));
    }

    @Test
    void rollsBackOuterTransactionWhenNestedTransactionFails() throws SQLException {
        Result<String> result = transactionManager.inTransaction(() -> {
            insertFixture("SYNTHETIC-NESTED-OUTER-001");
            Result<String> nested = transactionManager.inTransaction(() -> {
                insertFixture("SYNTHETIC-NESTED-INNER-001");
                return Result.failure("SYNTHETIC_NESTED_FAILURE", "Synthetic nested failure");
            });

            assertFalse(nested.isSuccess());
            return Result.success("outer completed");
        });

        assertTrue(result.isFailure());
        assertEquals("TRANSACTION_ROLLBACK_ONLY", result.getError().orElseThrow().code());
        assertEquals(0, fixtureCount("SYNTHETIC-NESTED-OUTER-001"));
        assertEquals(0, fixtureCount("SYNTHETIC-NESTED-INNER-001"));
    }

    private void insertFixture(String fixtureId) {
        try (PreparedStatement statement = transactionManager.currentConnection()
                .prepareStatement("INSERT INTO transaction_fixture (fixture_id) VALUES (?)")) {
            statement.setString(1, fixtureId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new AssertionError("Could not write synthetic transaction fixture", exception);
        }
    }

    private int fixtureCount(String fixtureId) throws SQLException {
        try (Connection connection = DriverManager.getConnection(databaseUrl);
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM transaction_fixture WHERE fixture_id = ?")) {
            statement.setString(1, fixtureId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}
