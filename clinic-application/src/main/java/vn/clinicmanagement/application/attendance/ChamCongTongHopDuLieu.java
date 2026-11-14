package vn.clinicmanagement.application.attendance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO tổng hợp kết quả chấm công của nhân viên theo kỳ hoặc khoảng thời gian.
 */
public record ChamCongTongHopDuLieu(
        /** Trường `employeeId`: nhân viên mã. */
        int employeeId,
        /** Trường `fromDate`: tu ngày. */
        LocalDate fromDate,
        /** Trường `toDate`: den ngày. */
        LocalDate toDate,
        int totalScheduledShifts,
        int attendedShifts,
        /** Trường `totalWorkHours`: `total` làm việc giờ. */
        BigDecimal totalWorkHours,
        /** Trường `totalLateHours`: `total` muon giờ. */
        BigDecimal totalLateHours,
        /** Trường `totalEarlyLeaveHours`: `total` som nghi giờ. */
        BigDecimal totalEarlyLeaveHours,
        /** Trường `lateCount`: muon dem. */
        int lateCount,
        /** Trường `earlyLeaveCount`: som nghi dem. */
        int earlyLeaveCount,
        List<ChamCongRecordDuLieu> records
) {
    /** Hàm `getEmployeeId`: lấy nhân viên mã. */
    public int getEmployeeId() { return employeeId; }
    /** Hàm `getFromDate`: lấy tu ngày. */
    public LocalDate getFromDate() { return fromDate; }
    /** Hàm `getToDate`: lấy den ngày. */
    public LocalDate getToDate() { return toDate; }
    /** Hàm `getTotalScheduledShifts`: lấy `Total` `Scheduled` `Shifts`. */
    public int getTotalScheduledShifts() { return totalScheduledShifts; }
    /** Hàm `getAttendedShifts`: lấy `Attended` `Shifts`. */
    public int getAttendedShifts() { return attendedShifts; }
    /** Hàm `getTotalWorkHours`: lấy `Total` làm việc giờ. */
    public BigDecimal getTotalWorkHours() { return totalWorkHours; }
    /** Hàm `getTotalLateHours`: lấy `Total` muon giờ. */
    public BigDecimal getTotalLateHours() { return totalLateHours; }
    /** Hàm `getTotalEarlyLeaveHours`: lấy `Total` som nghi giờ. */
    public BigDecimal getTotalEarlyLeaveHours() { return totalEarlyLeaveHours; }
    /** Hàm `getLateCount`: lấy muon dem. */
    public int getLateCount() { return lateCount; }
    /** Hàm `getEarlyLeaveCount`: lấy som nghi dem. */
    public int getEarlyLeaveCount() { return earlyLeaveCount; }
    /** Hàm `getRecords`: lấy `Records`. */
    public List<ChamCongRecordDuLieu> getRecords() { return records; }
}
