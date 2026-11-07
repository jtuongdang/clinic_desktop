package vn.clinicmanagement.application.attendance;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Query;

import java.time.LocalDate;

/**
 * Truy vấn chấm công của nhân viên theo kỳ hoặc khoảng thời gian.
 */
public record GetAttendanceByPeriodQuery(
        int employeeId,
        LocalDate fromDate,
        LocalDate toDate,
        Integer kyChamCongId,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext
) implements Query<AttendanceSummaryDto> {

    public GetAttendanceByPeriodQuery(int employeeId, LocalDate fromDate, LocalDate toDate) {
        this(employeeId, fromDate, toDate, null, null, null);
    }

    public GetAttendanceByPeriodQuery(int employeeId, LocalDate fromDate, LocalDate toDate,
                                     AuthorizationContext authorizationContext) {
        this(employeeId, fromDate, toDate, null, authorizationContext, null);
    }
}
