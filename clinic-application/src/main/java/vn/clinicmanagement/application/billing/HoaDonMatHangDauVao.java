package vn.clinicmanagement.application.billing;

import java.math.BigDecimal;

public record HoaDonMatHangDauVao(
        String itemType,
        int sourceId,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice
) {}
