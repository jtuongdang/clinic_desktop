package vn.clinicmanagement.application.billing;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.billing.record.*;
import vn.clinicmanagement.domain.billing.*;
import vn.clinicmanagement.domain.common.enums.*;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ClsVienPhiDongBoTest {
    private final CreateHoaDonHandlerTest.InMemoryInvoiceRepo repo = new CreateHoaDonHandlerTest.InMemoryInvoiceRepo();
    private final ClsVienPhiDongBo billing = new ClsVienPhiDongBo(repo);
    private final RecordThanhToanHandler payments = new RecordThanhToanHandler(repo, new CreateHoaDonHandlerTest.RecordingAuditPort());
    private final PhanQuyenNguCanh cashier = new PhanQuyenNguCanh(40, 5, "CASHIER", 1, 1, UUID.randomUUID());

    @Test void consultationThenTwoClsChargesAndSecondPaymentKeepTwoReceipts() {
        HoaDon invoice = invoice();
        assertTrue(payments.handle(payment(invoice, "120000")).isSuccess());
        int firstReceiptId = repo.receipts.get(0).getId();
        assertTrue(billing.addCharge(order(101, "85000"), "Xét nghiệm", 10, 1).isSuccess());
        assertTrue(billing.addCharge(order(102, "150000"), "Siêu âm", 10, 1).isSuccess());
        assertBalance(invoice, "355000", "120000", TrangThaiHoaDon.THANH_TOAN_MOT_PHAN);
        assertTrue(payments.handle(payment(invoice, "235000")).isSuccess());
        assertBalance(invoice, "355000", "355000", TrangThaiHoaDon.DA_THANH_TOAN);
        assertEquals(2, repo.receipts.size());
        assertEquals(firstReceiptId, repo.receipts.get(0).getId());
        assertMoney("120000", repo.receipts.get(0).getSoTien());
    }

    @Test void cancelBeforePaymentReducesDebtThenReturnsToPaid() {
        HoaDon invoice = invoice();
        payments.handle(payment(invoice, "120000")).valueOrThrow();
        billing.addCharge(order(101, "85000"), "Xét nghiệm", 10, 1).valueOrThrow();
        billing.addCharge(order(102, "150000"), "Siêu âm", 10, 1).valueOrThrow();
        billing.cancelCharge(order(102, "150000"), 10, 1).valueOrThrow();
        assertBalance(invoice, "205000", "120000", TrangThaiHoaDon.THANH_TOAN_MOT_PHAN);
        billing.cancelCharge(order(101, "85000"), 10, 1).valueOrThrow();
        assertBalance(invoice, "120000", "120000", TrangThaiHoaDon.DA_THANH_TOAN);
        assertEquals(2, invoice.getChiTietHoaDons().stream().filter(ChiTietHoaDon::isDeleted).count());
        assertEquals(1, repo.receipts.size());
    }

    @Test void refundRequiredDoesNotChangeLineOrAmounts() {
        HoaDon invoice = invoice();
        billing.addCharge(order(101, "85000"), "Xét nghiệm", 10, 1).valueOrThrow();
        payments.handle(payment(invoice, "205000")).valueOrThrow();
        var result = billing.cancelCharge(order(101, "85000"), 10, 1);
        assertEquals("REFUND_REQUIRED", result.getErrorCode().orElseThrow());
        assertBalance(invoice, "205000", "205000", TrangThaiHoaDon.DA_THANH_TOAN);
        assertTrue(invoice.getChiTietHoaDons().stream().noneMatch(ChiTietHoaDon::isDeleted));
    }

    @Test void repeatedChargeAndPaymentRequestsAreIdempotent() {
        HoaDon invoice = invoice();
        billing.addCharge(order(101, "85000"), "Xét nghiệm", 10, 1).valueOrThrow();
        billing.addCharge(order(101, "85000"), "Xét nghiệm", 10, 1).valueOrThrow();
        assertEquals(2, invoice.getChiTietHoaDons().size());
        var command = payment(invoice, "50000");
        var first = payments.handle(command).valueOrThrow();
        var retry = payments.handle(command).valueOrThrow();
        assertEquals(first.receiptId(), retry.receiptId());
        assertEquals(1, repo.receipts.size());
        assertBalance(invoice, "205000", "50000", TrangThaiHoaDon.THANH_TOAN_MOT_PHAN);
        var reused = new RecordThanhToanCommand(invoice.getId(), new BigDecimal("10000"),
                PhuongThucThanhToan.TIEN_MAT, null, "", cashier, null, command.requestId());
        assertEquals("PAYMENT_REQUEST_CONFLICT", payments.handle(reused).getErrorCode().orElseThrow());
    }

    @Test void cancelledMissingForeignAndFinalizedInvoicesRejectCharges() {
        var order = order(101, "85000");
        assertEquals("INVOICE_NOT_FOUND", billing.addCharge(order, "XN", 10, 1).getErrorCode().orElseThrow());
        HoaDon invoice = invoice();
        assertEquals("AUTH_ACCESS_DENIED", billing.addCharge(order, "XN", 10, 2).getErrorCode().orElseThrow());
        invoice.setTrangThai(TrangThaiHoaDon.DA_HUY);
        assertEquals("INVOICE_CANCELLED", billing.addCharge(order, "XN", 10, 1).getErrorCode().orElseThrow());
        invoice.setTrangThai(TrangThaiHoaDon.CHO_THANH_TOAN);
        invoice.setSoHoaDonDienTu("E-1");
        assertEquals("INVOICE_FINALIZED", billing.addCharge(order, "XN", 10, 1).getErrorCode().orElseThrow());
        assertEquals(1, invoice.getChiTietHoaDons().size());
    }

    @Test void insuranceAdvanceAndDiscountArePreservedAndReceiptMismatchIsBlocked() {
        HoaDon invoice = invoice();
        invoice.setSoTienBaoHiemChiTra(new BigDecimal("20000"));
        invoice.setSoTienTamUng(new BigDecimal("10000"));
        invoice.setSoTienGiamGia(new BigDecimal("5000"));
        billing.addCharge(order(101, "85000"), "XN", 10, 1).valueOrThrow();
        assertMoney("170000", invoice.getSoTienBenhNhanChiTra());
        invoice.setDaThanhToan(BigDecimal.ONE);
        assertEquals("BILLING_BALANCE_MISMATCH", billing.cancelCharge(order(101, "85000"), 10, 1).getErrorCode().orElseThrow());
        assertEquals("BILLING_BALANCE_MISMATCH", payments.handle(payment(invoice, "10000")).getErrorCode().orElseThrow());
    }

    private HoaDon invoice() {
        HoaDon invoice = new HoaDon();
        invoice.setMaLuotKham(1); invoice.setMaBenhNhan(2); invoice.setMaCoSo(1); invoice.setSoHoaDon("TEST");
        ChiTietHoaDon consultation = new ChiTietHoaDon();
        consultation.setLoaiDoiTuong("DICH_VU_KHAM"); consultation.setMaDoiTuong(11);
        consultation.setMoTa("Khám Nhi"); consultation.setSoLuong(BigDecimal.ONE); consultation.setDonGia(new BigDecimal("120000"));
        invoice.addChiTiet(consultation); invoice.recalculateCharges(); repo.save(invoice);
        return invoice;
    }
    private ChiDinhDichVu order(int id, String price) {
        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setId(id); order.setMaLuotKham(1); order.setSoLuong(BigDecimal.ONE); order.setDonGia(new BigDecimal(price));
        return order;
    }
    private RecordThanhToanCommand payment(HoaDon invoice, String amount) {
        return new RecordThanhToanCommand(invoice.getId(), new BigDecimal(amount), PhuongThucThanhToan.TIEN_MAT, null, "", cashier, null);
    }
    private void assertBalance(HoaDon invoice, String total, String paid, TrangThaiHoaDon status) {
        assertMoney(total, invoice.getTongTien()); assertMoney(total, invoice.getSoTienBenhNhanChiTra());
        assertMoney(paid, invoice.getDaThanhToan()); assertEquals(status, invoice.getTrangThai());
        var debt = repo.getCongNoByHoaDon(invoice.getId()).orElseThrow();
        assertMoney(total, debt.getSoTienNo()); assertMoney(paid, debt.getSoTienDaTra());
        assertEquals(status == TrangThaiHoaDon.DA_THANH_TOAN ? TrangThaiCongNo.DA_TAT_TOAN : TrangThaiCongNo.DANG_NO, debt.getTrangThai());
    }
    private static void assertMoney(String amount, BigDecimal actual) {
        assertEquals(0, new BigDecimal(amount).compareTo(actual));
    }
}
