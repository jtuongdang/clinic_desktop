package vn.clinicmanagement.application.inventory;

import java.util.List;

public record ApproveStocktakeReceipt(
        int stocktakeId,
        int totalBatchesAdjusted,
        List<StocktakeAdjustmentLineDto> adjustments) {

    public ApproveStocktakeReceipt {
        adjustments = adjustments == null ? List.of() : List.copyOf(adjustments);
    }
}
