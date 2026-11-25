package vn.clinicmanagement.application.examination;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dữ liệu một bệnh nhân trong hàng đợi của màn hình Khám Bệnh (lấy từ bảng LuotKham & BenhNhan).
 */
public record KhamBenhHangDoiMatHang(
        int patientId,
        String patientCode,
        String fullName,
        String genderCode,
        LocalDate birthDate,
        String address,
        String phone,
        String ethnicity,
        String occupation,
        String nationality,
        String cccd,
        int visitId,
        String queueNumberDisplay,
        String visitStatus,
        String registrationType,
        String payerType,
        String departmentName,
        String clinicRoomName,
        String doctorName,
        LocalDateTime admissionTime,
        LocalDateTime dischargeTime,
        String reason,
        String bhytNumber,
        int bhytRate) {
}
