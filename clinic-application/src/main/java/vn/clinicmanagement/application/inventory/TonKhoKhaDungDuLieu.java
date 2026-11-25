package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;
import java.util.List;

public record TonKhoKhaDungDuLieu(
        int warehouseId,
        int itemId,
        String itemCode,
        String itemName,
        BigDecimal tongTonKhaDung,
        List<TonKhoLoDuLieu> batches) {
}
