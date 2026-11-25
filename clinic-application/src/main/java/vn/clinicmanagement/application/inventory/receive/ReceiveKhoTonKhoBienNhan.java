package vn.clinicmanagement.application.inventory.receive;

import java.math.BigDecimal;

public record ReceiveKhoTonKhoBienNhan(
        int receiptId,
        String soPhieuNhap,
        BigDecimal tongTien,
        int totalBatchesReceived) {
}
