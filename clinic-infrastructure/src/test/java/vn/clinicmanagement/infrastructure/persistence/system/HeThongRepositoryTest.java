package vn.clinicmanagement.infrastructure.persistence.system;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.PhamViDuLieu;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeThongRepositoryTest {
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
    void hydratesPermissionMatrixBaseMetadataAndFiltersDeletedRows() {
        LocalDateTime createdAt = LocalDateTime.of(2025, 3, 4, 5, 6);
        LocalDateTime updatedAt = LocalDateTime.of(2025, 4, 5, 6, 7);
        SyntheticJdbc jdbc = new SyntheticJdbc(List.of(), List.of(
                matrixRow(6, 23, 3L, createdAt, 12, updatedAt, 13, false, null, null),
                matrixRow(7, 23, 4L, createdAt, 12, updatedAt, 13, true, updatedAt, 14)));
        Harness harness = new Harness(jdbc);

        List<MaTranPhanQuyen> matrix = harness.readMatrix(23);

        assertEquals(1, matrix.size());
        MaTranPhanQuyen permission = matrix.get(0);
        assertEquals(6, permission.getId());
        assertEquals(23, permission.getMaVaiTro());
        assertEquals("reception", permission.getPhanHe());
        assertEquals(HanhDongQuyen.XEM, permission.getHanhDong());
        assertEquals(PhamViDuLieu.KHOA_PHONG, permission.getPhamViDuLieu());
        assertTrue(permission.isDuocPhep());
        assertEquals(3L, permission.getVersion());
        assertEquals(createdAt, permission.getCreatedAt());
        assertEquals(12, permission.getCreatedBy());
        assertEquals(updatedAt, permission.getUpdatedAt());
        assertEquals(13, permission.getUpdatedBy());
        assertFalse(permission.isDeleted());
        assertNull(permission.getDeletedAt());
        assertNull(permission.getDeletedBy());
        assertEquals(23, jdbc.boundRoleId);
        assertTrue(jdbc.sql.contains("version, createdAt, createdBy, updatedAt, updatedBy, isDeleted, deletedAt, deletedBy"));
        assertTrue(jdbc.sql.contains("WHERE maVaiTro=? AND isDeleted=0"));
        assertTrue(jdbc.executedInsideTransaction);
    }

    private static Map<String, Object> matrixRow(
            int id, int roleId, long version, LocalDateTime createdAt, int createdBy,
            LocalDateTime updatedAt, int updatedBy, boolean deleted,
            LocalDateTime deletedAt, Integer deletedBy) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("maVaiTro", roleId);
        row.put("phanHe", "reception");
        row.put("hanhDong", HanhDongQuyen.XEM.name());
        row.put("phamViDuLieu", PhamViDuLieu.KHOA_PHONG.name());
        row.put("duocPhep", true);
        row.put("version", version);
        row.put("createdAt", Timestamp.valueOf(createdAt));
        row.put("createdBy", createdBy);
        row.put("updatedAt", Timestamp.valueOf(updatedAt));
        row.put("updatedBy", updatedBy);
        row.put("isDeleted", deleted);
        row.put("deletedAt", deletedAt == null ? null : Timestamp.valueOf(deletedAt));
        row.put("deletedBy", deletedBy);
        return row;
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
        private final HeThongRepository repository;
        private final JdbcGiaoDichQuanLy transactions;

        private Harness(SyntheticJdbc jdbc) {
            JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
            transactions = transactionManager(unitOfWork, jdbc.connection);
            repository = new HeThongRepository(new JdbcKetNoiNhaCungCap(transactions), unitOfWork);
        }

        private Optional<String> read(String key) {
            KetQua<Optional<String>> result = transactions.inTransaction(
                    () -> KetQua.success(repository.getSystemParameterValue(key)));
            assertTrue(result.isSuccess());
            return result.getValue().orElseThrow();
        }

        private List<MaTranPhanQuyen> readMatrix(int roleId) {
            KetQua<List<MaTranPhanQuyen>> result = transactions.inTransaction(
                    () -> KetQua.success(repository.getMatrixByRoleId(roleId)));
            assertTrue(result.isSuccess());
            return result.getValue().orElseThrow();
        }

        private static JdbcGiaoDichQuanLy transactionManager(
                JdbcUnitOfWork unitOfWork, java.sql.Connection connection) {
            try {
                Class<?> supplierType = Class.forName(
                        "vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager$ConnectionSupplier");
                Object supplier = Proxy.newProxyInstance(
                        supplierType.getClassLoader(),
                        new Class<?>[]{supplierType},
                        (proxy, method, args) -> connection);
                Constructor<JdbcGiaoDichQuanLy> constructor = JdbcGiaoDichQuanLy.class
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
        private final List<Map<String, Object>> matrixRows;
        private String sql;
        private String boundKey;
        private int boundRoleId;
        private boolean transactionActive;
        private boolean executedInsideTransaction;

        private SyntheticJdbc(List<ParameterRow> rows) {
            this(rows, List.of());
        }

        private SyntheticJdbc(List<ParameterRow> rows, List<Map<String, Object>> matrixRows) {
            this.rows = rows;
            this.matrixRows = matrixRows;
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
                        case "setInt" -> {
                            boundRoleId = (int) args[1];
                            yield null;
                        }
                        case "executeQuery" -> {
                            executedInsideTransaction = transactionActive;
                            if (sql.contains("FROM MaTranPhanQuyen")) {
                                List<Map<String, Object>> matching = matrixRows.stream()
                                        .filter(row -> ((Number) row.get("maVaiTro")).intValue() == boundRoleId)
                                        .filter(row -> !((boolean) row.get("isDeleted")))
                                        .toList();
                                yield resultSet(matching);
                            }
                            List<ParameterRow> matching = new ArrayList<>();
                            for (ParameterRow row : rows) {
                                if (row.key().equals(boundKey)
                                        && (!sql.contains("isDeleted=0") || !row.deleted())) {
                                    matching.add(row);
                                }
                            }
                            yield resultSet(matching.stream()
                                    .map(row -> Map.<String, Object>of("giaTri", row.value()))
                                    .toList());
                        }
                        case "close" -> null;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private static java.sql.ResultSet resultSet(List<Map<String, Object>> matching) {
            int[] cursor = {-1};
            return (java.sql.ResultSet) Proxy.newProxyInstance(
                    java.sql.ResultSet.class.getClassLoader(),
                    new Class<?>[]{java.sql.ResultSet.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "next" -> ++cursor[0] < matching.size();
                        case "getString" -> matching.get(cursor[0]).get(args[0]);
                        case "getInt" -> ((Number) matching.get(cursor[0]).get(args[0])).intValue();
                        case "getLong" -> ((Number) matching.get(cursor[0]).get(args[0])).longValue();
                        case "getBoolean" -> (boolean) matching.get(cursor[0]).get(args[0]);
                        case "getObject", "getTimestamp" -> matching.get(cursor[0]).get(args[0]);
                        case "close" -> null;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
