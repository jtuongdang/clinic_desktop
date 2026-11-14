package vn.clinicmanagement.application.attendance;

import vn.clinicmanagement.domain.identity.CaTruc;
import vn.clinicmanagement.domain.identity.ChamCong;
import vn.clinicmanagement.domain.identity.DieuChinhChamCong;
import vn.clinicmanagement.domain.identity.LichLamViec;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Khế ước lưu trữ và truy vấn dữ liệu ca trực, lịch làm việc và chấm công.
 */
public interface ChamCongRepository {
    /** Hàm `getScheduleById`: lấy `Schedule` theo mã. */
    Optional<LichLamViec> getScheduleById(int scheduleId);
    /** Hàm `getSchedulesByEmployeeAndDateRange`: lấy `Schedules` theo nhân viên va ngày khoang. */
    List<LichLamViec> getSchedulesByEmployeeAndDateRange(int employeeId, LocalDate fromDate, LocalDate toDate);
    /** Hàm `saveSchedule`: lưu `Schedule`. */
    void saveSchedule(LichLamViec schedule);

    /** Hàm `getShiftById`: lấy `Shift` theo mã. */
    Optional<CaTruc> getShiftById(int shiftId);
    /** Hàm `saveShift`: lưu `Shift`. */
    void saveShift(CaTruc shift);

    /** Hàm `getAttendanceById`: lấy chamcong theo mã. */
    Optional<ChamCong> getAttendanceById(int attendanceId);
    /** Hàm `getAttendanceByScheduleId`: lấy chamcong theo `Schedule` mã. */
    Optional<ChamCong> getAttendanceByScheduleId(int scheduleId);
    /** Hàm `getAttendanceByEmployeeAndDateRange`: lấy chamcong theo nhân viên va ngày khoang. */
    List<ChamCong> getAttendanceByEmployeeAndDateRange(int employeeId, LocalDate fromDate, LocalDate toDate);
    /** Hàm `saveAttendance`: lưu chamcong. */
    ChamCong saveAttendance(ChamCong attendance);

    /** Hàm `saveAdjustment`: lưu điều chỉnh. */
    void saveAdjustment(DieuChinhChamCong adjustment);
    /** Hàm `getAdjustmentsByAttendanceId`: lấy `Adjustments` theo chamcong mã. */
    List<DieuChinhChamCong> getAdjustmentsByAttendanceId(int attendanceId);
}
