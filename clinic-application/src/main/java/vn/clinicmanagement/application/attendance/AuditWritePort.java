package vn.clinicmanagement.application.attendance;

import java.time.Instant;

/**
 * Cổng ghi nhận nhật ký kiểm toán cho phân hệ Chấm công & Ca trực.
 */
public interface AuditWritePort {
    void recordAudit(AttendanceAuditEntry entry);

    default void recordAudit(String action, int actorEmployeeId, int attendanceId, String reason) {
        recordAudit(new AttendanceAuditEntry(action, actorEmployeeId, attendanceId, reason, Instant.now()));
    }
}
