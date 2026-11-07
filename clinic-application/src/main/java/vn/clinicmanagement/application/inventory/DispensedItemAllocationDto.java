package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;

public record DispensedItemAllocationDto(
        int itemId,
        int tonKhoId,
        String soLo,
        BigDecimal quantityDispensed) {
}
