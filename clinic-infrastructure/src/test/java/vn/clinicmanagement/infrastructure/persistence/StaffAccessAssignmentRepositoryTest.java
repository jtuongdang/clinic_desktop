package vn.clinicmanagement.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.identity.NhanVienWorkScope;
import vn.clinicmanagement.domain.identity.VaiTro;
import vn.clinicmanagement.infrastructure.persistence.identity.NhanVienRepositoryImpl;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffAccessAssignmentRepositoryTest {
    @Test
    void mapsEveryActiveAssignedRoleIdAndCodeWithEmployeeBinding() {
        SyntheticJdbc jdbc = new SyntheticJdbc(List.of(
                roleRow(4, "DOCTOR"),
                roleRow(9, "PHARMACIST")));
        Harness harness = new Harness(jdbc);
        List<VaiTro> roles = harness.read(repo -> repo.getAssignedRoles(27));

        assertEquals(List.of(4, 9), roles.stream().map(VaiTro::getId).toList());
        assertEquals(List.of("DOCTOR", "PHARMACIST"),
                roles.stream().map(VaiTro::getTenVaiTro).toList());
        assertEquals(27, jdbc.boundParameters.get(1));
        assertTrue(jdbc.sql.contains("INNER JOIN NhanVienVaiTro nvt ON nvt.maNhanVien=nv.id"));
        assertTrue(jdbc.sql.contains("INNER JOIN VaiTro vt ON vt.id=nvt.maVaiTro"));
        assertTrue(jdbc.sql.contains("nv.hoatDong=1"));
        assertTrue(jdbc.sql.contains("nv.isDeleted=0"));
        assertTrue(jdbc.sql.contains("nvt.isDeleted=0"));
        assertTrue(jdbc.sql.contains("vt.isDeleted=0"));
        assertTrue(jdbc.sql.contains("vt.version, vt.createdAt, vt.createdBy, vt.updatedAt, vt.updatedBy, "
                + "vt.isDeleted, vt.deletedAt, vt.deletedBy"));
        assertEquals(2L, roles.get(0).getVersion());
        assertEquals(LocalDateTime.of(2025, 1, 2, 3, 4), roles.get(0).getCreatedAt());
        assertEquals(11, roles.get(0).getCreatedBy());
        assertEquals(LocalDateTime.of(2025, 2, 3, 4, 5), roles.get(0).getUpdatedAt());
        assertEquals(12, roles.get(0).getUpdatedBy());
        assertFalse(roles.get(0).isDeleted());
        assertNull(roles.get(0).getDeletedAt());
        assertNull(roles.get(0).getDeletedBy());
        assertTrue(jdbc.executedInsideTransaction);
    }

    private static Map<String, Object> roleRow(int id, String name) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("tenVaiTro", name);
        row.put("version", 2L);
        row.put("createdAt", Timestamp.valueOf(LocalDateTime.of(2025, 1, 2, 3, 4)));
        row.put("createdBy", 11);
        row.put("updatedAt", Timestamp.valueOf(LocalDateTime.of(2025, 2, 3, 4, 5)));
        row.put("updatedBy", 12);
        row.put("isDeleted", false);
        row.put("deletedAt", null);
        row.put("deletedBy", null);
        return row;
    }

    @Test
    void mapsOnlyAssignedFacilityDepartmentPairsAndEnforcesOwnershipAndActiveRows() {
        SyntheticJdbc jdbc = new SyntheticJdbc(List.of(
                Map.of("maCoSo", 2, "tenCoSo", "Co so Dong", "maKhoa", 12, "tenKhoa", "Khoa Noi"),
                Map.of("maCoSo", 5, "tenCoSo", "Co so Tay", "maKhoa", 31, "tenKhoa", "Khoa Nhi")));
        Harness harness = new Harness(jdbc);
        List<NhanVienWorkScope> scopes = harness.read(repo -> repo.getAssignedWorkScopes(42));

        assertEquals(List.of(
                new NhanVienWorkScope(2, "Co so Dong", 12, "Khoa Noi"),
                new NhanVienWorkScope(5, "Co so Tay", 31, "Khoa Nhi")), scopes);
        assertEquals(42, jdbc.boundParameters.get(1));
        assertTrue(jdbc.sql.contains("INNER JOIN PhanQuyenCoSo pq ON pq.maNhanVien=nv.id"));
        assertTrue(jdbc.sql.contains("INNER JOIN NhanVienKhoa nk ON nk.maNhanVien=nv.id"));
        assertTrue(jdbc.sql.contains("INNER JOIN CoSo cs ON cs.id=pq.maCoSo"));
        assertTrue(jdbc.sql.contains("cs.tenCoSo AS tenCoSo"));
        assertTrue(jdbc.sql.contains("k.tenKhoa AS tenKhoa"));
        assertTrue(jdbc.sql.contains("k.maCoSo=cs.id AND k.maCoSo=pq.maCoSo"));
        assertTrue(jdbc.sql.contains("nv.hoatDong=1"));
        assertTrue(jdbc.sql.contains("nv.isDeleted=0"));
        assertTrue(jdbc.sql.contains("pq.isDeleted=0"));
        assertTrue(jdbc.sql.contains("cs.hoatDong=1"));
        assertTrue(jdbc.sql.contains("cs.isDeleted=0"));
        assertTrue(jdbc.sql.contains("nk.isDeleted=0"));
        assertTrue(jdbc.sql.contains("k.hoatDong=1"));
        assertTrue(jdbc.sql.contains("k.isDeleted=0"));
        assertTrue(jdbc.sql.contains("SELECT DISTINCT"));
        assertTrue(jdbc.executedInsideTransaction);
    }

    @Test
    void returnsEmptyListsWhenEmployeeHasNoActiveAssignments() {
        SyntheticJdbc jdbc = new SyntheticJdbc(List.of());
        Harness harness = new Harness(jdbc);

        List<VaiTro> roles = harness.read(repo -> repo.getAssignedRoles(73));
        assertTrue(roles.isEmpty());

        jdbc.reset(List.of());
        List<NhanVienWorkScope> scopes = harness.read(repo -> repo.getAssignedWorkScopes(73));
        assertTrue(scopes.isEmpty());
        assertEquals(73, jdbc.boundParameters.get(1));
        assertTrue(jdbc.executedInsideTransaction);
    }

    @FunctionalInterface
    private interface RepositoryRead<T> {
        T execute(NhanVienRepositoryImpl repository);
    }

    private static final class Harness {
        private final NhanVienRepositoryImpl repository;
        private final JdbcTransactionManager transactions;

        private Harness(SyntheticJdbc jdbc) {
            JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
            transactions = new JdbcTransactionManager(unitOfWork, () -> jdbc.connection);
            repository = new NhanVienRepositoryImpl(new JdbcConnectionProvider(transactions), unitOfWork);
        }

        private <T> T read(RepositoryRead<T> operation) {
            Result<T> result = transactions.inTransaction(() -> Result.success(operation.execute(repository)));
            assertTrue(result.isSuccess());
            return result.getValue().orElseThrow();
        }
    }

    private static final class SyntheticJdbc {
        private final Connection connection;
        private List<Map<String, Object>> rows;
        private String sql;
        private final Map<Integer, Object> boundParameters = new LinkedHashMap<>();
        private boolean transactionActive;
        private boolean executedInsideTransaction;

        private SyntheticJdbc(List<Map<String, Object>> rows) {
            this.rows = rows;
            connection = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
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
                        case "setInt" -> {
                            boundParameters.put((int) args[0], args[1]);
                            yield null;
                        }
                        case "executeQuery" -> {
                            executedInsideTransaction = transactionActive;
                            yield resultSet();
                        }
                        case "close" -> null;
                        case "toString" -> "SyntheticPreparedStatement";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private ResultSet resultSet() {
            List<Map<String, Object>> resultRows = new ArrayList<>(rows);
            int[] cursor = {-1};
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(),
                    new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "next" -> ++cursor[0] < resultRows.size();
                        case "getInt" -> ((Number) resultRows.get(cursor[0]).get(args[0])).intValue();
                        case "getString" -> (String) resultRows.get(cursor[0]).get(args[0]);
                        case "getLong" -> ((Number) resultRows.get(cursor[0]).get(args[0])).longValue();
                        case "getBoolean" -> (boolean) resultRows.get(cursor[0]).get(args[0]);
                        case "getObject", "getTimestamp" -> resultRows.get(cursor[0]).get(args[0]);
                        case "close" -> null;
                        case "toString" -> "SyntheticResultSet";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private void reset(List<Map<String, Object>> newRows) {
            rows = newRows;
            sql = null;
            boundParameters.clear();
            executedInsideTransaction = false;
        }
    }
}
