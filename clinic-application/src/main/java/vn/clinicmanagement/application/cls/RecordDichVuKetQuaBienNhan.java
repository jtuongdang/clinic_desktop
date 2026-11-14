package vn.clinicmanagement.application.cls;

import java.time.LocalDateTime;

public record RecordDichVuKetQuaBienNhan(
        /** Trường `orderId`: yêu cầu mã. */
        int orderId,
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `status`: trạng thái. */
        String status,
        /** Trường `performedBy`: `performed` theo. */
        int performedBy,
        /** Trường `resultItemCount`: kết quả mathang dem. */
        int resultItemCount,
        /** Trường `performedAt`: `performed` luc. */
        LocalDateTime performedAt,
        /** Trường `returnedAt`: `returned` luc. */
        LocalDateTime returnedAt
) {}
