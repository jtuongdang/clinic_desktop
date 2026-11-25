package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import vn.clinicmanagement.application.examination.KhamBenhQueueActionsPort;
import vn.clinicmanagement.application.examination.KhamBenhQueueActionsPort.InpatientAdmissionRow;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/** Màn hình Quản Lý Buồng / Giường Nội Trú cho bệnh nhân xử trí Nhập viện. */
public final class QuanLyBuongGiuongNoiTruHopThoai extends JPanel {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private final KhamBenhQueueActionsPort port;
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"STT", "Mã LK", "Mã BN", "Họ và tên", "Giới tính", "Khoa", "Hướng xử trí", "Buồng / Giường", "Ngày vào"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel lblStatus = new JLabel("Đang tải danh sách nội trú...");
    private List<InpatientAdmissionRow> rows = List.of();

    public QuanLyBuongGiuongNoiTruHopThoai(KhamBenhQueueActionsPort port) {
        this.port = port;
        setLayout(new BorderLayout(0, 8));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 12, 10, 12));
        setPreferredSize(new Dimension(1020, 520));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("QUẢN LÝ BUỒNG / GIƯỜNG BỆNH NỘI TRÚ");
        title.setFont(UngDungPhongChu.BODY_BOLD.deriveFont(15f));
        title.setForeground(new Color(10, 86, 179));
        top.add(title, BorderLayout.WEST);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        bar.setOpaque(false);
        JButton btnReload = new JButton("Làm mới");
        JButton btnAssign = new JButton("Phân Buồng / Giường");
        btnReload.addActionListener(e -> loadData());
        btnAssign.addActionListener(e -> assignSelectedBed());
        bar.add(btnReload);
        bar.add(btnAssign);
        top.add(bar, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        table.setFont(UngDungPhongChu.BODY);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(new Color(189, 202, 215)));
        add(sp, BorderLayout.CENTER);

        lblStatus.setFont(UngDungPhongChu.BODY);
        add(lblStatus, BorderLayout.SOUTH);

        loadData();
    }

    public JTable getTable() {
        return table;
    }

    public void loadData() {
        var auth = KhamBenhPhienNguCanh.current();
        if (port == null || auth == null) {
            lblStatus.setText("Cần phiên đăng nhập và kết nối CSDL để tải danh sách nội trú.");
            return;
        }
        new SwingWorker<List<InpatientAdmissionRow>, Void>() {
            @Override
            protected List<InpatientAdmissionRow> doInBackground() {
                return port.listInpatientAdmissions(auth.facilityId());
            }

            @Override
            protected void done() {
                try {
                    rows = get();
                    model.setRowCount(0);
                    int stt = 1;
                    for (InpatientAdmissionRow r : rows) {
                        model.addRow(new Object[]{
                                stt++, r.visitCode(), r.patientCode(), r.patientName(),
                                r.gender(), r.departmentName(), r.treatmentDirection(),
                                r.bedAssignment(),
                                r.admittedAt() != null ? r.admittedAt().format(DT_FMT) : ""
                        });
                    }
                    lblStatus.setText("Số bệnh nhân nội trú / nhập viện: " + rows.size());
                } catch (Exception ex) {
                    lblStatus.setText("Lỗi tải danh sách nội trú: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void assignSelectedBed() {
        if (GraphicsEnvironment.isHeadless()) return;
        int idx = table.getSelectedRow();
        var auth = KhamBenhPhienNguCanh.current();
        if (idx < 0 || idx >= rows.size() || auth == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một bệnh nhân nhập viện.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        InpatientAdmissionRow selected = rows.get(idx);
        JComboBox<String> cboWard = new JComboBox<>(new String[]{"Buồng 101", "Buồng 102", "Buồng 201", "Buồng 202", "Buồng Cấp Cứu", "Buồng Hồi Sức"});
        cboWard.setEditable(true);
        JTextField txtBed = new JTextField("G01", 10);
        JTextField txtNote = new JTextField(16);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.add(new JLabel("Bệnh nhân:")); form.add(new JLabel(selected.patientName() + " (" + selected.patientCode() + ")"));
        form.add(new JLabel("Buồng bệnh:")); form.add(cboWard);
        form.add(new JLabel("Giường bệnh:")); form.add(txtBed);
        form.add(new JLabel("Ghi chú:")); form.add(txtNote);

        if (JOptionPane.showConfirmDialog(this, form, "Phân Buồng / Giường Nội Trú", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
            return;
        }
        String ward = Objects.toString(cboWard.getEditor().getItem(), "").trim();
        String bed = txtBed.getText().trim();
        if (ward.isEmpty() || bed.isEmpty()) return;

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                try {
                    port.assignInpatientBed(selected.visitId(), ward, bed, txtNote.getText().trim(), auth.employeeId());
                    return null;
                } catch (Exception ex) {
                    return ex.getMessage();
                }
            }

            @Override
            protected void done() {
                try {
                    String err = get();
                    if (err != null) JOptionPane.showMessageDialog(QuanLyBuongGiuongNoiTruHopThoai.this, err, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    else loadData();
                } catch (Exception ignored) {}
            }
        }.execute();
    }
}
