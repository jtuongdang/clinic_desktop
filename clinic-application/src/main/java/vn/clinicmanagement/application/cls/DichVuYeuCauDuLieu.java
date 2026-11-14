package vn.clinicmanagement.application.cls;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record DichVuYeuCauDuLieu(
        /** Trường `orderId`: yêu cầu mã. */
        int orderId,
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `serviceId`: dịch vụ mã. */
        int serviceId,
        /** Trường `serviceCode`: dịch vụ mã. */
        String serviceCode,
        /** Trường `serviceName`: dịch vụ tên. */
        String serviceName,
        /** Trường `serviceType`: dịch vụ loai. */
        String serviceType,
        /** Trường `doctorId`: bacsi mã. */
        int doctorId,
        BigDecimal quantity,
        /** Trường `unitPrice`: đơn vị `Price`. */
        BigDecimal unitPrice,
        /** Trường `status`: trạng thái. */
        String status,
        /** Trường `resultSummary`: kết quả tonghop. */
        String resultSummary,
        String conclusion,
        String imageAttachmentUrls,
        /** Trường `roomId`: phòng mã. */
        Integer roomId,
        /** Trường `technicianId`: `technician` mã. */
        Integer technicianId,
        /** Trường `orderedAt`: `ordered` luc. */
        LocalDateTime orderedAt,
        /** Trường `performedAt`: `performed` luc. */
        LocalDateTime performedAt,
        /** Trường `returnedAt`: `returned` luc. */
        LocalDateTime returnedAt,
        List<XetNghiemKetQuaDuLieu> testResults
) {}
