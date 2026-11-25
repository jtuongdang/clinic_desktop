package vn.clinicmanagement.desktop.modules.hr;

import vn.clinicmanagement.application.hr.NhanSuReadPort;
import vn.clinicmanagement.application.hr.NhanSuReadPort.DepartmentOption;
import vn.clinicmanagement.application.hr.NhanSuReadPort.EmployeeDetail;
import vn.clinicmanagement.application.hr.NhanSuReadPort.PositionOption;
import vn.clinicmanagement.application.hr.NhanSuReadPort.RoleOption;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

/**
 * Form cập nhật thông tin nhân viên theo phong cách form cập nhật thuốc (EHC).
 */
public class CapNhatNhanVienHopThoai extends JPanel {
    private static final Font TEXT = new Font("Tahoma", Font.PLAIN, 12);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0.##");

    private final NhanSuReadPort readPort;
    private final int employeeId;
    private final int facilityId;
    private final int actorId;
    private final Runnable onSaved;

    private final Map<String, JComponent> fields = new LinkedHashMap<>();
    private final List<JCheckBox> roleBoxes = new ArrayList<>();
    private final List<JCheckBox> deptBoxes = new ArrayList<>();
    private final JPanel rolesPanel = new JPanel(new GridLayout(0, 4, 6, 4));
    private final JPanel deptsPanel = new JPanel(new GridLayout(0, 4, 6, 4));

    private final JPanel form = new JPanel();
    private final JLabel status = new JLabel("Đang tải thông tin nhân viên...");
    private final JButton save = new JButton("▣  Lưu (F2)");

    private List<PositionOption> positions = List.of();
    private boolean loaded;
    private boolean saving;

