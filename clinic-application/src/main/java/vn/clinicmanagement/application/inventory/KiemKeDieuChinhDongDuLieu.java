package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;

public record KiemKeDieuChinhDongDuLieu(
        /** Trường `tonKhoId`: `ton` kho mã. */
        int tonKhoId,
        /** Trường `qtyBefore`: `qty` trước. */
        BigDecimal qtyBefore,
        /** Trường `qtyAfter`: `qty` sau. */
        BigDecimal qtyAfter,
        BigDecimal chenhLech) {
}
