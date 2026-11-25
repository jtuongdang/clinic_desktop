package vn.clinicmanagement.application.billing;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.billing.payer.ChangeChiTietHoaDonDoiTuongCommand;
import vn.clinicmanagement.application.billing.payer.ChangeChiTietHoaDonDoiTuongHandler;
import vn.clinicmanagement.application.billing.refund.RecordHoanTraCommand;
import vn.clinicmanagement.application.billing.refund.RecordHoanTraHandler;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;
import vn.clinicmanagement.domain.common.enums.TrangThaiHoaDon;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static vn.clinicmanagement.application.billing.ChangeChiTietHoaDonDoiTuongHandlerTest.*;

class RecordHoanTraHandlerTest {

    @Test
    void disbursesCashRefundWithoutMutatingOriginalReceiptsAndRebalancesWhenSwitchedBackToThuPhi() {
        var repo = new TestInvoiceRepo(visit5015PaidInvoice());
        var payerHandler = new ChangeChiTietHoaDonDoiTuongHandler(repo, catalogWithValidCard(80), new NoopAudit());
        var refundHandler = new RecordHoanTraHandler(repo, new NoopAudit());

        // Bước 1: Chuyển Khám Nội tổng quát từ Thu phí (100k) -> BHYT (39k, hưởng 80% = 31.2k, BN trả 7.8k)
        var step1 = payerHandler.handle(new ChangeChiTietHoaDonDoiTuongCommand(
                2001, 3001, "BHYT", auth("DOCTOR", 1), actor("DOCTOR", 1)));
        assertTrue(step1.isSuccess());
        assertEquals(0, bd("92200.00").compareTo(step1.valueOrThrow().pendingRefundAmount()));

        // Bước 2: Thu ngân chi hoàn tiền mặt 92,200đ -> sinh PhieuHoanTra, PhieuThu gốc vẫn giữ 2 phiếu (185k + 110k)
        UUID reqId = UUID.randomUUID();
        var refundCmd = new RecordHoanTraCommand(
                2001, bd("92200.00"), PhuongThucThanhToan.TIEN_MAT,
                "Hoàn tiền chuyển đối tượng BHYT", auth("CASHIER", 1), actor("CASHIER", 1), reqId);
        var step2 = refundHandler.handle(refundCmd);
        assertTrue(step2.isSuccess());
        var refundReceipt = step2.valueOrThrow();
        assertEquals(1, refundReceipt.refundId());
        assertEquals(0, bd("92200.00").compareTo(refundReceipt.refundedAmount()));
        assertEquals(0, bd("202800.00").compareTo(refundReceipt.netPaidAmount()));
        assertEquals(0, bd("92200.00").compareTo(refundReceipt.totalRefundedAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(refundReceipt.pendingRefundAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(refundReceipt.remainingAmount()));
        assertEquals(2, repo.getPhieuThuByHoaDon(2001).size());
        assertEquals(1, repo.getPhieuHoanTraByHoaDon(2001).size());

        // Gửi trùng requestId (idempotent): không tạo phiếu hoàn thứ 2
        var duplicate = refundHandler.handle(refundCmd);
        assertTrue(duplicate.isSuccess());
        assertEquals(1, repo.getPhieuHoanTraByHoaDon(2001).size());

        // Bước 3: Sau khi đã chi hoàn 92,200đ tiền mặt, nếu chuyển ngược BHYT -> Thu phí:
        // Bệnh nhân phải trả tăng lại 295,000 > Đã thu ròng (202,800) => Còn nợ = 92,200 (THANH_TOAN_MOT_PHAN)
        var step3 = payerHandler.handle(new ChangeChiTietHoaDonDoiTuongCommand(
                2001, 3001, "THU_PHI", auth("CASHIER", 1), actor("CASHIER", 1)));
        assertTrue(step3.isSuccess());
        var afterRevert = step3.valueOrThrow();
        assertEquals(0, bd("295000.00").compareTo(afterRevert.patientPaidAmount()));
        assertEquals(0, bd("202800.00").compareTo(afterRevert.amountPaid()));
        assertEquals(0, bd("92200.00").compareTo(afterRevert.remainingAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(afterRevert.pendingRefundAmount()));
        assertEquals(TrangThaiHoaDon.THANH_TOAN_MOT_PHAN, afterRevert.invoiceStatus());
    }

    @Test
    void rejectsUnauthorizedRoleOrExcessRefundOrNoPendingRefund() {
        var repo = new TestInvoiceRepo(visit5015PaidInvoice());
        var payerHandler = new ChangeChiTietHoaDonDoiTuongHandler(repo, catalogWithValidCard(80), new NoopAudit());
        var refundHandler = new RecordHoanTraHandler(repo, new NoopAudit());

        // Khi chưa phát sinh tiền thừa (Cần hoàn = 0), không cho chi hoàn
        var noRefund = refundHandler.handle(new RecordHoanTraCommand(
                2001, bd("10000.00"), PhuongThucThanhToan.TIEN_MAT, "Hoàn thử",
                auth("CASHIER", 1), actor("CASHIER", 1)));
        assertTrue(noRefund.isFailure());
        assertEquals("NO_REFUND_DUE", noRefund.getErrorCode().orElseThrow());

        // Tạo khoản cần hoàn 92,200
        payerHandler.handle(new ChangeChiTietHoaDonDoiTuongCommand(
                2001, 3001, "BHYT", auth("DOCTOR", 1), actor("DOCTOR", 1)));

        // Bác sĩ không có quyền chi tiền hoàn mặt tại quầy
        var doctorDenied = refundHandler.handle(new RecordHoanTraCommand(
                2001, bd("92200.00"), PhuongThucThanhToan.TIEN_MAT, "Bác sĩ hoàn",
                auth("DOCTOR", 1), actor("DOCTOR", 1)));
        assertTrue(doctorDenied.isFailure());
        assertEquals("AUTHORIZATION_DENIED", doctorDenied.getErrorCode().orElseThrow());

        // Hoàn vượt quá số tiền cần hoàn (100,000 > 92,200)
        var excess = refundHandler.handle(new RecordHoanTraCommand(
                2001, bd("100000.00"), PhuongThucThanhToan.TIEN_MAT, "Hoàn lố",
                auth("CASHIER", 1), actor("CASHIER", 1)));
        assertTrue(excess.isFailure());
        assertEquals("EXCESS_REFUND", excess.getErrorCode().orElseThrow());
    }
}
