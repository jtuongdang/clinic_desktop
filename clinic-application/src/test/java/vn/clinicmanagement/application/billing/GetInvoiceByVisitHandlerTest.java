package vn.clinicmanagement.application.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.billing.ChiTietHoaDon;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.billing.PhieuThu;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetInvoiceByVisitHandlerTest {

    private CreateInvoiceHandlerTest.InMemoryInvoiceRepo invoiceRepo;
    private CreateInvoiceHandlerTest.RecordingAuditPort auditPort;
    private GetInvoiceByVisitHandler handler;

    @BeforeEach
    void setUp() {
        invoiceRepo = new CreateInvoiceHandlerTest.InMemoryInvoiceRepo();
        auditPort = new CreateInvoiceHandlerTest.RecordingAuditPort();
        handler = new GetInvoiceByVisitHandler(invoiceRepo, auditPort);
    }

    @Test
    @DisplayName("Returns invoice with lines and receipts for cashier")
    void returnsInvoiceWithLinesAndReceipts() {
        HoaDon invoice = new HoaDon();
        invoice.setId(10);
        invoice.setSoHoaDon("HD261107-1001");
        invoice.setMaLuotKham(201);
        invoice.setMaBenhNhan(301);
        invoice.setMaCoSo(1);
        invoice.setTongTien(new BigDecimal("200000"));
        invoice.setSoTienBenhNhanChiTra(new BigDecimal("200000"));
        invoice.setDaThanhToan(new BigDecimal("100000"));
        invoice.setTrangThai(TrangThaiHoaDon.THANH_TOAN_MOT_PHAN);

        ChiTietHoaDon item = new ChiTietHoaDon();
        item.setId(1);
        item.setLoaiDoiTuong("KHAM_BENH");
        item.setMoTa("Khám lâm sàng tổng quát");
        item.setSoLuong(BigDecimal.ONE);
        item.setDonGia(new BigDecimal("200000"));
        invoice.addChiTiet(item);
        invoiceRepo.save(invoice);

        PhieuThu pt = new PhieuThu();
        pt.setId(5);
        pt.setMaHoaDon(10);
        pt.setSoTien(new BigDecimal("100000"));
        pt.setPhuongThuc(PhuongThucThanhToan.TIEN_MAT);
        pt.setNguoiThu(40);
        pt.setNgayThu(LocalDateTime.now());
        invoiceRepo.savePhieuThu(pt);

        AuthorizationContext auth = new AuthorizationContext(
                40, 5, "CASHIER", 1, 1, UUID.randomUUID());
        GetInvoiceByVisitQuery query = new GetInvoiceByVisitQuery(201, auth, null);

        Result<InvoiceDto> result = handler.handle(query);
        assertTrue(result.isSuccess());

        InvoiceDto dto = result.getValue().orElseThrow();
        assertEquals("HD261107-1001", dto.invoiceNumber());
        assertEquals(new BigDecimal("200000"), dto.totalAmount());
        assertEquals(new BigDecimal("100000"), dto.amountPaid());
        assertEquals(new BigDecimal("100000"), dto.remainingAmount());
        assertEquals(1, dto.lines().size());
        assertEquals(1, dto.receipts().size());
        assertEquals(new BigDecimal("100000"), dto.receipts().get(0).amount());
    }

    @Test
    @DisplayName("Invoice not found returns INVOICE_NOT_FOUND")
    void invoiceNotFoundReturnsError() {
        AuthorizationContext auth = new AuthorizationContext(
                40, 5, "CASHIER", 1, 1, UUID.randomUUID());
        GetInvoiceByVisitQuery query = new GetInvoiceByVisitQuery(999, auth, null);

        Result<InvoiceDto> result = handler.handle(query);
        assertTrue(result.isFailure());
        assertEquals("INVOICE_NOT_FOUND", result.getErrorCode().orElse(""));
    }

    @Test
    @DisplayName("Unauthorized role is denied")
    void unauthorizedRoleIsDenied() {
        AuthorizationContext auth = new AuthorizationContext(
                50, 6, "PHARMACIST", 1, 1, UUID.randomUUID());
        GetInvoiceByVisitQuery query = new GetInvoiceByVisitQuery(201, auth, null);

        Result<InvoiceDto> result = handler.handle(query);
        assertTrue(result.isFailure());
        assertEquals("AUTHORIZATION_DENIED", result.getErrorCode().orElse(""));
    }
}
