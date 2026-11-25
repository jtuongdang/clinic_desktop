package vn.clinicmanagement.application.reception;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dòng dữ liệu lịch sử khám và điều trị của người bệnh tại phân hệ Tiếp Đón.
 */
public record TiepDonLichSuKhamMatHang(
        int visitId,
        int patientId,
        String roomName,
        int queueNumber,
        String queueNumberDisplay,
        String status,
        LocalDateTime registeredAt,
        LocalDate followUpDate,
        LocalDate visitDate,
        String doctorName,
        LocalDateTime completedAt,
        String reason
) {}
