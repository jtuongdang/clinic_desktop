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
public interface AttendanceRepository {
    Optional<LichLamViec> getScheduleById(int scheduleId);
    List<LichLamViec> getSchedulesByEmployeeAndDateRange(int employeeId, LocalDate fromDate, LocalDate toDate);
    void saveSchedule(LichLamViec schedule);

    Optional<CaTruc> getShiftById(int shiftId);
    void saveShift(CaTruc shift);

    Optional<ChamCong> getAttendanceById(int attendanceId);
    Optional<ChamCong> getAttendanceByScheduleId(int scheduleId);
    List<ChamCong> getAttendanceByEmployeeAndDateRange(int employeeId, LocalDate fromDate, LocalDate toDate);
    ChamCong saveAttendance(ChamCong attendance);

    void saveAdjustment(DieuChinhChamCong adjustment);
    List<DieuChinhChamCong> getAdjustmentsByAttendanceId(int attendanceId);
}
