package vn.clinicmanagement.application.cls;

import java.time.LocalDateTime;

public record CancelDichVuYeuCauBienNhan(
        /** Trường `orderId`: yêu cầu mã. */
        int orderId,
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `status`: trạng thái. */
        String status,
        /** Trường `cancelledBy`: dahuy theo. */
        int cancelledBy,
        /** Trường `reason`: lý do. */
        String reason,
        /** Trường `cancelledAt`: dahuy luc. */
        LocalDateTime cancelledAt
) {}
