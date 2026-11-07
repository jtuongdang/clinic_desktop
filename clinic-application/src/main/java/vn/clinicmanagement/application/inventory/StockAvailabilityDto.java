package vn.clinicmanagement.application.inventory;

import java.math.BigDecimal;
import java.util.List;

public record StockAvailabilityDto(
        int warehouseId,
        int itemId,
        String itemCode,
        String itemName,
        BigDecimal tongTonKhaDung,
        List<StockBatchDto> batches) {
}
