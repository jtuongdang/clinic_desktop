package vn.clinicmanagement.desktop.modules.hr;

import vn.clinicmanagement.application.hr.NhanSuReadPort;
import vn.clinicmanagement.application.hr.NhanSuReadPort.PermissionRow;
import vn.clinicmanagement.application.hr.NhanSuReadPort.RoleOption;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hộp thoại cấu hình ma trận phân quyền chi tiết theo vai trò (dbo.MaTranPhanQuyen).
 */
public class MaTranPhanQuyenHopThoai extends JPanel {
    private static final String[] MODULES = {
            "TIEP_DON", "KHAM_BENH", "THU_NGAN", "NHA_THUOC",
            "XET_NGHIEM", "CDHA", "NHAN_SU", "BAO_CAO", "HE_THONG"
    };
    private static final String[] ACTIONS = {"XEM", "THEM", "SUA", "XOA", "DUYET", "IN_XUAT"};
    private static final String[] SCOPES = {"TOAN_HE_THONG", "CO_SO", "KHOA", "CA_NHAN"};

    private final NhanSuReadPort readPort;
    private final int actorId;
    private final JComboBox<RoleOption> cboRole = new JComboBox<>();
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Phân Hệ", "Hành Động", "Phạm Vi Dữ Liệu", "Được Phép"}, 0) {
        @Override
        public Class<?> getColumnClass(int col) {
            return col == 3 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == 2 || col == 3;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel status = new JLabel("Chọn vai trò để xem hoặc cập nhật quyền.");
    private final JButton btnSave = new JButton("Lưu Phân Quyền");

    public MaTranPhanQuyenHopThoai(NhanSuReadPort readPort, int actorId) {
        this.readPort = readPort;
        this.actorId = actorId;
        setLayout(new BorderLayout(0, 8));
        setBackground(new Color(249, 249, 249));
        setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        top.setOpaque(false);
        JLabel lbl = new JLabel("Vai trò:");
        lbl.setFont(UngDungPhongChu.BODY_BOLD);
        cboRole.setPreferredSize(new Dimension(240, 26));
        cboRole.addActionListener(e -> loadSelectedRole());

        JButton btnCheckAll = new JButton("Chọn tất cả");
        btnCheckAll.addActionListener(e -> setAllChecked(true));
        JButton btnUncheckAll = new JButton("Bỏ chọn tất cả");
        btnUncheckAll.addActionListener(e -> setAllChecked(false));

        top.add(lbl);
        top.add(cboRole);
        top.add(btnCheckAll);
        top.add(btnUncheckAll);
        add(top, BorderLayout.NORTH);

        table.setRowHeight(24);
        table.setFont(UngDungPhongChu.BODY);
        table.getTableHeader().setFont(UngDungPhongChu.BODY_BOLD);
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JComboBox<>(SCOPES)));
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        status.setFont(UngDungPhongChu.BODY);
        btnSave.setFont(UngDungPhongChu.BODY_BOLD);
        btnSave.setForeground(new Color(18, 92, 166));
        btnSave.addActionListener(e -> savePermissions());
        footer.add(status, BorderLayout.WEST);
        footer.add(btnSave, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        loadRoles();
    }

    private void loadRoles() {
        new SwingWorker<List<RoleOption>, Void>() {
            @Override
            protected List<RoleOption> doInBackground() {
                return readPort.listRoles();
            }

            @Override
            protected void done() {
                try {
                    cboRole.removeAllItems();
                    for (RoleOption r : get()) {
                        cboRole.addItem(r);
                    }
                    if (cboRole.getItemCount() > 0) {
                        cboRole.setSelectedIndex(0);
                    }
                } catch (Exception ex) {
                    status.setText("Lỗi tải vai trò: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void loadSelectedRole() {
        if (!(cboRole.getSelectedItem() instanceof RoleOption role)) return;
        new SwingWorker<List<PermissionRow>, Void>() {
            @Override
            protected List<PermissionRow> doInBackground() {
                return readPort.listRolePermissions(role.id());
            }

            @Override
            protected void done() {
                try {
                    Map<String, PermissionRow> existing = new HashMap<>();
                    for (PermissionRow p : get()) {
                        existing.put(p.phanHe() + "::" + p.hanhDong(), p);
                    }
                    model.setRowCount(0);
                    boolean isAdmin = "ADMIN".equalsIgnoreCase(role.code()) || "ADMIN".equalsIgnoreCase(role.name());
                    for (String mod : MODULES) {
                        for (String act : ACTIONS) {
                            PermissionRow p = existing.get(mod + "::" + act);
                            String scope = p != null ? p.phamViDuLieu() : "TOAN_HE_THONG";
                            boolean allowed = p != null ? p.duocPhep() : isAdmin;
                            model.addRow(new Object[]{mod, act, scope, allowed});
                        }
                    }
                    status.setText("Đang hiển thị " + model.getRowCount() + " quyền của vai trò " + role.name());
                } catch (Exception ex) {
                    status.setText("Lỗi tải ma trận quyền: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void setAllChecked(boolean checked) {
        for (int r = 0; r < model.getRowCount(); r++) {
            model.setValueAt(checked, r, 3);
        }
    }

    private void savePermissions() {
        if (!(cboRole.getSelectedItem() instanceof RoleOption role)) return;
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        List<PermissionRow> rows = new ArrayList<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            rows.add(new PermissionRow(
                    0,
                    role.id(),
                    String.valueOf(model.getValueAt(r, 0)),
                    String.valueOf(model.getValueAt(r, 1)),
                    String.valueOf(model.getValueAt(r, 2)),
                    Boolean.TRUE.equals(model.getValueAt(r, 3))
            ));
        }
        btnSave.setEnabled(false);
        status.setText("Đang lưu ma trận phân quyền...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                readPort.saveRolePermissions(role.id(), rows, actorId);
                return null;
            }

            @Override
            protected void done() {
                btnSave.setEnabled(true);
                try {
                    get();
                    status.setText("Đã lưu phân quyền cho vai trò " + role.name() + ".");
                } catch (Exception ex) {
                    status.setText("Lỗi lưu phân quyền: " + ex.getMessage());
                }
            }
        }.execute();
    }

    public static void showDialog(Component parent, NhanSuReadPort readPort, int actorId) {
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        JDialog dialog = new JDialog(owner, "EHC - Ma Trận Phân Quyền Vai Trò", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(new MaTranPhanQuyenHopThoai(readPort, actorId));
        dialog.setSize(760, 540);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
}
