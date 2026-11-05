package vn.clinicmanagement.infrastructure.persistence.identity;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NhanVienRepositoryImplTest {
    @Test
    void successfulDeleteAdvancesDatabaseAndEntityVersionsOnce() {
        SyntheticJdbc jdbc = new SyntheticJdbc(4, true);
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = transactionManager(unitOfWork, jdbc.connection);
        NhanVienRepositoryImpl repository = new NhanVienRepositoryImpl(
                new JdbcConnectionProvider(transactions), unitOfWork);
        NhanVien employee = employee(42, 4L);

        Result<Integer> result = transactions.inTransaction(() -> {
            repository.delete(employee);
            return Result.success(unitOfWork.saveChanges());
        });

        assertTrue(result.isSuccess());
        assertEquals(1, result.getValue().orElseThrow());
        assertEquals(5L, jdbc.databaseVersion);
        assertEquals(5L, employee.getVersion());
        assertTrue(jdbc.databaseDeleted);
        assertTrue(employee.isDeleted());
    }

    @Test
    void staleDeleteLeavesDatabaseAndEntityStateUnchanged() {
        SyntheticJdbc jdbc = new SyntheticJdbc(5, true);
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = transactionManager(unitOfWork, jdbc.connection);
        NhanVienRepositoryImpl repository = new NhanVienRepositoryImpl(
                new JdbcConnectionProvider(transactions), unitOfWork);
        NhanVien employee = employee(42, 4L);

        Result<Integer> result = transactions.inTransaction(() -> {
            try {
                repository.delete(employee);
                throw new AssertionError("Expected stale-version conflict.");
            } catch (IllegalStateException conflict) {
                assertEquals("Nhân viên không tồn tại hoặc dữ liệu đã được cập nhật bởi người khác.",
                        conflict.getMessage());
                return Result.success(unitOfWork.saveChanges());
            }
        });

        assertTrue(result.isSuccess());
        assertEquals(0, result.getValue().orElseThrow());
        assertEquals(5L, jdbc.databaseVersion);
        assertFalse(jdbc.databaseDeleted);
        assertEquals(4L, employee.getVersion());
        assertFalse(employee.isDeleted());
    }

    @Test
    void missingOutputIdFailsBeforeRecordingUnitOfWorkChange() {
        SyntheticJdbc jdbc = new SyntheticJdbc(0, false);
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = transactionManager(unitOfWork, jdbc.connection);
        NhanVienRepositoryImpl repository = new NhanVienRepositoryImpl(
                new JdbcConnectionProvider(transactions), unitOfWork);
        NhanVien employee = new NhanVien();

        Result<Integer> result = transactions.inTransaction(() -> {
            try {
                repository.add(employee);
                throw new AssertionError("Expected missing OUTPUT INSERTED.id failure.");
            } catch (IllegalStateException failure) {
                assertEquals("Không nhận được ID nhân viên sau khi lưu.", failure.getMessage());
                return Result.success(unitOfWork.saveChanges());
            }
        });

        assertTrue(result.isSuccess());
        assertEquals(0, result.getValue().orElseThrow());
        assertNull(employee.getId());
    }

    private static NhanVien employee(int id, long version) {
        NhanVien employee = new NhanVien();
        employee.setId(id);
        employee.setVersion(version);
        return employee;
    }

    private static JdbcTransactionManager transactionManager(
            JdbcUnitOfWork unitOfWork, Connection connection) {
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

    private static final class SyntheticJdbc {
        private final Connection connection;
        private long databaseVersion;
        private final boolean outputId;
        private boolean databaseDeleted;
        private final Map<Integer, Object> parameters = new LinkedHashMap<>();

        private SyntheticJdbc(long version, boolean outputId) {
            databaseVersion = version;
            this.outputId = outputId;
            connection = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "prepareStatement" -> preparedStatement();
                        case "setAutoCommit", "commit", "rollback", "close" -> null;
                        case "isClosed" -> false;
                        case "toString" -> "SyntheticConnection";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private PreparedStatement preparedStatement() {
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if (name.startsWith("set")) {
                            parameters.put((int) args[0], args.length > 1 ? args[1] : null);
                            return null;
                        }
                        return switch (name) {
                            case "executeUpdate" -> {
                                long expectedVersion = ((Number) parameters.get(3)).longValue();
                                if (expectedVersion == databaseVersion && !databaseDeleted) {
                                    databaseVersion++;
                                    databaseDeleted = true;
                                    yield 1;
                                }
                                yield 0;
                            }
                            case "executeQuery" -> resultSet();
                            case "close" -> null;
                            case "toString" -> "SyntheticPreparedStatement";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> throw new UnsupportedOperationException(name);
                        };
                    });
        }

        private ResultSet resultSet() {
            int[] cursor = {-1};
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(),
                    new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "next" -> outputId && ++cursor[0] == 0;
                        case "getInt" -> 66;
                        case "close" -> null;
                        case "toString" -> "SyntheticResultSet";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
