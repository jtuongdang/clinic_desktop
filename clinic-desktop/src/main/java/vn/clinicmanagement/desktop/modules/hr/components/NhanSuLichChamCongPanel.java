package vn.clinicmanagement.desktop.modules.hr.components;

import vn.clinicmanagement.application.hr.NhanSuReadPort;
import vn.clinicmanagement.application.hr.NhanSuReadPort.*;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.desktop.modules.hr.CaTrucQuanLyHopThoai;
import vn.clinicmanagement.desktop.modules.print.hr.PhieuLuongInHopThoai;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import static vn.clinicmanagement.desktop.modules.hr.components.NhanSuBangHoTro.*;

/**
 * Quản lý ma trận Lịch Làm Việc / Ca Trực và Bảng Chấm Công Tháng theo dạng lưới nhân viên x ngày.
 */
public final class NhanSuLichChamCongPanel {
    private final NhanSuReadPort readPort;
    private final Supplier<List<EmployeeRow>> employeesSupplier;
    private final Consumer<String> onStatus;
    private final IntConsumer onAttendanceCount;
    private final Runnable onSwitchToSchedules;

    private final DefaultTableModel schedModel = readOnlyModel(new String[]{"STT", "Mã NV", "Nhân viên"});
    private final DefaultTableModel attModel = readOnlyModel(new String[]{"STT", "Mã NV", "Nhân viên"});
    private final JTable tblSchedules = createStyledTable(schedModel);
    private final JTable tblAttendance = createStyledTable(attModel);
    private final JPanel schedulesView;
    private final JPanel attendanceView;

    private List<ScheduleRow> allSchedules = List.of();
    private List<AttendanceRow> allAttendance = List.of();
    private final Map<String, ScheduleRow> scheduleCellMap = new HashMap<>();
    private final List<Integer> scheduleRowEmpIds = new ArrayList<>();
    private final Map<String, AttendanceRow> attendanceCellMap = new HashMap<>();

    private int matrixMonth = LocalDate.now().getMonthValue();
    private int matrixYear = LocalDate.now().getYear();
    private String lastKw = "";
    private String lastDept = "Tất cả khoa";

    public NhanSuLichChamCongPanel(
            NhanSuReadPort readPort,
            Supplier<List<EmployeeRow>> employeesSupplier,
            Consumer<String> onStatus,
            IntConsumer onAttendanceCount,
            Runnable onSwitchToSchedules) {
        this.readPort = Objects.requireNonNull(readPort, "readPort");
        this.employeesSupplier = Objects.requireNonNull(employeesSupplier, "employeesSupplier");
        this.onStatus = onStatus;
        this.onAttendanceCount = onAttendanceCount;
        this.onSwitchToSchedules = onSwitchToSchedules;

        this.schedulesView = wrapMatrixWithLegend(tblSchedules,
                "Ký hiệu ca:   [S] Ca sáng   [C] Ca chiều   [HC] Hành chính   [Đ] Trực đêm     • Nhấp đúp ô ngày để xếp hoặc xóa ca trực");
        this.attendanceView = wrapMatrixWithLegend(tblAttendance,
                "Ký hiệu công:   [✓] Đúng giờ   [M] Đi muộn   [S] Về sớm   [MS] Muộn & Sớm   [P] Nghỉ phép     • Nhấp đúp ô ngày để điều chỉnh giờ công");

        initInteractions();
    }

