package vn.clinicmanagement.application.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.inventory.TonKho;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GetInventoryReportHandler Tests (RPT-01)")
class GetKhoBaoCaoHandlerTest {

    private BaoCaoTestHoTro.InMemoryKhoRepository khoRepo;
    private BaoCaoTestHoTro.RecordingAuditWritePort auditPort;
    private GetKhoBaoCaoHandler handler;

    @BeforeEach
    void setUp() {
        khoRepo = new BaoCaoTestHoTro.InMemoryKhoRepository();
        auditPort = new BaoCaoTestHoTro.RecordingAuditWritePort();
        handler = new GetKhoBaoCaoHandler(khoRepo, auditPort);
    }

    @Test
    @DisplayName("ADMIN, DIRECTOR, PHARMACIST được phép truy vấn báo cáo kho dược")
    void shouldAllowAuthorizedRolesToQueryInventoryReport() {
        String[] authorizedRoles = {"ADMIN", "DIRECTOR", "PHARMACIST", "admin", "director", "pharmacist"};

        for (String role : authorizedRoles) {
            GetKhoBaoCaoQuery query = new GetKhoBaoCaoQuery(role, 1);
            KetQua<KhoBaoCaoDuLieu> result = handler.handle(query);

            assertTrue(result.isSuccess(), "Vai trò " + role + " phải được phép truy vấn báo cáo kho");
            assertNotNull(result.valueOrThrow());
        }
    }

    @Test
    @DisplayName("Từ chối DOCTOR, ACCOUNTANT, RECEPTIONIST, CASHIER và null truy vấn báo cáo kho dược")
    void shouldDenyUnauthorizedRolesFromQueryingInventoryReport() {
        String[] unauthorizedRoles = {"DOCTOR", "ACCOUNTANT", "RECEPTIONIST", "CASHIER", "LAB_TECH", "GUEST", "", null};

        for (String role : unauthorizedRoles) {
            GetKhoBaoCaoQuery query = new GetKhoBaoCaoQuery(role, 1);
            KetQua<KhoBaoCaoDuLieu> result = handler.handle(query);

            assertTrue(result.isFailure(), "Vai trò " + role + " phải bị từ chối");
            assertEquals("AUTHZ_DENIED", result.getError().orElseThrow().code());
        }
    }

    @Test
    @DisplayName("Tổng hợp chính xác số lượng tồn, giá trị tồn (nhập/bán), cảnh báo cận hạn và hết hạn")
    void shouldAggregateStockQuantitiesValuesAndExpiryAlertsAccurately() {
        LocalDate today = LocalDate.now();

        // 1. Mặt hàng Paracetamol (ID 1)
        MatHang para = new MatHang();
        para.setId(1);
        para.setMaItem("TH-PARA-500");
        para.setTenItem("Paracetamol 500mg");
        para.setDonViCoBan("Viên");
        para.setTonToiThieu(new BigDecimal("100")); // Mức tồn tối thiểu 100
        khoRepo.saveItem(para);

        // Lô 1: Đã hết hạn (hạn dùng hôm qua), 50 viên x giá nhập 1,000 / giá bán 1,500
        TonKho tk1 = new TonKho();
        tk1.setMaKho(1);
        tk1.setMaItem(1);
        tk1.setSoLo("LOT-EXP");
        tk1.setHanSuDung(today.minusDays(1));
        tk1.setSoLuongTon(new BigDecimal("50"));
        tk1.setGiaNhap(new BigDecimal("1000"));
        tk1.setGiaBan(new BigDecimal("1500"));
        khoRepo.saveTonKho(tk1);

        // Lô 2: Sắp hết hạn (còn 30 ngày < 180 ngày cảnh báo), 30 viên x giá nhập 1,000 / giá bán 1,500
        TonKho tk2 = new TonKho();
        tk2.setMaKho(1);
        tk2.setMaItem(1);
        tk2.setSoLo("LOT-NEAR");
        tk2.setHanSuDung(today.plusDays(30));
        tk2.setSoLuongTon(new BigDecimal("30"));
        tk2.setGiaNhap(new BigDecimal("1000"));
        tk2.setGiaBan(new BigDecimal("1500"));
        khoRepo.saveTonKho(tk2);

        // 2. Mặt hàng Amoxicillin (ID 2): Lô bình thường (hạn dùng 1 năm sau), 200 viên x giá nhập 2,000 / giá bán 3,000
        MatHang amox = new MatHang();
        amox.setId(2);
        amox.setMaItem("TH-AMOX-500");
        amox.setTenItem("Amoxicillin 500mg");
        amox.setDonViCoBan("Viên");
        amox.setTonToiThieu(new BigDecimal("50"));
        khoRepo.saveItem(amox);

        TonKho tk3 = new TonKho();
        tk3.setMaKho(1);
        tk3.setMaItem(2);
        tk3.setSoLo("LOT-OK");
        tk3.setHanSuDung(today.plusYears(1));
        tk3.setSoLuongTon(new BigDecimal("200"));
        tk3.setGiaNhap(new BigDecimal("2000"));
        tk3.setGiaBan(new BigDecimal("3000"));
        khoRepo.saveTonKho(tk3);

        GetKhoBaoCaoQuery query = new GetKhoBaoCaoQuery("PHARMACIST", 1, 180);
        KetQua<KhoBaoCaoDuLieu> result = handler.handle(query);

        assertTrue(result.isSuccess());
        KhoBaoCaoDuLieu dto = result.valueOrThrow();

        // Tổng số mặt hàng: 2 (Paracetamol & Amoxicillin)
        assertEquals(2, dto.getTotalItems());

        // Tổng số lượng tồn: 50 + 30 + 200 = 280
        assertEquals(new BigDecimal("280"), dto.getTotalStockQuantity());

        // Tổng giá trị tồn nhập: (50 * 1,000) + (30 * 1,000) + (200 * 2,000) = 50,000 + 30,000 + 400,000 = 480,000
        assertEquals(new BigDecimal("480000"), dto.getTotalStockValue());

        // Tổng giá trị bán lẻ: (50 * 1,500) + (30 * 1,500) + (200 * 3,000) = 75,000 + 45,000 + 600,000 = 720,000
        assertEquals(new BigDecimal("720000"), dto.getTotalRetailValue());

        // Cảnh báo hết hạn: 1 lô (LOT-EXP)
        assertEquals(1, dto.getExpiredItemCount());

        // Cảnh báo cận hạn: 1 lô (LOT-NEAR)
        assertEquals(1, dto.getNearExpiryItemCount());

        // Danh sách cảnh báo hạn dùng (hết hạn + cận hạn = 2 lô)
        assertEquals(2, dto.getExpiryAlerts().size());

        // Cảnh báo tồn thấp: Paracetamol có tổng tồn = 80 <= tồn tối thiểu 100
        assertEquals(1, dto.getLowStockItemCount());
        assertFalse(dto.getLowStockAlerts().isEmpty());
    }

