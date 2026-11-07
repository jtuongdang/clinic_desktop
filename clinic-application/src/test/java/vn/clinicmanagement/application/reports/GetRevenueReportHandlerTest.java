package vn.clinicmanagement.application.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.billing.ChiTietHoaDon;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuThu;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GetRevenueReportHandler Tests (RPT-01)")
class GetRevenueReportHandlerTest {

    private ReportTestSupport.InMemoryInvoiceRepository invoiceRepo;
    private ReportTestSupport.RecordingAuditWritePort auditPort;
    private GetRevenueReportHandler handler;

    @BeforeEach
    void setUp() {
        invoiceRepo = new ReportTestSupport.InMemoryInvoiceRepository();
        auditPort = new ReportTestSupport.RecordingAuditWritePort();
        handler = new GetRevenueReportHandler(invoiceRepo, auditPort);
    }

    @Test
    @DisplayName("ADMIN, DIRECTOR, ACCOUNTANT được phép truy vấn báo cáo doanh thu")
    void shouldAllowAuthorizedRolesToQueryRevenueReport() {
        String[] authorizedRoles = {"ADMIN", "DIRECTOR", "ACCOUNTANT", "admin", "director", "accountant"};

        for (String role : authorizedRoles) {
            GetRevenueReportQuery query = new GetRevenueReportQuery(role, LocalDate.now().minusDays(7), LocalDate.now());
            Result<RevenueReportDto> result = handler.handle(query);

            assertTrue(result.isSuccess(), "Vai trò " + role + " phải được phép truy vấn báo cáo doanh thu");
            assertNotNull(result.valueOrThrow());
        }
    }

    @Test
    @DisplayName("Từ chối DOCTOR, PHARMACIST, RECEPTIONIST, CASHIER và null truy vấn báo cáo doanh thu")
    void shouldDenyUnauthorizedRolesFromQueryingRevenueReport() {
        String[] unauthorizedRoles = {"DOCTOR", "PHARMACIST", "RECEPTIONIST", "CASHIER", "LAB_TECH", "GUEST", "", null};

        for (String role : unauthorizedRoles) {
            GetRevenueReportQuery query = new GetRevenueReportQuery(role, LocalDate.now().minusDays(7), LocalDate.now());
            Result<RevenueReportDto> result = handler.handle(query);

            assertTrue(result.isFailure(), "Vai trò " + role + " phải bị từ chối");
            assertEquals("AUTHZ_DENIED", result.getError().orElseThrow().code());
        }
    }

