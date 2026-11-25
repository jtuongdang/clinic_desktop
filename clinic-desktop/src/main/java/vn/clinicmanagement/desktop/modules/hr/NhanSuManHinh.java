package vn.clinicmanagement.desktop.modules.hr;

import vn.clinicmanagement.application.hr.NhanSuReadPort.DepartmentOption;
import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.desktop.modules.hr.components.*;
import vn.clinicmanagement.desktop.modules.hr.components.NhanSuBangHoTro.ViewSection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static vn.clinicmanagement.desktop.modules.hr.components.NhanSuBangHoTro.exportTableCsv;

/**
 * Màn hình Quản Lý Nhân Sự, Chấm Công & Tiền Lương (theo kiến trúc điều phối các panel con).
 */
public class NhanSuManHinh extends CoSoManHinhManHinh {
    private static final String SEARCH_PLACEHOLDER = "Tìm kiếm (Ctrl+F)";

    private final NhanSuGhepNoiGoc.Services services;
    private ViewSection activeSection = ViewSection.ALL_EMPLOYEES;

    private NhanSuThanhCongCu toolbar;
    private final NhanSuCayDanhMucPanel treePanel;
    private final NhanSuHoSoPanel hoSoPanel;
    private final NhanSuLichChamCongPanel lichChamCongPanel;
    private NhanSuTienLuongPanel tienLuongPanel;

    private final JTextField txtSearch = new JTextField(SEARCH_PLACEHOLDER);
    private final JLabel lblSectionTitle = new JLabel("DANH SÁCH NHÂN VIÊN");
    private final JLabel lblStatus = new JLabel("Sẵn sàng.");
    private final CardLayout tableCards = new CardLayout();
    private final JPanel tableCardPanel = new JPanel(tableCards);

    public NhanSuManHinh() {
        this(GraphicsEnvironment.isHeadless() || System.getProperty("surefire.test.class.path") != null
                ? NhanSuGhepNoiGoc.createDefault()
                : NhanSuGhepNoiGoc.createProduction());
    }