    @Test
    @DisplayName("Báo lỗi QUERY_INVALID khi query là null")
    void shouldFailWhenQueryIsNull() {
        KetQua<KhoBaoCaoDuLieu> result = handler.handle(null);

        assertTrue(result.isFailure());
        assertEquals("QUERY_INVALID", result.getError().orElseThrow().code());
    }

    @Test
    @DisplayName("Xử lý an toàn khi kho trống không có dữ liệu")
    void shouldHandleEmptyWarehouseGracefully() {
        GetKhoBaoCaoQuery query = new GetKhoBaoCaoQuery("PHARMACIST", 999);
        KetQua<KhoBaoCaoDuLieu> result = handler.handle(query);

        assertTrue(result.isSuccess());
        KhoBaoCaoDuLieu dto = result.valueOrThrow();
        assertEquals(0, dto.getTotalItems());
        assertEquals(BigDecimal.ZERO, dto.getTotalStockQuantity());
        assertEquals(BigDecimal.ZERO, dto.getTotalStockValue());
        assertEquals(0, dto.getExpiredItemCount());
        assertEquals(0, dto.getNearExpiryItemCount());
        assertTrue(dto.getItems().isEmpty());
    }

    @Test
    @DisplayName("Ghi nhật ký kiểm toán CATALOG_VIEW khi truy vấn thành công")
    void shouldRecordAuditLogOnSuccess() {
        UUID sessionId = UUID.randomUUID();
        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(30, 3, "PHARMACIST", 1, 1, sessionId);
        KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(30, 3, "PHARMACIST", 1, 1, "WS-PHARM-01", UUID.randomUUID());

        GetKhoBaoCaoQuery query = new GetKhoBaoCaoQuery(auth, actor, 1);
        KetQua<KhoBaoCaoDuLieu> result = handler.handle(query);

        assertTrue(result.isSuccess());
        assertFalse(auditPort.systemAudits.isEmpty());
        assertEquals(KiemToanHanhDongMa.CATALOG_VIEW, auditPort.systemAudits.get(0).actionCode());
        assertEquals(KiemToanKetQua.SUCCESS, auditPort.systemAudits.get(0).outcome());
    }

    @Test
    @DisplayName("Kiểm tra định dạng bảng 7 cột toTableRow trên InventoryReportItemDto")
    void shouldFormatTableRowAccurately() {
        KhoBaoCaoMatHangDuLieu item = new KhoBaoCaoMatHangDuLieu(
                1, 10, "ITM-01", "Panadol Extra", "Hộp", 1, "Kho Dược Chính",
                "LOT-2026", LocalDate.of(2027, 5, 20),
                new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("500"), new BigDecimal("120000"), new BigDecimal("150000"),
                new BigDecimal("60000000"), 400L,
                false, false, false,
                "BÌNH THƯỜNG", "Tồn kho an toàn"
        );

        Object[] row = item.toTableRow(1);
        assertEquals(7, row.length);
        assertEquals("1", row[0]);
        assertTrue(row[1].toString().contains("Panadol Extra"));
        assertTrue(row[1].toString().contains("LOT-2026"));
        assertEquals("Hộp", row[2]);
        assertEquals("100", row[3]);
        assertEquals("500", row[4]);
        assertTrue(row[6].toString().contains("20/05/2027"));
    }
}
