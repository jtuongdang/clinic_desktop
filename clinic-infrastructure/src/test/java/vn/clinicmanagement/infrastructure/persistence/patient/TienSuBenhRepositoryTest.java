package vn.clinicmanagement.infrastructure.persistence.patient;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.LoaiTienSu;
import vn.clinicmanagement.domain.patient.TienSuBenh;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TienSuBenhRepositoryTest {
    private static final int PATIENT_ID = 52;
    private static final int FACILITY_ID = 3;
    private static final int DEPARTMENT_ID = 8;

    @Test
    void queryFiltersByExactPairApprovedCategoriesAndActiveNonNullScope() {
        SyntheticJdbc jdbc = new SyntheticJdbc(List.of(historyRow()));
        Persistence persistence = persistence(jdbc);

        KetQua<List<TienSuBenh>> result = persistence.transactions.inTransaction(() ->
                KetQua.success(persistence.repository.getHistoryByPatientAndScope(
                        PATIENT_ID, FACILITY_ID, DEPARTMENT_ID)));

        assertTrue(result.isSuccess());
        assertEquals(Map.of(1, PATIENT_ID, 2, FACILITY_ID, 3, DEPARTMENT_ID),
                jdbc.statements.get(0).bindings);
        String sql = jdbc.statements.get(0).sql;
        assertTrue(sql.contains("maBenhNhan=? AND maCoSo=? AND maKhoa=?"));
        assertTrue(sql.contains("maCoSo IS NOT NULL AND maKhoa IS NOT NULL"));
        assertTrue(sql.contains("isDeleted=0"));
        assertTrue(sql.contains("loaiTienSu IN ('BENH_NEN', 'DI_UNG', 'PHAU_THUAT')"));
        assertFalse(sql.contains("TIEN_SU_GIA_DINH"));
        assertFalse(sql.contains("THOI_QUEN_SINH_HOAT"));
    }

    @Test
    void mapsApprovedHistoryAndScopeMetadataWithoutLosingClinicalDescription() {
        SyntheticJdbc jdbc = new SyntheticJdbc(List.of(historyRow()));
        Persistence persistence = persistence(jdbc);

        List<TienSuBenh> result = persistence.transactions.inTransaction(() ->
                KetQua.success(persistence.repository.getHistoryByPatientAndScope(
                        PATIENT_ID, FACILITY_ID, DEPARTMENT_ID))).valueOrThrow();

        assertEquals(1, result.size());
        TienSuBenh history = result.get(0);
        assertEquals(91, history.getId());
        assertEquals(PATIENT_ID, history.getMaBenhNhan());
        assertEquals(LoaiTienSu.DI_UNG, history.getLoaiTienSu());
        assertEquals("synthetic allergy narrative", history.getMoTa());
        assertEquals(FACILITY_ID, history.getMaCoSo());
        assertEquals(DEPARTMENT_ID, history.getMaKhoa());
        assertEquals(2L, history.getVersion());
        assertEquals(LocalDateTime.of(2026, 11, 6, 1, 2, 3), history.getCreatedAt());
        assertEquals(EMPLOYEE_ID, history.getCreatedBy());
        assertFalse(history.isDeleted());
    }

    @Test
    void emptyDatabaseResultReturnsAnEmptyList() {
        SyntheticJdbc jdbc = new SyntheticJdbc(List.of());
        Persistence persistence = persistence(jdbc);

        List<TienSuBenh> result = persistence.transactions.inTransaction(() ->
                KetQua.success(persistence.repository.getHistoryByPatientAndScope(
                        PATIENT_ID, FACILITY_ID, DEPARTMENT_ID))).valueOrThrow();

        assertTrue(result.isEmpty());
        assertEquals(1, jdbc.statements.size());
    }

    @Test
    void invalidIdentifiersAreRejectedBeforeOpeningJdbcConnection() {
        SyntheticJdbc jdbc = new SyntheticJdbc(List.of());
        Persistence persistence = persistence(jdbc);

        assertThrows(IllegalArgumentException.class,
                () -> persistence.repository.getHistoryByPatientAndScope(0, FACILITY_ID, DEPARTMENT_ID));
        assertTrue(jdbc.statements.isEmpty());
    }

    @Test
    void activeUpdateLookupIncludesHistoryPatientExactPairAndActiveFilters() {
        SyntheticJdbc jdbc = new SyntheticJdbc(List.of(historyRow()));
        Persistence persistence = persistence(jdbc);
        persistence.transactions.inTransaction(() -> KetQua.success(persistence.repository
                .getActiveByIdPatientAndScope(91, PATIENT_ID, FACILITY_ID, DEPARTMENT_ID)));
        RecordedStatement statement = jdbc.statements.get(0);
        assertEquals(Map.of(1, 91, 2, PATIENT_ID, 3, FACILITY_ID, 4, DEPARTMENT_ID), statement.bindings);
        assertTrue(statement.sql.contains("WHERE id=? AND maBenhNhan=? AND maCoSo=? AND maKhoa=?"));
        assertTrue(statement.sql.contains("isDeleted=0"));
        assertTrue(statement.sql.contains("maCoSo IS NOT NULL AND maKhoa IS NOT NULL"));
    }

    @Test
    void insertAndUpdateUseTrustedOwnershipAndOptimisticActiveScopePredicates() {
        SyntheticJdbc jdbc = new SyntheticJdbc(List.of());
        Persistence persistence = persistence(jdbc);
        TienSuBenh created = new TienSuBenh();
        created.setMaBenhNhan(PATIENT_ID);
        created.setLoaiTienSu(LoaiTienSu.DI_UNG);
        created.setMoTa("synthetic allergy narrative");
        created.setOwnershipScope(FACILITY_ID, DEPARTMENT_ID);
        created.setCreatedBy(EMPLOYEE_ID);
        assertTrue(persistence.transactions.inTransaction(() ->
                KetQua.success(persistence.repository.add(created))).isSuccess());
        RecordedStatement insert = jdbc.statements.get(0);
        assertTrue(insert.sql.contains("maCoSo, maKhoa, createdAt, createdBy"));
        assertTrue(insert.sql.contains("SYSUTCDATETIME()"));
        assertTrue(insert.sql.contains("OUTPUT INSERTED.id INTO @GeneratedKeys"));
        assertEquals(FACILITY_ID, insert.bindings.get(4));
        assertEquals(DEPARTMENT_ID, insert.bindings.get(5));

        TienSuBenh changed = new TienSuBenh();
        changed.setId(91);
        changed.setMaBenhNhan(PATIENT_ID);
        changed.setLoaiTienSu(LoaiTienSu.BENH_NEN);
        changed.setMoTa("synthetic updated narrative");
        changed.setOwnershipScope(FACILITY_ID, DEPARTMENT_ID);
        changed.setUpdatedBy(EMPLOYEE_ID);
        assertTrue(persistence.transactions.inTransaction(() ->
                KetQua.success(persistence.repository.update(changed, 2L))).valueOrThrow());
        RecordedStatement update = jdbc.statements.get(1);
        assertTrue(update.sql.contains("WHERE id=? AND maBenhNhan=? AND maCoSo=? AND maKhoa=?"));
        assertTrue(update.sql.contains("maCoSo IS NOT NULL AND maKhoa IS NOT NULL AND isDeleted=0 AND version=?"));
        assertEquals(91, update.bindings.get(4));
        assertEquals(PATIENT_ID, update.bindings.get(5));
        assertEquals(FACILITY_ID, update.bindings.get(6));
        assertEquals(DEPARTMENT_ID, update.bindings.get(7));
        assertEquals(2L, update.bindings.get(8));
    }

    private static Map<String, Object> historyRow() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", 91);
        row.put("maBenhNhan", PATIENT_ID);
        row.put("loaiTienSu", "DI_UNG");
        row.put("moTa", "synthetic allergy narrative");
        row.put("maCoSo", FACILITY_ID);
        row.put("maKhoa", DEPARTMENT_ID);
        row.put("version", 2L);
        row.put("createdAt", Timestamp.valueOf(LocalDateTime.of(2026, 11, 6, 1, 2, 3)));
        row.put("createdBy", EMPLOYEE_ID);
        row.put("updatedAt", null);
        row.put("updatedBy", null);
        row.put("isDeleted", false);
        row.put("deletedAt", null);
        row.put("deletedBy", null);
        return row;
    }

    private static final int EMPLOYEE_ID = 17;

    private static Persistence persistence(SyntheticJdbc jdbc) {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = transactionManager(unitOfWork, jdbc.connection);
        JdbcKetNoiNhaCungCap connections = new JdbcKetNoiNhaCungCap(transactions);
        return new Persistence(transactions, new TienSuBenhRepository(connections));
    }

    private static JdbcGiaoDichQuanLy transactionManager(JdbcUnitOfWork unitOfWork, Connection connection) {
        try {
            Class<?> supplierType = Class.forName(
                    "vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager$ConnectionSupplier");
            Object supplier = Proxy.newProxyInstance(supplierType.getClassLoader(), new Class<?>[]{supplierType},
                    (proxy, method, args) -> connection);
            Constructor<JdbcGiaoDichQuanLy> constructor = JdbcGiaoDichQuanLy.class
                    .getDeclaredConstructor(JdbcUnitOfWork.class, supplierType);
            constructor.setAccessible(true);
            return constructor.newInstance(unitOfWork, supplier);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not configure synthetic JDBC transaction.", exception);
        }
    }

    private record Persistence(
            JdbcGiaoDichQuanLy transactions,
            TienSuBenhRepository repository) {}

    private static final class SyntheticJdbc {
        final Connection connection;
        final List<RecordedStatement> statements = new ArrayList<>();
        private final List<Map<String, Object>> rows;

        SyntheticJdbc(List<Map<String, Object>> rows) {
            this.rows = rows;
            connection = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setAutoCommit", "commit", "rollback", "close" -> null;
                        case "prepareStatement" -> prepareStatement((String) args[0]);
                        case "isClosed" -> false;
                        case "toString" -> "SyntheticPatientHistoryConnection";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private PreparedStatement prepareStatement(String sql) {
            RecordedStatement statement = new RecordedStatement(sql, rows);
            statements.add(statement);
            return statement.preparedStatement;
        }
    }

    private static final class RecordedStatement {
        final String sql;
        final Map<Integer, Object> bindings = new LinkedHashMap<>();
        final PreparedStatement preparedStatement;
        private final List<Map<String, Object>> rows;

        RecordedStatement(String sql, List<Map<String, Object>> rows) {
            this.sql = sql;
            this.rows = rows;
            preparedStatement = (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setInt" -> {
                            bindings.put((Integer) args[0], args[1]);
                            yield null;
                        }
                        case "setString", "setLong" -> { bindings.put((Integer) args[0], args[1]); yield null; }
                        case "executeQuery" -> resultSet();
                        case "execute" -> true;
                        case "executeUpdate" -> 1;
                        case "getResultSet" -> generatedKeyResultSet();
                        case "getUpdateCount" -> -1;
                        case "getMoreResults" -> false;
                        case "close" -> null;
                        case "toString" -> "SyntheticPatientHistoryStatement";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private ResultSet resultSet() {
            int[] cursor = {-1};
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "next" -> ++cursor[0] < rows.size();
                        case "getInt" -> ((Number) currentRow(cursor).get(args[0])).intValue();
                        case "getLong" -> ((Number) currentRow(cursor).get(args[0])).longValue();
                        case "getBoolean" -> (Boolean) currentRow(cursor).get(args[0]);
                        case "getString" -> (String) currentRow(cursor).get(args[0]);
                        case "getObject" -> currentRow(cursor).get(args[0]);
                        case "getTimestamp" -> (Timestamp) currentRow(cursor).get(args[0]);
                        case "close" -> null;
                        case "toString" -> "SyntheticPatientHistoryResultSet";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private ResultSet generatedKeyResultSet() {
            int[] cursor = {-1};
            return (ResultSet) Proxy.newProxyInstance(ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "next" -> ++cursor[0] == 0;
                        case "getInt" -> 101;
                        case "close" -> null;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private Map<String, Object> currentRow(int[] cursor) {
            if (cursor[0] < 0 || cursor[0] >= rows.size()) {
                throw new IllegalStateException("ResultSet cursor is not on a row.");
            }
            return rows.get(cursor[0]);
        }
    }
}
