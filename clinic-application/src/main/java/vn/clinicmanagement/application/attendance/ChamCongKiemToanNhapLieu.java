package vn.clinicmanagement.application.attendance;

import java.time.Instant;
import java.util.Objects;

/**
 * Bản ghi kiểm toán sự kiện điều chỉnh chấm công.
 */
public record ChamCongKiemToanNhapLieu(
        String action,
        int actorEmployeeId,
        int attendanceId,
        String reason,
        Instant timestamp
) {
    public ChamCongKiemToanNhapLieu {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(reason, "reason");
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
