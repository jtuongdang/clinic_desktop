package vn.clinicmanagement.application.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.facility.Khoa;
import vn.clinicmanagement.domain.facility.PhongKham;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GetVisitStatisticsHandler Tests (RPT-01)")
class GetLuotKhamThongKeHandlerTest {

    private BaoCaoTestHoTro.InMemoryVisitRepository visitRepo;
    private BaoCaoTestHoTro.RecordingAuditWritePort auditPort;
    private GetLuotKhamThongKeHandler handler;

    @BeforeEach
    void setUp() {
        visitRepo = new BaoCaoTestHoTro.InMemoryVisitRepository();
        auditPort = new BaoCaoTestHoTro.RecordingAuditWritePort();
        handler = new GetLuotKhamThongKeHandler(visitRepo, auditPort);
    }

    @Test
    @DisplayName("ADMIN, DIRECTOR, DOCTOR được phép truy vấn thống kê lượt khám")
    void shouldAllowAuthorizedRolesToQueryVisitStatistics() {
        String[] authorizedRoles = {"ADMIN", "DIRECTOR", "DOCTOR", "admin", "director", "doctor"};

        for (String role : authorizedRoles) {
            GetLuotKhamThongKeQuery query = new GetLuotKhamThongKeQuery(role, LocalDate.now().minusDays(7), LocalDate.now());
            KetQua<LuotKhamThongKeDuLieu> result = handler.handle(query);

            assertTrue(result.isSuccess(), "Vai trò " + role + " phải được phép truy vấn");
            assertNotNull(result.valueOrThrow());
        }
    }

    @Test
    @DisplayName("Từ chối ACCOUNTANT, PHARMACIST, RECEPTIONIST, CASHIER và null truy vấn thống kê lượt khám")
    void shouldDenyUnauthorizedRolesFromQueryingVisitStatistics() {
        String[] unauthorizedRoles = {"ACCOUNTANT", "PHARMACIST", "RECEPTIONIST", "CASHIER", "LAB_TECH", "GUEST", "", null};

        for (String role : unauthorizedRoles) {
            GetLuotKhamThongKeQuery query = new GetLuotKhamThongKeQuery(role, LocalDate.now().minusDays(7), LocalDate.now());
            KetQua<LuotKhamThongKeDuLieu> result = handler.handle(query);

            assertTrue(result.isFailure(), "Vai trò " + role + " phải bị từ chối");
            assertEquals("AUTHZ_DENIED", result.getError().orElseThrow().code());
        }
    }

    @Test
    @DisplayName("Tổng hợp chính xác số lượt khám theo khoa/phòng, trạng thái và tỷ lệ hoàn tất")
    void shouldAggregateVisitCountsAndCompletionRateAccurately() {
        LocalDate today = LocalDate.now();

        Khoa khoaNoi = new Khoa();
        khoaNoi.setTenKhoa("Khoa Nội");

        Khoa khoaNgoai = new Khoa();
        khoaNgoai.setTenKhoa("Khoa Ngoại");

        PhongKham phong101 = new PhongKham();
        phong101.setTenPhong("Phòng 101 - Nội tổng quát");

        // 1. Khoa Nội: 3 lượt (2 hoàn tất, 1 đang khám)
        LuotKham v1 = new LuotKham();
        v1.setMaKhoa(1);
        v1.setKhoa(khoaNoi);
        v1.setMaPhongKham(101);
        v1.setPhongKham(phong101);
        v1.setNgayKham(today);
        v1.setTrangThai(TrangThaiLuotKham.HOAN_THANH);
        visitRepo.save(v1);

        LuotKham v2 = new LuotKham();
        v2.setMaKhoa(1);
        v2.setKhoa(khoaNoi);
        v2.setMaPhongKham(101);
        v2.setPhongKham(phong101);
        v2.setNgayKham(today);
        v2.setTrangThai(TrangThaiLuotKham.DA_KHAM);
        visitRepo.save(v2);

        LuotKham v3 = new LuotKham();
        v3.setMaKhoa(1);
        v3.setKhoa(khoaNoi);
        v3.setNgayKham(today);
        v3.setTrangThai(TrangThaiLuotKham.DANG_KHAM);
        visitRepo.save(v3);

        // 2. Khoa Ngoại: 2 lượt (1 chờ khám, 1 đã hủy)
        LuotKham v4 = new LuotKham();
        v4.setMaKhoa(2);
        v4.setKhoa(khoaNgoai);
        v4.setNgayKham(today);
        v4.setTrangThai(TrangThaiLuotKham.CHO_KHAM);
        visitRepo.save(v4);

        LuotKham v5 = new LuotKham();
        v5.setMaKhoa(2);
        v5.setKhoa(khoaNgoai);
        v5.setNgayKham(today);
        v5.setTrangThai(TrangThaiLuotKham.DA_HUY);
        visitRepo.save(v5);

        GetLuotKhamThongKeQuery query = new GetLuotKhamThongKeQuery("DOCTOR", today.minusDays(1), today);
        KetQua<LuotKhamThongKeDuLieu> result = handler.handle(query);

        assertTrue(result.isSuccess());
        LuotKhamThongKeDuLieu dto = result.valueOrThrow();

        // Tổng: 5 lượt
        assertEquals(5, dto.getTotalVisits());
        // Hoàn tất: 2 (v1 + v2)
        assertEquals(2, dto.getCompletedVisits());
        // Chờ: 1 (v4)
        assertEquals(1, dto.getWaitingVisits());
        // Đang khám: 1 (v3)
        assertEquals(1, dto.getInProgressVisits());
        // Hủy: 1 (v5)
        assertEquals(1, dto.getCancelledVisits());

        // Tỷ lệ hoàn tất toàn viện: 2 / 5 = 40.0%
        assertEquals(40.0, dto.getCompletionRate());

        // Phân bố khoa: Khoa Nội 3, Khoa Ngoại 2
        assertEquals(3L, dto.getDepartmentBreakdown().get("Khoa Nội"));
        assertEquals(2L, dto.getDepartmentBreakdown().get("Khoa Ngoại"));

        // Phân bố trạng thái
        assertEquals(1L, dto.getStatusBreakdown().get("HOAN_THANH"));
        assertEquals(1L, dto.getStatusBreakdown().get("DA_KHAM"));
        assertEquals(1L, dto.getStatusBreakdown().get("DANG_KHAM"));
        assertEquals(1L, dto.getStatusBreakdown().get("CHO_KHAM"));
        assertEquals(1L, dto.getStatusBreakdown().get("DA_HUY"));

        // Items có ít nhất toàn viện + 2 khoa
        assertTrue(dto.getItems().size() >= 3);
    }

