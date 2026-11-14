package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;
import java.util.List;

public record TonKhoKhaDungDuLieu(
        /** Trường `warehouseId`: kho mã. */
        int warehouseId,
        /** Trường `itemId`: mathang mã. */
        int itemId,
        /** Trường `itemCode`: mathang mã. */
        String itemCode,
        /** Trường `itemName`: mathang tên. */
        String itemName,
        BigDecimal tongTonKhaDung,
        List<TonKhoLoDuLieu> batches) {
}
