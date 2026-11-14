package vn.clinicmanagement.application.attendance;

import java.time.Instant;

/**
 * Cổng ghi nhận nhật ký kiểm toán cho phân hệ Chấm công & Ca trực.
 */
public interface KiemToanWriteCongKetNoi {
    /** Hàm `recordAudit`: ghi nhận kiểm toán. */
    void recordAudit(ChamCongKiemToanNhapLieu entry);

    /** Hàm `recordAudit`: ghi nhận kiểm toán. */
    default void recordAudit(String action, int actorEmployeeId, int attendanceId, String reason) {
        recordAudit(new ChamCongKiemToanNhapLieu(action, actorEmployeeId, attendanceId, reason, Instant.now()));
    }
}
