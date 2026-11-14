package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;

public record DaCapPhatMatHangPhanBoDuLieu(
        /** Trường `itemId`: mathang mã. */
        int itemId,
        /** Trường `tonKhoId`: `ton` kho mã. */
        int tonKhoId,
        String soLo,
        /** Trường `quantityDispensed`: `quantity` dacapphat. */
        BigDecimal quantityDispensed) {
}
