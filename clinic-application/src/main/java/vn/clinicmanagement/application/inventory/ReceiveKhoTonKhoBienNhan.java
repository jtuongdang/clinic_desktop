package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;

public record ReceiveKhoTonKhoBienNhan(
        /** Trường `receiptId`: biên nhận mã. */
        int receiptId,
        String soPhieuNhap,
        BigDecimal tongTien,
        int totalBatchesReceived) {
}
