package vn.clinicmanagement.desktop.modules.hr.components;

import vn.clinicmanagement.application.hr.NhanSuReadPort.EmployeeRow;
import vn.clinicmanagement.application.hr.NhanSuReadPort.PositionOption;
import vn.clinicmanagement.application.hr.NhanSuReadPort.RoleOption;
import vn.clinicmanagement.application.hr.assign.AssignNhanVienVaiTroCommand;
import vn.clinicmanagement.application.hr.create.CreateNhanVienCommand;
import vn.clinicmanagement.application.hr.update.UpdateNhanVienTrangThaiCommand;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;
import vn.clinicmanagement.desktop.modules.hr.CapNhatNhanVienHopThoai;
import vn.clinicmanagement.desktop.modules.hr.MaTranPhanQuyenHopThoai;
import vn.clinicmanagement.desktop.modules.hr.NhanSuGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.hr.components.NhanSuBangHoTro.ViewSection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static vn.clinicmanagement.desktop.modules.hr.components.NhanSuBangHoTro.*;

/**
 * Panel quản lý danh sách hồ sơ nhân viên, phân quyền và thao tác tài khoản.
 */
public final class NhanSuHoSoPanel extends JPanel {
    private final NhanSuGhepNoiGoc.Services services;
    private final DefaultTableModel empModel = readOnlyModel(new String[]{
            "STT", "Mã NV", "Họ và tên", "Tài khoản", "Chức vụ", "SĐT", "Email", "Vai trò", "Khoa phòng", "Trạng thái"});
    private final JTable tblEmployees = createStyledTable(empModel);

    private List<EmployeeRow> allEmployees = List.of();
    private List<EmployeeRow> displayedEmployees = List.of();

    private final Consumer<String> onStatus;
    private final BiConsumer<Integer, Long> onEmployeeCounts;
    private final Runnable onScheduleNeeded;
    private final BiConsumer<Integer, Boolean> onRecordAttendanceForEmp;
    private final Runnable onEmployeesReloaded;

