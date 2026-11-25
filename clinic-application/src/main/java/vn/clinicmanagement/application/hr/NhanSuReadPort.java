package vn.clinicmanagement.application.hr;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Cổng đọc/ghi dữ liệu tổng hợp cho phân hệ Nhân sự, Chấm công và Tiền lương. */
public interface NhanSuReadPort {

    record EmployeeRow(
            int id,
            String maNhanVien,
            String fullName,
            String username,
            String phone,
            String email,
            String positionName,
            String roles,
            String departments,
            boolean active
    ) {}

    record RoleOption(int id, String code, String name) {
        @Override
        public String toString() {
            return code + " - " + name;
        }
    }

    record PositionOption(int id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    record DepartmentOption(int id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    record EmployeeDetail(
            int id,
            String maNhanVien,
            String ho,
            String ten,
            String soDienThoai,
            String email,
            String diaChi,
            String gioiTinh,
            LocalDate ngaySinh,
            Integer maChucVu,
            String username,
            String chungChiHanhNghe,
            LocalDate ngayVaoLam,
            BigDecimal luongCoBan,
            BigDecimal heSoLuong,
            BigDecimal phuCap,
            boolean hoatDong,
            List<Integer> roleIds,
            List<Integer> departmentIds,
            LocalDate ngayNghiViec,
            String lyDoNghiViec,
            LocalDateTime dangNhapCuoi
    ) {
        public EmployeeDetail {
            roleIds = roleIds != null ? List.copyOf(roleIds) : List.of();
            departmentIds = departmentIds != null ? List.copyOf(departmentIds) : List.of();
        }

        public EmployeeDetail(
                int id, String maNhanVien, String ho, String ten, String soDienThoai, String email,
                String diaChi, String gioiTinh, LocalDate ngaySinh, Integer maChucVu, String username,
                String chungChiHanhNghe, LocalDate ngayVaoLam, BigDecimal luongCoBan, BigDecimal heSoLuong,
                BigDecimal phuCap, boolean hoatDong, List<Integer> roleIds, List<Integer> departmentIds) {
            this(id, maNhanVien, ho, ten, soDienThoai, email, diaChi, gioiTinh, ngaySinh, maChucVu, username,
                    chungChiHanhNghe, ngayVaoLam, luongCoBan, heSoLuong, phuCap, hoatDong, roleIds, departmentIds,
                    null, "", null);
        }
    }

    record ShiftOption(int id, String name, LocalTime startTime, LocalTime endTime) {
        @Override
        public String toString() {
            return name + " (" + startTime + " - " + endTime + ")";
        }
    }

    record ShiftTypeOption(int id, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    record ShiftDetail(int id, int shiftTypeId, String shiftTypeName, String name, LocalTime startTime, LocalTime endTime, boolean active) {}

    record RoomOption(int id, int departmentId, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    record ScheduleRow(
            int id,
            int employeeId,
            String maNhanVien,
            String employeeName,
            int shiftId,
            String shiftName,
            LocalTime startTime,
            LocalTime endTime,
            String departmentName,
            String roomName,
            LocalDate workDate,
            String status,
            String note
    ) {}

    record PermissionRow(
            int id,
            int roleId,
            String phanHe,
            String hanhDong,
            String phamViDuLieu,
            boolean duocPhep
    ) {}

    record AttendanceRow(
            int id,
            String maNhanVien,
            String fullName,
            LocalDate workDate,
            String shiftName,
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            String status,
            BigDecimal hoursWorked,
            BigDecimal lateHours
    ) {}

    record PayrollPeriodOption(int id, String name, int month, int year, boolean closed) {
        @Override
        public String toString() {
            return name + (closed ? " (Đã chốt)" : "");
        }
    }

    record PayrollPeriodSummary(
            int id,
            String name,
            int month,
            int year,
            LocalDate fromDate,
            LocalDate toDate,
            boolean closed,
            int employeeCount,
            BigDecimal totalNetSalary
    ) {}

    record PayrollRow(
            int id,
            String maNhanVien,
            String fullName,
            String positionName,
            BigDecimal baseSalary,
            BigDecimal factor,
            BigDecimal allowance,
            BigDecimal examCommission,
            BigDecimal deduction,
            BigDecimal netSalary
    ) {}

    List<EmployeeRow> listEmployees(int facilityId);

    List<RoleOption> listRoles();

    List<PositionOption> listPositions();

    List<AttendanceRow> listAttendance(int facilityId, int month, int year);

    void recordCheckInOrOut(int employeeId, int facilityId, boolean checkOut, int actorId);

    List<PayrollPeriodOption> listPayrollPeriods();

    List<PayrollRow> listPayroll(int periodId);

    void calculateOrCreatePayrollPeriod(int month, int year, int actorId);

    void lockPayrollPeriod(int periodId, int actorId);

    default EmployeeDetail getEmployeeDetail(int employeeId) {
        return null;
    }

    default List<DepartmentOption> listDepartments(int facilityId) {
        return List.of();
    }

    default void saveEmployeeDetail(EmployeeDetail detail, char[] newPassword, int actorId) {}

    default void resetEmployeePassword(int employeeId, char[] newPassword, int actorId) {}

    default List<ShiftOption> listShifts() { return List.of(); }

    default List<RoomOption> listRooms(int facilityId) { return List.of(); }

    default List<ScheduleRow> listSchedules(int facilityId, int month, int year) { return List.of(); }

    default void createSchedule(int employeeId, int shiftId, int facilityId, Integer departmentId, Integer roomId,
                                LocalDate workDate, String note, int actorId) {}

    default void deleteSchedule(int scheduleId, int actorId) {}

    default void adjustAttendance(int attendanceId, LocalDateTime checkIn, LocalDateTime checkOut, String status, int actorId) {}

    default void updatePayrollLine(int payrollLineId, BigDecimal allowance, BigDecimal examCommission, BigDecimal deduction, int actorId) {}

    default void reopenPayrollPeriod(int periodId, int actorId) {}

    default List<PayrollPeriodSummary> listPayrollPeriodSummaries() {
        return listPayrollPeriods().stream()
                .map(p -> new PayrollPeriodSummary(
                        p.id(), p.name(), p.month(), p.year(),
                        LocalDate.of(p.year(), p.month(), 1),
                        LocalDate.of(p.year(), p.month(), 1).withDayOfMonth(LocalDate.of(p.year(), p.month(), 1).lengthOfMonth()),
                        p.closed(), 0, BigDecimal.ZERO))
                .toList();
    }

    default List<PermissionRow> listRolePermissions(int roleId) { return List.of(); }

    default void saveRolePermissions(int roleId, List<PermissionRow> permissions, int actorId) {}

    default List<ShiftTypeOption> listShiftTypes() { return List.of(); }

    default List<ShiftDetail> listAllShifts() { return List.of(); }

    default void saveShift(ShiftDetail shift, int actorId) {}

    default int createScheduleBatch(int employeeId, int shiftId, int facilityId, Integer departmentId, Integer roomId,
                                    LocalDate fromDate, LocalDate toDate, boolean skipSundays, String note, int actorId) {
        int count = 0;
        LocalDate d = fromDate;
        while (!d.isAfter(toDate)) {
            if (!skipSundays || d.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) {
                createSchedule(employeeId, shiftId, facilityId, departmentId, roomId, d, note, actorId);
                count++;
            }
            d = d.plusDays(1);
        }
        return count;
    }

    default boolean hasPermission(String roleName, String phanHe, String hanhDong) {
        return true;
    }
}
