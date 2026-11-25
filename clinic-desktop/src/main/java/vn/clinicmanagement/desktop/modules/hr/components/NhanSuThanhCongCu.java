package vn.clinicmanagement.desktop.modules.hr.components;

import vn.clinicmanagement.application.hr.NhanSuReadPort.PayrollPeriodOption;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.hr.components.NhanSuBangHoTro.ViewSection;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocBieuTuong;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.util.function.Consumer;

import static vn.clinicmanagement.desktop.modules.hr.components.NhanSuBangHoTro.addMenuItem;

/**
 * Thanh công cụ trên cùng của phân hệ Quản Lý Nhân Sự với 4 nhóm menu xổ xuống và bộ lọc nhanh.
 */
public final class NhanSuThanhCongCu extends JPanel {

    public record Actions(
            Runnable onCreateEmployee,
            Runnable onEditSelectedEmployee,
            Runnable onAssignRoles,
            Runnable onResetPassword,
            Runnable onOpenPermissionMatrix,
            Runnable onToggleEmployeeStatus,
            Runnable onOpenScheduleDialog,
            Runnable onManageShifts,
            Runnable onCheckIn,
            Runnable onCheckOut,
            Runnable onAdjustAttendance,
            Runnable onPrintMatrix,
            Runnable onCalculatePayroll,
            Runnable onAdjustPayrollLine,
            Runnable onShowPayslip,
            Runnable onPrintPeriodPayroll,
            Runnable onLockPayroll,
            Runnable onReopenPayroll,
            Runnable onExportCsv,
            Consumer<ViewSection> onSwitchSection,
            Runnable onFilterChanged,
            Runnable onPeriodSelected,
            Runnable onMonthYearChanged,
            Consumer<String> onQuickSearch,
            Runnable onReloadAll
    ) {}

    private final JTextField txtQuickSearch = new JTextField(14);
    private final JComboBox<Integer> cboMonth = new JComboBox<>();
    private final JComboBox<Integer> cboYear = new JComboBox<>();
    private final JComboBox<PayrollPeriodOption> cboPeriod = new JComboBox<>();
    private final JComboBox<String> cboDeptFilter = new JComboBox<>();