    public NhanSuHoSoPanel(
            NhanSuGhepNoiGoc.Services services,
            Consumer<String> onStatus,
            BiConsumer<Integer, Long> onEmployeeCounts,
            Runnable onScheduleNeeded,
            BiConsumer<Integer, Boolean> onRecordAttendanceForEmp,
            Runnable onEmployeesReloaded) {
        super(new BorderLayout());
        this.services = Objects.requireNonNull(services, "services");
        this.onStatus = onStatus;
        this.onEmployeeCounts = onEmployeeCounts;
        this.onScheduleNeeded = onScheduleNeeded;
        this.onRecordAttendanceForEmp = onRecordAttendanceForEmp;
        this.onEmployeesReloaded = onEmployeesReloaded;

        JPopupMenu empMenu = new JPopupMenu();
        addMenuItem(empMenu, "Cập Nhật Hồ Sơ Chi Tiết", this::editSelectedEmployee);
        addMenuItem(empMenu, "Phân Vai Trò", this::openAssignRolesDialog);
        addMenuItem(empMenu, "Đặt Lại Mật Khẩu", this::openResetPasswordDialog);
        addMenuItem(empMenu, "Xếp Lịch Làm Việc", () -> {
            if (onScheduleNeeded != null) onScheduleNeeded.run();
        });
        empMenu.addSeparator();
        addMenuItem(empMenu, "Check-in Hôm Nay", () -> triggerAttendanceForSelected(false));
        addMenuItem(empMenu, "Check-out Hôm Nay", () -> triggerAttendanceForSelected(true));
        empMenu.addSeparator();
        addMenuItem(empMenu, "Khóa / Mở Khóa Tài Khoản", this::toggleSelectedEmployeeStatus);
        tblEmployees.setComponentPopupMenu(empMenu);

        tblEmployees.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int viewRow = tblEmployees.rowAtPoint(e.getPoint());
                    if (viewRow < 0) viewRow = tblEmployees.getSelectedRow();
                    if (viewRow >= 0) {
                        int modelRow = tblEmployees.convertRowIndexToModel(viewRow);
                        if (modelRow >= 0 && modelRow < displayedEmployees.size()) {
                            openEditEmployeeDialog(displayedEmployees.get(modelRow).id());
                        }
                    }
                }
            }
        });

        add(scrollOf(tblEmployees), BorderLayout.CENTER);
    }

    public JTable getTable() { return tblEmployees; }
    public List<EmployeeRow> getAllEmployees() { return allEmployees; }

    public Integer getSelectedEmployeeId() {
        int viewRow = tblEmployees.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = tblEmployees.convertRowIndexToModel(viewRow);
        if (modelRow >= 0 && modelRow < displayedEmployees.size()) {
            return displayedEmployees.get(modelRow).id();
        }
        return null;
    }

    public void loadEmployees(ViewSection activeSection, String kw, String selectedDept) {
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null) return;
        new SwingWorker<List<EmployeeRow>, Void>() {
            @Override protected List<EmployeeRow> doInBackground() { return services.readPort().listEmployees(auth.facilityId()); }
            @Override protected void done() {
                try {
                    allEmployees = get();
                    if (onEmployeeCounts != null) {
                        onEmployeeCounts.accept(allEmployees.size(), allEmployees.stream().filter(EmployeeRow::active).count());
                    }
                    filterEmployees(activeSection, kw, selectedDept);
                    if (onEmployeesReloaded != null) onEmployeesReloaded.run();
                    if (onStatus != null) onStatus.accept("Đã tải " + allEmployees.size() + " nhân viên.");
                } catch (Exception ex) {
                    if (onStatus != null) onStatus.accept("Không thể tải danh sách nhân viên: " + ex.getMessage());
                }
            }
        }.execute();
    }

    public void filterEmployees(ViewSection activeSection, String kw, String selectedDept) {
        empModel.setRowCount(0);
        List<EmployeeRow> filtered = new ArrayList<>();
        int stt = 1;
        for (EmployeeRow e : allEmployees) {
            if (activeSection == ViewSection.ACTIVE_EMPLOYEES && !e.active()) continue;
            if (activeSection == ViewSection.LOCKED_EMPLOYEES && e.active()) continue;
            if (selectedDept != null && !"Tất cả khoa".equals(selectedDept) && !e.departments().contains(selectedDept)) continue;
            String hay = (e.maNhanVien() + " " + e.fullName() + " " + e.username() + " " + e.roles() + " " + e.departments()).toLowerCase(Locale.ROOT);
            if (!kw.isBlank() && !hay.contains(kw)) continue;
            filtered.add(e);
            empModel.addRow(new Object[]{
                    stt++, e.maNhanVien(), e.fullName(), e.username(), e.positionName(),
                    e.phone(), e.email(), e.roles(), e.departments(),
                    e.active() ? "Đang hoạt động" : "Đã khóa"
            });
        }
        displayedEmployees = List.copyOf(filtered);
    }

    public void openCreateEmployeeDialog(Runnable afterSave) {
        if (GraphicsEnvironment.isHeadless()) return;
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null) return;
        JTextField txtCode = new JTextField(14), txtHo = new JTextField(14), txtTen = new JTextField(14);
        JTextField txtPhone = new JTextField(14), txtEmail = new JTextField(14), txtUser = new JTextField(14);
        JPasswordField txtPass = new JPasswordField(14);
        JComboBox<PositionOption> cboPos = new JComboBox<>();
        for (PositionOption pos : services.readPort().listPositions()) cboPos.addItem(pos);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.add(new JLabel("Mã nhân viên (*):")); form.add(txtCode);
        form.add(new JLabel("Họ (*):")); form.add(txtHo);
        form.add(new JLabel("Tên (*):")); form.add(txtTen);
        form.add(new JLabel("Số điện thoại:")); form.add(txtPhone);
        form.add(new JLabel("Email:")); form.add(txtEmail);
        form.add(new JLabel("Chức vụ:")); form.add(cboPos);
        form.add(new JLabel("Tên đăng nhập (*):")); form.add(txtUser);
        form.add(new JLabel("Mật khẩu (* >=6 ký tự):")); form.add(txtPass);

        if (JOptionPane.showConfirmDialog(this, form, "Thêm Nhân Viên Mới", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        PositionOption pos = (PositionOption) cboPos.getSelectedItem();
        var cmd = new CreateNhanVienCommand(auth.employeeId(), txtCode.getText().trim(), txtHo.getText().trim(),
                txtTen.getText().trim(), txtPhone.getText().trim(), txtEmail.getText().trim(),
                pos != null ? pos.id() : null, txtUser.getText().trim(), txtPass.getPassword());
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                var res = services.createEmployee().handle(cmd);
                return res.isSuccess() ? null : res.getError().map(err -> err.message()).orElse("Không thể tạo nhân viên.");
            }
            @Override protected void done() {
                try {
                    String err = get();
                    if (err != null) JOptionPane.showMessageDialog(NhanSuHoSoPanel.this, err, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    else { if (afterSave != null) afterSave.run(); if (onStatus != null) onStatus.accept("Đã thêm nhân viên mới."); }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void editSelectedEmployee() {
        if (GraphicsEnvironment.isHeadless()) return;
        Integer id = getSelectedEmployeeId();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một nhân viên trong bảng.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        openEditEmployeeDialog(id);
    }

    public void openEditEmployeeDialog(int employeeId) {
        if (GraphicsEnvironment.isHeadless()) return;
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null) return;
        CapNhatNhanVienHopThoai.showDialog(
                this,
                services.readPort(),
                employeeId,
                auth.facilityId(),
                auth.employeeId(),
                onEmployeesReloaded);
    }

    public CapNhatNhanVienHopThoai createEmployeeEditPanel(int employeeId) {
        var auth = KhamBenhPhienNguCanh.current();
        int facilityId = auth != null ? auth.facilityId() : 1;
        int actorId = auth != null ? auth.employeeId() : 1;
        return new CapNhatNhanVienHopThoai(
                services.readPort(),
                employeeId,
                facilityId,
                actorId,
                onEmployeesReloaded);
    }

    public void openAssignRolesDialog() {
        if (GraphicsEnvironment.isHeadless()) return;
        int row = tblEmployees.getSelectedRow();
        var auth = KhamBenhPhienNguCanh.current();
        if (row < 0 || row >= displayedEmployees.size() || auth == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một nhân viên trong bảng.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        EmployeeRow emp = displayedEmployees.get(row);
        List<RoleOption> roles = services.readPort().listRoles();
        List<JCheckBox> checks = new ArrayList<>();
        JPanel p = new JPanel(new GridLayout(0, 1, 4, 4));
        p.add(new JLabel("Chọn vai trò cho: " + emp.fullName()));
        for (RoleOption r : roles) {
            JCheckBox cb = new JCheckBox(r.toString(), emp.roles().contains(r.name()));
            cb.putClientProperty("roleId", r.id());
            checks.add(cb);
            p.add(cb);
        }
        if (JOptionPane.showConfirmDialog(this, p, "Phân Vai Trò Nhân Viên", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        List<Integer> roleIds = new ArrayList<>();
        for (JCheckBox cb : checks) if (cb.isSelected()) roleIds.add((Integer) cb.getClientProperty("roleId"));
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                var res = services.assignRoles().handle(new AssignNhanVienVaiTroCommand(auth.employeeId(), emp.id(), roleIds));
                return res.isSuccess() ? null : res.getError().map(err -> err.message()).orElse("Không thể phân vai trò.");
            }
            @Override protected void done() {
                try {
                    String err = get();
                    if (err != null) JOptionPane.showMessageDialog(NhanSuHoSoPanel.this, err, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    else if (onEmployeesReloaded != null) onEmployeesReloaded.run();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void openResetPasswordDialog() {
        if (GraphicsEnvironment.isHeadless()) return;
        var auth = KhamBenhPhienNguCanh.current();
        int viewRow = tblEmployees.getSelectedRow();
        if (auth == null || viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một nhân viên trong bảng.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = tblEmployees.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= displayedEmployees.size()) return;
        EmployeeRow emp = displayedEmployees.get(modelRow);
        JPasswordField pf = new JPasswordField(16);
        JPanel p = new JPanel(new GridLayout(0, 1, 4, 4));
        p.add(new JLabel("Nhập mật khẩu mới cho " + emp.fullName() + " (tối thiểu 6 ký tự):"));
        p.add(pf);
        if (JOptionPane.showConfirmDialog(this, p, "Đặt Lại Mật Khẩu", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        char[] pw = pf.getPassword();
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                try { services.readPort().resetEmployeePassword(emp.id(), pw, auth.employeeId()); return null; }
                catch (Exception ex) { return ex.getMessage(); }
            }
            @Override protected void done() {
                Arrays.fill(pw, '\0');
                try {
                    String err = get();
                    if (err != null) JOptionPane.showMessageDialog(NhanSuHoSoPanel.this, err, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    else if (onStatus != null) onStatus.accept("Đã đặt lại mật khẩu cho " + emp.fullName());
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    public void openPermissionMatrixDialog() {
        if (GraphicsEnvironment.isHeadless()) return;
        var auth = KhamBenhPhienNguCanh.current();
        if (auth == null) return;
        MaTranPhanQuyenHopThoai.showDialog(this, services.readPort(), auth.employeeId());
    }

    public void toggleSelectedEmployeeStatus() {
        if (GraphicsEnvironment.isHeadless()) return;
        int row = tblEmployees.getSelectedRow();
        var auth = KhamBenhPhienNguCanh.current();
        if (row < 0 || row >= displayedEmployees.size() || auth == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một nhân viên trong bảng.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        EmployeeRow emp = displayedEmployees.get(row);
        boolean nextActive = !emp.active();
        if (JOptionPane.showConfirmDialog(this, (nextActive ? "Mở khóa" : "Khóa") + " tài khoản " + emp.fullName() + "?",
                "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                var res = services.updateStatus().handle(new UpdateNhanVienTrangThaiCommand(auth.employeeId(), emp.id(), nextActive));
                return res.isSuccess() ? null : res.getError().map(err -> err.message()).orElse("Không thể cập nhật trạng thái.");
            }
            @Override protected void done() {
                try {
                    String err = get();
                    if (err != null) JOptionPane.showMessageDialog(NhanSuHoSoPanel.this, err, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    else if (onEmployeesReloaded != null) onEmployeesReloaded.run();
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void triggerAttendanceForSelected(boolean checkOut) {
        Integer empId = getSelectedEmployeeId();
        if (empId != null && onRecordAttendanceForEmp != null) {
            onRecordAttendanceForEmp.accept(empId, checkOut);
        }
    }
}
