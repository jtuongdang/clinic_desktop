package vn.clinicmanagement.application.attendance;

import java.time.Instant;
import java.util.Objects;

/**
 * Bản ghi kiểm toán sự kiện điều chỉnh chấm công.
 */
public record ChamCongKiemToanNhapLieu(
        /** Trường `action`: hành động. */
        String action,
        /** Trường `actorEmployeeId`: tacnhan nhân viên mã. */
        int actorEmployeeId,
        /** Trường `attendanceId`: chamcong mã. */
        int attendanceId,
        /** Trường `reason`: lý do. */
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
