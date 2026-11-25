package vn.clinicmanagement.infrastructure.persistence.service;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.LoaiDichVu;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiTietKetQuaXetNghiem;
import vn.clinicmanagement.domain.service.DanhMucDichVu;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChiDinhDichVuRepositoryTest {

    @Test
    void searchDichVuQueriesActiveExaminationServicesWithCorrectFilters() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        jdbc.rows = List.of(
                Map.of(
                        "id", 1,
                        "maDichVu", "DV_KHAM_NHI",
                        "tenDichVu", "Khám Nhi chuyên khoa",
                        "loaiDichVu", "KHAM_BENH",
                        "donGia", new BigDecimal("120000"),
                        "giaBHYT", new BigDecimal("39000"),
                        "hoatDong", true,
                        "moTa", "Khám nhi"
                ),
                Map.of(
                        "id", 2,
                        "maDichVu", "DV_KHAM_NOI",
                        "tenDichVu", "Khám Nội tổng quát",
                        "loaiDichVu", "KHAM_BENH",
                        "donGia", new BigDecimal("100000"),
                        "giaBHYT", new BigDecimal("39000"),
                        "hoatDong", true,
                        "moTa", "Khám nội"
                )
        );

        JdbcUnitOfWork uow = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy tx = transactionManager(uow, jdbc.createConnection());
        JdbcKetNoiNhaCungCap connProvider = new JdbcKetNoiNhaCungCap(tx);
        ChiDinhDichVuRepository repo = new ChiDinhDichVuRepository(connProvider, uow);

        List<DanhMucDichVu> list = tx.inTransaction(() -> KetQua.success(repo.searchDichVu("", LoaiDichVu.KHAM)))
                .getValue().orElseThrow();

        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("DV_KHAM_NHI", list.get(0).getMaDichVu());
        assertEquals(LoaiDichVu.KHAM, list.get(0).getLoaiDichVu());
        assertEquals("Khám Nhi chuyên khoa", list.get(0).getTenDichVu());
        assertTrue(jdbc.sql.contains("WHERE isDeleted = 0"));
        assertTrue(jdbc.sql.contains("loaiDichVu IN ('KHAM_BENH', 'KHAM')"));
        assertTrue(jdbc.sql.contains("nhomDichVu"));
    }

    @Test
    void searchDichVuMapsNhomDichVuForParaclinicalGroups() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        jdbc.rows = List.of(
                Map.of(
                        "id", 4,
                        "maDichVu", "XN_GLUCOSE",
                        "tenDichVu", "Định lượng Glucose máu",
                        "loaiDichVu", "XET_NGHIEM",
                        "nhomDichVu", "Hóa sinh",
                        "donGia", new BigDecimal("45000"),
                        "giaBHYT", new BigDecimal("25000"),
                        "hoatDong", true,
                        "moTa", "Đường huyết"
                )
        );

        JdbcUnitOfWork uow = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy tx = transactionManager(uow, jdbc.createConnection());
        JdbcKetNoiNhaCungCap connProvider = new JdbcKetNoiNhaCungCap(tx);
        ChiDinhDichVuRepository repo = new ChiDinhDichVuRepository(connProvider, uow);

        List<DanhMucDichVu> list = tx.inTransaction(() -> KetQua.success(repo.searchDichVu("Hóa sinh", LoaiDichVu.XET_NGHIEM)))
                .getValue().orElseThrow();

        assertEquals(1, list.size());
        assertEquals("Hóa sinh", list.get(0).getNhomDichVu());
        assertEquals(LoaiDichVu.XET_NGHIEM, list.get(0).getLoaiDichVu());
    }

    @Test
    void saveInsertsServiceOrderAndAssignsGeneratedId() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        jdbc.rows = List.of(Map.of("1", 505));

        JdbcUnitOfWork uow = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy tx = transactionManager(uow, jdbc.createConnection());
        JdbcKetNoiNhaCungCap connProvider = new JdbcKetNoiNhaCungCap(tx);
        ChiDinhDichVuRepository repo = new ChiDinhDichVuRepository(connProvider, uow);

        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setMaLuotKham(2020);
        order.setMaDichVu(3);
        order.setMaBacSiChiDinh(2);
        order.setSoLuong(BigDecimal.ONE);
        order.setDonGia(new BigDecimal("110000"));
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_CHI_DINH);
        order.setThoiGianChiDinh(LocalDateTime.of(2026, 11, 18, 13, 30));

        tx.inTransaction(() -> {
            repo.save(order);
            return KetQua.success(order);
        });

        assertEquals(Integer.valueOf(505), order.getId());
        assertTrue(jdbc.insertSql.startsWith("INSERT INTO ChiDinhDichVu"));
        assertTrue(jdbc.insertSql.contains("OUTPUT INSERTED.id"));
        assertEquals(2020, jdbc.insertParameters.get(1));
        assertEquals(3, jdbc.insertParameters.get(2));
    }

    @Test
    void getByLuotKhamMapsServiceOrdersAndLabResults() {
        SyntheticJdbc jdbc = new SyntheticJdbc();
        jdbc.rows = List.of(
                Map.ofEntries(
                        Map.entry("id", 71),
                        Map.entry("maLuotKham", 2020),
                        Map.entry("maDichVu", 3),
                        Map.entry("maBacSiChiDinh", 2),
                        Map.entry("soLuong", BigDecimal.ONE),
                        Map.entry("donGia", new BigDecimal("110000")),
                        Map.entry("trangThai", "DA_CHI_DINH"),
                        Map.entry("thoiGianChiDinh", Timestamp.valueOf(LocalDateTime.of(2026, 11, 18, 13, 30))),
                        Map.entry("version", 0L)
                )
        );

        JdbcUnitOfWork uow = new JdbcUnitOfWork();
        JdbcGiaoDichQuanLy tx = transactionManager(uow, jdbc.createConnection());
        JdbcKetNoiNhaCungCap connProvider = new JdbcKetNoiNhaCungCap(tx);
        ChiDinhDichVuRepository repo = new ChiDinhDichVuRepository(connProvider, uow);

        List<ChiDinhDichVu> orders = tx.inTransaction(() -> KetQua.success(repo.getByLuotKham(2020)))
                .getValue().orElseThrow();
        assertEquals(1, orders.size());
        assertEquals(71, orders.get(0).getId());
        assertEquals(TrangThaiChiDinhDichVu.DA_CHI_DINH, orders.get(0).getTrangThai());
        assertTrue(jdbc.sql.contains("FROM ChiDinhDichVu WHERE isDeleted = 0 AND maLuotKham = ?"));

        jdbc.rows = List.of(
                Map.of(
                        "id", 12,
                        "maChiDinhDichVu", 71,
                        "tenChiSo", "WBC",
                        "giaTri", "6.8",
                        "donViDo", "G/L",
                        "khoangThamChieu", "4.0 - 10.0",
                        "danhGia", "Bình thường",
                        "version", 0L
                )
        );
        List<ChiTietKetQuaXetNghiem> details = tx.inTransaction(() -> KetQua.success(repo.getKetQuaByChiDinh(71)))
                .getValue().orElseThrow();
        assertEquals(1, details.size());
        assertEquals("WBC", details.get(0).getTenChiSo());
        assertEquals("6.8", details.get(0).getGiaTri());
    }

    private static JdbcGiaoDichQuanLy transactionManager(
            JdbcUnitOfWork unitOfWork, Connection connection) {
        try {
            Class<?> supplierType = Class.forName(
                    "vn.clinicmanagement.infrastructure.persistence.JdbcGiaoDichQuanLy$ConnectionSupplier");
            Object supplier = Proxy.newProxyInstance(
                    supplierType.getClassLoader(),
                    new Class<?>[]{supplierType},
                    (proxy, method, args) -> connection);
            java.lang.reflect.Constructor<JdbcGiaoDichQuanLy> constructor = JdbcGiaoDichQuanLy.class
                    .getDeclaredConstructor(JdbcUnitOfWork.class, supplierType);
            constructor.setAccessible(true);
            return constructor.newInstance(unitOfWork, supplier);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Could not configure synthetic JDBC transaction.", ex);
        }
    }

    private static final class SyntheticJdbc {
        private String sql;
        private String insertSql;
        private final Map<Integer, Object> insertParameters = new LinkedHashMap<>();
        private List<Map<String, Object>> rows = new ArrayList<>();
        private final Map<Integer, Object> parameters = new LinkedHashMap<>();

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

        private PreparedStatement prepareStatement(String statement) {
            this.sql = statement;
            if (statement.startsWith("INSERT INTO ChiDinhDichVu")) this.insertSql = statement;
            this.parameters.clear();
            return (PreparedStatement) Proxy.newProxyInstance(
                    PreparedStatement.class.getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "setObject", "setString", "setInt", "setBigDecimal", "setTimestamp" -> {
                            parameters.put((Integer) args[0], args[1]);
                            if (statement.startsWith("INSERT INTO ChiDinhDichVu")) insertParameters.put((Integer) args[0], args[1]);
                            yield null;
                        }
                        case "executeQuery" -> resultSet(rows);
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
                        case "next" -> {
                            cursor[0]++;
                            yield cursor[0] < rowsToIterate.size();
                        }
                        case "getInt" -> {
                            Object val = rowsToIterate.get(cursor[0]).get(String.valueOf(args[0]));
                            yield val instanceof Number ? ((Number) val).intValue() : 0;
                        }
                        case "getLong" -> {
                            Object val = rowsToIterate.get(cursor[0]).get(args[0]);
                            yield val instanceof Number ? ((Number) val).longValue() : 0L;
                        }
                        case "getObject" -> rowsToIterate.get(cursor[0]).get(args[0]);
                        case "getTimestamp" -> {
                            Object val = rowsToIterate.get(cursor[0]).get(args[0]);
                            yield val instanceof Timestamp ? val : null;
                        }
                        case "getString" -> {
                            Object val = rowsToIterate.get(cursor[0]).get(args[0]);
                            yield val != null ? val.toString() : null;
                        }
                        case "getBigDecimal" -> {
                            Object val = rowsToIterate.get(cursor[0]).get(args[0]);
                            yield val instanceof BigDecimal ? val : null;
                        }
                        case "getBoolean" -> {
                            Object val = rowsToIterate.get(cursor[0]).get(args[0]);
                            yield Boolean.TRUE.equals(val);
                        }
                        case "close" -> null;
                        default -> null;
                    });
        }
    }
}
