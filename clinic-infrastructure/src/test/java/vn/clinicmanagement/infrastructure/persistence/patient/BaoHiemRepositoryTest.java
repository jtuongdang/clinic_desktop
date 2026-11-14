package vn.clinicmanagement.infrastructure.persistence.patient;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.LoaiBaoHiem;
import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaoHiemRepositoryTest {
    private static final List<String> INSURANCE_COLUMNS = List.of(
            "id", "maBenhNhan", "loaiBaoHiem", "soBaoHiem", "ngayBatDau", "ngayHetHan",
            "mucHuong", "maNoiSong", "maNoiDangKyKCB", "noiDangKyKCB", "diaChiThe",
            "dungTuyen", "coGiayKhaiSinh", "khongGiuThe", "mienCungChiTra", "ngayMienCungChiTra",
            "ngayDu5NamLienTuc", "version", "createdAt", "createdBy", "updatedAt", "updatedBy",
            "isDeleted", "deletedAt", "deletedBy");

    @Test
    void getLatestByPatientIdMapsAllColumnsIncludingNonBhytInsuranceType() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        jdbc.rows = List.of(syntheticInsuranceRow(101, 202, "BAO_HIEM_TAI_NAN"));
        Persistence persistence = persistence(jdbc);

        KetQua<Optional<BaoHiem>> result = persistence.transactions.inTransaction(
                () -> KetQua.success(persistence.repository.getLatestByPatientId(202)));

        assertTrue(result.isSuccess());
        assertSelectedInsuranceColumns(jdbc.sql);
        assertTrue(jdbc.sql.contains("WHERE maBenhNhan=? AND isDeleted=0 ORDER BY ngayHetHan DESC, id DESC"));
        assertEquals(202, jdbc.parameters.get(1));

        BaoHiem card = result.getValue().orElseThrow().orElseThrow();
        assertEquals(101, card.getId());
        assertEquals(202, card.getMaBenhNhan());
        assertEquals(LoaiBaoHiem.BAO_HIEM_TAI_NAN, card.getLoaiBaoHiem());
        assertEquals("TN-999-888", card.getSoBaoHiem());
        assertEquals(LocalDate.of(2025, 1, 1), card.getNgayBatDau());
        assertEquals(LocalDate.of(2025, 12, 31), card.getNgayHetHan());
        assertEquals(80, card.getMucHuong());
        assertEquals("K1", card.getMaNoiSong());
        assertEquals("79014", card.getMaNoiDangKyKCB());
        assertEquals("BV Quan 1", card.getNoiDangKyKCB());
        assertEquals("123 Nguyen Trai, Q1", card.getDiaChiThe());
        assertTrue(card.isDungTuyen());
        assertTrue(card.isCoGiayKhaiSinh());
        assertFalse(card.isKhongGiuThe());
        assertTrue(card.isMienCungChiTra());
        assertEquals(LocalDate.of(2025, 6, 1), card.getNgayMienCungChiTra());
        assertEquals(LocalDate.of(2024, 1, 1), card.getNgayDu5NamLienTuc());
        assertEquals(5L, card.getVersion());
        assertEquals(LocalDateTime.of(2025, 1, 1, 8, 0, 0), card.getCreatedAt());
        assertEquals(10, card.getCreatedBy());
        assertEquals(LocalDateTime.of(2025, 2, 1, 9, 30, 0), card.getUpdatedAt());
        assertEquals(20, card.getUpdatedBy());
        assertFalse(card.isDeleted());
        assertNull(card.getDeletedAt());
        assertNull(card.getDeletedBy());
    }

    @Test
    void getLatestByPatientIdMapsUnknownLoaiBaoHiemToKhac() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        jdbc.rows = List.of(syntheticInsuranceRow(102, 202, "CUSTOM_UNKNOWN_TYPE"));
        Persistence persistence = persistence(jdbc);

        KetQua<Optional<BaoHiem>> result = persistence.transactions.inTransaction(
                () -> KetQua.success(persistence.repository.getLatestByPatientId(202)));

        assertTrue(result.isSuccess());
        BaoHiem card = result.getValue().orElseThrow().orElseThrow();
        assertEquals(LoaiBaoHiem.KHAC, card.getLoaiBaoHiem());
    }

    @Test
    void getLatestByPatientIdReturnsEmptyWhenNoRecordFound() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        jdbc.rows = List.of();
        Persistence persistence = persistence(jdbc);

        KetQua<Optional<BaoHiem>> result = persistence.transactions.inTransaction(
                () -> KetQua.success(persistence.repository.getLatestByPatientId(999)));

        assertTrue(result.isSuccess());
        assertTrue(result.getValue().orElseThrow().isEmpty());
        assertEquals(999, jdbc.parameters.get(1));
    }

    @Test
    void getByIdMapsCardCorrectly() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        jdbc.rows = List.of(syntheticInsuranceRow(101, 202, "BHYT"));
        Persistence persistence = persistence(jdbc);

        KetQua<Optional<BaoHiem>> result = persistence.transactions.inTransaction(
                () -> KetQua.success(persistence.repository.getById(101)));

        assertTrue(result.isSuccess());
        assertSelectedInsuranceColumns(jdbc.sql);
        assertTrue(jdbc.sql.contains("WHERE id=? AND isDeleted=0"));
        assertEquals(101, jdbc.parameters.get(1));
        assertEquals(LoaiBaoHiem.BHYT, result.getValue().orElseThrow().orElseThrow().getLoaiBaoHiem());
    }

    @Test
    void getByPatientIdReturnsAllMatchingCards() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        jdbc.rows = List.of(
                syntheticInsuranceRow(101, 202, "BHYT"),
                syntheticInsuranceRow(100, 202, "BAO_HIEM_TAI_NAN"));
        Persistence persistence = persistence(jdbc);

        KetQua<List<BaoHiem>> result = persistence.transactions.inTransaction(
                () -> KetQua.success(persistence.repository.getByPatientId(202)));

        assertTrue(result.isSuccess());
        List<BaoHiem> cards = result.getValue().orElseThrow();
        assertEquals(2, cards.size());
        assertEquals(101, cards.get(0).getId());
        assertEquals(100, cards.get(1).getId());
    }

    @Test
    void addBindsAll17ParametersWithDefaultNullLoaiBaoHiemSetsGeneratedIdAndRecordsUnitOfWork() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        jdbc.generatedId = 88;
        Persistence persistence = persistence(jdbc);

        BaoHiem card = new BaoHiem();
        card.setLoaiBaoHiem(null); // Explicitly null to verify default to BHYT
        card.setMaBenhNhan(202);
        card.setSoBaoHiem("DN4010123456789");
        card.setNgayBatDau(LocalDate.of(2025, 1, 1));
        card.setNgayHetHan(LocalDate.of(2025, 12, 31));
        card.setMucHuong(80);
        card.setMaNoiSong("K1");
        card.setMaNoiDangKyKCB("79014");
        card.setNoiDangKyKCB("BV Quan 1");
        card.setDiaChiThe("123 Nguyen Trai, Q1");
        card.setDungTuyen(true);
        card.setCoGiayKhaiSinh(false);
        card.setKhongGiuThe(true);
        card.setMienCungChiTra(true);
        card.setNgayMienCungChiTra(LocalDate.of(2025, 6, 1));
        card.setNgayDu5NamLienTuc(LocalDate.of(2024, 1, 1));
        card.setCreatedBy(10);

        KetQua<Integer> insertResult = persistence.transactions.inTransaction(() -> {
            persistence.repository.add(card);
            return KetQua.success(persistence.unitOfWork.saveChanges());
        });

        assertTrue(insertResult.isSuccess());
        assertEquals(1, insertResult.getValue().orElseThrow(), "Active transaction must record unit of work change");
        assertEquals(88, card.getId(), "add() must set generated ID on card");

        assertTrue(jdbc.sql.startsWith("INSERT INTO BaoHiem"));
        assertTrue(jdbc.sql.contains("OUTPUT INSERTED.id VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"));
        assertEquals(17, jdbc.parameters.size());

        assertEquals(202, jdbc.parameters.get(1));
        assertEquals("BHYT", jdbc.parameters.get(2), "Null loaiBaoHiem must default to BHYT");
        assertEquals("DN4010123456789", jdbc.parameters.get(3));
        assertEquals(LocalDate.of(2025, 1, 1), jdbc.parameters.get(4));
        assertEquals(LocalDate.of(2025, 12, 31), jdbc.parameters.get(5));
        assertEquals(80, jdbc.parameters.get(6));
        assertEquals("K1", jdbc.parameters.get(7));
        assertEquals("79014", jdbc.parameters.get(8));
        assertEquals("BV Quan 1", jdbc.parameters.get(9));
        assertEquals("123 Nguyen Trai, Q1", jdbc.parameters.get(10));
        assertEquals(true, jdbc.parameters.get(11));
        assertEquals(false, jdbc.parameters.get(12));
        assertEquals(true, jdbc.parameters.get(13));
        assertEquals(true, jdbc.parameters.get(14));
        assertEquals(LocalDate.of(2025, 6, 1), jdbc.parameters.get(15));
        assertEquals(LocalDate.of(2024, 1, 1), jdbc.parameters.get(16));
        assertEquals(10, jdbc.parameters.get(17));
    }

    @Test
    void addPreservesNonBhytLoaiBaoHiem() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        Persistence persistence = persistence(jdbc);

        BaoHiem card = new BaoHiem();
        card.setLoaiBaoHiem(LoaiBaoHiem.BAO_HIEM_TAI_NAN);
        card.setMaBenhNhan(202);
        card.setSoBaoHiem("TN-999-888");

        KetQua<Integer> insertResult = persistence.transactions.inTransaction(() -> {
            persistence.repository.add(card);
            return KetQua.success(card.getId());
        });

        assertTrue(insertResult.isSuccess());
        assertEquals("BAO_HIEM_TAI_NAN", jdbc.parameters.get(2), "Non-BHYT loaiBaoHiem must be preserved");
    }

    @Test
    void updateBindsAll19ParametersPreservesNonBhytAndIncrementsVersionOnSuccessfulUpdate() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        jdbc.executeUpdateResult = 1;
        Persistence persistence = persistence(jdbc);

        BaoHiem card = new BaoHiem();
        card.setId(101);
        card.setVersion(3L);
        card.setLoaiBaoHiem(LoaiBaoHiem.BAO_HIEM_TAI_NAN);
        card.setMaBenhNhan(202);
        card.setSoBaoHiem("TN-999-888");
        card.setNgayBatDau(LocalDate.of(2025, 1, 1));
        card.setNgayHetHan(LocalDate.of(2025, 12, 31));
        card.setMucHuong(100);
        card.setMaNoiSong("K2");
        card.setMaNoiDangKyKCB("79015");
        card.setNoiDangKyKCB("BV Cho Ray");
        card.setDiaChiThe("456 Tran Hung Dao");
        card.setDungTuyen(false);
        card.setCoGiayKhaiSinh(true);
        card.setKhongGiuThe(false);
        card.setMienCungChiTra(false);
        card.setNgayMienCungChiTra(null);
        card.setNgayDu5NamLienTuc(null);
        card.setUpdatedBy(20);

        KetQua<Integer> updateResult = persistence.transactions.inTransaction(() -> {
            persistence.repository.update(card);
            return KetQua.success(persistence.unitOfWork.saveChanges());
        });

        assertTrue(updateResult.isSuccess());
        assertEquals(1, updateResult.getValue().orElseThrow());

        assertTrue(jdbc.sql.startsWith("UPDATE BaoHiem SET"));
        assertTrue(jdbc.sql.contains("WHERE id=? AND version=? AND isDeleted=0"));
        assertEquals(19, jdbc.parameters.size());

        assertEquals(202, jdbc.parameters.get(1));
        assertEquals("BAO_HIEM_TAI_NAN", jdbc.parameters.get(2), "Non-BHYT loaiBaoHiem must be preserved in update");
        assertEquals("TN-999-888", jdbc.parameters.get(3));
        assertEquals(LocalDate.of(2025, 1, 1), jdbc.parameters.get(4));
        assertEquals(LocalDate.of(2025, 12, 31), jdbc.parameters.get(5));
        assertEquals(100, jdbc.parameters.get(6));
        assertEquals("K2", jdbc.parameters.get(7));
        assertEquals("79015", jdbc.parameters.get(8));
        assertEquals("BV Cho Ray", jdbc.parameters.get(9));
        assertEquals("456 Tran Hung Dao", jdbc.parameters.get(10));
        assertEquals(false, jdbc.parameters.get(11));
        assertEquals(true, jdbc.parameters.get(12));
        assertEquals(false, jdbc.parameters.get(13));
        assertEquals(false, jdbc.parameters.get(14));
        assertNull(jdbc.parameters.get(15));
        assertNull(jdbc.parameters.get(16));
        assertEquals(20, jdbc.parameters.get(17), "Parameter 17 must be updatedBy");
        assertEquals(101, jdbc.parameters.get(18), "Parameter 18 must be id");
        assertEquals(3L, jdbc.parameters.get(19), "Parameter 19 must be version");

        assertEquals(4L, card.getVersion(), "Card version must increment by 1 on successful update");
        assertNotNull(card.getUpdatedAt(), "Card updatedAt must be updated");
    }

    @Test
    void updateThrowsIllegalStateExceptionOnZeroRowOptimisticLockConflict() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        jdbc.executeUpdateResult = 0; // Simulate 0 rows updated
        Persistence persistence = persistence(jdbc);

        BaoHiem card = new BaoHiem();
        card.setId(101);
        card.setVersion(3L);
        card.setMaBenhNhan(202);

        KetQua<Integer> updateResult = persistence.transactions.inTransaction(() -> {
            try {
                persistence.repository.update(card);
                throw new AssertionError("Expected optimistic lock conflict.");
            } catch (IllegalStateException conflict) {
                assertEquals("Thẻ BHYT đã thay đổi hoặc không còn tồn tại.", conflict.getMessage());
                return KetQua.success(persistence.unitOfWork.saveChanges());
            }
        });

        assertTrue(updateResult.isSuccess());
        assertEquals(0, updateResult.getValue().orElseThrow(), "No unit-of-work changes should be recorded on conflict");
        assertEquals(3L, card.getVersion(), "Version should remain unchanged on optimistic lock failure");
    }

    @Test
    void updateThrowsIllegalArgumentExceptionWhenCardIdIsNull() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        Persistence persistence = persistence(jdbc);

        BaoHiem card = new BaoHiem();
        card.setId(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> persistence.repository.update(card));
        assertEquals("Không thể cập nhật thẻ BHYT chưa có ID.", ex.getMessage());
    }

    private static void assertSelectedInsuranceColumns(String sql) {
        assertTrue(sql.startsWith("SELECT"), "Expected SELECT statement");
        for (String column : INSURANCE_COLUMNS) {
            assertTrue(sql.contains(column), "SQL should include insurance column " + column);
        }
    }

    private static Map<String, Object> syntheticInsuranceRow(int id, int maBenhNhan, String loaiBaoHiem) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("maBenhNhan", maBenhNhan);
        row.put("loaiBaoHiem", loaiBaoHiem);
        row.put("soBaoHiem", "TN-999-888");
        row.put("ngayBatDau", Date.valueOf("2025-01-01"));
        row.put("ngayHetHan", Date.valueOf("2025-12-31"));
        row.put("mucHuong", 80);
        row.put("maNoiSong", "K1");
        row.put("maNoiDangKyKCB", "79014");
        row.put("noiDangKyKCB", "BV Quan 1");
        row.put("diaChiThe", "123 Nguyen Trai, Q1");
        row.put("dungTuyen", true);
        row.put("coGiayKhaiSinh", true);
        row.put("khongGiuThe", false);
        row.put("mienCungChiTra", true);
        row.put("ngayMienCungChiTra", Date.valueOf("2025-06-01"));
        row.put("ngayDu5NamLienTuc", Date.valueOf("2024-01-01"));
        row.put("version", 5L);
        row.put("createdAt", Timestamp.valueOf("2025-01-01 08:00:00"));
        row.put("createdBy", 10);
        row.put("updatedAt", Timestamp.valueOf("2025-02-01 09:30:00"));
        row.put("updatedBy", 20);
        row.put("isDeleted", false);
        row.put("deletedAt", null);
        row.put("deletedBy", null);
        return row;
    }

    private static Persistence persistence(SyntheticJdbc jdbc) {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy transactions = transactionManager(unitOfWork, jdbc.connection);
        BaoHiemRepository repository = new BaoHiemRepository(
                new JdbcKetNoiNhaCungCap(transactions), unitOfWork);
        return new Persistence(transactions, repository, unitOfWork);
    }

    private static JdbcGiaoDichQuanLy transactionManager(
            JdbcUnitOfWork unitOfWork, Connection connection) {
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

    private record Persistence(
            JdbcGiaoDichQuanLy transactions,
            BaoHiemRepository repository,
            JdbcUnitOfWork unitOfWork) {}

    private static final class SyntheticJdbc {
        private final Connection connection;
        private List<Map<String, Object>> rows = new ArrayList<>();
        private Map<Integer, Object> parameters = new LinkedHashMap<>();
        private String sql = "";
        private int executeUpdateResult = 1;
        private int generatedId = 88;
        private boolean autoCommit = true;

        private SyntheticJdbc() {
            connection = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setAutoCommit" -> {
                            autoCommit = (boolean) args[0];
                            yield null;
                        }
                        case "getAutoCommit" -> autoCommit;
                        case "commit", "rollback", "close" -> null;
                        case "prepareStatement" -> prepareStatement((String) args[0]);
                        case "isClosed" -> false;
                        case "toString" -> "SyntheticBaoHiemConnection";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private PreparedStatement prepareStatement(String statement) {
            sql = statement;
            parameters = new LinkedHashMap<>();
            List<Map<String, Object>> activeRows = statement.startsWith("INSERT")
                    ? List.of(Map.of())
                    : rows;
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setString", "setObject", "setInt", "setLong", "setDate", "setTimestamp",
                                "setBoolean" -> {
                            parameters.put((Integer) args[0], args[1]);
                            yield null;
                        }
                        case "executeQuery" -> resultSet(activeRows);
                        case "executeUpdate" -> executeUpdateResult;
                        case "close" -> null;
                        case "toString" -> "SyntheticBaoHiemPreparedStatement";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private ResultSet resultSet(List<Map<String, Object>> rowsToIterate) {
            int[] cursor = {-1};
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(),
                    new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "next" -> {
                            cursor[0]++;
                            yield cursor[0] < rowsToIterate.size();
                        }
                        case "getInt" -> {
                            if (args[0] instanceof Number) {
                                yield generatedId;
                            }
                            Object value = currentVal(rowsToIterate, cursor[0], args[0]);
                            yield value == null ? 0 : ((Number) value).intValue();
                        }
                        case "getLong" -> {
                            Object value = currentVal(rowsToIterate, cursor[0], args[0]);
                            yield value == null ? 0L : ((Number) value).longValue();
                        }
                        case "getString" -> {
                            Object value = currentVal(rowsToIterate, cursor[0], args[0]);
                            yield value == null ? null : value.toString();
                        }
                        case "getDate" -> {
                            Object value = currentVal(rowsToIterate, cursor[0], args[0]);
                            if (value instanceof Date d) yield d;
                            if (value instanceof LocalDate ld) yield Date.valueOf(ld);
                            yield null;
                        }
                        case "getTimestamp" -> {
                            Object value = currentVal(rowsToIterate, cursor[0], args[0]);
                            if (value instanceof Timestamp ts) yield ts;
                            if (value instanceof LocalDateTime ldt) yield Timestamp.valueOf(ldt);
                            yield null;
                        }
                        case "getObject" -> currentVal(rowsToIterate, cursor[0], args[0]);
                        case "getBoolean" -> Boolean.TRUE.equals(currentVal(rowsToIterate, cursor[0], args[0]));
                        case "close" -> null;
                        case "toString" -> "SyntheticBaoHiemResultSet";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private Object currentVal(List<Map<String, Object>> rowsToIterate, int cursorIndex, Object column) {
            if (cursorIndex < 0 || cursorIndex >= rowsToIterate.size()) return null;
            return column instanceof String name ? rowsToIterate.get(cursorIndex).get(name) : null;
        }
    }
}
