package vn.clinicmanagement.application.attendance;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.time.LocalDateTime;

/**
 * Lệnh ghi nhận check-in hoặc check-out theo lịch làm việc.
 */
public record RecordAttendanceCommand(
        int maLichLamViec,
        AttendanceType type,
        LocalDateTime timestamp,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext
) implements Command<RecordAttendanceReceipt> {

    public RecordAttendanceCommand(int maLichLamViec, AttendanceType type, LocalDateTime timestamp) {
        this(maLichLamViec, type, timestamp,
                type == AttendanceType.CHECK_IN ? timestamp : null,
                type == AttendanceType.CHECK_OUT ? timestamp : null,
                null, null);
    }

    public RecordAttendanceCommand(int maLichLamViec, AttendanceType type, LocalDateTime timestamp,
                                  AuthorizationContext authorizationContext) {
        this(maLichLamViec, type, timestamp,
                type == AttendanceType.CHECK_IN ? timestamp : null,
                type == AttendanceType.CHECK_OUT ? timestamp : null,
                authorizationContext, null);
    }

    public static RecordAttendanceCommand checkIn(int maLichLamViec, LocalDateTime checkInTime) {
        return new RecordAttendanceCommand(maLichLamViec, AttendanceType.CHECK_IN, checkInTime, checkInTime, null, null, null);
    }

    public static RecordAttendanceCommand checkIn(int maLichLamViec, LocalDateTime checkInTime,
                                                  AuthorizationContext authorizationContext,
                                                  AuditActorContext auditActorContext) {
        return new RecordAttendanceCommand(maLichLamViec, AttendanceType.CHECK_IN, checkInTime, checkInTime, null,
                authorizationContext, auditActorContext);
    }

    public static RecordAttendanceCommand checkOut(int maLichLamViec, LocalDateTime checkOutTime) {
        return new RecordAttendanceCommand(maLichLamViec, AttendanceType.CHECK_OUT, checkOutTime, null, checkOutTime, null, null);
    }

    public static RecordAttendanceCommand checkOut(int maLichLamViec, LocalDateTime checkOutTime,
                                                   AuthorizationContext authorizationContext,
                                                   AuditActorContext auditActorContext) {
        return new RecordAttendanceCommand(maLichLamViec, AttendanceType.CHECK_OUT, checkOutTime, null, checkOutTime,
                authorizationContext, auditActorContext);
    }

    public static RecordAttendanceCommand recordBoth(int maLichLamViec, LocalDateTime checkInTime, LocalDateTime checkOutTime) {
        return new RecordAttendanceCommand(maLichLamViec, AttendanceType.BOTH, null, checkInTime, checkOutTime, null, null);
    }

    public static RecordAttendanceCommand recordBoth(int maLichLamViec, LocalDateTime checkInTime, LocalDateTime checkOutTime,
                                                     AuthorizationContext authorizationContext,
                                                     AuditActorContext auditActorContext) {
        return new RecordAttendanceCommand(maLichLamViec, AttendanceType.BOTH, null, checkInTime, checkOutTime,
                authorizationContext, auditActorContext);
    }
}
