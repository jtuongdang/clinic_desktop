package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;

public record ReceiveWarehouseStockReceipt(
        int receiptId,
        String soPhieuNhap,
        BigDecimal tongTien,
        int totalBatchesReceived) {
}