    @Test
    @DisplayName("Báo lỗi INVALID_DATE_RANGE khi từ ngày lớn hơn đến ngày")
    void shouldFailWhenFromDateIsAfterToDate() {
        LocalDate today = LocalDate.now();
        GetLuotKhamThongKeQuery query = new GetLuotKhamThongKeQuery("ADMIN", today, today.minusDays(2));

        KetQua<LuotKhamThongKeDuLieu> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertEquals("INVALID_DATE_RANGE", result.getError().orElseThrow().code());
    }

    @Test
    @DisplayName("Báo lỗi QUERY_INVALID khi query là null")
    void shouldFailWhenQueryIsNull() {
        KetQua<LuotKhamThongKeDuLieu> result = handler.handle(null);

        assertTrue(result.isFailure());
        assertEquals("QUERY_INVALID", result.getError().orElseThrow().code());
    }

    @Test
    @DisplayName("Xử lý an toàn khi danh sách lượt khám trống")
    void shouldHandleEmptyVisitsGracefully() {
        GetLuotKhamThongKeQuery query = new GetLuotKhamThongKeQuery("ADMIN", LocalDate.now().minusDays(1), LocalDate.now());
        KetQua<LuotKhamThongKeDuLieu> result = handler.handle(query);

        assertTrue(result.isSuccess());
        LuotKhamThongKeDuLieu dto = result.valueOrThrow();
        assertEquals(0, dto.getTotalVisits());
        assertEquals(0, dto.getCompletedVisits());
        assertEquals(0.0, dto.getCompletionRate());
        assertNotNull(dto.getItems());
    }

    @Test
    @DisplayName("Ghi nhật ký kiểm toán CATALOG_VIEW khi truy vấn thành công")
    void shouldRecordAuditLogOnSuccess() {
        UUID sessionId = UUID.randomUUID();
        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(20, 2, "DOCTOR", 1, 1, sessionId);
        KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(20, 2, "DOCTOR", 1, 1, "WS-DOC-01", UUID.randomUUID());

        GetLuotKhamThongKeQuery query = new GetLuotKhamThongKeQuery(auth, actor, LocalDate.now().minusDays(1), LocalDate.now());
        KetQua<LuotKhamThongKeDuLieu> result = handler.handle(query);

        assertTrue(result.isSuccess());
        assertFalse(auditPort.systemAudits.isEmpty());
        assertEquals(KiemToanHanhDongMa.CATALOG_VIEW, auditPort.systemAudits.get(0).actionCode());
        assertEquals(KiemToanKetQua.SUCCESS, auditPort.systemAudits.get(0).outcome());
    }

    @Test
    @DisplayName("Kiểm tra định dạng bảng 7 cột toTableRow trên VisitStatisticsItemDto")
    void shouldFormatTableRowAccurately() {
        LuotKhamThongKeMatHangDuLieu item = new LuotKhamThongKeMatHangDuLieu(
                1, 1, "Khoa Cấp Cứu", 100, 95, 3, 2, 0, 95.0, "Đạt chỉ tiêu"
        );

        Object[] row = item.toTableRow(1);
        assertEquals(7, row.length);
        assertEquals("1", row[0]);
        assertEquals("Khoa Cấp Cứu", row[1]);
        assertEquals("Lượt", row[2]);
        assertEquals("100", row[4]);
        assertTrue(row[6].toString().contains("Hoàn tất: 95"));
    }
}
