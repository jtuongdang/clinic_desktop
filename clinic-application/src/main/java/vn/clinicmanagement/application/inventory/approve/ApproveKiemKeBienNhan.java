package vn.clinicmanagement.application.inventory.approve;

import vn.clinicmanagement.application.inventory.KiemKeDieuChinhDongDuLieu;

import java.util.List;

public record ApproveKiemKeBienNhan(
        int stocktakeId,
        int totalBatchesAdjusted,
        List<KiemKeDieuChinhDongDuLieu> adjustments) {

    public ApproveKiemKeBienNhan {
        adjustments = adjustments == null ? List.of() : List.copyOf(adjustments);
    }
}
