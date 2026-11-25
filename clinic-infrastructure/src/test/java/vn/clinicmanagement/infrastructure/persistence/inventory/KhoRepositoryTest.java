package vn.clinicmanagement.infrastructure.persistence.inventory;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.LoaiKho;
import vn.clinicmanagement.domain.common.enums.LoaiMatHang;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy;
import vn.clinicmanagement.infrastructure.persistence.JdbcKetNoiNhaCungCap;
import vn.clinicmanagement.infrastructure.persistence.JdbcUnitOfWork;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KhoRepositoryTest {

    @Test
    void searchItemsQueriesActiveItemsAndAttachesTonKho() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        Map<String, Object> itemRow = new HashMap<>();
        itemRow.put("id", 7);
        itemRow.put("maItem", "THUOC007");
        itemRow.put("tenItem", "Paracetamol (Mypara 500) 500mg");
        itemRow.put("loaiItem", "THUOC");
        itemRow.put("hoatChat", "Paracetamol");
        itemRow.put("hamLuong", "500mg");
        itemRow.put("donViCoBan", "Viên");
        itemRow.put("quyCachDongGoi", "Hộp 10 vỉ x 10 viên");
        itemRow.put("giaNhapMacDinh", new BigDecimal("150"));
        itemRow.put("giaBanMacDinh", new BigDecimal("210"));
        itemRow.put("tonToiThieu", new BigDecimal("10"));
        itemRow.put("tonToiDa", new BigDecimal("5000"));
        itemRow.put("dangBaoChe", "Viên nén");
        itemRow.put("nuocSanXuat", "Việt Nam");
        itemRow.put("hangSanXuat", "Pharmedic");
        itemRow.put("soDangKy", "VD-12345");
        itemRow.put("hoatDong", true);
        jdbc.itemRows = List.of(itemRow);

        Map<String, Object> stockRow = new HashMap<>();
        stockRow.put("id", 91);
        stockRow.put("maKho", 1);
        stockRow.put("maItem", 7);
        stockRow.put("soLo", "LO-2026-01");
        stockRow.put("hanSuDung", Date.valueOf(LocalDate.of(2028, 12, 31)));
        stockRow.put("soLuongTon", new BigDecimal("4601"));
        stockRow.put("giaNhap", new BigDecimal("150"));
        stockRow.put("giaBan", new BigDecimal("210"));
        jdbc.tonKhoRows = List.of(stockRow);

        JdbcUnitOfWork uow = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy tx = transactionManager(uow, jdbc.createConnection());
        KhoRepository repo = new KhoRepository(new JdbcKetNoiNhaCungCap(tx), uow);

        List<MatHang> items = tx.inTransaction(() -> KetQua.success(repo.searchItems("Para", true)))
                .getValue().orElseThrow();

        assertEquals(1, items.size());
        MatHang first = items.get(0);
        assertEquals(7, first.getId());
        assertEquals("THUOC007", first.getMaItem());
        assertEquals(LoaiMatHang.THUOC, first.getLoaiItem());
        assertEquals(new BigDecimal("210"), first.getGiaBanMacDinh());
        assertEquals(1, first.getTonKhos().size());
        assertEquals(new BigDecimal("4601"), first.getTonKhos().get(0).getSoLuongTon());
        assertTrue(jdbc.executedSqls.stream().anyMatch(s -> s.contains("FROM Item WHERE isDeleted = 0")));
        assertTrue(jdbc.executedSqls.stream().anyMatch(s -> s.contains("FROM TonKho WHERE isDeleted = 0")));
    }

    @Test
    void getKhoByIdMapsKhoLeToLoaiKhoThuoc() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        Map<String, Object> khoRow = new HashMap<>();
        khoRow.put("id", 1);
        khoRow.put("maCoSo", 1);
        khoRow.put("tenKho", "Kho Thuốc Ngoại Trú");
        khoRow.put("loaiKho", "KHO_LE");
        khoRow.put("trangThai", "HOAT_DONG");
        khoRow.put("maNguoiPhuTrach", 2);
        jdbc.khoRows = List.of(khoRow);

        JdbcUnitOfWork uow = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy tx = transactionManager(uow, jdbc.createConnection());
        KhoRepository repo = new KhoRepository(new JdbcKetNoiNhaCungCap(tx), uow);

        Kho kho = tx.inTransaction(() -> KetQua.success(repo.getKhoById(1).orElseThrow()))
                .getValue().orElseThrow();

        assertEquals("Kho Thuốc Ngoại Trú", kho.getTenKho());
        assertEquals(LoaiKho.THUOC, kho.getLoaiKho());
        assertEquals(1, kho.getMaCoSo());
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
        private List<Map<String, Object>> itemRows = List.of();
        private List<Map<String, Object>> tonKhoRows = List.of();
        private List<Map<String, Object>> khoRows = List.of();

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
            List<Map<String, Object>> rowsForQuery;
            if (sql.contains("FROM TonKho")) {
                rowsForQuery = tonKhoRows;
            } else if (sql.contains("FROM Kho")) {
                rowsForQuery = khoRows;
            } else {
                rowsForQuery = itemRows;
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
                            Object val = rowsToIterate.get(cursor[0]).get(String.valueOf(args[0]));
                            yield val instanceof Number n ? n.intValue() : 0;
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
                        case "getDate" -> {
                            Object val = rowsToIterate.get(cursor[0]).get(String.valueOf(args[0]));
                            yield val instanceof Date d ? d : null;
                        }
                        case "getBoolean" -> Boolean.TRUE.equals(rowsToIterate.get(cursor[0]).get(String.valueOf(args[0])));
                        case "close" -> null;
                        default -> null;
                    });
        }
    }
}
