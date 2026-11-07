package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;

public record StocktakeAdjustmentLineDto(
        int tonKhoId,
        BigDecimal qtyBefore,
        BigDecimal qtyAfter,
        BigDecimal chenhLech) {
}
