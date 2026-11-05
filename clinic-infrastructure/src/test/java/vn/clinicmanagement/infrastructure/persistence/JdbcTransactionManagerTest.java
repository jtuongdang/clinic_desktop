package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcTransactionManagerTest {
    @Test
    void commitsSuccessfulResultWithoutRollback() {
        SyntheticConnection synthetic = new SyntheticConnection();
        JdbcTransactionManager transactions = transactionManager(new JdbcUnitOfWork(), synthetic);

        Result<String> result = transactions.inTransaction(() -> Result.success("synthetic-success"));

        assertTrue(result.isSuccess());
        assertEquals(1, synthetic.commits);
        assertEquals(0, synthetic.rollbacks);
        assertEquals(1, synthetic.closes);
    }

    @Test
    void rollsBackFailedResult() {
        SyntheticConnection synthetic = new SyntheticConnection();
        JdbcTransactionManager transactions = transactionManager(new JdbcUnitOfWork(), synthetic);

        Result<String> result = transactions.inTransaction(
                () -> Result.failure("SYNTHETIC_FAILURE", "Synthetic operation failure"));

        assertTrue(result.isFailure());
        assertEquals("SYNTHETIC_FAILURE", result.getError().orElseThrow().code());
        assertEquals(0, synthetic.commits);
        assertEquals(1, synthetic.rollbacks);
    }

    @Test
    void rollsBackNullResultAndReturnsFailure() {
        SyntheticConnection synthetic = new SyntheticConnection();
        JdbcTransactionManager transactions = transactionManager(new JdbcUnitOfWork(), synthetic);

        Result<String> result = transactions.inTransaction(() -> null);

        assertTrue(result.isFailure());
        assertEquals("TRANSACTION_FAILED", result.getError().orElseThrow().code());
        assertEquals(0, synthetic.commits);
        assertEquals(1, synthetic.rollbacks);
    }

    @Test
    void rollsBackWhenNestedFailureIsIgnoredBySuccessfulOuterOperation() {
        SyntheticConnection synthetic = new SyntheticConnection();
        JdbcTransactionManager transactions = transactionManager(new JdbcUnitOfWork(), synthetic);

        Result<String> result = transactions.inTransaction(() -> {
            transactions.inTransaction(
                    () -> Result.failure("NESTED_FAILURE", "Synthetic nested operation failure"));
            return Result.success("synthetic-outer-success");
        });

        assertTrue(result.isFailure());
        assertEquals("TRANSACTION_ROLLBACK_ONLY", result.getError().orElseThrow().code());
        assertEquals(0, synthetic.commits);
        assertEquals(1, synthetic.rollbacks);
    }

    @Test
    void marksTransactionRollbackOnlyAfterNestedRuntimeFailure() {
        SyntheticConnection synthetic = new SyntheticConnection();
        JdbcTransactionManager transactions = transactionManager(new JdbcUnitOfWork(), synthetic);

        Result<String> result = transactions.inTransaction(() -> {
            Result<String> nested = transactions.inTransaction(() -> {
                throw new IllegalStateException("Synthetic nested failure");
            });
            assertTrue(nested.isFailure());
            return Result.success("synthetic-outer-success");
        });

        assertTrue(result.isFailure());
        assertEquals("TRANSACTION_ROLLBACK_ONLY", result.getError().orElseThrow().code());
        assertEquals(0, synthetic.commits);
        assertEquals(1, synthetic.rollbacks);
    }

    @Test
    void clearsConnectionAndUnitOfWorkStateBetweenSequentialTransactions() {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        SyntheticConnection first = new SyntheticConnection();
        SyntheticConnection second = new SyntheticConnection();
        AtomicInteger connectionIndex = new AtomicInteger();
        List<SyntheticConnection> connections = List.of(first, second);
        JdbcTransactionManager transactions = new JdbcTransactionManager(
                unitOfWork, () -> connections.get(connectionIndex.getAndIncrement()).connection);
        JdbcConnectionProvider provider = new JdbcConnectionProvider(transactions);

        Result<String> firstResult = transactions.inTransaction(() -> {
            assertTrue(provider.isTransactionActive());
            try (JdbcConnectionProvider.Lease lease = provider.acquire()) {
                assertSame(first.connection, lease.connection());
            } catch (SQLException exception) {
                throw new AssertionError(exception);
            }
            unitOfWork.recordChanges(2);
            return Result.success("synthetic-first");
        });
        assertTrue(firstResult.isSuccess());
        assertFalse(provider.isTransactionActive());
        assertEquals(0, unitOfWork.saveChanges());

        Result<String> secondResult = transactions.inTransaction(() -> {
            assertTrue(provider.isTransactionActive());
            assertEquals(0, unitOfWork.saveChanges());
            try (JdbcConnectionProvider.Lease lease = provider.acquire()) {
                assertSame(second.connection, lease.connection());
            } catch (SQLException exception) {
                throw new AssertionError(exception);
            }
            unitOfWork.recordChanges(3);
            return Result.success("synthetic-second");
        });

        assertTrue(secondResult.isSuccess());
        assertFalse(provider.isTransactionActive());
        assertNotSame(first.connection, second.connection);
        assertEquals(1, first.commits);
        assertEquals(1, second.commits);
        assertEquals(0, unitOfWork.saveChanges());
    }

    @Test
    void returnsSafeFailureAndCleansUpWhenConnectionSetupThrowsSqlException() {
        SyntheticConnection synthetic = new SyntheticConnection(true);
        JdbcTransactionManager transactions = transactionManager(new JdbcUnitOfWork(), synthetic);

        Result<String> result = transactions.inTransaction(() -> Result.success("unused"));

        assertTrue(result.isFailure());
        assertEquals("TRANSACTION_FAILED", result.getError().orElseThrow().code());
        assertFalse(result.getError().orElseThrow().message().contains("Synthetic SQL detail"));
        assertEquals(0, synthetic.commits);
        assertEquals(0, synthetic.rollbacks);
        assertEquals(1, synthetic.closes);
        assertNull(transactions.currentConnection());
    }

    private static JdbcTransactionManager transactionManager(
            JdbcUnitOfWork unitOfWork, SyntheticConnection synthetic) {
        return new JdbcTransactionManager(unitOfWork, () -> synthetic.connection);
    }

    private static final class SyntheticConnection {
        private final Connection connection;
        private final boolean failOnAutoCommit;
        private int commits;
        private int rollbacks;
        private int closes;

        private SyntheticConnection() {
            this(false);
        }

        private SyntheticConnection(boolean failOnAutoCommit) {
            this.failOnAutoCommit = failOnAutoCommit;
            connection = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setAutoCommit" -> {
                            if (this.failOnAutoCommit) {
                                throw new SQLException("Synthetic SQL detail");
                            }
                            yield null;
                        }
                        case "commit" -> {
                            commits++;
                            yield null;
                        }
                        case "rollback" -> {
                            rollbacks++;
                            yield null;
                        }
                        case "close" -> {
                            closes++;
                            yield null;
                        }
                        case "isClosed" -> closes > 0;
                        case "toString" -> "SyntheticConnection";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
