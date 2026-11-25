package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;

public record KiemKeDieuChinhDongDuLieu(
        int tonKhoId,
        BigDecimal qtyBefore,
        BigDecimal qtyAfter,
        BigDecimal chenhLech) {
}