    public NhanSuManHinh(NhanSuGhepNoiGoc.Services services) {
        super("user", "Quản Lý Nhân Sự", "👥");
        this.services = Objects.requireNonNull(services, "services");
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        this.treePanel = new NhanSuCayDanhMucPanel(this::switchSection);

        this.hoSoPanel = new NhanSuHoSoPanel(
                services,
                lblStatus::setText,
                (total, active) -> {
                    treePanel.setTotalEmployees(total);
                    treePanel.setActiveEmployees(active);
                },
                () -> openScheduleForSelected(),
                (empId, checkOut) -> recordAttendanceFor(empId, checkOut),
                this::reloadEmployeesOnly
        );

        this.lichChamCongPanel = new NhanSuLichChamCongPanel(
                services.readPort(),
                hoSoPanel::getAllEmployees,
                lblStatus::setText,
                treePanel::setAttendanceCount,
                () -> switchSection(ViewSection.SCHEDULES)
        );

        this.toolbar = new NhanSuThanhCongCu(new NhanSuThanhCongCu.Actions(
                () -> guarded("THEM", () -> hoSoPanel.openCreateEmployeeDialog(this::reloadEmployeesOnly)),
                () -> guarded("SUA", hoSoPanel::editSelectedEmployee),
                () -> guarded("SUA", hoSoPanel::openAssignRolesDialog),
                () -> guarded("SUA", hoSoPanel::openResetPasswordDialog),
                () -> guarded("DUYET", hoSoPanel::openPermissionMatrixDialog),
                () -> guarded("SUA", hoSoPanel::toggleSelectedEmployeeStatus),
                () -> guarded("THEM", this::openScheduleForSelected),
                () -> guarded("SUA", lichChamCongPanel::openShiftManager),
                () -> recordAttendanceFor(null, false),
                () -> recordAttendanceFor(null, true),
                () -> guarded("SUA", lichChamCongPanel::openAdjustAttendanceDialog),
                () -> guarded("IN_XUAT", () -> lichChamCongPanel.printMatrix(activeSection == ViewSection.ATTENDANCE)),
                () -> guarded("DUYET", () -> tienLuongPanel.calculatePayroll(toolbar.selectedMonth(), toolbar.selectedYear())),
                () -> guarded("SUA", tienLuongPanel::openAdjustPayrollLineDialog),
                () -> guarded("IN_XUAT", tienLuongPanel::showPayslipDialog),
                () -> guarded("IN_XUAT", tienLuongPanel::showPeriodPayrollPrintDialog),
                () -> guarded("DUYET", tienLuongPanel::lockPayroll),
                () -> guarded("DUYET", tienLuongPanel::reopenPayroll),
                () -> guarded("IN_XUAT", this::exportActiveTableCsv),
                this::switchSection,
                this::applyFilter,
                () -> tienLuongPanel.loadPayrollLines(searchKeyword()),
                this::onMonthYearChanged,
                kw -> {
                    txtSearch.setForeground(Color.BLACK);
                    txtSearch.setText(kw);
                    applyFilter();
                },
                this::reloadAll
        ));

        this.tienLuongPanel = new NhanSuTienLuongPanel(
                services.readPort(),
                toolbar.getCboPeriod(),
                lblStatus::setText,
                lblSectionTitle::setText,
                treePanel::setTotalPayrollText,
                (m, y) -> {
                    toolbar.getCboMonth().setSelectedItem(m);
                    toolbar.getCboYear().setSelectedItem(y);
                    onMonthYearChanged();
                    switchSection(ViewSection.ATTENDANCE);
                }
        );

        add(toolbar, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, buildRightTablePanel());
        splitPane.setDividerLocation(245);
        splitPane.setDividerSize(4);
        splitPane.setBorder(null);
        add(splitPane, BorderLayout.CENTER);

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control F"), "focus-search");
        getActionMap().put("focus-search", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                txtSearch.requestFocusInWindow();
                txtSearch.selectAll();
            }
        });

        reloadAll();
    }

    public JTable getTblEmployees() { return hoSoPanel.getTable(); }
    public JTable getTblSchedules() { return lichChamCongPanel.getTblSchedules(); }
    public JTable getTblAttendance() { return lichChamCongPanel.getTblAttendance(); }
    public JTable getTblPayrollPeriods() { return tienLuongPanel.getTblPayrollPeriods(); }
    public JTable getTblPayroll() { return tienLuongPanel.getTblPayroll(); }

    public CapNhatNhanVienHopThoai createEmployeeEditPanel(int employeeId) {
        return hoSoPanel.createEmployeeEditPanel(employeeId);
    }

    private JPanel buildRightTablePanel() {
        JPanel right = new JPanel(new BorderLayout(0, 0));
        right.setBackground(Color.WHITE);

        JPanel north = new JPanel(new BorderLayout(0, 2));
        north.setBackground(Color.WHITE);
        lblSectionTitle.setFont(UngDungPhongChu.BODY_BOLD);
        lblSectionTitle.setForeground(new Color(10, 86, 179));
        lblSectionTitle.setBorder(new EmptyBorder(5, 8, 4, 8));
        north.add(lblSectionTitle, BorderLayout.NORTH);

        JPanel searchBar = new JPanel(new BorderLayout(5, 0));
        searchBar.setBackground(new Color(255, 255, 204));
        searchBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 180), 1),
                new EmptyBorder(3, 8, 3, 8)));
        txtSearch.setFont(UngDungPhongChu.BODY);
        txtSearch.setForeground(new Color(120, 120, 120));
        txtSearch.setBackground(new Color(255, 255, 204));
        txtSearch.setBorder(null);
        txtSearch.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (SEARCH_PLACEHOLDER.equals(txtSearch.getText())) {
                    txtSearch.setText("");
                    txtSearch.setForeground(Color.BLACK);
                }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (txtSearch.getText().trim().isEmpty()) {
                    txtSearch.setText(SEARCH_PLACEHOLDER);
                    txtSearch.setForeground(new Color(120, 120, 120));
                }
            }
        });
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        searchBar.add(txtSearch, BorderLayout.CENTER);
        searchBar.add(new JLabel("🔍"), BorderLayout.EAST);
        north.add(searchBar, BorderLayout.SOUTH);
        right.add(north, BorderLayout.NORTH);

        tableCardPanel.add(hoSoPanel, "EMPLOYEES");
        tableCardPanel.add(lichChamCongPanel.getSchedulesView(), "SCHEDULES");
        tableCardPanel.add(lichChamCongPanel.getAttendanceView(), "ATTENDANCE");
        tableCardPanel.add(tienLuongPanel, "PAYROLL");
        right.add(tableCardPanel, BorderLayout.CENTER);

        lblStatus.setFont(UngDungPhongChu.BODY);
        lblStatus.setForeground(new Color(70, 85, 100));
        lblStatus.setBorder(new EmptyBorder(4, 8, 4, 8));
        right.add(lblStatus, BorderLayout.SOUTH);
        return right;
    }

    private void switchSection(ViewSection section) {
        activeSection = section;
        lblSectionTitle.setText(section.label.toUpperCase(Locale.ROOT));
        switch (section) {
            case ALL_EMPLOYEES, ACTIVE_EMPLOYEES, LOCKED_EMPLOYEES -> {
                tableCards.show(tableCardPanel, "EMPLOYEES");
                hoSoPanel.filterEmployees(activeSection, searchKeyword(), selectedDept());
            }
            case SCHEDULES -> {
                tableCards.show(tableCardPanel, "SCHEDULES");
                lichChamCongPanel.renderSchedules(toolbar.selectedMonth(), toolbar.selectedYear(), searchKeyword(), selectedDept());
            }
            case ATTENDANCE -> {
                tableCards.show(tableCardPanel, "ATTENDANCE");
                lichChamCongPanel.renderAttendance(toolbar.selectedMonth(), toolbar.selectedYear(), searchKeyword(), selectedDept());
            }
            case PAYROLL -> {
                tableCards.show(tableCardPanel, "PAYROLL");
                tienLuongPanel.showPayrollPeriodsOverview(searchKeyword());
            }
        }
    }

    private void applyFilter() {
        String kw = searchKeyword();
        String dept = selectedDept();
        switch (activeSection) {
            case ALL_EMPLOYEES, ACTIVE_EMPLOYEES, LOCKED_EMPLOYEES -> hoSoPanel.filterEmployees(activeSection, kw, dept);
            case SCHEDULES -> lichChamCongPanel.renderSchedules(toolbar.selectedMonth(), toolbar.selectedYear(), kw, dept);
            case ATTENDANCE -> lichChamCongPanel.renderAttendance(toolbar.selectedMonth(), toolbar.selectedYear(), kw, dept);
            case PAYROLL -> tienLuongPanel.applyFilter(kw);
        }
    }

    private String searchKeyword() {
        String s = txtSearch.getText().trim();
        return SEARCH_PLACEHOLDER.equals(s) ? "" : s.toLowerCase(Locale.ROOT);
    }

    private String selectedDept() {
        return Objects.toString(toolbar.getCboDeptFilter().getSelectedItem(), "Tất cả khoa");
    }

    @Override public void onShow() { reloadAll(); }
    @Override public void refreshData() { reloadAll(); }

    public void reloadAll() {
        loadDepartmentsFilter();
        reloadEmployeesOnly();
        onMonthYearChanged();
        tienLuongPanel.loadPayrollPeriods(searchKeyword());
    }

    private void reloadEmployeesOnly() {
        hoSoPanel.loadEmployees(activeSection, searchKeyword(), selectedDept());
    }

    private void onMonthYearChanged() {
        int m = toolbar.selectedMonth();
        int y = toolbar.selectedYear();
        lichChamCongPanel.loadSchedules(m, y, searchKeyword(), selectedDept());
        lichChamCongPanel.loadAttendance(m, y, searchKeyword(), selectedDept());
    }

    private void loadDepartmentsFilter() {
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null) return;
        new SwingWorker<List<DepartmentOption>, Void>() {
            @Override protected List<DepartmentOption> doInBackground() { return services.readPort().listDepartments(auth.facilityId()); }
            @Override protected void done() {
                try {
                    JComboBox<String> cbo = toolbar.getCboDeptFilter();
                    String current = Objects.toString(cbo.getSelectedItem(), "Tất cả khoa");
                    cbo.removeAllItems();
                    cbo.addItem("Tất cả khoa");
                    for (DepartmentOption d : get()) cbo.addItem(d.name());
                    cbo.setSelectedItem(current);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void openScheduleForSelected() {
        lichChamCongPanel.openScheduleDialogFor(hoSoPanel.getSelectedEmployeeId(), LocalDate.now());
    }

    private void recordAttendanceFor(Integer empId, boolean checkOut) {
        lichChamCongPanel.recordAttendance(empId, checkOut);
    }

    private void guarded(String actionCode, Runnable task) {
        var auth = KhamBenhPhienNguCanh.current();
        if (auth != null && !services.readPort().hasPermission(auth.roleCode(), "NHAN_SU", actionCode)) {
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(this,
                        "Vai trò [" + auth.roleCode() + "] không được cấp quyền [" + actionCode + "] trên phân hệ Nhân Sự.",
                        "Không có quyền truy cập", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        task.run();
    }

    private void exportActiveTableCsv() {
        JTable target = switch (activeSection) {
            case SCHEDULES -> lichChamCongPanel.getTblSchedules();
            case ATTENDANCE -> lichChamCongPanel.getTblAttendance();
            case PAYROLL -> tienLuongPanel.getActiveTable();
            default -> hoSoPanel.getTable();
        };
        exportTableCsv(this, target, activeSection.name().toLowerCase(Locale.ROOT) + ".csv", lblStatus::setText);
    }
}
