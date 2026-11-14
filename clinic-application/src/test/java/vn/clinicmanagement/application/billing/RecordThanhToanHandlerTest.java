package vn.clinicmanagement.application.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.billing.CongNoBenhNhan;
import vn.clinicmanagement.domain.billing.HoaDon;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;
import vn.clinicmanagement.domain.common.enums.TrangThaiCongNo;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordThanhToanHandlerTest {

    private CreateHoaDonHandlerTest.InMemoryInvoiceRepo invoiceRepo;
    private CreateHoaDonHandlerTest.RecordingAuditPort auditPort;
    private RecordThanhToanHandler handler;

    @BeforeEach
    void setUp() {
        invoiceRepo = new CreateHoaDonHandlerTest.InMemoryInvoiceRepo();
        auditPort = new CreateHoaDonHandlerTest.RecordingAuditPort();
        handler = new RecordThanhToanHandler(invoiceRepo, auditPort);
    }

    @Test
    @DisplayName("Cashier records partial payment, creates debt record, and transitions to THANH_TOAN_MOT_PHAN")
    void recordPartialPaymentCreatesDebt() {
        HoaDon invoice = new HoaDon();
        invoice.setId(10);
        invoice.setSoHoaDon("HD261107-1010");
        invoice.setMaLuotKham(201);
        invoice.setMaBenhNhan(301);
        invoice.setMaCoSo(1);
        invoice.setTongTien(new BigDecimal("500000"));
        invoice.setSoTienBenhNhanChiTra(new BigDecimal("500000"));
        invoice.setDaThanhToan(BigDecimal.ZERO);
        invoice.setTrangThai(TrangThaiHoaDon.CHO_THANH_TOAN);
        invoiceRepo.save(invoice);

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(
                40, 5, "CASHIER", 1, 1, UUID.randomUUID());
        KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(
                40, 5, "CASHIER", 1, 1, "WS-CASHIER-01", UUID.randomUUID());

        RecordThanhToanCommand command = new RecordThanhToanCommand(
                10,
                new BigDecimal("200000"),
                PhuongThucThanhToan.TIEN_MAT,
                LocalDate.now().plusDays(15),
                "Bệnh nhân trả trước một phần",
                auth,
                actor
        );

        KetQua<RecordThanhToanBienNhan> result = handler.handle(command);
        assertTrue(result.isSuccess());

        RecordThanhToanBienNhan receipt = result.getValue().orElseThrow();
        assertEquals(new BigDecimal("200000"), receipt.amountPaidThisTime());
        assertEquals(new BigDecimal("200000"), receipt.totalPaidSoFar());
        assertEquals(new BigDecimal("300000"), receipt.remainingDebt());
        assertEquals(TrangThaiHoaDon.THANH_TOAN_MOT_PHAN, receipt.invoiceStatus());
        assertEquals(TrangThaiCongNo.DANG_NO, receipt.debtStatus());

        // Verify debt persisted
        Optional<CongNoBenhNhan> debtOpt = invoiceRepo.getCongNoByHoaDon(10);
        assertTrue(debtOpt.isPresent());
        CongNoBenhNhan debt = debtOpt.get();
        assertEquals(new BigDecimal("500000"), debt.getSoTienNo());
        assertEquals(new BigDecimal("200000"), debt.getSoTienDaTra());
        assertEquals(new BigDecimal("300000"), debt.getSoTienConLai());
        assertEquals(TrangThaiCongNo.DANG_NO, debt.getTrangThai());

        // Verify receipt
        assertEquals(1, invoiceRepo.receipts.size());
        assertEquals(new BigDecimal("200000"), invoiceRepo.receipts.get(0).getSoTien());

        // Verify audit
        assertFalse(auditPort.systemEntries.isEmpty());
        assertEquals(KiemToanHanhDongMa.BILL_PAYMENT_COLLECTED, auditPort.systemEntries.get(0).actionCode());
    }

    @Test
    @DisplayName("Cashier records second payment completing total, transitions to DA_THANH_TOAN and clears debt")
    void recordFullPaymentClearsDebt() {
        HoaDon invoice = new HoaDon();
        invoice.setId(11);
        invoice.setSoHoaDon("HD261107-1011");
        invoice.setMaLuotKham(202);
        invoice.setMaBenhNhan(302);
        invoice.setMaCoSo(1);
        invoice.setTongTien(new BigDecimal("300000"));
        invoice.setSoTienBenhNhanChiTra(new BigDecimal("300000"));
        invoice.setDaThanhToan(new BigDecimal("100000"));
        invoice.setTrangThai(TrangThaiHoaDon.THANH_TOAN_MOT_PHAN);
        invoiceRepo.save(invoice);

        CongNoBenhNhan existingDebt = new CongNoBenhNhan();
        existingDebt.setMaHoaDon(11);
        existingDebt.setMaBenhNhan(302);
        existingDebt.setSoTienNo(new BigDecimal("300000"));
        existingDebt.setSoTienDaTra(new BigDecimal("100000"));
        existingDebt.setHanThanhToan(LocalDate.now().plusDays(20));
        existingDebt.setTrangThai(TrangThaiCongNo.DANG_NO);
        invoiceRepo.saveCongNo(existingDebt);

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(
                40, 5, "CASHIER", 1, 1, UUID.randomUUID());
        KiemToanTacNhanNguCanh actor = new KiemToanTacNhanNguCanh(
                40, 5, "CASHIER", 1, 1, "WS-CASHIER-01", UUID.randomUUID());

        RecordThanhToanCommand command = new RecordThanhToanCommand(
                11,
                new BigDecimal("200000"),
                PhuongThucThanhToan.CHUYEN_KHOAN,
                null,
                "Tất toán phần còn lại bằng chuyển khoản",
                auth,
                actor
        );

        KetQua<RecordThanhToanBienNhan> result = handler.handle(command);
        assertTrue(result.isSuccess());

        RecordThanhToanBienNhan receipt = result.getValue().orElseThrow();
        assertEquals(new BigDecimal("200000"), receipt.amountPaidThisTime());
        assertEquals(new BigDecimal("300000"), receipt.totalPaidSoFar());
        assertEquals(BigDecimal.ZERO, receipt.remainingDebt());
        assertEquals(TrangThaiHoaDon.DA_THANH_TOAN, receipt.invoiceStatus());
        assertEquals(TrangThaiCongNo.DA_TAT_TOAN, receipt.debtStatus());

        Optional<CongNoBenhNhan> debtOpt = invoiceRepo.getCongNoByHoaDon(11);
        assertTrue(debtOpt.isPresent());
        assertEquals(TrangThaiCongNo.DA_TAT_TOAN, debtOpt.get().getTrangThai());
    }

    @Test
    @DisplayName("Overpayment exceeding remaining debt returns OVERPAYMENT error")
    void overpaymentReturnsError() {
        HoaDon invoice = new HoaDon();
        invoice.setId(12);
        invoice.setTongTien(new BigDecimal("100000"));
        invoice.setSoTienBenhNhanChiTra(new BigDecimal("100000"));
        invoice.setDaThanhToan(new BigDecimal("80000"));
        invoice.setTrangThai(TrangThaiHoaDon.THANH_TOAN_MOT_PHAN);
        invoiceRepo.save(invoice);

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(40, 5, "CASHIER", 1, 1, UUID.randomUUID());

        RecordThanhToanCommand command = new RecordThanhToanCommand(
                12, new BigDecimal("50000"), PhuongThucThanhToan.TIEN_MAT, null, "", auth, null);

        KetQua<RecordThanhToanBienNhan> result = handler.handle(command);
        assertTrue(result.isFailure());
        assertEquals("OVERPAYMENT", result.getErrorCode().orElse(""));
    }

    @Test
    @DisplayName("Payment on already paid invoice returns ALREADY_PAID")
    void alreadyPaidInvoiceReturnsError() {
        HoaDon invoice = new HoaDon();
        invoice.setId(13);
        invoice.setTongTien(new BigDecimal("100000"));
        invoice.setSoTienBenhNhanChiTra(new BigDecimal("100000"));
        invoice.setDaThanhToan(new BigDecimal("100000"));
        invoice.setTrangThai(TrangThaiHoaDon.DA_THANH_TOAN);
        invoiceRepo.save(invoice);

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(40, 5, "CASHIER", 1, 1, UUID.randomUUID());

        RecordThanhToanCommand command = new RecordThanhToanCommand(
                13, new BigDecimal("10000"), PhuongThucThanhToan.TIEN_MAT, null, "", auth, null);

        KetQua<RecordThanhToanBienNhan> result = handler.handle(command);
        assertTrue(result.isFailure());
        assertEquals("ALREADY_PAID", result.getErrorCode().orElse(""));
    }

    @Test
    @DisplayName("Payment on cancelled invoice returns INVOICE_CANCELLED")
    void cancelledInvoiceReturnsError() {
        HoaDon invoice = new HoaDon();
        invoice.setId(14);
        invoice.setTrangThai(TrangThaiHoaDon.DA_HUY);
        invoiceRepo.save(invoice);

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(40, 5, "CASHIER", 1, 1, UUID.randomUUID());

        RecordThanhToanCommand command = new RecordThanhToanCommand(
                14, new BigDecimal("10000"), PhuongThucThanhToan.TIEN_MAT, null, "", auth, null);

        KetQua<RecordThanhToanBienNhan> result = handler.handle(command);
        assertTrue(result.isFailure());
        assertEquals("INVOICE_CANCELLED", result.getErrorCode().orElse(""));
    }

    @Test
    @DisplayName("Unauthorized role fails with AUTHORIZATION_DENIED")
    void unauthorizedRoleFails() {
        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(20, 2, "DOCTOR", 1, 1, UUID.randomUUID());

        RecordThanhToanCommand command = new RecordThanhToanCommand(
                10, new BigDecimal("10000"), PhuongThucThanhToan.TIEN_MAT, null, "", auth, null);

        KetQua<RecordThanhToanBienNhan> result = handler.handle(command);
        assertTrue(result.isFailure());
        assertEquals("AUTHORIZATION_DENIED", result.getErrorCode().orElse(""));
    }
}
