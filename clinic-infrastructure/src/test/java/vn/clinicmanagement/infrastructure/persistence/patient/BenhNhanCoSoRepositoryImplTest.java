package vn.clinicmanagement.infrastructure.persistence.patient;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenhNhanCoSoRepositoryImplTest {
    @Test
    void requiresAnExistingTransactionBeforeReadingOrWriting() {
        SyntheticJdbc jdbc = new SyntheticJdbc(false);
        Persistence persistence = persistence(jdbc);

        assertThrows(IllegalStateException.class,
                () -> persistence.repository.ensureActiveLink(41, 11, 5));

        assertTrue(jdbc.statements.isEmpty());
        assertEquals(0, persistence.unitOfWork.saveChanges());
    }

    @Test
    void insertsWithPatientFacilityAndAuthenticatedActorAndRecordsOneChange() {
        SyntheticJdbc jdbc = new SyntheticJdbc(false);
        Persistence persistence = persistence(jdbc);

        Result<Integer> result = persistence.transactions.inTransaction(() -> {
            assertTrue(persistence.connections.isTransactionActive());
            assertTrue(persistence.repository.ensureActiveLink(41, 11, 5));
            assertEquals(1, persistence.unitOfWork.saveChanges());
            return Result.success(jdbc.insertCount);
        });

        assertTrue(result.isSuccess());
        assertEquals(1, result.valueOrThrow());
        assertEquals(2, jdbc.statements.size());
        RecordedStatement select = jdbc.statements.get(0);
        assertTrue(select.sql.contains("UPDLOCK, HOLDLOCK"));
        assertTrue(select.sql.contains("INDEX(UX_BenhNhanCoSo_Active_Patient_Facility)"));
        assertTrue(select.sql.contains("maBenhNhan=? AND maCoSo=? AND isDeleted=0"));
        assertEquals(Map.of(1, 41, 2, 11), select.bindings);
        RecordedStatement insert = jdbc.statements.get(1);
        assertTrue(insert.sql.startsWith("INSERT INTO BenhNhanCoSo (maBenhNhan, maCoSo, createdBy)"));
        assertEquals(Map.of(1, 41, 2, 11, 3, 5), insert.bindings);
        assertEquals(1, jdbc.commits);
        assertEquals(0, jdbc.rollbacks);
        assertEquals(0, persistence.unitOfWork.saveChanges());
    }

    @Test
    void activeLinkIsIdempotentAndDoesNotRecordAChange() {
        SyntheticJdbc jdbc = new SyntheticJdbc(true);
        Persistence persistence = persistence(jdbc);

        Result<Integer> result = persistence.transactions.inTransaction(() -> {
            assertFalse(persistence.repository.ensureActiveLink(41, 11, 5));
            assertFalse(persistence.repository.ensureActiveLink(41, 11, 5));
            assertEquals(0, persistence.unitOfWork.saveChanges());
            return Result.success(jdbc.insertCount);
        });

        assertTrue(result.isSuccess());
        assertEquals(0, result.valueOrThrow());
        assertEquals(2, jdbc.statements.size());
        assertEquals(0, jdbc.insertCount);
    }

    @Test
    void softDeletedOnlyLinkIsNotReusedAndAReplacementIsInserted() {
        SyntheticJdbc jdbc = new SyntheticJdbc(false);
        jdbc.hasSoftDeletedLink = true;
        Persistence persistence = persistence(jdbc);

        Result<Integer> result = persistence.transactions.inTransaction(() -> {
            assertTrue(persistence.repository.ensureActiveLink(41, 11, 5));
            return Result.success(persistence.unitOfWork.saveChanges());
        });

        assertTrue(result.isSuccess());
        assertEquals(1, result.valueOrThrow());
        assertTrue(jdbc.hasSoftDeletedLink);
        assertTrue(jdbc.hasActiveLink);
        assertEquals(1, jdbc.insertCount);
        assertEquals(0, persistence.unitOfWork.saveChanges());
    }

    private static Persistence persistence(SyntheticJdbc jdbc) {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = transactionManager(unitOfWork, jdbc.connection);
        JdbcConnectionProvider connections = new JdbcConnectionProvider(transactions);
        return new Persistence(transactions, connections, unitOfWork,
                new BenhNhanCoSoRepositoryImpl(connections, unitOfWork));
    }

    private static JdbcTransactionManager transactionManager(
            JdbcUnitOfWork unitOfWork, Connection connection) {
        try {
            Class<?> supplierType = Class.forName(
                    "vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager$ConnectionSupplier");
            Object supplier = Proxy.newProxyInstance(
                    supplierType.getClassLoader(), new Class<?>[]{supplierType},
                    (proxy, method, args) -> connection);
            Constructor<JdbcTransactionManager> constructor = JdbcTransactionManager.class
                    .getDeclaredConstructor(JdbcUnitOfWork.class, supplierType);
            constructor.setAccessible(true);
            return constructor.newInstance(unitOfWork, supplier);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Could not configure synthetic JDBC transaction.", ex);
        }
    }

    private record Persistence(JdbcTransactionManager transactions, JdbcConnectionProvider connections,
                               JdbcUnitOfWork unitOfWork, BenhNhanCoSoRepositoryImpl repository) {}

    private static final class SyntheticJdbc {
        private final Connection connection;
        private final List<RecordedStatement> statements = new ArrayList<>();
        private boolean hasActiveLink;
        private boolean hasSoftDeletedLink;
        private int insertCount;
        private int commits;
        private int rollbacks;

        private SyntheticJdbc(boolean hasActiveLink) {
            this.hasActiveLink = hasActiveLink;
            connection = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setAutoCommit", "close" -> null;
                        case "prepareStatement" -> prepareStatement((String) args[0]);
                        case "commit" -> {
                            commits++;
                            yield null;
                        }
                        case "rollback" -> {
                            rollbacks++;
                            yield null;
                        }
                        case "isClosed" -> false;
                        case "toString" -> "SyntheticPatientFacilityConnection";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private PreparedStatement prepareStatement(String sql) {
            RecordedStatement statement = new RecordedStatement(this, sql);
            statements.add(statement);
            return statement.preparedStatement;
        }

        private ResultSet resultSet(boolean hasRow) {
            int[] cursor = {-1};
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "next" -> hasRow && ++cursor[0] == 0;
                        case "getInt" -> 1;
                        case "close" -> null;
                        case "toString" -> "SyntheticPatientFacilityResultSet";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }

    private static final class RecordedStatement {
        private final SyntheticJdbc jdbc;
        private final String sql;
        private final Map<Integer, Object> bindings = new LinkedHashMap<>();
        private final PreparedStatement preparedStatement;

        private RecordedStatement(SyntheticJdbc jdbc, String sql) {
            this.jdbc = jdbc;
            this.sql = sql;
            preparedStatement = (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setInt" -> {
                            bindings.put((Integer) args[0], args[1]);
                            yield null;
                        }
                        case "executeQuery" -> jdbc.resultSet(jdbc.hasActiveLink);
                        case "executeUpdate" -> {
                            jdbc.insertCount++;
                            jdbc.hasActiveLink = true;
                            yield 1;
                        }
                        case "close" -> null;
                        case "toString" -> "SyntheticPatientFacilityPreparedStatement";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
