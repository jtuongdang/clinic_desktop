package vn.clinicmanagement.infrastructure.persistence.system;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemRepositoryImplTest {
    @Test
    void returnsTheValueForAnActiveSystemParameter() {
        SyntheticJdbc jdbc = new SyntheticJdbc(List.of(
                new ParameterRow("SESSION_IDLE_TIMEOUT_MINUTES", "15", false)));
        Harness harness = new Harness(jdbc);

        assertEquals(Optional.of("15"), harness.read("SESSION_IDLE_TIMEOUT_MINUTES"));
        assertEquals("SESSION_IDLE_TIMEOUT_MINUTES", jdbc.boundKey);
        assertTrue(jdbc.sql.contains("WHERE maThamSo=? AND isDeleted=0"));
        assertTrue(jdbc.executedInsideTransaction);
    }

    @Test
    void returnsEmptyWhenTheParameterIsAbsent() {
        SyntheticJdbc jdbc = new SyntheticJdbc(List.of());
        Harness harness = new Harness(jdbc);

        assertEquals(Optional.empty(), harness.read("UNKNOWN_PARAMETER"));
        assertEquals("UNKNOWN_PARAMETER", jdbc.boundKey);
        assertTrue(jdbc.sql.contains("WHERE maThamSo=? AND isDeleted=0"));
    }

    @Test
    void excludesSoftDeletedParameterRows() {
        SyntheticJdbc jdbc = new SyntheticJdbc(List.of(
                new ParameterRow("SESSION_IDLE_TIMEOUT_MINUTES", "99", true)));
        Harness harness = new Harness(jdbc);

        assertEquals(Optional.empty(), harness.read("SESSION_IDLE_TIMEOUT_MINUTES"));
        assertTrue(jdbc.sql.contains("isDeleted=0"));
    }

    @Test
    void migrationSeedsBothDefaultsIdempotentlyWithoutUpdatingExistingValues() throws IOException {
        String migration;
        try (var stream = getClass().getResourceAsStream(
                "/migrations/V5__Seed_Session_Timeout_Parameters.sql")) {
            assertNotNull(stream);
            migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertEquals(2, occurrences(migration, "IF NOT EXISTS"));
        assertTrue(migration.contains(
                "WHERE maThamSo = 'SESSION_IDLE_TIMEOUT_MINUTES'"));
        assertTrue(migration.contains(
                "VALUES ('SESSION_IDLE_TIMEOUT_MINUTES', N'Thời gian không hoạt động của phiên', N'15'"));
        assertTrue(migration.contains(
                "WHERE maThamSo = 'SESSION_ABSOLUTE_TIMEOUT_HOURS'"));
        assertTrue(migration.contains(
                "VALUES ('SESSION_ABSOLUTE_TIMEOUT_HOURS', N'Tuổi thọ tối đa của phiên', N'12'"));
        assertFalse(migration.toUpperCase().contains("UPDATE THAMSOHETHONG"));
    }

    private static int occurrences(String text, String value) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(value, offset)) >= 0) {
            count++;
            offset += value.length();
        }
        return count;
    }

    private static final class Harness {
        private final SystemRepositoryImpl repository;
        private final JdbcTransactionManager transactions;

        private Harness(SyntheticJdbc jdbc) {
            JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
            transactions = transactionManager(unitOfWork, jdbc.connection);
            repository = new SystemRepositoryImpl(new JdbcConnectionProvider(transactions), unitOfWork);
        }

        private Optional<String> read(String key) {
            Result<Optional<String>> result = transactions.inTransaction(
                    () -> Result.success(repository.getSystemParameterValue(key)));
            assertTrue(result.isSuccess());
            return result.getValue().orElseThrow();
        }

        private static JdbcTransactionManager transactionManager(
                JdbcUnitOfWork unitOfWork, java.sql.Connection connection) {
            try {
                Class<?> supplierType = Class.forName(
                        "vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager$ConnectionSupplier");
                Object supplier = Proxy.newProxyInstance(
                        supplierType.getClassLoader(),
                        new Class<?>[]{supplierType},
                        (proxy, method, args) -> connection);
                Constructor<JdbcTransactionManager> constructor = JdbcTransactionManager.class
                        .getDeclaredConstructor(JdbcUnitOfWork.class, supplierType);
                constructor.setAccessible(true);
                return constructor.newInstance(unitOfWork, supplier);
            } catch (ReflectiveOperationException ex) {
                throw new AssertionError("Could not configure synthetic JDBC transaction.", ex);
            }
        }
    }

    private record ParameterRow(String key, String value, boolean deleted) {}

    private static final class SyntheticJdbc {
        private final java.sql.Connection connection;
        private final List<ParameterRow> rows;
        private String sql;
        private String boundKey;
        private boolean transactionActive;
        private boolean executedInsideTransaction;

        private SyntheticJdbc(List<ParameterRow> rows) {
            this.rows = rows;
            connection = (java.sql.Connection) Proxy.newProxyInstance(
                    java.sql.Connection.class.getClassLoader(),
                    new Class<?>[]{java.sql.Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setAutoCommit" -> {
                            transactionActive = !(boolean) args[0];
                            yield null;
                        }
                        case "prepareStatement" -> {
                            sql = (String) args[0];
                            yield preparedStatement();
                        }
                        case "commit", "rollback", "close" -> {
                            transactionActive = false;
                            yield null;
                        }
                        case "isClosed" -> false;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private java.sql.PreparedStatement preparedStatement() {
            return (java.sql.PreparedStatement) Proxy.newProxyInstance(
                    java.sql.PreparedStatement.class.getClassLoader(),
                    new Class<?>[]{java.sql.PreparedStatement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setString" -> {
                            boundKey = (String) args[1];
                            yield null;
                        }
                        case "executeQuery" -> {
                            executedInsideTransaction = transactionActive;
                            List<ParameterRow> matching = new ArrayList<>();
                            for (ParameterRow row : rows) {
                                if (row.key().equals(boundKey)
                                        && (!sql.contains("isDeleted=0") || !row.deleted())) {
                                    matching.add(row);
                                }
                            }
                            yield resultSet(matching);
                        }
                        case "close" -> null;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private static java.sql.ResultSet resultSet(List<ParameterRow> matching) {
            int[] cursor = {-1};
            return (java.sql.ResultSet) Proxy.newProxyInstance(
                    java.sql.ResultSet.class.getClassLoader(),
                    new Class<?>[]{java.sql.ResultSet.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "next" -> ++cursor[0] < matching.size();
                        case "getString" -> matching.get(cursor[0]).value();
                        case "close" -> null;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