    private void initInteractions() {
        tblSchedules.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = tblSchedules.rowAtPoint(e.getPoint());
                    int col = tblSchedules.columnAtPoint(e.getPoint());
                    if (row >= 0 && col >= 3) {
                        int day = col - 2;
                        int days = YearMonth.of(matrixYear, matrixMonth).lengthOfMonth();
                        if (day >= 1 && day <= days) {
                            ScheduleRow existing = scheduleCellMap.get(row + ":" + day);
                            if (existing != null) {
                                deleteScheduleRow(existing);
                            } else if (row < scheduleRowEmpIds.size()) {
                                openScheduleDialogFor(scheduleRowEmpIds.get(row), LocalDate.of(matrixYear, matrixMonth, day));
                            }
                        }
                    }
                }
            }
        });

        JPopupMenu schedMenu = new JPopupMenu();
        addMenuItem(schedMenu, "Xếp Lịch Làm Việc Mới", () -> openScheduleDialogFor(null, LocalDate.now()));
        addMenuItem(schedMenu, "Xóa Lịch Trực Ô Đang Chọn", this::deleteSelectedSchedule);
        tblSchedules.setComponentPopupMenu(schedMenu);

        JPopupMenu attMenu = new JPopupMenu();
        addMenuItem(attMenu, "Điều Chỉnh Giờ Check-in / Check-out", this::openAdjustAttendanceDialog);
        tblAttendance.setComponentPopupMenu(attMenu);
        tblAttendance.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    openAdjustAttendanceDialog();
                }
            }
        });
    }

    public JPanel getSchedulesView() { return schedulesView; }
    public JPanel getAttendanceView() { return attendanceView; }
    public JTable getTblSchedules() { return tblSchedules; }
    public JTable getTblAttendance() { return tblAttendance; }

    public void loadSchedules(int month, int year, String kw, String dept) {
        this.matrixMonth = month;
        this.matrixYear = year;
        this.lastKw = kw;
        this.lastDept = dept;
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null) return;
        new SwingWorker<List<ScheduleRow>, Void>() {
            @Override protected List<ScheduleRow> doInBackground() { return readPort.listSchedules(auth.facilityId(), month, year); }
            @Override protected void done() {
                try {
                    allSchedules = get();
                    renderSchedules(month, year, lastKw, lastDept);
                } catch (Exception ex) {
                    if (onStatus != null) onStatus.accept("Không thể tải lịch làm việc: " + ex.getMessage());
                }
            }
        }.execute();
    }

    public void loadAttendance(int month, int year, String kw, String dept) {
        this.matrixMonth = month;
        this.matrixYear = year;
        this.lastKw = kw;
        this.lastDept = dept;
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null) return;
        new SwingWorker<List<AttendanceRow>, Void>() {
            @Override protected List<AttendanceRow> doInBackground() { return readPort.listAttendance(auth.facilityId(), month, year); }
            @Override protected void done() {
                try {
                    allAttendance = get();
                    if (onAttendanceCount != null) onAttendanceCount.accept(allAttendance.size());
                    renderAttendance(month, year, lastKw, lastDept);
                } catch (Exception ex) {
                    if (onStatus != null) onStatus.accept("Không thể tải chấm công: " + ex.getMessage());
                }
            }
        }.execute();
    }

    public void renderSchedules(int m, int y, String kw, String selectedDept) {
        this.matrixMonth = m;
        this.matrixYear = y;
        this.lastKw = kw;
        this.lastDept = selectedDept;
        int days = YearMonth.of(y, m).lengthOfMonth();

        List<String> cols = new ArrayList<>();
        cols.add("STT");
        cols.add("Mã NV");
        cols.add("Nhân viên");
        for (int d = 1; d <= days; d++) cols.add(String.format("%02d", d));
        cols.add("Tổng ca");
        schedModel.setColumnIdentifiers(cols.toArray());
        schedModel.setRowCount(0);
        scheduleCellMap.clear();
        scheduleRowEmpIds.clear();

        Map<Integer, List<ScheduleRow>> byEmp = new LinkedHashMap<>();
        Map<Integer, String> empCodeMap = new HashMap<>();
        Map<Integer, String> empNameMap = new HashMap<>();
        for (EmployeeRow e : employeesSupplier.get()) {
            if (!e.active()) continue;
            if (selectedDept != null && !"Tất cả khoa".equals(selectedDept) && !e.departments().contains(selectedDept)) continue;
            String hay = (e.maNhanVien() + " " + e.fullName() + " " + e.departments()).toLowerCase(Locale.ROOT);
            if (!kw.isBlank() && !hay.contains(kw)) continue;
            byEmp.put(e.id(), new ArrayList<>());
            empCodeMap.put(e.id(), e.maNhanVien());
            empNameMap.put(e.id(), e.fullName());
        }
        for (ScheduleRow s : allSchedules) {
            if (selectedDept != null && !"Tất cả khoa".equals(selectedDept) && !s.departmentName().equals(selectedDept)) continue;
            String hay = (s.maNhanVien() + " " + s.employeeName() + " " + s.shiftName() + " " + s.departmentName() + " " + s.roomName()).toLowerCase(Locale.ROOT);
            if (!kw.isBlank() && !hay.contains(kw)) continue;
            byEmp.computeIfAbsent(s.employeeId(), k -> new ArrayList<>()).add(s);
            empCodeMap.putIfAbsent(s.employeeId(), s.maNhanVien());
            empNameMap.putIfAbsent(s.employeeId(), s.employeeName());
        }

        int rowIdx = 0;
        for (var entry : byEmp.entrySet()) {
            int empId = entry.getKey();
            scheduleRowEmpIds.add(empId);
            List<ScheduleRow> rows = entry.getValue();
            Object[] rowData = new Object[3 + days + 1];
            rowData[0] = rowIdx + 1;
            rowData[1] = empCodeMap.getOrDefault(empId, "");
            rowData[2] = empNameMap.getOrDefault(empId, "");
            int count = 0;
            for (ScheduleRow s : rows) {
                if (s.workDate() == null) continue;
                int day = s.workDate().getDayOfMonth();
                if (day < 1 || day > days) continue;
                scheduleCellMap.put(rowIdx + ":" + day, s);
                rowData[2 + day] = shiftBadge(s.shiftName());
                count++;
            }
            rowData[3 + days] = count;
            schedModel.addRow(rowData);
            rowIdx++;
        }
        configureMatrixTable(tblSchedules, days, 1, false);
    }

    public void renderAttendance(int m, int y, String kw, String selectedDept) {
        this.matrixMonth = m;
        this.matrixYear = y;
        this.lastKw = kw;
        this.lastDept = selectedDept;
        int days = YearMonth.of(y, m).lengthOfMonth();

        List<String> cols = new ArrayList<>();
        cols.add("STT");
        cols.add("Mã NV");
        cols.add("Nhân viên");
        for (int d = 1; d <= days; d++) cols.add(String.format("%02d", d));
        cols.add("Đúng giờ");
        cols.add("Muộn/Sớm");
        cols.add("Tổng giờ");
        attModel.setColumnIdentifiers(cols.toArray());
        attModel.setRowCount(0);
        attendanceCellMap.clear();

        List<EmployeeRow> allEmployees = employeesSupplier.get();
        Map<String, List<AttendanceRow>> byEmp = new LinkedHashMap<>();
        for (EmployeeRow e : allEmployees) {
            if (!e.active()) continue;
            if (selectedDept != null && !"Tất cả khoa".equals(selectedDept) && !e.departments().contains(selectedDept)) continue;
            String hay = (e.maNhanVien() + " " + e.fullName() + " " + e.departments()).toLowerCase(Locale.ROOT);
            if (!kw.isBlank() && !hay.contains(kw)) continue;
            byEmp.put(e.maNhanVien(), new ArrayList<>());
        }
        for (AttendanceRow r : allAttendance) {
            String hay = (r.maNhanVien() + " " + r.fullName() + " " + r.shiftName() + " " + r.status()).toLowerCase(Locale.ROOT);
            if (!kw.isBlank() && !hay.contains(kw)) continue;
            byEmp.computeIfAbsent(r.maNhanVien(), k -> new ArrayList<>()).add(r);
        }

        int rowIdx = 0;
        for (var entry : byEmp.entrySet()) {
            String maNv = entry.getKey();
            List<AttendanceRow> rows = entry.getValue();
            String fullName = rows.isEmpty()
                    ? allEmployees.stream().filter(e -> e.maNhanVien().equals(maNv)).map(EmployeeRow::fullName).findFirst().orElse(maNv)
                    : rows.get(0).fullName();
            Object[] rowData = new Object[3 + days + 3];
            rowData[0] = rowIdx + 1;
            rowData[1] = maNv;
            rowData[2] = fullName;
            int onTime = 0, lateOrEarly = 0;
            BigDecimal totalHours = BigDecimal.ZERO;
            for (AttendanceRow r : rows) {
                if (r.workDate() == null) continue;
                int day = r.workDate().getDayOfMonth();
                if (day < 1 || day > days) continue;
                attendanceCellMap.put(rowIdx + ":" + day, r);
                rowData[2 + day] = attendanceBadge(r.status());
                if ("DUNG_GIO".equals(r.status())) onTime++;
                else if ("DI_MUON".equals(r.status()) || "VE_SOM".equals(r.status()) || "DI_MUON_VE_SOM".equals(r.status())) lateOrEarly++;
                if (r.hoursWorked() != null) totalHours = totalHours.add(r.hoursWorked());
            }
            rowData[3 + days] = onTime;
            rowData[4 + days] = lateOrEarly;
            rowData[5 + days] = totalHours;
            attModel.addRow(rowData);
            rowIdx++;
        }
        configureMatrixTable(tblAttendance, days, 3, true);
    }

    public void recordAttendance(Integer targetEmployeeId, boolean checkOut) {
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null) return;
        int empId = targetEmployeeId != null ? targetEmployeeId : auth.employeeId();
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                try { readPort.recordCheckInOrOut(empId, auth.facilityId(), checkOut, auth.employeeId()); return null; }
                catch (Exception ex) { return ex.getMessage(); }
            }
            @Override protected void done() {
                try {
                    String err = get();
                    if (err != null && !GraphicsEnvironment.isHeadless()) {
                        JOptionPane.showMessageDialog(attendanceView, err, "Chấm công", JOptionPane.ERROR_MESSAGE);
                    } else {
                        loadAttendance(matrixMonth, matrixYear, lastKw, lastDept);
                        loadSchedules(matrixMonth, matrixYear, lastKw, lastDept);
                        if (onStatus != null) onStatus.accept(checkOut ? "Đã ghi nhận Check-out." : "Đã ghi nhận Check-in.");
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void openScheduleDialogFor(Integer preselectEmployeeId, LocalDate preselectDate) {
        if (GraphicsEnvironment.isHeadless()) return;
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null) return;
        List<EmployeeRow> allEmployees = employeesSupplier.get();
        List<ShiftOption> shifts = readPort.listShifts();
        List<DepartmentOption> depts = readPort.listDepartments(auth.facilityId());
        List<RoomOption> rooms = readPort.listRooms(auth.facilityId());
        if (allEmployees.isEmpty() || shifts.isEmpty()) {
            JOptionPane.showMessageDialog(schedulesView, "Cần có danh sách nhân viên và ca trực hoạt động.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JComboBox<EmployeeRow> cboEmp = new JComboBox<>(allEmployees.toArray(new EmployeeRow[0]));
        cboEmp.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof EmployeeRow e) setText(e.maNhanVien() + " - " + e.fullName());
                return this;
            }
        });
        if (preselectEmployeeId != null) {
            for (EmployeeRow e : allEmployees) {
                if (e.id() == preselectEmployeeId) { cboEmp.setSelectedItem(e); break; }
            }
        }
        JComboBox<ShiftOption> cboShift = new JComboBox<>(shifts.toArray(new ShiftOption[0]));
        JComboBox<DepartmentOption> cboDept = new JComboBox<>();
        cboDept.addItem(new DepartmentOption(0, "(Không chọn khoa)"));
        for (DepartmentOption d : depts) cboDept.addItem(d);
        JComboBox<RoomOption> cboRoom = new JComboBox<>();
        cboRoom.addItem(new RoomOption(0, 0, "(Không chọn phòng)"));
        for (RoomOption r : rooms) cboRoom.addItem(r);
        LocalDate baseDate = preselectDate != null ? preselectDate : LocalDate.now();
        JTextField txtFromDate = new JTextField(baseDate.format(DATE_FMT), 12);
        JTextField txtToDate = new JTextField(baseDate.format(DATE_FMT), 12);
        JCheckBox chkSkipSun = new JCheckBox("Bỏ qua Chủ Nhật", true);
        JTextField txtNote = new JTextField(16);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.add(new JLabel("Nhân viên (*):")); form.add(cboEmp);
        form.add(new JLabel("Ca trực (*):")); form.add(cboShift);
        form.add(new JLabel("Từ ngày (dd/MM/yyyy):")); form.add(txtFromDate);
        form.add(new JLabel("Đến ngày (dd/MM/yyyy):")); form.add(txtToDate);
        form.add(new JLabel("Khoa:")); form.add(cboDept);
        form.add(new JLabel("Phòng khám:")); form.add(cboRoom);
        form.add(new JLabel("Ghi chú:")); form.add(txtNote);
        form.add(new JLabel("")); form.add(chkSkipSun);

        if (JOptionPane.showConfirmDialog(schedulesView, form, "Xếp Lịch Làm Việc / Hàng Loạt", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            LocalDate fromDate = LocalDate.parse(txtFromDate.getText().trim(), DATE_FMT);
            LocalDate toDate = LocalDate.parse(txtToDate.getText().trim(), DATE_FMT);
            EmployeeRow emp = (EmployeeRow) cboEmp.getSelectedItem();
            ShiftOption shift = (ShiftOption) cboShift.getSelectedItem();
            DepartmentOption dept = (DepartmentOption) cboDept.getSelectedItem();
            RoomOption room = (RoomOption) cboRoom.getSelectedItem();
            Integer deptId = dept != null && dept.id() > 0 ? dept.id() : null;
            Integer roomId = room != null && room.id() > 0 ? room.id() : null;
            boolean skipSun = chkSkipSun.isSelected();
            new SwingWorker<Integer, Void>() {
                private String error;
                @Override protected Integer doInBackground() {
                    try {
                        return readPort.createScheduleBatch(emp.id(), shift.id(), auth.facilityId(), deptId, roomId,
                                fromDate, toDate, skipSun, txtNote.getText().trim(), auth.employeeId());
                    } catch (Exception ex) {
                        error = ex.getMessage();
                        return 0;
                    }
                }
                @Override protected void done() {
                    try {
                        int count = get();
                        if (error != null) JOptionPane.showMessageDialog(schedulesView, error, "Lỗi", JOptionPane.ERROR_MESSAGE);
                        else {
                            loadSchedules(matrixMonth, matrixYear, lastKw, lastDept);
                            if (onSwitchToSchedules != null) onSwitchToSchedules.run();
                            if (onStatus != null) onStatus.accept("Đã xếp " + count + " ca trực cho " + emp.fullName());
                        }
                    } catch (Exception ignored) {}
                }
            }.execute();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(schedulesView, "Ngày làm việc không đúng định dạng dd/MM/yyyy.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void openShiftManager() {
        if (GraphicsEnvironment.isHeadless()) return;
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null) return;
        CaTrucQuanLyHopThoai.showDialog(schedulesView, readPort, auth.employeeId());
    }

    public void printMatrix(boolean isAttendance) {
        if (GraphicsEnvironment.isHeadless()) return;
        JTable target = isAttendance ? tblAttendance : tblSchedules;
        String title = isAttendance ? "BẢNG CHẤM CÔNG THÁNG " + matrixMonth + "/" + matrixYear
                : "BẢNG PHÂN CÔNG LỊCH LÀM VIỆC THÁNG " + matrixMonth + "/" + matrixYear;
        String subtitle = "Khoa: " + lastDept + "  •  Tổng số nhân sự: " + target.getRowCount();
        PhieuLuongInHopThoai.showTablePrintDialog(SwingUtilities.getWindowAncestor(target), title, subtitle, target);
    }

    public void deleteSelectedSchedule() {
        if (GraphicsEnvironment.isHeadless()) return;
        int row = tblSchedules.getSelectedRow();
        int col = tblSchedules.getSelectedColumn();
        if (row < 0 || col < 3) {
            JOptionPane.showMessageDialog(schedulesView, "Vui lòng chọn ô ngày có ca trực muốn xóa.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        ScheduleRow s = scheduleCellMap.get(row + ":" + (col - 2));
        if (s == null) {
            JOptionPane.showMessageDialog(schedulesView, "Ô ngày đang chọn chưa có ca trực.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        deleteScheduleRow(s);
    }

    private void deleteScheduleRow(ScheduleRow s) {
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null) return;
        if (JOptionPane.showConfirmDialog(schedulesView,
                "Ca trực: " + s.shiftName() + " (" + s.startTime() + " - " + s.endTime() + ")\n"
                        + "Nhân viên: " + s.employeeName() + " | Ngày: " + s.workDate().format(DATE_FMT)
                        + "\nTrạng thái: " + s.status() + "\n\nBạn có muốn XÓA lịch trực này không?",
                "Chi Tiết / Xóa Lịch Trực", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                try { readPort.deleteSchedule(s.id(), auth.employeeId()); return null; }
                catch (Exception ex) { return ex.getMessage(); }
            }
            @Override protected void done() {
                try {
                    String err = get();
                    if (err != null) JOptionPane.showMessageDialog(schedulesView, err, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    else { loadSchedules(matrixMonth, matrixYear, lastKw, lastDept); if (onStatus != null) onStatus.accept("Đã xóa lịch trực."); }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void openAdjustAttendanceDialog() {
        if (GraphicsEnvironment.isHeadless()) return;
        var auth = KhamBenhPhienNguCanh.current();
        int row = tblAttendance.getSelectedRow();
        int col = tblAttendance.getSelectedColumn();
        if (auth == null || row < 0 || col < 3) {
            JOptionPane.showMessageDialog(attendanceView, "Vui lòng chọn một ô ngày đã có chấm công.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        AttendanceRow att = attendanceCellMap.get(row + ":" + (col - 2));
        if (att == null) {
            JOptionPane.showMessageDialog(attendanceView, "Ngày đang chọn chưa có dữ liệu chấm công.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JTextField txtIn = new JTextField(att.checkIn() != null ? att.checkIn().format(DT_FMT) : "", 16);
        JTextField txtOut = new JTextField(att.checkOut() != null ? att.checkOut().format(DT_FMT) : "", 16);
        JComboBox<String> cboStatus = new JComboBox<>(new String[]{"DUNG_GIO", "DI_MUON", "VE_SOM", "DI_MUON_VE_SOM", "NGHI_PHEP"});
        cboStatus.setSelectedItem(att.status());

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.add(new JLabel("Nhân viên:")); form.add(new JLabel(att.fullName()));
        form.add(new JLabel("Giờ vào (HH:mm dd/MM/yyyy):")); form.add(txtIn);
        form.add(new JLabel("Giờ ra (HH:mm dd/MM/yyyy):")); form.add(txtOut);
        form.add(new JLabel("Trạng thái:")); form.add(cboStatus);

        if (JOptionPane.showConfirmDialog(attendanceView, form, "Điều Chỉnh Giờ Công", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            LocalDateTime inDt = txtIn.getText().trim().isEmpty() ? null : LocalDateTime.parse(txtIn.getText().trim(), DT_FMT);
            LocalDateTime outDt = txtOut.getText().trim().isEmpty() ? null : LocalDateTime.parse(txtOut.getText().trim(), DT_FMT);
            String nextStatus = Objects.toString(cboStatus.getSelectedItem(), "DUNG_GIO");
            new SwingWorker<String, Void>() {
                @Override protected String doInBackground() {
                    try { readPort.adjustAttendance(att.id(), inDt, outDt, nextStatus, auth.employeeId()); return null; }
                    catch (Exception ex) { return ex.getMessage(); }
                }
                @Override protected void done() {
                    try {
                        String err = get();
                        if (err != null) JOptionPane.showMessageDialog(attendanceView, err, "Lỗi", JOptionPane.ERROR_MESSAGE);
                        else { loadAttendance(matrixMonth, matrixYear, lastKw, lastDept); if (onStatus != null) onStatus.accept("Đã điều chỉnh giờ công cho " + att.fullName()); }
                    } catch (Exception ignored) {}
                }
            }.execute();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(attendanceView, "Định dạng ngày giờ phải là HH:mm dd/MM/yyyy.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel wrapMatrixWithLegend(JTable tbl, String legendText) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        JLabel legend = new JLabel(legendText);
        legend.setFont(UngDungPhongChu.SMALL);
        legend.setForeground(new Color(65, 80, 95));
        legend.setBackground(new Color(245, 248, 252));
        legend.setOpaque(true);
        legend.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 228, 238)),
                new EmptyBorder(4, 8, 4, 8)));
        p.add(legend, BorderLayout.NORTH);
        p.add(scrollOf(tbl), BorderLayout.CENTER);
        return p;
    }

    private void configureMatrixTable(JTable tbl, int days, int summaryCols, boolean isAttendance) {
        tbl.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tbl.setCellSelectionEnabled(true);
        tbl.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tbl.setShowGrid(true);
        tbl.setGridColor(new Color(230, 234, 240));
        var cm = tbl.getColumnModel();
        if (cm.getColumnCount() < 3 + days + summaryCols) return;
        cm.getColumn(0).setPreferredWidth(38);
        cm.getColumn(1).setPreferredWidth(72);
        cm.getColumn(2).setPreferredWidth(155);
        for (int d = 1; d <= days; d++) cm.getColumn(2 + d).setPreferredWidth(32);
        for (int s = 0; s < summaryCols; s++) cm.getColumn(3 + days + s).setPreferredWidth(68);
        var renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                lbl.setHorizontalAlignment(col == 2 ? SwingConstants.LEFT : SwingConstants.CENTER);
                lbl.setToolTipText(null);
                if (!isSelected) {
                    lbl.setBackground(Color.WHITE);
                    lbl.setForeground(new Color(30, 35, 45));
                    if (col >= 3 && col < 3 + days) {
                        int day = col - 2;
                        boolean sunday = LocalDate.of(matrixYear, matrixMonth, day).getDayOfWeek() == DayOfWeek.SUNDAY;
                        String text = Objects.toString(value, "");
                        if (!text.isEmpty()) {
                            lbl.setFont(UngDungPhongChu.SMALL.deriveFont(Font.BOLD));
                            applyMatrixBadgeColors(lbl, text, isAttendance);
                        } else if (sunday) {
                            lbl.setBackground(new Color(243, 244, 247));
                        }
                        if (isAttendance) {
                            AttendanceRow att = attendanceCellMap.get(row + ":" + day);
                            if (att != null) {
                                String inStr = att.checkIn() != null ? att.checkIn().format(DT_FMT) : "-";
                                String outStr = att.checkOut() != null ? att.checkOut().format(DT_FMT) : "-";
                                lbl.setToolTipText(att.fullName() + " | " + att.shiftName() + " | Vào: " + inStr + " | Ra: " + outStr + " | " + att.hoursWorked() + "h");
                            }
                        } else {
                            ScheduleRow sch = scheduleCellMap.get(row + ":" + day);
                            if (sch != null) {
                                lbl.setToolTipText(sch.employeeName() + " | " + sch.shiftName() + " (" + sch.startTime() + "-" + sch.endTime() + ") | " + sch.departmentName() + " - " + sch.roomName());
                            }
                        }
                    } else if (col >= 3 + days) {
                        lbl.setFont(UngDungPhongChu.BODY_BOLD);
                        lbl.setBackground(new Color(248, 250, 253));
                    }
                }
                return lbl;
            }
        };
        for (int c = 0; c < cm.getColumnCount(); c++) cm.getColumn(c).setCellRenderer(renderer);
    }

    private static void applyMatrixBadgeColors(JLabel lbl, String badge, boolean isAttendance) {
        if (isAttendance) {
            switch (badge) {
                case "✓" -> { lbl.setBackground(new Color(232, 245, 233)); lbl.setForeground(new Color(27, 94, 32)); }
                case "M" -> { lbl.setBackground(new Color(255, 243, 224)); lbl.setForeground(new Color(230, 81, 0)); }
                case "S" -> { lbl.setBackground(new Color(255, 253, 231)); lbl.setForeground(new Color(245, 127, 23)); }
                case "MS" -> { lbl.setBackground(new Color(255, 235, 238)); lbl.setForeground(new Color(183, 28, 28)); }
                default -> { lbl.setBackground(new Color(236, 239, 241)); lbl.setForeground(new Color(69, 90, 100)); }
            }
        } else {
            switch (badge) {
                case "S" -> { lbl.setBackground(new Color(227, 242, 253)); lbl.setForeground(new Color(13, 71, 161)); }
                case "C" -> { lbl.setBackground(new Color(255, 248, 225)); lbl.setForeground(new Color(230, 81, 0)); }
                case "HC" -> { lbl.setBackground(new Color(232, 245, 233)); lbl.setForeground(new Color(27, 94, 32)); }
                default -> { lbl.setBackground(new Color(243, 229, 245)); lbl.setForeground(new Color(74, 20, 140)); }
            }
        }
    }

    private static String shiftBadge(String shiftName) {
        if (shiftName == null) return "Ca";
        String low = shiftName.toLowerCase(Locale.ROOT);
        if (low.contains("sáng")) return "S";
        if (low.contains("chiều")) return "C";
        if (low.contains("hành chính")) return "HC";
        if (low.contains("đêm") || low.contains("trực")) return "Đ";
        return shiftName.length() > 3 ? shiftName.substring(0, 3) : shiftName;
    }

    private static String attendanceBadge(String status) {
        if (status == null) return "✓";
        return switch (status) {
            case "DI_MUON" -> "M";
            case "VE_SOM" -> "S";
            case "DI_MUON_VE_SOM" -> "MS";
            case "NGHI_PHEP" -> "P";
            default -> "✓";
        };
    }
}