    public CapNhatNhanVienHopThoai(
            NhanSuReadPort readPort,
            int employeeId,
            int facilityId,
            int actorId,
            Runnable onSaved) {
        this.readPort = Objects.requireNonNull(readPort, "readPort");
        this.employeeId = employeeId;
        this.facilityId = facilityId;
        this.actorId = actorId;
        this.onSaved = onSaved;

        setLayout(new BorderLayout());
        setBackground(new Color(249, 249, 249));

        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(new Color(249, 249, 249));
        form.setBorder(new EmptyBorder(12, 16, 12, 16));

        form.add(section("Thông tin cơ bản nhân viên"));
        form.add(row(
                field("maNhanVien", "Mã nhân viên", "code", 3),
                field("ho", "Họ và tên đệm*", "text", 4),
                field("ten", "Tên*", "text", 3),
                field("gioiTinh", "Giới tính", "gender", 2)
        ));
        form.add(row(
                field("ngaySinh", "Ngày sinh (dd/MM/yyyy)", "date", 3),
                field("soDienThoai", "Số điện thoại", "text", 3),
                field("email", "Email", "text", 3),
                field("diaChi", "Địa chỉ", "text", 4)
        ));

        form.add(Box.createVerticalStrut(6));
        form.add(section("Thông tin công tác, lương & phụ cấp"));
        form.add(row(
                field("username", "Tên đăng nhập*", "text", 3),
                field("matKhauMoi", "Mật khẩu mới (để trống nếu giữ)", "password", 3),
                field("chucVu", "Chức vụ", "position", 3),
                field("chungChiHanhNghe", "Chứng chỉ hành nghề", "text", 3)
        ));
        form.add(row(
                field("ngayVaoLam", "Ngày vào làm (dd/MM/yyyy)", "date", 3),
                field("luongCoBan", "Lương cơ bản (VNĐ)", "number", 3),
                field("heSoLuong", "Hệ số lương", "number", 3),
                field("phuCap", "Phụ cấp (VNĐ)", "number", 3)
        ));
        form.add(row(
                field("ngayNghiViec", "Ngày nghỉ việc (dd/MM/yyyy)", "date", 3),
                field("lyDoNghiViec", "Lý do nghỉ việc", "text", 6),
                field("dangNhapCuoi", "Đăng nhập gần nhất", "readonly", 3)
        ));

        form.add(Box.createVerticalStrut(6));
        form.add(section("Phân quyền, khoa phòng & trạng thái"));
        form.add(checkRow(new String[][]{
                {"hoatDong", "Đang hoạt động"}
        }));

        JLabel lblRoles = new JLabel("Vai trò phân quyền:");
        lblRoles.setFont(TEXT.deriveFont(Font.BOLD));
        lblRoles.setBorder(new EmptyBorder(6, 2, 3, 0));
        lblRoles.setAlignmentX(LEFT_ALIGNMENT);
        form.add(lblRoles);

        rolesPanel.setOpaque(false);
        rolesPanel.setAlignmentX(LEFT_ALIGNMENT);
        form.add(rolesPanel);

        JLabel lblDepts = new JLabel("Khoa / Phòng ban công tác:");
        lblDepts.setFont(TEXT.deriveFont(Font.BOLD));
        lblDepts.setBorder(new EmptyBorder(8, 2, 3, 0));
        lblDepts.setAlignmentX(LEFT_ALIGNMENT);
        form.add(lblDepts);

        deptsPanel.setOpaque(false);
        deptsPanel.setAlignmentX(LEFT_ALIGNMENT);
        form.add(deptsPanel);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(239, 239, 239));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(214, 214, 214)));
        footer.setPreferredSize(new Dimension(100, 43));

        JButton menu = new JButton("☰");
        menu.setFocusPainted(false);
        menu.setPreferredSize(new Dimension(42, 30));
        JPopupMenu popup = new JPopupMenu();
        JMenuItem close = new JMenuItem("Đóng");
        close.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) window.dispose();
        });
        popup.add(close);
        menu.addActionListener(e -> popup.show(menu, 0, -popup.getPreferredSize().height));

        JPanel leftFooter = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        leftFooter.setOpaque(false);
        leftFooter.add(menu);

        status.setFont(TEXT);
        status.setForeground(new Color(90, 90, 90));
        leftFooter.add(status);
        footer.add(leftFooter, BorderLayout.WEST);

        save.setFont(TEXT.deriveFont(Font.BOLD));
        save.setForeground(new Color(18, 92, 166));
        save.setFocusPainted(false);
        save.setPreferredSize(new Dimension(104, 30));
        save.setEnabled(false);
        save.addActionListener(e -> save());

        JPanel rightFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        rightFooter.setOpaque(false);
        rightFooter.add(save);
        footer.add(rightFooter, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "save-employee");
        getActionMap().put("save-employee", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                save();
            }
        });

        loadInitialData();
    }

    private record LoadPayload(
            EmployeeDetail detail,
            List<PositionOption> positions,
            List<RoleOption> roles,
            List<DepartmentOption> departments) {}

    private void loadInitialData() {
        new SwingWorker<LoadPayload, Void>() {
            @Override
            protected LoadPayload doInBackground() {
                EmployeeDetail detail = readPort.getEmployeeDetail(employeeId);
                List<PositionOption> pos = readPort.listPositions();
                List<RoleOption> roles = readPort.listRoles();
                List<DepartmentOption> depts = readPort.listDepartments(facilityId);
                return new LoadPayload(detail, pos, roles, depts);
            }

            @Override
            protected void done() {
                try {
                    LoadPayload payload = get();
                    applyLoadedPayload(payload);
                } catch (Exception ex) {
                    status.setForeground(new Color(170, 0, 0));
                    status.setText("Không tải được dữ liệu nhân viên: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void applyLoadedPayload(LoadPayload payload) {
        this.positions = payload.positions() != null ? payload.positions() : List.of();
        EmployeeDetail d = payload.detail();
        if (d == null) {
            status.setForeground(new Color(170, 0, 0));
            status.setText("Không tìm thấy hồ sơ nhân viên #" + employeeId);
            return;
        }

        setText("maNhanVien", d.maNhanVien());
        setText("ho", d.ho());
        setText("ten", d.ten());
        setCombo("gioiTinh", toDisplayGender(d.gioiTinh()));
        setText("ngaySinh", d.ngaySinh() != null ? d.ngaySinh().format(DATE_FMT) : "");
        setText("soDienThoai", d.soDienThoai());
        setText("email", d.email());
        setText("diaChi", d.diaChi());
        setText("username", d.username());
        setText("matKhauMoi", "");
        setText("chungChiHanhNghe", d.chungChiHanhNghe());
        setText("ngayVaoLam", d.ngayVaoLam() != null ? d.ngayVaoLam().format(DATE_FMT) : "");
        setText("luongCoBan", formatDecimal(d.luongCoBan()));
        setText("heSoLuong", formatDecimal(d.heSoLuong()));
        setText("phuCap", formatDecimal(d.phuCap()));
        setText("ngayNghiViec", d.ngayNghiViec() != null ? d.ngayNghiViec().format(DATE_FMT) : "");
        setText("lyDoNghiViec", d.lyDoNghiViec());
        setText("dangNhapCuoi", d.dangNhapCuoi() != null
                ? d.dangNhapCuoi().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "Chưa đăng nhập");
        setCheck("hoatDong", d.hoatDong());

        @SuppressWarnings("unchecked")
        JComboBox<String> posCombo = (JComboBox<String>) fields.get("chucVu");
        posCombo.removeAllItems();
        posCombo.addItem("");
        String selectedPosName = "";
        for (PositionOption p : positions) {
            posCombo.addItem(p.name());
            if (d.maChucVu() != null && d.maChucVu() == p.id()) {
                selectedPosName = p.name();
            }
        }
        posCombo.setSelectedItem(selectedPosName);

        Set<Integer> assignedRoles = new HashSet<>(d.roleIds() != null ? d.roleIds() : List.of());
        rolesPanel.removeAll();
        roleBoxes.clear();
        for (RoleOption role : payload.roles()) {
            JCheckBox cb = new JCheckBox(role.name(), assignedRoles.contains(role.id()));
            cb.setFont(TEXT);
            cb.setOpaque(false);
            cb.putClientProperty("roleId", role.id());
            roleBoxes.add(cb);
            rolesPanel.add(cb);
        }

        Set<Integer> assignedDepts = new HashSet<>(d.departmentIds() != null ? d.departmentIds() : List.of());
        deptsPanel.removeAll();
        deptBoxes.clear();
        for (DepartmentOption dept : payload.departments()) {
            JCheckBox cb = new JCheckBox(dept.name(), assignedDepts.contains(dept.id()));
            cb.setFont(TEXT);
            cb.setOpaque(false);
            cb.putClientProperty("deptId", dept.id());
            deptBoxes.add(cb);
            deptsPanel.add(cb);
        }

        loaded = true;
        save.setEnabled(true);
        status.setForeground(new Color(90, 90, 90));
        status.setText("Nhấn Lưu (F2) để cập nhật hồ sơ nhân viên");
        form.revalidate();
        form.repaint();
    }

    public void save() {
        if (saving || !loaded) return;

        String maNv = getText("maNhanVien");
        String ho = getText("ho");
        String ten = getText("ten");
        String username = getText("username");
        if (ho.isEmpty() || ten.isEmpty() || username.isEmpty()) {
            status.setForeground(new Color(170, 0, 0));
            status.setText("Vui lòng nhập đầy đủ Họ, Tên và Tên đăng nhập.");
            return;
        }

        LocalDate ngaySinh;
        LocalDate ngayVaoLam;
        LocalDate ngayNghiViec;
        BigDecimal luongCoBan;
        BigDecimal heSoLuong;
        BigDecimal phuCap;
        try {
            ngaySinh = parseDate(getText("ngaySinh"), "Ngày sinh");
            ngayVaoLam = parseDate(getText("ngayVaoLam"), "Ngày vào làm");
            ngayNghiViec = parseDate(getText("ngayNghiViec"), "Ngày nghỉ việc");
            luongCoBan = parseDecimal(getText("luongCoBan"), "Lương cơ bản");
            heSoLuong = parseDecimal(getText("heSoLuong"), "Hệ số lương");
            phuCap = parseDecimal(getText("phuCap"), "Phụ cấp");
        } catch (IllegalArgumentException ex) {
            status.setForeground(new Color(170, 0, 0));
            status.setText(ex.getMessage());
            return;
        }

        Integer maChucVu = null;
        @SuppressWarnings("unchecked")
        JComboBox<String> posCombo = (JComboBox<String>) fields.get("chucVu");
        String selectedPos = Objects.toString(posCombo.getSelectedItem(), "");
        for (PositionOption p : positions) {
            if (p.name().equals(selectedPos)) {
                maChucVu = p.id();
                break;
            }
        }

        List<Integer> selectedRoleIds = new ArrayList<>();
        for (JCheckBox cb : roleBoxes) {
            if (cb.isSelected()) {
                selectedRoleIds.add((Integer) cb.getClientProperty("roleId"));
            }
        }
        List<Integer> selectedDeptIds = new ArrayList<>();
        for (JCheckBox cb : deptBoxes) {
            if (cb.isSelected()) {
                selectedDeptIds.add((Integer) cb.getClientProperty("deptId"));
            }
        }

        EmployeeDetail updated = new EmployeeDetail(
                employeeId,
                maNv,
                ho,
                ten,
                getText("soDienThoai"),
                getText("email"),
                getText("diaChi"),
                toStorageGender(getCombo("gioiTinh")),
                ngaySinh,
                maChucVu,
                username,
                getText("chungChiHanhNghe"),
                ngayVaoLam,
                luongCoBan,
                heSoLuong,
                phuCap,
                isChecked("hoatDong"),
                selectedRoleIds,
                selectedDeptIds,
                ngayNghiViec,
                getText("lyDoNghiViec"),
                null
        );

        JPasswordField pwField = (JPasswordField) fields.get("matKhauMoi");
        char[] newPassword = pwField.getPassword();

        saving = true;
        save.setEnabled(false);
        status.setForeground(new Color(90, 90, 90));
        status.setText("Đang lưu thông tin nhân viên...");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                readPort.saveEmployeeDetail(updated, newPassword, actorId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (newPassword != null) {
                        Arrays.fill(newPassword, '\0');
                    }
                    if (onSaved != null) {
                        onSaved.run();
                    }
                    status.setForeground(new Color(0, 120, 0));
                    status.setText("Đã lưu nhân viên " + updated.maNhanVien());
                    Window window = SwingUtilities.getWindowAncestor(CapNhatNhanVienHopThoai.this);
                    if (window != null) {
                        window.dispose();
                    }
                } catch (Exception ex) {
                    status.setForeground(new Color(170, 0, 0));
                    String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                    status.setText("Lỗi khi lưu: " + msg);
                } finally {
                    saving = false;
                    save.setEnabled(true);
                }
            }
        }.execute();
    }

    private JLabel section(String title) {
        JLabel label = new JLabel(title);
        label.setFont(TEXT.deriveFont(Font.BOLD, 16f));
        label.setBorder(new EmptyBorder(3, 0, 4, 0));
        label.setAlignmentX(LEFT_ALIGNMENT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        label.setPreferredSize(new Dimension(100, 26));
        return label;
    }

    private record Cell(JPanel panel, int width) {}

    private Cell field(String key, String title, String type, int width) {
        JPanel cell = new JPanel(new BorderLayout(0, 2));
        cell.setOpaque(false);
        cell.setBorder(new EmptyBorder(0, 2, 2, 3));
        JLabel label = new JLabel(title);
        label.setFont(TEXT);
        cell.add(label, BorderLayout.NORTH);

        JComponent input;
        if ("gender".equals(type)) {
            JComboBox<String> combo = new JComboBox<>(new String[]{"", "Nam", "Nữ", "Khác"});
            combo.setFont(TEXT.deriveFont(13f));
            input = combo;
        } else if ("position".equals(type)) {
            JComboBox<String> combo = new JComboBox<>(new String[]{""});
            combo.setFont(TEXT.deriveFont(13f));
            input = combo;
        } else if ("password".equals(type)) {
            JPasswordField pw = new JPasswordField();
            pw.setFont(TEXT.deriveFont(13f));
            input = pw;
        } else {
            JTextField text = new JTextField();
            text.setFont(TEXT.deriveFont(13f));
            if ("number".equals(type)) {
                text.setHorizontalAlignment(SwingConstants.RIGHT);
                text.setText("0");
            } else if ("code".equals(type)) {
                text.setForeground(new Color(190, 0, 0));
                text.setFont(TEXT.deriveFont(Font.BOLD, 13f));
            } else if ("date".equals(type)) {
                text.setToolTipText("dd/MM/yyyy");
            } else if ("readonly".equals(type)) {
                text.setEditable(false);
                text.setBackground(new Color(240, 240, 240));
            }
            if ("ho".equals(key) || "ten".equals(key) || "username".equals(key)) {
                text.setBackground(new Color(255, 255, 215));
            }
            input = text;
        }

        input.setName(key);
        input.setMinimumSize(new Dimension(0, 24));
        input.setPreferredSize(new Dimension(80, 24));
        input.getAccessibleContext().setAccessibleName(title);
        fields.put(key, input);
        cell.add(input, BorderLayout.CENTER);
        return new Cell(cell, width);
    }

    private JPanel row(Cell... cells) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setPreferredSize(new Dimension(100, 46));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        row.setMinimumSize(new Dimension(0, 46));
        int x = 0;
        for (Cell cell : cells) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = x;
            c.gridy = 0;
            c.gridwidth = cell.width();
            c.weightx = cell.width();
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            cell.panel().setMinimumSize(new Dimension(0, 42));
            row.add(cell.panel(), c);
            x += cell.width();
        }
        return row;
    }

    private JPanel checkRow(String[][] items) {
        JPanel row = new JPanel(new GridLayout(1, 3, 3, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        row.setPreferredSize(new Dimension(100, 26));
        for (String[] item : items) {
            JCheckBox input = new JCheckBox(item[1]);
            input.setFont(TEXT);
            input.setOpaque(false);
            input.setName(item[0]);
            fields.put(item[0], input);
            row.add(input);
        }
        for (int i = items.length; i < 3; i++) {
            row.add(new JLabel());
        }
        return row;
    }

    private void setText(String key, String value) {
        JComponent comp = fields.get(key);
        if (comp instanceof JTextField tf) {
            tf.setText(value != null ? value : "");
        }
    }

    private String getText(String key) {
        JComponent comp = fields.get(key);
        if (comp instanceof JTextField tf) {
            return tf.getText().trim();
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    private void setCombo(String key, String value) {
        JComponent comp = fields.get(key);
        if (comp instanceof JComboBox<?> combo) {
            ((JComboBox<String>) combo).setSelectedItem(value != null ? value : "");
        }
    }

    private String getCombo(String key) {
        JComponent comp = fields.get(key);
        if (comp instanceof JComboBox<?> combo) {
            return Objects.toString(combo.getSelectedItem(), "").trim();
        }
        return "";
    }

    private void setCheck(String key, boolean checked) {
        JComponent comp = fields.get(key);
        if (comp instanceof JCheckBox cb) {
            cb.setSelected(checked);
        }
    }

    private boolean isChecked(String key) {
        JComponent comp = fields.get(key);
        return comp instanceof JCheckBox cb && cb.isSelected();
    }

    private static String toDisplayGender(String code) {
        if (code == null) return "";
        return switch (code.trim().toUpperCase(Locale.ROOT)) {
            case "M", "NAM" -> "Nam";
            case "F", "NU", "NỮ" -> "Nữ";
            case "O", "KHAC", "KHÁC" -> "Khác";
            default -> code.trim();
        };
    }

    private static String toStorageGender(String label) {
        if (label == null) return "";
        return switch (label.trim().toLowerCase(Locale.ROOT)) {
            case "nam" -> "M";
            case "nữ", "nu" -> "F";
            case "khác", "khac" -> "O";
            default -> label.trim();
        };
    }

    private static String formatDecimal(BigDecimal value) {
        if (value == null) return "0";
        return MONEY_FMT.format(value);
    }

    private static LocalDate parseDate(String text, String label) {
        if (text == null || text.isBlank()) return null;
        try {
            return LocalDate.parse(text.trim(), DATE_FMT);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(label + " không đúng định dạng dd/MM/yyyy.");
        }
    }

    private static BigDecimal parseDecimal(String text, String label) {
        if (text == null || text.isBlank()) return BigDecimal.ZERO;
        try {
            String normalized = text.trim().replace(",", "").replace(" ", "");
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " phải là số hợp lệ.");
        }
    }

    public boolean isLoaded() {
        return loaded;
    }

    public JComponent getField(String key) {
        return fields.get(key);
    }

    public JButton getSaveButton() {
        return save;
    }

    public String getStatusText() {
        return status.getText();
    }

    public List<JCheckBox> getRoleBoxes() {
        return List.copyOf(roleBoxes);
    }

    public List<JCheckBox> getDeptBoxes() {
        return List.copyOf(deptBoxes);
    }

    public static void showDialog(
            Component parent,
            NhanSuReadPort readPort,
            int employeeId,
            int facilityId,
            int actorId,
            Runnable onSaved) {
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        JDialog dialog = new JDialog(owner, "EHC - Cập Nhật Nhân Viên", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        CapNhatNhanVienHopThoai panel = new CapNhatNhanVienHopThoai(readPort, employeeId, facilityId, actorId, onSaved);
        dialog.setContentPane(panel);
        dialog.setSize(980, 620);
        dialog.setMinimumSize(new Dimension(760, 480));
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
}
