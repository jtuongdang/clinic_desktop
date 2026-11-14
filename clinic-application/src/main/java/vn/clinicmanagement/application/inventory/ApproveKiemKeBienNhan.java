package vn.clinicmanagement.application.inventory;

import java.util.List;

public record ApproveKiemKeBienNhan(
        /** Trường `stocktakeId`: kiemke mã. */
        int stocktakeId,
        int totalBatchesAdjusted,
        List<KiemKeDieuChinhDongDuLieu> adjustments) {

    public ApproveKiemKeBienNhan {
        adjustments = adjustments == null ? List.of() : List.copyOf(adjustments);
    }
}
