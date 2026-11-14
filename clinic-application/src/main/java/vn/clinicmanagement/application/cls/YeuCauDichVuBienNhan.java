package vn.clinicmanagement.application.cls;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record YeuCauDichVuBienNhan(
        /** Trường `orderId`: yêu cầu mã. */
        int orderId,
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `serviceId`: dịch vụ mã. */
        int serviceId,
        /** Trường `serviceName`: dịch vụ tên. */
        String serviceName,
        BigDecimal quantity,
        /** Trường `unitPrice`: đơn vị `Price`. */
        BigDecimal unitPrice,
        /** Trường `status`: trạng thái. */
        String status,
        /** Trường `orderedAt`: `ordered` luc. */
        LocalDateTime orderedAt
) {}