    public NhanSuThanhCongCu(Actions actions) {
        super(new BorderLayout());
        setPreferredSize(new Dimension(1100, 58));
        setBackground(new Color(235, 241, 247));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(189, 202, 215)));

        LocalDate today = LocalDate.now();
        for (int m = 1; m <= 12; m++) cboMonth.addItem(m);
        cboMonth.setSelectedItem(today.getMonthValue());
        for (int y = today.getYear() - 2; y <= today.getYear() + 1; y++) cboYear.addItem(y);
        cboYear.setSelectedItem(today.getYear());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);

        JButton btnProfile = createToolbarButton("Hồ Sơ NV ▾", NhaThuocBieuTuong.createPlusIcon(), 84);
        JButton btnAttendance = createToolbarButton("Chấm Công ▾", NhaThuocBieuTuong.createListIcon(), 90);
        JButton btnPayroll = createToolbarButton("Tiền Lương ▾", NhaThuocBieuTuong.createReportIcon(), 90);
        JButton btnList = createToolbarButton("Danh Sách ▾", NhaThuocBieuTuong.createMenuIcon(), 88);

        JPopupMenu profileMenu = new JPopupMenu();
        addMenuItem(profileMenu, "Thêm Nhân Viên Mới", actions.onCreateEmployee());
        addMenuItem(profileMenu, "Cập Nhật Nhân Viên Đang Chọn", actions.onEditSelectedEmployee());
        addMenuItem(profileMenu, "Phân Vai Trò", actions.onAssignRoles());
        addMenuItem(profileMenu, "Đặt Lại Mật Khẩu", actions.onResetPassword());
        addMenuItem(profileMenu, "Ma Trận Phân Quyền", actions.onOpenPermissionMatrix());
        profileMenu.addSeparator();
        addMenuItem(profileMenu, "Khóa / Mở Khóa Tài Khoản", actions.onToggleEmployeeStatus());
        btnProfile.addActionListener(e -> profileMenu.show(btnProfile, 0, btnProfile.getHeight()));

        JPopupMenu attendanceMenu = new JPopupMenu();
        addMenuItem(attendanceMenu, "Xếp Lịch Làm Việc / Hàng Loạt", actions.onOpenScheduleDialog());
        addMenuItem(attendanceMenu, "Quản Lý Danh Mục Ca Trực", actions.onManageShifts());
        attendanceMenu.addSeparator();
        addMenuItem(attendanceMenu, "Check-in Nhân Viên", actions.onCheckIn());
        addMenuItem(attendanceMenu, "Check-out Nhân Viên", actions.onCheckOut());
        addMenuItem(attendanceMenu, "Điều Chỉnh Giờ Công", actions.onAdjustAttendance());
        attendanceMenu.addSeparator();
        addMenuItem(attendanceMenu, "In Lịch Làm Việc / Chấm Công", actions.onPrintMatrix());
        addMenuItem(attendanceMenu, "Xem Lịch Làm Việc / Ca Trực", () -> actions.onSwitchSection().accept(ViewSection.SCHEDULES));
        addMenuItem(attendanceMenu, "Xem Bảng Chấm Công Tháng", () -> actions.onSwitchSection().accept(ViewSection.ATTENDANCE));
        btnAttendance.addActionListener(e -> attendanceMenu.show(btnAttendance, 0, btnAttendance.getHeight()));

        JPopupMenu payrollMenu = new JPopupMenu();
        addMenuItem(payrollMenu, "Tính / Tạo Kỳ Lương", actions.onCalculatePayroll());
        addMenuItem(payrollMenu, "Điều Chỉnh Dòng Lương Đang Chọn", actions.onAdjustPayrollLine());
        addMenuItem(payrollMenu, "Xem / In Phiếu Lương Cá Nhân", actions.onShowPayslip());
        addMenuItem(payrollMenu, "In Bảng Lương Toàn Kỳ", actions.onPrintPeriodPayroll());
        payrollMenu.addSeparator();
        addMenuItem(payrollMenu, "Chốt Kỳ Lương", actions.onLockPayroll());
        addMenuItem(payrollMenu, "Mở Khóa Kỳ Lương", actions.onReopenPayroll());
        payrollMenu.addSeparator();
        addMenuItem(payrollMenu, "Xem Kỳ Lương & Bảng Lương", () -> actions.onSwitchSection().accept(ViewSection.PAYROLL));
        btnPayroll.addActionListener(e -> payrollMenu.show(btnPayroll, 0, btnPayroll.getHeight()));

        JPopupMenu listMenu = new JPopupMenu();
        addMenuItem(listMenu, "Tất Cả Nhân Sự", () -> actions.onSwitchSection().accept(ViewSection.ALL_EMPLOYEES));
        addMenuItem(listMenu, "Nhân Sự Đang Hoạt Động", () -> actions.onSwitchSection().accept(ViewSection.ACTIVE_EMPLOYEES));
        addMenuItem(listMenu, "Nhân Sự Đã Khóa", () -> actions.onSwitchSection().accept(ViewSection.LOCKED_EMPLOYEES));
        addMenuItem(listMenu, "Lịch Làm Việc / Ca Trực", () -> actions.onSwitchSection().accept(ViewSection.SCHEDULES));
        addMenuItem(listMenu, "Bảng Chấm Công Tháng", () -> actions.onSwitchSection().accept(ViewSection.ATTENDANCE));
        addMenuItem(listMenu, "Kỳ Lương & Bảng Lương", () -> actions.onSwitchSection().accept(ViewSection.PAYROLL));
        listMenu.addSeparator();
        addMenuItem(listMenu, "Xuất CSV Bảng Hiện Tại", actions.onExportCsv());
        btnList.addActionListener(e -> listMenu.show(btnList, 0, btnList.getHeight()));

        left.add(btnProfile);
        left.add(btnAttendance);
        left.add(btnPayroll);
        left.add(btnList);
        add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 14));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(0, 4, 0, 8));

        right.add(new JLabel("Khoa:"));
        cboDeptFilter.addItem("Tất cả khoa");
        cboDeptFilter.setPreferredSize(new Dimension(130, 24));
        cboDeptFilter.addActionListener(e -> actions.onFilterChanged().run());
        right.add(cboDeptFilter);

        right.add(new JLabel("Tháng:"));
        cboMonth.setPreferredSize(new Dimension(56, 24));
        cboMonth.addActionListener(e -> actions.onMonthYearChanged().run());
        right.add(cboMonth);

        right.add(new JLabel("Năm:"));
        cboYear.setPreferredSize(new Dimension(68, 24));
        cboYear.addActionListener(e -> actions.onMonthYearChanged().run());
        right.add(cboYear);

        right.add(new JLabel("Kỳ:"));
        cboPeriod.setPreferredSize(new Dimension(170, 24));
        cboPeriod.addActionListener(e -> actions.onPeriodSelected().run());
        right.add(cboPeriod);

        right.add(new JLabel("Tìm:"));
        txtQuickSearch.setPreferredSize(new Dimension(115, 24));
        txtQuickSearch.addActionListener(e -> actions.onQuickSearch().accept(txtQuickSearch.getText()));
        right.add(txtQuickSearch);

        JButton btnRefresh = new JButton("Tìm Kiếm");
        btnRefresh.setFont(UngDungPhongChu.BODY_BOLD);
        btnRefresh.addActionListener(e -> actions.onReloadAll().run());
        right.add(btnRefresh);
        add(right, BorderLayout.EAST);
    }

    public JComboBox<Integer> getCboMonth() { return cboMonth; }
    public JComboBox<Integer> getCboYear() { return cboYear; }
    public JComboBox<PayrollPeriodOption> getCboPeriod() { return cboPeriod; }
    public JComboBox<String> getCboDeptFilter() { return cboDeptFilter; }

    public int selectedMonth() {
        return cboMonth.getSelectedItem() instanceof Integer m ? m : LocalDate.now().getMonthValue();
    }

    public int selectedYear() {
        return cboYear.getSelectedItem() instanceof Integer y ? y : LocalDate.now().getYear();
    }

    private static JButton createToolbarButton(String text, Icon icon, int width) {
        JButton btn = new JButton(text, icon);
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setFont(UngDungPhongChu.SMALL);
        btn.setForeground(new Color(20, 30, 45));
        btn.setPreferredSize(new Dimension(width, 54));
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(3, 4, 3, 4));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