    @Test
    @DisplayName("Tính toán chính xác tổng doanh thu và phân nhóm khám bệnh, cận lâm sàng, thuốc")
    void shouldCalculateRevenueAggregatesByCategoriesAccurately() {
        LocalDate today = LocalDate.now();

        // Hóa đơn 1: Khám bệnh (150,000) + Cận lâm sàng (250,000)
        HoaDon hd1 = new HoaDon();
        hd1.setSoHoaDon("HD-001");
        hd1.setNgayLap(today.atTime(9, 30));
        hd1.setTongTien(new BigDecimal("400000"));
        hd1.setSoTienBaoHiemChiTra(new BigDecimal("80000"));
        hd1.setSoTienBenhNhanChiTra(new BigDecimal("320000"));
        hd1.setTrangThai(TrangThaiHoaDon.DA_THANH_TOAN);

        ChiTietHoaDon ct1_1 = new ChiTietHoaDon();
        ct1_1.setLoaiDoiTuong("KHAM_BENH");
        ct1_1.setMoTa("Khám tổng quát");
        ct1_1.setSoLuong(BigDecimal.ONE);
        ct1_1.setDonGia(new BigDecimal("150000"));
        hd1.addChiTiet(ct1_1);

        ChiTietHoaDon ct1_2 = new ChiTietHoaDon();
        ct1_2.setLoaiDoiTuong("CAN_LAM_SANG");
        ct1_2.setMoTa("Siêu âm ổ bụng");
        ct1_2.setSoLuong(BigDecimal.ONE);
        ct1_2.setDonGia(new BigDecimal("250000"));
        hd1.addChiTiet(ct1_2);

        // Thanh toán tiền mặt 320,000
        PhieuThu pt1 = new PhieuThu();
        pt1.setSoTien(new BigDecimal("320000"));
        pt1.setPhuongThuc(PhuongThucThanhToan.TIEN_MAT);
        hd1.getPhieuThus().add(pt1);
        invoiceRepo.save(hd1);

        // Hóa đơn 2: Thuốc (500,000) thanh toán chuyển khoản
        HoaDon hd2 = new HoaDon();
        hd2.setSoHoaDon("HD-002");
        hd2.setNgayLap(today.atTime(11, 0));
        hd2.setTongTien(new BigDecimal("500000"));
        hd2.setSoTienBaoHiemChiTra(BigDecimal.ZERO);
        hd2.setSoTienBenhNhanChiTra(new BigDecimal("500000"));
        hd2.setTrangThai(TrangThaiHoaDon.DA_THANH_TOAN);

        ChiTietHoaDon ct2_1 = new ChiTietHoaDon();
        ct2_1.setLoaiDoiTuong("THUOC");
        ct2_1.setMoTa("Kháng sinh Cefuroxime");
        ct2_1.setSoLuong(new BigDecimal("10"));
        ct2_1.setDonGia(new BigDecimal("50000"));
        hd2.addChiTiet(ct2_1);

        PhieuThu pt2 = new PhieuThu();
        pt2.setSoTien(new BigDecimal("500000"));
        pt2.setPhuongThuc(PhuongThucThanhToan.CHUYEN_KHOAN);
        hd2.getPhieuThus().add(pt2);
        invoiceRepo.save(hd2);

        GetRevenueReportQuery query = new GetRevenueReportQuery("ACCOUNTANT", today.minusDays(1), today);
        Result<RevenueReportDto> result = handler.handle(query);

        assertTrue(result.isSuccess());
        RevenueReportDto dto = result.valueOrThrow();

        // Tổng doanh thu: 400,000 + 500,000 = 900,000
        assertEquals(new BigDecimal("900000"), dto.getTotalRevenue());
        assertEquals(new BigDecimal("150000"), dto.getConsultationRevenue());
        assertEquals(new BigDecimal("250000"), dto.getParaclinicalRevenue());
        assertEquals(new BigDecimal("500000"), dto.getMedicationRevenue());
        assertEquals(BigDecimal.ZERO, dto.getOtherRevenue());

        // BHYT & Người bệnh cùng chi trả
        assertEquals(new BigDecimal("80000"), dto.getInsurancePaidAmount());
        assertEquals(new BigDecimal("820000"), dto.getPatientPaidAmount());

        // Tiền mặt & Chuyển khoản
        assertEquals(new BigDecimal("320000"), dto.getCashAmount());
        assertEquals(new BigDecimal("500000"), dto.getTransferAmount());

        // Số lượng hóa đơn
        assertEquals(2, dto.getTotalInvoices());
        assertEquals(2, dto.getPaidInvoices());

        // Tỷ trọng %
        assertTrue(dto.getMedicationPercentage() > dto.getParaclinicalPercentage());
        assertTrue(dto.getParaclinicalPercentage() > dto.getConsultationPercentage());
    }

