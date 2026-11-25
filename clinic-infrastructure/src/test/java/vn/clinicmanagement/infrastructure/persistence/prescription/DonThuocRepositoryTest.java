package vn.clinicmanagement.infrastructure.persistence.prescription;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.prescription.ChiTietDonThuoc;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DonThuocRepositoryTest {

    @Test
    void getByLuotKhamIdLoadsPrescriptionHeadersAndJoinedMedicineDetails() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        Map<String, Object> headerRow = new HashMap<>();
        headerRow.put("id", 55);
        headerRow.put("maLuotKham", 12);
        headerRow.put("maBacSi", 3);
        headerRow.put("maCoSo", 1);
        headerRow.put("maKhoXuat", 1);
        headerRow.put("ngayKe", Timestamp.valueOf(LocalDateTime.of(2026, 11, 18, 10, 30)));
        headerRow.put("trangThai", "DA_DUYET");
        headerRow.put("ghiChu", "Uống đủ nước");
        headerRow.put("loiDanDonThuoc", "Tái khám sau 3 ngày");
        headerRow.put("version", 1L);
        jdbc.headerRows = List.of(headerRow);

        Map<String, Object> detailRow = new HashMap<>();
        detailRow.put("id", 101);
        detailRow.put("maDonThuoc", 55);
        detailRow.put("maItem", 7);
        detailRow.put("soLuongKe", new BigDecimal("10"));
        detailRow.put("soLuongDaXuat", BigDecimal.ZERO);
        detailRow.put("lieuDung", "1 gói/lần");
        detailRow.put("tanSuat", "Ngày 2 lần");
        detailRow.put("soNgay", 5);
        detailRow.put("huongDan", "Uống trước ăn");
        detailRow.put("donGiaKe", new BigDecimal("3357"));
        detailRow.put("duongDung", "Uống");
        detailRow.put("sang", new BigDecimal("1"));
        detailRow.put("trua", BigDecimal.ZERO);
        detailRow.put("chieu", new BigDecimal("1"));
        detailRow.put("toi", BigDecimal.ZERO);
        detailRow.put("itemCode", "THUOC004");
        detailRow.put("itemName", "Gastropulgite 2.5g");
        detailRow.put("itemUnit", "Gói");
        detailRow.put("itemPack", "Hộp 30 gói");
        detailRow.put("itemPrice", new BigDecimal("3357"));
        detailRow.put("itemType", "THUOC");
        jdbc.detailRows = List.of(detailRow);

        JdbcUnitOfWork uow = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy tx = transactionManager(uow, jdbc.createConnection());
        DonThuocRepository repo = new DonThuocRepository(new JdbcKetNoiNhaCungCap(tx), uow);

        List<DonThuoc> list = tx.inTransaction(() -> KetQua.success(repo.getByLuotKhamId(12)))
                .getValue().orElseThrow();

        assertEquals(1, list.size());
        DonThuoc rx = list.get(0);
        assertEquals(55, rx.getId());
        assertEquals(TrangThaiDonThuoc.DA_DUYET, rx.getTrangThai());
        assertEquals("Uống đủ nước", rx.getGhiChu());
        assertEquals(1, rx.getChiTietDonThuocs().size());
        ChiTietDonThuoc detail = rx.getChiTietDonThuocs().get(0);
        assertEquals("THUOC004", detail.getItem().getMaItem());
        assertEquals("Gastropulgite 2.5g", detail.getItem().getTenItem());
        assertEquals(new BigDecimal("10"), detail.getSoLuongKe());
    }

    @Test
    void addPersistsHeaderAndDetailsAssigningGeneratedIds() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        jdbc.nextInsertedId = 700;

        DonThuoc rx = new DonThuoc();
        rx.setMaLuotKham(12);
        rx.setMaBacSi(3);
        rx.setMaCoSo(1);
        rx.setMaKhoXuat(1);
        rx.setTrangThai(TrangThaiDonThuoc.DA_DUYET);
        rx.setGhiChu("Đơn mới");

        ChiTietDonThuoc detail = new ChiTietDonThuoc();
        detail.setMaItem(7);
        detail.setSoLuongKe(new BigDecimal("6"));
        detail.setDonGiaKe(new BigDecimal("210"));
        detail.setDuongDung("Uống");
        detail.setHuongDan("Uống sau ăn");
        rx.setChiTietDonThuocs(List.of(detail));

        JdbcUnitOfWork uow = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy tx = transactionManager(uow, jdbc.createConnection());
        DonThuocRepository repo = new DonThuocRepository(new JdbcKetNoiNhaCungCap(tx), uow);

        tx.inTransaction(() -> {
            repo.add(rx);
            return KetQua.success(rx);
        });

        assertNotNull(rx.getId());
        assertEquals(700, rx.getId());
        assertEquals(700, detail.getMaDonThuoc());
        assertNotNull(detail.getId());
        assertTrue(jdbc.executedSqls.stream().anyMatch(s -> s.contains("INSERT INTO DonThuoc")));
        assertTrue(jdbc.executedSqls.stream().anyMatch(s -> s.contains("INSERT INTO ChiTietDonThuoc")));
    }

    private static JdbcGiaoDichQuanLy transactionManager(JdbcUnitOfWork unitOfWork, Connection connection) {
        try {
            Class<?> supplierType = Class.forName(
                    "vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy$ConnectionSupplier");
            Object supplier = Proxy.newProxyInstance(
                    supplierType.getClassLoader(),
                    new Class<?>[]{supplierType},
                    (proxy, method, args) -> connection);
            var constructor = JdbcGiaoDichQuanLy.class.getDeclaredConstructor(JdbcUnitOfWork.class, supplierType);
            constructor.setAccessible(true);
            return constructor.newInstance(unitOfWork, supplier);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Could not configure synthetic JDBC transaction.", ex);
        }
    }

    private static final class SyntheticJdbc {
        private final List<String> executedSqls = new ArrayList<>();
        private List<Map<String, Object>> headerRows = List.of();
        private List<Map<String, Object>> detailRows = List.of();
        private int nextInsertedId = 100;

        private Connection createConnection() {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "close" -> null;
                        case "prepareStatement" -> prepareStatement((String) args[0]);
                        default -> null;
                    });
        }

        private PreparedStatement prepareStatement(String sql) {
            executedSqls.add(sql);
            boolean isInsert = sql.contains("INSERT INTO");
            List<Map<String, Object>> rowsForQuery;
            if (isInsert) {
                rowsForQuery = List.of(Map.of("GENERATED_ID", nextInsertedId++));
            } else if (sql.contains("FROM ChiTietDonThuoc")) {
                rowsForQuery = detailRows;
            } else {
                rowsForQuery = headerRows;
            }
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "executeQuery" -> resultSet(rowsForQuery);
                        case "executeUpdate" -> 1;
                        case "close" -> null;
                        default -> null;
                    });
        }

        private ResultSet resultSet(List<Map<String, Object>> rowsToIterate) {
            int[] cursor = {-1};
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(),
                    new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "next" -> ++cursor[0] < rowsToIterate.size();
                        case "getInt" -> {
                            Map<String, Object> row = rowsToIterate.get(cursor[0]);
                            Object val = args[0] instanceof Integer ? row.get("GENERATED_ID") : row.get(String.valueOf(args[0]));
                            yield val instanceof Number n ? n.intValue() : 0;
                        }
                        case "getLong" -> {
                            Object val = rowsToIterate.get(cursor[0]).get(String.valueOf(args[0]));
                            yield val instanceof Number n ? n.longValue() : 0L;
                        }
                        case "getObject" -> rowsToIterate.get(cursor[0]).get(String.valueOf(args[0]));
                        case "getString" -> {
                            Object val = rowsToIterate.get(cursor[0]).get(String.valueOf(args[0]));
                            yield val != null ? val.toString() : null;
                        }
                        case "getBigDecimal" -> {
                            Object val = rowsToIterate.get(cursor[0]).get(String.valueOf(args[0]));
                            yield val instanceof BigDecimal bd ? bd : null;
                        }
                        case "getTimestamp" -> {
                            Object val = rowsToIterate.get(cursor[0]).get(String.valueOf(args[0]));
                            yield val instanceof Timestamp ts ? ts : null;
                        }
                        case "close" -> null;
                        default -> null;
                    });
        }
    }
}
