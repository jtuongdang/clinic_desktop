package vn.clinicmanagement.desktop.modules.hr;

import vn.clinicmanagement.application.hr.NhanSuReadPort;
import vn.clinicmanagement.application.hr.NhanSuReadPort.ShiftDetail;
import vn.clinicmanagement.application.hr.NhanSuReadPort.ShiftTypeOption;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Hộp thoại quản lý danh mục Ca Trực (dbo.CaTruc).
 */
public final class CaTrucQuanLyHopThoai extends JPanel {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final NhanSuReadPort readPort;
    private final int actorId;
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Loại ca", "Tên ca trực", "Giờ bắt đầu", "Giờ kết thúc", "Hoạt động"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    private final JComboBox<ShiftTypeOption> cboType = new JComboBox<>();
    private final JTextField txtName = new JTextField(16);
    private final JTextField txtStart = new JTextField("07:30", 8);
    private final JTextField txtEnd = new JTextField("11:30", 8);
    private final JCheckBox chkActive = new JCheckBox("Đang hoạt động", true);
    private final JLabel status = new JLabel("Chọn một ca để sửa hoặc nhấn Làm Mới để thêm ca mới.");

    private List<ShiftDetail> allShifts = List.of();
    private int editingId = 0;

    public CaTrucQuanLyHopThoai(NhanSuReadPort readPort, int actorId) {
        this.readPort = Objects.requireNonNull(readPort, "readPort");
        this.actorId = actorId;
        setLayout(new BorderLayout(0, 8));
        setBackground(new Color(249, 249, 249));
        setBorder(new EmptyBorder(10, 12, 10, 12));

        table.setRowHeight(24);
        table.setFont(UngDungPhongChu.BODY);
        table.getTableHeader().setFont(UngDungPhongChu.BODY_BOLD);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onRowSelected();
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel formWrap = new JPanel(new BorderLayout(0, 6));
        formWrap.setOpaque(false);
        formWrap.setBorder(BorderFactory.createTitledBorder("Thông tin Ca Trực"));

        JPanel grid = new JPanel(new GridLayout(2, 4, 10, 6));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(4, 8, 4, 8));
        grid.add(new JLabel("Loại ca:"));
        grid.add(cboType);
        grid.add(new JLabel("Tên ca trực (*):"));
        grid.add(txtName);
        grid.add(new JLabel("Giờ bắt đầu (HH:mm):"));
        grid.add(txtStart);
        grid.add(new JLabel("Giờ kết thúc (HH:mm):"));
        grid.add(txtEnd);
        formWrap.add(grid, BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(0, 8, 4, 8));
        chkActive.setOpaque(false);
        actions.add(chkActive, BorderLayout.WEST);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        JButton btnNew = new JButton("Thêm Mới (Reset)");
        btnNew.addActionListener(e -> resetForm());
        JButton btnSave = new JButton("Lưu Ca Trực");
        btnSave.setFont(UngDungPhongChu.BODY_BOLD);
        btnSave.setForeground(new Color(18, 92, 166));
        btnSave.addActionListener(e -> saveShift());
        btns.add(btnNew);
        btns.add(btnSave);
        actions.add(btns, BorderLayout.EAST);
        formWrap.add(actions, BorderLayout.SOUTH);

        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setOpaque(false);
        south.add(formWrap, BorderLayout.CENTER);
        status.setFont(UngDungPhongChu.SMALL);
        south.add(status, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        loadData();
    }

    private void loadData() {
        new SwingWorker<Void, Void>() {
            List<ShiftTypeOption> types;
            List<ShiftDetail> shifts;

            @Override
            protected Void doInBackground() {
                types = readPort.listShiftTypes();
                shifts = readPort.listAllShifts();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    cboType.removeAllItems();
                    for (ShiftTypeOption t : types) cboType.addItem(t);
                    allShifts = shifts;
                    model.setRowCount(0);
                    for (ShiftDetail s : allShifts) {
                        model.addRow(new Object[]{
                                s.id(), s.shiftTypeName(), s.name(),
                                s.startTime().format(TIME_FMT), s.endTime().format(TIME_FMT),
                                s.active() ? "Đang hoạt động" : "Ngưng"
                        });
                    }
                    status.setText("Đã tải " + allShifts.size() + " ca trực.");
                } catch (Exception ex) {
                    status.setText("Lỗi tải ca trực: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void onRowSelected() {
        int r = table.getSelectedRow();
        if (r < 0 || r >= allShifts.size()) return;
        ShiftDetail s = allShifts.get(r);
        editingId = s.id();
        for (int i = 0; i < cboType.getItemCount(); i++) {
            if (cboType.getItemAt(i).id() == s.shiftTypeId()) {
                cboType.setSelectedIndex(i);
                break;
            }
        }
        txtName.setText(s.name());
        txtStart.setText(s.startTime().format(TIME_FMT));
        txtEnd.setText(s.endTime().format(TIME_FMT));
        chkActive.setSelected(s.active());
        status.setText("Đang sửa ca #" + s.id() + ": " + s.name());
    }

    private void resetForm() {
        editingId = 0;
        table.clearSelection();
        txtName.setText("");
        txtStart.setText("07:30");
        txtEnd.setText("11:30");
        chkActive.setSelected(true);
        status.setText("Nhập thông tin ca mới và bấm Lưu Ca Trực.");
    }

    private void saveShift() {
        if (!(cboType.getSelectedItem() instanceof ShiftTypeOption type)) return;
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            status.setText("Tên ca trực không được để trống.");
            return;
        }
        LocalTime st;
        LocalTime et;
        try {
            st = LocalTime.parse(txtStart.getText().trim(), TIME_FMT);
            et = LocalTime.parse(txtEnd.getText().trim(), TIME_FMT);
        } catch (Exception ex) {
            status.setText("Giờ phải đúng định dạng HH:mm (ví dụ 07:30).");
            return;
        }
        ShiftDetail detail = new ShiftDetail(editingId, type.id(), type.name(), name, st, et, chkActive.isSelected());
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                readPort.saveShift(detail, actorId);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    resetForm();
                    loadData();
                    status.setText("Đã lưu ca trực thành công.");
                } catch (Exception ex) {
                    status.setText("Lỗi lưu ca trực: " + ex.getMessage());
                }
            }
        }.execute();
    }

    public static void showDialog(Component parent, NhanSuReadPort readPort, int actorId) {
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        JDialog dialog = new JDialog(owner, "EHC - Quản Lý Danh Mục Ca Trực", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(new CaTrucQuanLyHopThoai(readPort, actorId));
        dialog.setSize(760, 480);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
}
