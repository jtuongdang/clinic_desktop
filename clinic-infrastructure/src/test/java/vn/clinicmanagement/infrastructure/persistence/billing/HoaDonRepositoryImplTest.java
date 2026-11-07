package vn.clinicmanagement.infrastructure.persistence.billing;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.billing.ChiTietHoaDon;
import vn.clinicmanagement.domain.billing.CongNoBenhNhan;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuThu;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;
import vn.clinicmanagement.domain.common.enums.TrangThaiCongNo;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;
import vn.clinicmanagement.infrastructure.persistence.JdbcConnectionProvider;
import vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class HoaDonRepositoryImplTest {
    private static final int INVOICE_ID = 71;
    private static final int VISIT_ID = 18;
    private static final int PATIENT_ID = 24;
    private static final int FACILITY_ID = 3;

    @Test
    void saveInsertsAndUpdatesInvoiceWithItsLines() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        Persistence persistence = persistence(jdbc);
        HoaDon invoice = invoice();
        invoice.addChiTiet(line());

        assertSame(invoice, inTransaction(persistence, () -> persistence.repository.save(invoice)));
        assertEquals(400, invoice.getId());
        assertEquals(401, invoice.getChiTietHoaDons().get(0).getId());
        assertEquals(400, invoice.getChiTietHoaDons().get(0).getMaHoaDon());
        assertEquals(2, jdbc.statements.size());
        assertTrue(jdbc.statements.get(0).sql.startsWith("INSERT INTO HoaDon"));
        assertEquals("HD-SYNTH-71", jdbc.statements.get(0).bindings.get(1));
        assertEquals(new BigDecimal("725.00"), jdbc.statements.get(0).bindings.get(6));
        assertTrue(jdbc.statements.get(1).sql.startsWith("INSERT INTO ChiTietHoaDon"));
        assertEquals(400, jdbc.statements.get(1).bindings.get(1));

        invoice.setTongTien(new BigDecimal("800.00"));
        invoice.getChiTietHoaDons().get(0).setDonGia(new BigDecimal("800.00"));
        inTransaction(persistence, () -> persistence.repository.save(invoice));
        assertEquals(1L, invoice.getVersion());
        assertTrue(jdbc.statements.get(2).sql.startsWith("UPDATE HoaDon"));
        assertEquals(400, jdbc.statements.get(3).bindings.get(1));
        assertTrue(jdbc.statements.get(3).sql.startsWith("DELETE FROM ChiTietHoaDon"));
        assertEquals(400, jdbc.statements.get(4).bindings.get(1));
        assertEquals(new BigDecimal("800.00"), jdbc.statements.get(4).bindings.get(6));
    }

    @Test
    void invoiceLookupsHydrateLinesFromSyntheticRows() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        jdbc.rowsByTable.put("HoaDon", List.of(invoiceRow()));
        jdbc.rowsByTable.put("ChiTietHoaDon", List.of(lineRow()));
        Persistence persistence = persistence(jdbc);

        HoaDon byNumber = inTransaction(persistence,
                () -> persistence.repository.getBySoHoaDon("HD-SYNTH-71")).orElseThrow();
        HoaDon byVisit = inTransaction(persistence,
                () -> persistence.repository.getByLuotKham(VISIT_ID)).orElseThrow();
        List<HoaDon> byPatient = inTransaction(persistence,
                () -> persistence.repository.getByBenhNhan(PATIENT_ID));

        assertEquals(INVOICE_ID, byNumber.getId());
        assertEquals(TrangThaiHoaDon.THANH_TOAN_MOT_PHAN, byNumber.getTrangThai());
        assertEquals(1, byNumber.getChiTietHoaDons().size());
        assertEquals("synthetic service", byNumber.getChiTietHoaDons().get(0).getMoTa());
        assertSame(byNumber, byNumber.getChiTietHoaDons().get(0).getHoaDon());
        assertEquals(INVOICE_ID, byVisit.getId());
        assertEquals(1, byPatient.size());
        assertEquals("HD-SYNTH-71", jdbc.statements.get(0).bindings.get(1));
        assertTrue(jdbc.statements.get(0).sql.contains("soHoaDon=?"));
        assertEquals(PATIENT_ID, jdbc.statements.get(4).bindings.get(1));
        assertTrue(jdbc.statements.get(4).sql.contains("maBenhNhan=?"));
    }

    @Test
    void receiptAndDebtWritesAndReadsUseSyntheticJdbcRows() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        jdbc.rowsByTable.put("PhieuThu", List.of(receiptRow()));
        jdbc.rowsByTable.put("CongNoBenhNhan", List.of(debtRow()));
        Persistence persistence = persistence(jdbc);
        PhieuThu receipt = new PhieuThu();
        receipt.setMaHoaDon(INVOICE_ID);
        receipt.setSoTien(new BigDecimal("125.00"));
        receipt.setPhuongThuc(PhuongThucThanhToan.CHUYEN_KHOAN);
        receipt.setNguoiThu(9);
        receipt.setGhiChu("synthetic receipt");
        CongNoBenhNhan debt = new CongNoBenhNhan();
        debt.setMaHoaDon(INVOICE_ID);
        debt.setMaBenhNhan(PATIENT_ID);
        debt.setSoTienNo(new BigDecimal("600.00"));
        debt.setSoTienDaTra(new BigDecimal("125.00"));
        debt.setHanThanhToan(LocalDate.of(2026, 12, 1));
        debt.setTrangThai(TrangThaiCongNo.DANG_NO);

        inTransaction(persistence, () -> {
            persistence.repository.savePhieuThu(receipt);
            persistence.repository.saveCongNo(debt);
            return null;
        });
        List<PhieuThu> receipts = inTransaction(persistence,
                () -> persistence.repository.getPhieuThuByHoaDon(INVOICE_ID));
        var debtByInvoice = inTransaction(persistence,
                () -> persistence.repository.getCongNoByHoaDon(INVOICE_ID)).orElseThrow();
        List<CongNoBenhNhan> debtsByPatient = inTransaction(persistence,
                () -> persistence.repository.getCongNoByBenhNhan(PATIENT_ID));

        assertEquals(400, receipt.getId());
        assertEquals(401, debt.getId());
        assertTrue(jdbc.statements.get(0).sql.startsWith("INSERT INTO PhieuThu"));
        assertEquals("CHUYEN_KHOAN", jdbc.statements.get(0).bindings.get(4));
        assertTrue(jdbc.statements.get(1).sql.startsWith("INSERT INTO CongNoBenhNhan"));
        assertEquals(LocalDate.of(2026, 12, 1), jdbc.statements.get(1).bindings.get(5));
        assertEquals(1, receipts.size());
        assertEquals(PhuongThucThanhToan.CHUYEN_KHOAN, receipts.get(0).getPhuongThuc());
        assertEquals(new BigDecimal("475.00"), debtByInvoice.getSoTienConLai());
        assertEquals(1, debtsByPatient.size());
    }

    @Test
    void invalidInvoiceDoesNotAcquireJdbcConnection() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        HoaDon invoice = invoice();
        invoice.setMaLuotKham(0);

        assertThrows(IllegalArgumentException.class, () -> persistence(jdbc).repository.save(invoice));
        assertTrue(jdbc.statements.isEmpty());
    }

    private static HoaDon invoice() {
        HoaDon invoice = new HoaDon();
        invoice.setSoHoaDon("HD-SYNTH-71");
        invoice.setMaLuotKham(VISIT_ID);
        invoice.setMaBenhNhan(PATIENT_ID);
        invoice.setMaCoSo(FACILITY_ID);
        invoice.setNgayLap(LocalDateTime.of(2026, 11, 7, 9, 0));
        invoice.setTongTien(new BigDecimal("725.00"));
        invoice.setSoTienBaoHiemChiTra(new BigDecimal("100.00"));
        invoice.setSoTienBenhNhanChiTra(new BigDecimal("625.00"));
        invoice.setTrangThai(TrangThaiHoaDon.CHO_THANH_TOAN);
        return invoice;
    }

    private static ChiTietHoaDon line() {
        ChiTietHoaDon line = new ChiTietHoaDon();
        line.setLoaiDoiTuong("DICH_VU");
        line.setMaDoiTuong(6);
        line.setMoTa("synthetic service");
        line.setSoLuong(BigDecimal.ONE);
        line.setDonGia(new BigDecimal("725.00"));
        return line;
    }

    private static Map<String, Object> invoiceRow() {
        Map<String, Object> row = auditRow();
        row.put("id", INVOICE_ID);
        row.put("soHoaDon", "HD-SYNTH-71");
        row.put("maLuotKham", VISIT_ID);
        row.put("maBenhNhan", PATIENT_ID);
        row.put("maCoSo", FACILITY_ID);
        row.put("ngayLap", Timestamp.valueOf(LocalDateTime.of(2026, 11, 7, 9, 0)));
        row.put("tongTien", new BigDecimal("725.00"));
        row.put("soTienBaoHiemChiTra", new BigDecimal("100.00"));
        row.put("soTienBenhNhanChiTra", new BigDecimal("625.00"));
        row.put("daThanhToan", new BigDecimal("125.00"));
        row.put("soTienTamUng", BigDecimal.ZERO);
        row.put("soTienGiamGia", BigDecimal.ZERO);
        row.put("chietKhauPhanTram", BigDecimal.ZERO);
        row.put("lyDoGiamGia", null);
        row.put("soTienHoanTra", BigDecimal.ZERO);
        row.put("soHoaDonDienTu", null);
        row.put("maTraCuuHddt", null);
        row.put("trangThai", "THANH_TOAN_MOT_PHAN");
        row.put("ghiChu", "synthetic");
        return row;
    }

    private static Map<String, Object> lineRow() {
        Map<String, Object> row = auditRow();
        row.put("id", 91);
        row.put("maHoaDon", INVOICE_ID);
        row.put("loaiDoiTuong", "DICH_VU");
        row.put("maDoiTuong", 6);
        row.put("moTa", "synthetic service");
        row.put("soLuong", BigDecimal.ONE);
        row.put("donGia", new BigDecimal("725.00"));
        return row;
    }

    private static Map<String, Object> receiptRow() {
        Map<String, Object> row = auditRow();
        row.put("id", 93);
        row.put("maHoaDon", INVOICE_ID);
        row.put("soTien", new BigDecimal("125.00"));
        row.put("ngayThu", Timestamp.valueOf(LocalDateTime.of(2026, 11, 7, 10, 0)));
        row.put("phuongThuc", "CHUYEN_KHOAN");
        row.put("nguoiThu", 9);
        row.put("ghiChu", "synthetic receipt");
        return row;
    }

    private static Map<String, Object> debtRow() {
        Map<String, Object> row = auditRow();
        row.put("id", 94);
        row.put("maHoaDon", INVOICE_ID);
        row.put("maBenhNhan", PATIENT_ID);
        row.put("soTienNo", new BigDecimal("600.00"));
        row.put("soTienDaTra", new BigDecimal("125.00"));
        row.put("hanThanhToan", java.sql.Date.valueOf(LocalDate.of(2026, 12, 1)));
        row.put("trangThai", "DANG_NO");
        return row;
    }

    private static Map<String, Object> auditRow() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("version", 0L);
        row.put("createdAt", Timestamp.valueOf(LocalDateTime.of(2026, 11, 7, 9, 0)));
        row.put("createdBy", null);
        row.put("updatedAt", null);
        row.put("updatedBy", null);
        row.put("isDeleted", false);
        row.put("deletedAt", null);
        row.put("deletedBy", null);
        return row;
    }

    private static Persistence persistence(SyntheticJdbc jdbc) {
        JdbcUnitOfWork unitOfWork = new JdbcUnitOfWork();
        JdbcTransactionManager transactions = transactionManager(unitOfWork, jdbc.connection);
        JdbcConnectionProvider connections = new JdbcConnectionProvider(transactions);
        return new Persistence(transactions, new HoaDonRepositoryImpl(connections, unitOfWork));
    }

    private static JdbcTransactionManager transactionManager(JdbcUnitOfWork unitOfWork, Connection connection) {
        try {
            Class<?> supplierType = Class.forName(
                    "vn.clinicmanagement.infrastructure.persistence.JdbcTransactionManager$ConnectionSupplier");
            Object supplier = Proxy.newProxyInstance(supplierType.getClassLoader(), new Class<?>[]{supplierType},
                    (proxy, method, args) -> connection);
            Constructor<JdbcTransactionManager> constructor = JdbcTransactionManager.class
                    .getDeclaredConstructor(JdbcUnitOfWork.class, supplierType);
            constructor.setAccessible(true);
            return constructor.newInstance(unitOfWork, supplier);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not configure synthetic JDBC transaction.", exception);
        }
    }

    private static <T> T inTransaction(Persistence persistence, Supplier<T> operation) {
        Result<T> result = persistence.transactions.inTransaction(() -> Result.success(operation.get()));
        return result.valueOrThrow();
    }

    private record Persistence(JdbcTransactionManager transactions, HoaDonRepositoryImpl repository) {}

    private static final class SyntheticJdbc {
        final List<RecordedStatement> statements = new ArrayList<>();
        final Map<String, List<Map<String, Object>>> rowsByTable = new LinkedHashMap<>();
        private int nextId = 400;
        final Connection connection;

        SyntheticJdbc() {
            connection = (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setAutoCommit", "commit", "rollback", "close" -> null;
                        case "prepareStatement" -> prepareStatement((String) args[0]);
                        case "isClosed" -> false;
                        case "toString" -> "SyntheticBillingConnection";
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

        private List<Map<String, Object>> rowsFor(String sql) {
            if (sql.contains("FROM ChiTietHoaDon")) return rowsByTable.getOrDefault("ChiTietHoaDon", List.of());
            if (sql.contains("FROM PhieuThu")) return rowsByTable.getOrDefault("PhieuThu", List.of());
            if (sql.contains("FROM CongNoBenhNhan")) return rowsByTable.getOrDefault("CongNoBenhNhan", List.of());
            if (sql.contains("FROM HoaDon")) return rowsByTable.getOrDefault("HoaDon", List.of());
            throw new UnsupportedOperationException("Unexpected synthetic query: " + sql);
        }

        private ResultSet generatedKeyResultSet() {
            int generated = nextId++;
            int[] cursor = {-1};
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "next" -> ++cursor[0] == 0;
                        case "getInt" -> generated;
                        case "close" -> null;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }

    private static final class RecordedStatement {
        final String sql;
        final Map<Integer, Object> bindings = new LinkedHashMap<>();
        final PreparedStatement preparedStatement;
        private final SyntheticJdbc jdbc;

        RecordedStatement(SyntheticJdbc jdbc, String sql) {
            this.jdbc = jdbc;
            this.sql = sql;
            preparedStatement = (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(), new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setInt", "setLong", "setString", "setObject", "setBigDecimal", "setTimestamp" -> {
                            bindings.put((Integer) args[0], args[1]);
                            yield null;
                        }
                        case "executeQuery" -> sql.startsWith("SELECT")
                                ? resultSet(jdbc.rowsFor(sql)) : jdbc.generatedKeyResultSet();
                        case "executeUpdate" -> 1;
                        case "close" -> null;
                        case "toString" -> "SyntheticBillingStatement";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private ResultSet resultSet(List<Map<String, Object>> rows) {
            int[] cursor = {-1};
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(), new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "next" -> ++cursor[0] < rows.size();
                        case "getInt" -> ((Number) currentRow(rows, cursor).get(args[0])).intValue();
                        case "getLong" -> ((Number) currentRow(rows, cursor).get(args[0])).longValue();
                        case "getBoolean" -> (Boolean) currentRow(rows, cursor).get(args[0]);
                        case "getString" -> (String) currentRow(rows, cursor).get(args[0]);
                        case "getObject", "getBigDecimal" -> currentRow(rows, cursor).get(args[0]);
                        case "getTimestamp" -> (Timestamp) currentRow(rows, cursor).get(args[0]);
                        case "getDate" -> (java.sql.Date) currentRow(rows, cursor).get(args[0]);
                        case "close" -> null;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private static Map<String, Object> currentRow(List<Map<String, Object>> rows, int[] cursor) {
            if (cursor[0] < 0 || cursor[0] >= rows.size()) {
                throw new IllegalStateException("ResultSet cursor is not on a row.");
            }
            return rows.get(cursor[0]);
        }
    }
}
