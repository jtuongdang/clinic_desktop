package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcConnectionProviderTest {
    @Test
    void reportsWhetherTransactionIsActive() {
        SyntheticConnection synthetic = new SyntheticConnection();
        JdbcTransactionManager transactions = new JdbcTransactionManager(
                new JdbcUnitOfWork(), () -> synthetic.connection);
        JdbcConnectionProvider provider = new JdbcConnectionProvider(transactions);

        assertFalse(provider.isTransactionActive());

        Result<String> result = transactions.inTransaction(() -> {
            assertTrue(provider.isTransactionActive());
            return Result.success("synthetic-success");
        });

        assertTrue(result.isSuccess());
        assertFalse(provider.isTransactionActive());
        assertEquals(1, synthetic.closes.get());
    }

    @Test
    void acquiredLeaseDoesNotCloseActiveTransactionConnection() {
        SyntheticConnection synthetic = new SyntheticConnection();
        JdbcTransactionManager transactions = new JdbcTransactionManager(
                new JdbcUnitOfWork(), () -> synthetic.connection);
        JdbcConnectionProvider provider = new JdbcConnectionProvider(transactions);

        Result<String> result = transactions.inTransaction(() -> {
            try (JdbcConnectionProvider.Lease lease = provider.acquire()) {
                assertSame(synthetic.connection, lease.connection());
            } catch (SQLException exception) {
                throw new AssertionError(exception);
            }
            assertEquals(0, synthetic.closes.get());
            return Result.success("synthetic-success");
        });

        assertTrue(result.isSuccess());
        assertEquals(1, synthetic.closes.get());
    }

    private static final class SyntheticConnection {
        private final AtomicInteger closes = new AtomicInteger();
        private final Connection connection = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setAutoCommit", "commit", "rollback" -> null;
                    case "close" -> {
                        closes.incrementAndGet();
                        yield null;
                    }
                    case "isClosed" -> closes.get() > 0;
                    case "toString" -> "SyntheticConnection";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
