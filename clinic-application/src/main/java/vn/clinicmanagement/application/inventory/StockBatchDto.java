package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockBatchDto(
        int tonKhoId,
        String soLo,
        LocalDate hanSuDung,
        BigDecimal soLuongTon,
        BigDecimal giaNhap,
        BigDecimal giaBan,
        boolean expired) {
}
