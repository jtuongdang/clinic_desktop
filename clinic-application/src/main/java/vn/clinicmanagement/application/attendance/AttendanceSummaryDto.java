package vn.clinicmanagement.application.attendance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO tổng hợp kết quả chấm công của nhân viên theo kỳ hoặc khoảng thời gian.
 */
public record AttendanceSummaryDto(
        int employeeId,
        LocalDate fromDate,
        LocalDate toDate,
        int totalScheduledShifts,
        int attendedShifts,
        BigDecimal totalWorkHours,
        BigDecimal totalLateHours,
        BigDecimal totalEarlyLeaveHours,
        int lateCount,
        int earlyLeaveCount,
        List<AttendanceRecordDto> records
) {
    public int getEmployeeId() { return employeeId; }
    public LocalDate getFromDate() { return fromDate; }
    public LocalDate getToDate() { return toDate; }
    public int getTotalScheduledShifts() { return totalScheduledShifts; }
    public int getAttendedShifts() { return attendedShifts; }
    public BigDecimal getTotalWorkHours() { return totalWorkHours; }
    public BigDecimal getTotalLateHours() { return totalLateHours; }
    public BigDecimal getTotalEarlyLeaveHours() { return totalEarlyLeaveHours; }
    public int getLateCount() { return lateCount; }
    public int getEarlyLeaveCount() { return earlyLeaveCount; }
    public List<AttendanceRecordDto> getRecords() { return records; }
}