    @Test
    @DisplayName("Loại trừ hóa đơn ngoài khoảng thời gian và hóa đơn đã hủy")
    void shouldExcludeOutOfRangeAndCancelledInvoices() {
        LocalDate today = LocalDate.now();

        // Hóa đơn hôm nay hợp lệ (100,000)
        HoaDon hd1 = new HoaDon();
        hd1.setNgayLap(today.atTime(10, 0));
        hd1.setTongTien(new BigDecimal("100000"));
        hd1.setTrangThai(TrangThaiHoaDon.DA_THANH_TOAN);
        invoiceRepo.save(hd1);

        // Hóa đơn 2 tháng trước (ngoài khoảng)
        HoaDon hdOld = new HoaDon();
        hdOld.setNgayLap(today.minusMonths(2).atTime(10, 0));
        hdOld.setTongTien(new BigDecimal("200000"));
        invoiceRepo.save(hdOld);

        // Hóa đơn hôm nay nhưng đã hủy (DA_HUY)
        HoaDon hdCancelled = new HoaDon();
        hdCancelled.setNgayLap(today.atTime(12, 0));
        hdCancelled.setTongTien(new BigDecimal("300000"));
        hdCancelled.setTrangThai(TrangThaiHoaDon.DA_HUY);
        invoiceRepo.save(hdCancelled);

        GetRevenueReportQuery query = new GetRevenueReportQuery("ADMIN", today.minusDays(1), today);
        Result<RevenueReportDto> result = handler.handle(query);

        assertTrue(result.isSuccess());
        RevenueReportDto dto = result.valueOrThrow();
        assertEquals(new BigDecimal("100000"), dto.getTotalRevenue());
        assertEquals(1, dto.getTotalInvoices());
    }

    @Test
    @DisplayName("Báo lỗi INVALID_DATE_RANGE khi từ ngày lớn hơn đến ngày")
    void shouldFailWhenFromDateIsAfterToDate() {
        LocalDate today = LocalDate.now();
        GetRevenueReportQuery query = new GetRevenueReportQuery("ADMIN", today, today.minusDays(1));

        Result<RevenueReportDto> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertEquals("INVALID_DATE_RANGE", result.getError().orElseThrow().code());
    }

    @Test
    @DisplayName("Báo lỗi QUERY_INVALID khi query là null")
    void shouldFailWhenQueryIsNull() {
        Result<RevenueReportDto> result = handler.handle(null);

        assertTrue(result.isFailure());
        assertEquals("QUERY_INVALID", result.getError().orElseThrow().code());
    }

    @Test
    @DisplayName("Hỗ trợ trích xuất vai trò từ AuthorizationContext và AuditActorContext")
    void shouldExtractRoleFromAuthorizationContext() {
        UUID sessionId = UUID.randomUUID();
        AuthorizationContext auth = new AuthorizationContext(10, 1, "DIRECTOR", 1, 1, sessionId);
        AuditActorContext actor = new AuditActorContext(10, 1, "DIRECTOR", 1, 1, "WS-01", UUID.randomUUID());

        GetRevenueReportQuery query = new GetRevenueReportQuery(auth, actor, LocalDate.now().minusDays(5), LocalDate.now());
        Result<RevenueReportDto> result = handler.handle(query);

        assertTrue(result.isSuccess());
        assertEquals("DIRECTOR", query.getEffectiveRole());

        // Kiểm tra đã ghi audit CATALOG_VIEW
        assertFalse(auditPort.systemAudits.isEmpty());
        assertEquals(AuditActionCode.CATALOG_VIEW, auditPort.systemAudits.get(0).actionCode());
        assertEquals(AuditOutcome.SUCCESS, auditPort.systemAudits.get(0).outcome());
    }

    @Test
    @DisplayName("Kiểm tra định dạng bảng 7 cột toTableRow trên RevenueReportItemDto")
    void shouldFormatTableRowAccurately() {
        RevenueReportItemDto item = new RevenueReportItemDto(
                1, "Khám chuyên khoa", "VNĐ", 50,
                new BigDecimal("15000000"), new BigDecimal("12000000"), 25.0,
                BigDecimal.ZERO, new BigDecimal("15000000"), 100.0, "Tăng 25%"
        );

        Object[] row = item.toTableRow(1);
        assertEquals(7, row.length);
        assertEquals("1", row[0]);
        assertEquals("Khám chuyên khoa", row[1]);
        assertEquals("VNĐ", row[2]);
        assertTrue(row[4].toString().contains("15"));
        assertTrue(row[5].toString().contains("+25.0%"));
    }
}
