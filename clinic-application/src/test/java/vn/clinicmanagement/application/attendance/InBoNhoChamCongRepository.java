package vn.clinicmanagement.application.attendance;

import vn.clinicmanagement.domain.identity.CaTruc;
import vn.clinicmanagement.domain.identity.ChamCong;
import vn.clinicmanagement.domain.identity.DieuChinhChamCong;
import vn.clinicmanagement.domain.identity.LichLamViec;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Triển khai in-memory giả lập kho lưu trữ phục vụ 100% kiểm thử cách ly không chạm CSDL.
 */
public class InBoNhoChamCongRepository implements ChamCongRepository {

    public final Map<Integer, LichLamViec> schedules = new ConcurrentHashMap<>();
    public final Map<Integer, CaTruc> shifts = new ConcurrentHashMap<>();
    public final Map<Integer, ChamCong> attendances = new ConcurrentHashMap<>();
    public final Map<Integer, DieuChinhChamCong> adjustments = new ConcurrentHashMap<>();

    private final AtomicInteger scheduleIdSeq = new AtomicInteger(100);
    private final AtomicInteger shiftIdSeq = new AtomicInteger(200);
    private final AtomicInteger attendanceIdSeq = new AtomicInteger(300);
    private final AtomicInteger adjustmentIdSeq = new AtomicInteger(400);

    @Override
    public Optional<LichLamViec> getScheduleById(int scheduleId) {
        return Optional.ofNullable(schedules.get(scheduleId));
    }

    @Override
    public List<LichLamViec> getSchedulesByEmployeeAndDateRange(int employeeId, LocalDate fromDate, LocalDate toDate) {
        List<LichLamViec> result = new ArrayList<>();
        for (LichLamViec s : schedules.values()) {
            if (s.getMaNhanVien() == employeeId && s.getNgayLamViec() != null) {
                if (!s.getNgayLamViec().isBefore(fromDate) && !s.getNgayLamViec().isAfter(toDate)) {
                    result.add(s);
                }
            }
        }
        return result;
    }

    @Override
    public void saveSchedule(LichLamViec schedule) {
        if (schedule.getId() == null || schedule.getId() <= 0) {
            schedule.setId(scheduleIdSeq.incrementAndGet());
        }
        schedules.put(schedule.getId(), schedule);
    }

    @Override
    public Optional<CaTruc> getShiftById(int shiftId) {
        return Optional.ofNullable(shifts.get(shiftId));
    }

    @Override
    public void saveShift(CaTruc shift) {
        if (shift.getId() == null || shift.getId() <= 0) {
            shift.setId(shiftIdSeq.incrementAndGet());
        }
        shifts.put(shift.getId(), shift);
    }

    @Override
    public Optional<ChamCong> getAttendanceById(int attendanceId) {
        return Optional.ofNullable(attendances.get(attendanceId));
    }

    @Override
    public Optional<ChamCong> getAttendanceByScheduleId(int scheduleId) {
        return attendances.values().stream()
                .filter(a -> a.getMaLichLamViec() == scheduleId)
                .findFirst();
    }

    @Override
    public List<ChamCong> getAttendanceByEmployeeAndDateRange(int employeeId, LocalDate fromDate, LocalDate toDate) {
        List<ChamCong> result = new ArrayList<>();
        for (ChamCong a : attendances.values()) {
            LichLamViec sch = schedules.get(a.getMaLichLamViec());
            if (sch != null && sch.getMaNhanVien() == employeeId && sch.getNgayLamViec() != null) {
                if (!sch.getNgayLamViec().isBefore(fromDate) && !sch.getNgayLamViec().isAfter(toDate)) {
                    result.add(a);
                }
            }
        }
        return result;
    }

    @Override
    public ChamCong saveAttendance(ChamCong attendance) {
        if (attendance.getId() == null || attendance.getId() <= 0) {
            attendance.setId(attendanceIdSeq.incrementAndGet());
        }
        attendances.put(attendance.getId(), attendance);
        return attendance;
    }

    @Override
    public void saveAdjustment(DieuChinhChamCong adjustment) {
        if (adjustment.getId() == null || adjustment.getId() <= 0) {
            adjustment.setId(adjustmentIdSeq.incrementAndGet());
        }
        adjustments.put(adjustment.getId(), adjustment);
    }

    @Override
    public List<DieuChinhChamCong> getAdjustmentsByAttendanceId(int attendanceId) {
        List<DieuChinhChamCong> result = new ArrayList<>();
        for (DieuChinhChamCong d : adjustments.values()) {
            if (d.getMaChamCong() == attendanceId) {
                result.add(d);
            }
        }
        return result;
    }
}
