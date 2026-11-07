package vn.clinicmanagement.application.attendance;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lệnh điều chỉnh thông tin chấm công (check-in, check-out hoặc số giờ công kèm lý do).
 * Chỉ dành cho người có thẩm quyền ADMIN hoặc HR_ADMIN.
 */
public record AdjustAttendanceCommand(
        int attendanceId,
        LocalDateTime newCheckInTime,
        LocalDateTime newCheckOutTime,
        BigDecimal adjustedWorkHours,
        String reason,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext
) implements Command<AdjustAttendanceReceipt> {

    public AdjustAttendanceCommand(int attendanceId,
                                  LocalDateTime newCheckInTime,
                                  LocalDateTime newCheckOutTime,
                                  BigDecimal adjustedWorkHours,
                                  String reason,
                                  AuthorizationContext authorizationContext) {
        this(attendanceId, newCheckInTime, newCheckOutTime, adjustedWorkHours, reason, authorizationContext, null);
    }
}
