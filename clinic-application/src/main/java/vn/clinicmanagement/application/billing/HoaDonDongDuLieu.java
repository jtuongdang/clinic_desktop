package vn.clinicmanagement.application.billing;

import java.math.BigDecimal;

public record HoaDonDongDuLieu(
        int id,
        String itemType,
        int sourceId,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String payerType,
        BigDecimal insurancePrice,
        int coverageRate,
        BigDecimal insuranceAmount,
        BigDecimal discountAmount,
        BigDecimal patientAmount
) {
    public HoaDonDongDuLieu(
            int id,
            String itemType,
            int sourceId,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
        this(id, itemType, sourceId, description, quantity, unitPrice, lineTotal,
                "", BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                lineTotal != null ? lineTotal : BigDecimal.ZERO);
    }
}
