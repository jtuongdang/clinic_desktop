package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockReceiptItemInput(
        int itemId,
        String soLo,
        LocalDate hanSuDung,
        BigDecimal soLuong,
        BigDecimal giaNhap,
        BigDecimal giaBan) {
}
