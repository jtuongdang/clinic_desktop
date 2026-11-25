package vn.clinicmanagement.application.examination;

import java.time.LocalDateTime;

/**
 * Dữ liệu một dòng lịch sử lượt khám và điều trị của người bệnh trên màn hình Khám Bệnh.
 */
public record KhamBenhLichSuMatHang(
        int visitId,
        String visitStatus,
        LocalDateTime admissionTime,
        LocalDateTime dischargeTime,
        String registrationType,
        String payerType,
        String departmentName,
        String clinicRoomName,
        String bhytNumber) {
}
