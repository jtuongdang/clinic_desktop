package vn.clinicmanagement.desktop.modules.hr;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.hr.NhanSuReadPort;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;
import vn.clinicmanagement.desktop.core.session.PhienThuNganKiemThu;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class NhanSuManHinhTest {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    @AfterEach
    void logout() {
        NguoiDungPhien.getInstance().clearSession();
    }

    @Test
    void loadsEmployeesAttendanceAndPayrollViaPort() throws Exception {
        PhienThuNganKiemThu.login("ADMIN");
        var base = NhanSuGhepNoiGoc.createDefault();
        NhanSuReadPort fakePort = new NhanSuReadPort() {
            @Override
            public List<EmployeeRow> listEmployees(int facilityId) {
                assertEquals(1, facilityId);
                return List.of(new EmployeeRow(1, "NV001", "Phạm Quang Dương", "duongpq", "0901234567",
                        "duong@clinic.vn", "Bác sĩ", "Bác sĩ, Quản trị", "Khoa Khám Bệnh", true));
            }
            @Override public List<RoleOption> listRoles() { return List.of(new RoleOption(1, "ADMIN", "Quản trị")); }
            @Override public List<PositionOption> listPositions() { return List.of(new PositionOption(1, "Bác sĩ")); }
            @Override
            public List<ScheduleRow> listSchedules(int facilityId, int month, int year) {
                return List.of(new ScheduleRow(1, 1, "NV001", "Phạm Quang Dương", 1, "Ca sáng",
                        LocalTime.of(7, 30), LocalTime.of(11, 30), "Khoa Khám Bệnh", "Phòng 101",
                        LocalDate.of(2026, 11, 23), "CHUA_DIEM_DANH", "Trực chính"));
            }
            @Override
            public List<AttendanceRow> listAttendance(int facilityId, int month, int year) {
                return List.of(new AttendanceRow(10, "NV001", "Phạm Quang Dương", LocalDate.of(2026, 11, 23),
                        "Ca sáng", LocalDateTime.of(2026, 11, 23, 7, 30), LocalDateTime.of(2026, 11, 23, 11, 30),
                        "DUNG_GIO", new BigDecimal("4.00"), BigDecimal.ZERO));
            }
            @Override public void recordCheckInOrOut(int employeeId, int facilityId, boolean checkOut, int actorId) {}
            @Override
            public List<PayrollPeriodOption> listPayrollPeriods() {
                return List.of(new PayrollPeriodOption(1, "Kỳ lương Tháng 11/2026", 11, 2026, false));
            }
            @Override
            public List<PayrollRow> listPayroll(int periodId) {
                assertEquals(1, periodId);
                return List.of(new PayrollRow(100, "NV001", "Phạm Quang Dương", "Bác sĩ",
                        new BigDecimal("10000000"), BigDecimal.ONE, new BigDecimal("1000000"),
                        new BigDecimal("300000"), BigDecimal.ZERO, new BigDecimal("11300000")));
            }
            @Override public void calculateOrCreatePayrollPeriod(int month, int year, int actorId) {}
            @Override public void lockPayrollPeriod(int periodId, int actorId) {}
        };

        var services = new NhanSuGhepNoiGoc.Services(
                base.createEmployee(), base.assignRoles(), base.updateStatus(), fakePort);
        NhanSuManHinh[] screen = new NhanSuManHinh[1];
        SwingUtilities.invokeAndWait(() -> screen[0] = new NhanSuManHinh(services));

        for (int i = 0; i < 100; i++) {
            Thread.sleep(20);
            boolean[] ready = {false};
            SwingUtilities.invokeAndWait(() -> ready[0] =
                    screen[0].getTblEmployees().getRowCount() == 1
                            && screen[0].getTblSchedules().getRowCount() == 1
                            && screen[0].getTblAttendance().getRowCount() == 1
                            && screen[0].getTblPayroll().getRowCount() == 1);
            if (ready[0]) break;
        }

        SwingUtilities.invokeAndWait(() -> {
            assertEquals("NV001", screen[0].getTblEmployees().getValueAt(0, 1));
            assertEquals("Phạm Quang Dương", screen[0].getTblEmployees().getValueAt(0, 2));
            assertEquals("S", screen[0].getTblSchedules().getValueAt(0, 2 + 23));
            assertEquals("✓", screen[0].getTblAttendance().getValueAt(0, 2 + 23));
            assertEquals(1, screen[0].getTblPayrollPeriods().getRowCount());
            assertEquals("Kỳ lương Tháng 11/2026", screen[0].getTblPayrollPeriods().getValueAt(0, 1));
            assertEquals("11,300,000", screen[0].getTblPayroll().getValueAt(0, 9));
        });
    }

    @Test
    void loadsAndSavesEmployeeDetailInEditForm() throws Exception {
        PhienThuNganKiemThu.login("ADMIN");
        AtomicReference<NhanSuReadPort.EmployeeDetail> savedRef = new AtomicReference<>();
        NhanSuReadPort fakePort = new NhanSuReadPort() {
            @Override public List<EmployeeRow> listEmployees(int facilityId) { return List.of(); }
            @Override public List<RoleOption> listRoles() {
                return List.of(new RoleOption(1, "ADMIN", "Quản trị"), new RoleOption(2, "DOCTOR", "Bác sĩ"));
            }
            @Override public List<PositionOption> listPositions() {
                return List.of(new PositionOption(1, "Bác sĩ"), new PositionOption(2, "Điều dưỡng"));
            }
            @Override public List<DepartmentOption> listDepartments(int facilityId) {
                return List.of(new DepartmentOption(10, "Khoa Khám Bệnh"), new DepartmentOption(11, "Khoa Nội"));
            }
            @Override
            public EmployeeDetail getEmployeeDetail(int employeeId) {
                assertEquals(5, employeeId);
                return new EmployeeDetail(
                        5, "NV005", "Nguyễn Văn", "An", "0909123456", "an@clinic.vn", "123 Lê Lợi",
                        "Nam", LocalDate.of(1990, 5, 10), 1, "annv", "CCHN-005",
                        LocalDate.of(2022, 1, 15), new BigDecimal("12000000"), new BigDecimal("1.5"),
                        new BigDecimal("1500000"), true, List.of(2), List.of(10));
            }
            @Override
            public void saveEmployeeDetail(EmployeeDetail detail, char[] newPassword, int actorId) {
                savedRef.set(detail);
            }
            @Override public List<AttendanceRow> listAttendance(int facilityId, int month, int year) { return List.of(); }
            @Override public void recordCheckInOrOut(int employeeId, int facilityId, boolean checkOut, int actorId) {}
            @Override public List<PayrollPeriodOption> listPayrollPeriods() { return List.of(); }
            @Override public List<PayrollRow> listPayroll(int periodId) { return List.of(); }
            @Override public void calculateOrCreatePayrollPeriod(int month, int year, int actorId) {}
            @Override public void lockPayrollPeriod(int periodId, int actorId) {}
        };

        CapNhatNhanVienHopThoai[] form = new CapNhatNhanVienHopThoai[1];
        SwingUtilities.invokeAndWait(() ->
                form[0] = new CapNhatNhanVienHopThoai(fakePort, 5, 1, 99, () -> {}));

        for (int i = 0; i < 100; i++) {
            Thread.sleep(20);
            boolean[] ready = {false};
            SwingUtilities.invokeAndWait(() -> ready[0] = form[0].isLoaded());
            if (ready[0]) break;
        }

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(form[0].isLoaded());
            assertEquals("NV005", ((JTextField) form[0].getField("maNhanVien")).getText());
            assertEquals("Nguyễn Văn", ((JTextField) form[0].getField("ho")).getText());
            assertEquals("An", ((JTextField) form[0].getField("ten")).getText());
            assertEquals("0909123456", ((JTextField) form[0].getField("soDienThoai")).getText());

            ((JTextField) form[0].getField("soDienThoai")).setText("0988777666");
            ((JTextField) form[0].getField("luongCoBan")).setText("15000000");
            form[0].getSaveButton().doClick();
        });

        for (int i = 0; i < 100; i++) {
            Thread.sleep(20);
            if (savedRef.get() != null) break;
        }

        assertNotNull(savedRef.get());
        assertEquals("0988777666", savedRef.get().soDienThoai());
        assertEquals(0, new BigDecimal("15000000").compareTo(savedRef.get().luongCoBan()));
        assertEquals(List.of(2), savedRef.get().roleIds());
        assertEquals(List.of(10), savedRef.get().departmentIds());
    }
}
