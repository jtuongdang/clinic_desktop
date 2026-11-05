package vn.clinicmanagement.infrastructure.persistence.patient;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.patient.PatientIdentityConflictException;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BenhNhanRepositoryImplTest {
    @Test
    void successfulDeleteAdvancesDatabaseAndEntityVersionsOnce() {
        SyntheticJdbc jdbc = new SyntheticJdbc(7);
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = transactionManager(unitOfWork, jdbc.connection);
        BenhNhanRepositoryImpl repository = new BenhNhanRepositoryImpl(
                new JdbcConnectionProvider(transactions), unitOfWork);
        BenhNhan patient = new BenhNhan();
        patient.setId(17);
        patient.setVersion(7L);
        patient.setDeletedBy(4);

        Result<Integer> result = transactions.inTransaction(() -> {
            repository.delete(patient);
            return Result.success(unitOfWork.saveChanges());
        });

        assertTrue(result.isSuccess());
        assertEquals(1, result.getValue().orElseThrow());
        assertEquals(8L, jdbc.databaseVersion);
        assertEquals(8L, patient.getVersion());
        assertTrue(jdbc.databaseDeleted);
        assertTrue(patient.isDeleted());
    }

    @Test
    void staleDeleteLeavesDatabaseAndEntityStateUnchanged() {
        SyntheticJdbc jdbc = new SyntheticJdbc(9);
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = transactionManager(unitOfWork, jdbc.connection);
        BenhNhanRepositoryImpl repository = new BenhNhanRepositoryImpl(
                new JdbcConnectionProvider(transactions), unitOfWork);
        BenhNhan patient = new BenhNhan();
        patient.setId(17);
        patient.setVersion(8L);

        Result<Integer> result = transactions.inTransaction(() -> {
            try {
                repository.delete(patient);
                throw new AssertionError("Expected stale-version conflict.");
            } catch (IllegalStateException conflict) {
                assertEquals("Hồ sơ đã thay đổi hoặc không còn tồn tại.", conflict.getMessage());
                return Result.success(unitOfWork.saveChanges());
            }
        });

        assertTrue(result.isSuccess());
        assertEquals(0, result.getValue().orElseThrow());
        assertEquals(9L, jdbc.databaseVersion);
        assertFalse(jdbc.databaseDeleted);
        assertEquals(8L, patient.getVersion());
        assertFalse(patient.isDeleted());
    }

    @Test
    void scopedCccdLookupRequiresBothFacilityAndDepartmentOnLinkedVisit() {
        String sql = BenhNhanRepositoryImpl.scopedCccdSql();

        assertTrue(sql.contains("EXISTS (SELECT 1 FROM LuotKham lk"));
        assertTrue(sql.contains("lk.maBenhNhan=BenhNhan.id"));
        assertTrue(sql.contains("lk.maCoSo=?"));
        assertTrue(sql.contains("lk.maKhoa=?"));
        assertTrue(sql.contains("lk.isDeleted=0"));
        assertFalse(sql.contains("OR lk.maCoSo"));
    }

    @Test
    void scopedIdLookupRequiresBothFacilityAndDepartmentOnLinkedVisit() {
        String sql = BenhNhanRepositoryImpl.scopedIdSql();

        assertTrue(sql.contains("WHERE id=? AND isDeleted=0 AND EXISTS"));
        assertTrue(sql.contains("lk.maBenhNhan=BenhNhan.id"));
        assertTrue(sql.contains("lk.maCoSo=?"));
        assertTrue(sql.contains("lk.maKhoa=?"));
        assertTrue(sql.contains("lk.isDeleted=0"));
    }

    @Test
    void scopedSearchRequiresBothFacilityAndDepartmentOnNondeletedVisitAndCapsResults() {
        String sql = BenhNhanRepositoryImpl.scopedSearchSql(500);

        assertTrue(sql.contains("TOP (50)"));
        assertTrue(sql.contains("lk.maCoSo=?"));
        assertTrue(sql.contains("lk.maKhoa=?"));
        assertTrue(sql.contains("lk.isDeleted=0"));
        assertTrue(sql.contains("EXISTS (SELECT 1 FROM LuotKham lk"));
    }

    @Test
    void mapsOnlyTheActiveCccdIndexViolationWithoutLeakingTheDuplicateValue() {
        for (int errorCode : new int[]{2601, 2627}) {
            SQLException sqlException = new SQLException(
                    "Cannot insert duplicate key row with unique index 'UX_BenhNhan_Active_CCCD'. "
                            + "The duplicate key value is (CCCD:synthetic-cccd-value).",
                    "23000", errorCode);

            RuntimeException failure = BenhNhanRepositoryImpl.persistenceFailure(sqlException);

            assertInstanceOf(PatientIdentityConflictException.class, failure);
            assertNull(failure.getCause());
            assertTrue(failure.getMessage().contains("Định danh"));
            assertFalse(failure.getMessage().contains("synthetic-cccd-value"));
        }
    }

    @Test
    void doesNotMapOtherSqlServerUniqueKeysToDuplicateCccd() {
        for (int errorCode : new int[]{2601, 2627}) {
            SQLException sqlException = new SQLException(
                    "Cannot insert duplicate key row with unique index 'UX_BenhNhan_MaBenhNhanFormat'. "
                            + "The duplicate key value is (synthetic-patient-code).",
                    "23000", errorCode);

            RuntimeException failure = BenhNhanRepositoryImpl.persistenceFailure(sqlException);

            assertFalse(failure instanceof PatientIdentityConflictException);
            assertInstanceOf(IllegalStateException.class, failure);
            assertTrue(failure.getCause() == sqlException);
        }
    }

    @Test
    void doesNotMapDuplicateCodesWithoutTheCccdIndexName() {
        SQLException sqlException = new SQLException("Duplicate key: synthetic-value", "23000", 2601);

        RuntimeException failure = BenhNhanRepositoryImpl.persistenceFailure(sqlException);

        assertFalse(failure instanceof PatientIdentityConflictException);
        assertInstanceOf(IllegalStateException.class, failure);
    }

    @Test
    void keepsNonDuplicateSqlErrorsAsPersistenceFailures() {
        SQLException sqlException = new SQLException("Synthetic database failure", "42000", 102);

        RuntimeException failure = BenhNhanRepositoryImpl.persistenceFailure(sqlException);

        assertInstanceOf(IllegalStateException.class, failure);
        assertTrue(failure.getCause() == sqlException);
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
        private boolean databaseDeleted;
        private boolean transactionActive;
        private final Map<Integer, Object> parameters = new LinkedHashMap<>();

        private SyntheticJdbc(long version) {
            databaseVersion = version;
            connection = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setAutoCommit" -> {
                            transactionActive = !(boolean) args[0];
                            yield null;
                        }
                        case "prepareStatement" -> preparedStatement();
                        case "commit", "rollback", "close" -> {
                            transactionActive = false;
                            yield null;
                        }
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
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setObject", "setInt", "setLong" -> {
                            parameters.put((int) args[0], args[1]);
                            yield null;
                        }
                        case "executeUpdate" -> {
                            long expectedVersion = ((Number) parameters.get(3)).longValue();
                            if (expectedVersion == databaseVersion && !databaseDeleted) {
                                databaseVersion++;
                                databaseDeleted = true;
                                yield 1;
                            }
                            yield 0;
                        }
                        case "close" -> null;
                        case "toString" -> "SyntheticPreparedStatement";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
