package vn.clinicmanagement.desktop.modules.paraclinical;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.cls.XetNghiemKetQuaMatHangDauVao;
import vn.clinicmanagement.application.cls.RecordDichVuKetQuaCommand;
import vn.clinicmanagement.application.cls.RecordDichVuKetQuaBienNhan;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Hộp thoại Nhập Kết Quả Cận Lâm Sàng & Kết Luận Chuyên Khoa (HD-INTEG-03).
 * Đấu nối trực tiếp Clean Architecture qua RecordServiceResultHandler.
 */
public class CanLamSangKetQuaNhapLieuHopThoai extends JDialog {

    /** Trường `orderId`: yêu cầu mã. */
    private final int orderId;
    /** Trường `patientName`: bệnh nhân tên. */
    private final String patientName;
    /** Trường `serviceName`: dịch vụ tên. */
    private final String serviceName;
    /** Trường `services`: dịch vụ. */
    private final CanLamSangGhepNoiGoc.Services services;
    /** Trường `onSavedCallback`: tải `Saved` `Callback`. */
    private final Runnable onSavedCallback;

    /** Trường `tableModel`: bảng mô hình. */
    private DefaultTableModel tableModel;
    /** Trường `table`: bảng. */
    private JTable table;
    /** Trường `txtSummary`: `txt` tonghop. */
    private JTextArea txtSummary;
    private JTextArea txtConclusion;
    /** Trường `btnSave`: `btn` lưu. */
    private JButton btnSave;
    /** Trường `btnCancel`: `btn` hủy. */
    private JButton btnCancel;
    /** Trường `btnAddRow`: `btn` thêm `Row`. */
    private JButton btnAddRow;
    /** Trường `btnDeleteRow`: `btn` xóa `Row`. */
    private JButton btnDeleteRow;

    public CanLamSangKetQuaNhapLieuHopThoai(int orderId, String patientName, String serviceName,
                                         CanLamSangGhepNoiGoc.Services services) {
        this(null, orderId, patientName, serviceName, services, null);
    }

    public CanLamSangKetQuaNhapLieuHopThoai(Window owner, int orderId, String patientName, String serviceName,
                                         CanLamSangGhepNoiGoc.Services services, Runnable onSavedCallback) {
        super(owner, "Nhập Kết Quả Cận Lâm Sàng - CLS-" + orderId, ModalityType.MODELESS);
        this.orderId = orderId;
        this.patientName = patientName != null ? patientName : "Bệnh nhân";
        this.serviceName = serviceName != null ? serviceName : "Dịch vụ cận lâm sàng";
        this.services = services != null ? services : CanLamSangGhepNoiGoc.createDefault();
        this.onSavedCallback = onSavedCallback;

        initUI();
    }

    /** Hàm `initUI`: `init` giao diện. */
    private void initUI() {
        setSize(780, 560);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // --- 1. HEADER PANEL ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(235, 244, 252));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(185, 205, 225)),
                new EmptyBorder(10, 14, 10, 14)
        ));

        JLabel lblTitle = new JLabel("PHIẾU KẾT QUẢ CẬN LÂM SÀNG: " + serviceName.toUpperCase());
        lblTitle.setFont(UngDungPhongChu.SECTION);
        lblTitle.setForeground(new Color(16, 75, 130));

        JPanel subInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 2));
        subInfo.setOpaque(false);
        JLabel lblOrder = new JLabel("Mã chỉ định: CLS-" + orderId);
        lblOrder.setFont(UngDungPhongChu.BODY_BOLD);
        lblOrder.setForeground(new Color(180, 30, 30));

        JLabel lblPatient = new JLabel("Bệnh nhân: " + patientName);
        lblPatient.setFont(UngDungPhongChu.BODY_BOLD);
        lblPatient.setForeground(new Color(20, 30, 40));

        subInfo.add(lblOrder);
        subInfo.add(lblPatient);

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(subInfo, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // --- 2. CENTER: BẢNG CHỈ SỐ + KHUNG MÔ TẢ & KẾT LUẬN ---
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(new EmptyBorder(10, 14, 10, 14));

        // 2.1 Bảng chỉ số
        JLabel lblTableTitle = new JLabel("Chi Tiết Các Chỉ Số Đo Nghiệm:");
        lblTableTitle.setFont(UngDungPhongChu.BODY_BOLD);
        lblTableTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(lblTableTitle);
        centerPanel.add(Box.createVerticalStrut(6));

        String[] cols = {"Tên Chỉ Số", "Kết Quả", "Khoảng Tham Chiếu", "Đơn Vị", "Đánh Giá"};
        tableModel = new DefaultTableModel(cols, 0);
        populateDefaultIndicators();

        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setFont(UngDungPhongChu.BODY);
        table.getTableHeader().setFont(UngDungPhongChu.BODY_BOLD);
        table.getTableHeader().setBackground(new Color(240, 244, 250));

        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRender);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRender);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRender);
        table.getColumnModel().getColumn(4).setCellRenderer(centerRender);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(740, 150));
        tableScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(tableScroll);

        // Thanh nút thêm/xóa dòng chỉ số
        JPanel tableToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        tableToolbar.setOpaque(false);
        tableToolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnAddRow = new JButton("+ Thêm chỉ số");
        btnAddRow.setFont(UngDungPhongChu.SMALL);
        btnAddRow.addActionListener(e -> tableModel.addRow(new Object[]{"Chỉ số mới", "0.0", "4.0 - 10.0", "U/L", "Bình thường"}));

        btnDeleteRow = new JButton("- Xóa chỉ số");
        btnDeleteRow.setFont(UngDungPhongChu.SMALL);
        btnDeleteRow.addActionListener(e -> {
            int selected = table.getSelectedRow();
            if (selected >= 0 && selected < tableModel.getRowCount()) {
                tableModel.removeRow(selected);
            } else if (tableModel.getRowCount() > 0) {
                tableModel.removeRow(tableModel.getRowCount() - 1);
            }
        });
        tableToolbar.add(btnAddRow);
        tableToolbar.add(btnDeleteRow);
        centerPanel.add(tableToolbar);
        centerPanel.add(Box.createVerticalStrut(8));

        // 2.2 Khung mô tả hình ảnh / diễn biến
        JLabel lblSummary = new JLabel("Mô Tả Hình Ảnh / Diễn Biến Xét Nghiệm:");
        lblSummary.setFont(UngDungPhongChu.BODY_BOLD);
        lblSummary.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(lblSummary);
        centerPanel.add(Box.createVerticalStrut(4));

        txtSummary = new JTextArea("Các chỉ số huyết học/sinh hóa trong giới hạn sinh lý bình thường. Không phát hiện dấu hiệu viêm cấp.");
        txtSummary.setFont(UngDungPhongChu.BODY);
        txtSummary.setLineWrap(true);
        txtSummary.setWrapStyleWord(true);
        JScrollPane scrollSummary = new JScrollPane(txtSummary);
        scrollSummary.setPreferredSize(new Dimension(740, 60));
        scrollSummary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        scrollSummary.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(scrollSummary);
        centerPanel.add(Box.createVerticalStrut(8));

        // 2.3 Khung kết luận bác sĩ chuyên khoa
        JLabel lblConclusion = new JLabel("Kết Luận Của Bác Sĩ Chuyên Khoa CLS:");
        lblConclusion.setFont(UngDungPhongChu.BODY_BOLD);
        lblConclusion.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(lblConclusion);
        centerPanel.add(Box.createVerticalStrut(4));

        txtConclusion = new JTextArea("Bình thường, đề nghị theo dõi diễn tiến lâm sàng.");
        txtConclusion.setFont(UngDungPhongChu.BODY);
        txtConclusion.setLineWrap(true);
        txtConclusion.setWrapStyleWord(true);
        JScrollPane scrollConclusion = new JScrollPane(txtConclusion);
        scrollConclusion.setPreferredSize(new Dimension(740, 50));
        scrollConclusion.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        scrollConclusion.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(scrollConclusion);

        add(centerPanel, BorderLayout.CENTER);

        // --- 3. BOTTOM BUTTONS ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottomPanel.setBackground(new Color(245, 247, 250));
        bottomPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 220, 230)));

        btnSave = new JButton("💾 Lưu Kết Quả");
        btnSave.setFont(UngDungPhongChu.BODY_BOLD);
        btnSave.setBackground(new Color(16, 104, 188));
        btnSave.setForeground(Color.WHITE);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.addActionListener(e -> saveResult());

        btnCancel = new JButton("Đóng");
        btnCancel.setFont(UngDungPhongChu.BODY);
        btnCancel.addActionListener(e -> dispose());

        bottomPanel.add(btnSave);
        bottomPanel.add(btnCancel);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /** Hàm `populateDefaultIndicators`: `populate` macdinh `Indicators`. */
    private void populateDefaultIndicators() {
        String sName = serviceName.toLowerCase();
        if (sName.contains("huyết học") || sName.contains("máu") || sName.contains("công thức")) {
            tableModel.addRow(new Object[]{"Số lượng bạch cầu (WBC)", "6.8", "4.0 - 10.0", "G/L", "Bình thường"});
            tableModel.addRow(new Object[]{"Số lượng hồng cầu (RBC)", "4.5", "3.8 - 5.5", "T/L", "Bình thường"});
            tableModel.addRow(new Object[]{"Huyết sắc tố (Hb)", "138", "120 - 160", "g/L", "Bình thường"});
            tableModel.addRow(new Object[]{"Số lượng tiểu cầu (PLT)", "240", "150 - 450", "G/L", "Bình thường"});
        } else if (sName.contains("sinh hóa") || sName.contains("đường") || sName.contains("glucose")) {
            tableModel.addRow(new Object[]{"Glucose máu lúc đói", "5.2", "3.9 - 6.4", "mmol/L", "Bình thường"});
            tableModel.addRow(new Object[]{"Ure máu", "4.8", "2.5 - 7.5", "mmol/L", "Bình thường"});
            tableModel.addRow(new Object[]{"Creatinin máu", "72", "53 - 106", "µmol/L", "Bình thường"});
            tableModel.addRow(new Object[]{"AST (GOT)", "24", "0 - 37", "U/L", "Bình thường"});
            tableModel.addRow(new Object[]{"ALT (GPT)", "28", "0 - 40", "U/L", "Bình thường"});
        } else {
            tableModel.addRow(new Object[]{"Chỉ số khảo sát chính", "Bình thường", "Bình thường", "", "Bình thường"});
        }
    }

    /** Hàm `saveResult`: lưu kết quả. */
    public void saveResult() {
        btnSave.setEnabled(false);

        List<XetNghiemKetQuaMatHangDauVao> items = new ArrayList<>();
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            String name = Objects.toString(tableModel.getValueAt(r, 0), "");
            String val = Objects.toString(tableModel.getValueAt(r, 1), "");
            String ref = Objects.toString(tableModel.getValueAt(r, 2), "");
            String unit = Objects.toString(tableModel.getValueAt(r, 3), "");
            String eval = Objects.toString(tableModel.getValueAt(r, 4), "");
            items.add(new XetNghiemKetQuaMatHangDauVao(name, val, unit, ref, eval));
        }

        PhanQuyenNguCanh auth = new PhanQuyenNguCanh(1, 1, "LAB_TECH", 1, 1, UUID.randomUUID());
        KiemToanTacNhanNguCanh audit = new KiemToanTacNhanNguCanh(1, 1, "LAB_TECH", 1, 1, "WS01", UUID.randomUUID());

        RecordDichVuKetQuaCommand cmd = new RecordDichVuKetQuaCommand(
                orderId,
                txtSummary.getText().trim(),
                txtConclusion.getText().trim(),
                "",
                items,
                auth,
                audit
        );

        SwingWorker<KetQua<RecordDichVuKetQuaBienNhan>, Void> worker = new SwingWorker<>() {
            @Override
            protected KetQua<RecordDichVuKetQuaBienNhan> doInBackground() {
                return services.recordResult().handle(cmd);
            }

            @Override
            protected void done() {
                btnSave.setEnabled(true);
                try {
                    KetQua<RecordDichVuKetQuaBienNhan> res = get();
                    if (res != null && res.isSuccess()) {
                        if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                            JOptionPane.showMessageDialog(CanLamSangKetQuaNhapLieuHopThoai.this,
                                    "Đã lưu kết quả cận lâm sàng cho ca CLS-" + orderId + " thành công!",
                                    "Thành Công",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }
                        if (onSavedCallback != null) {
                            onSavedCallback.run();
                        }
                        dispose();
                    } else {
                        String msg = (res != null && res.getError().isPresent())
                                ? res.getError().get().message()
                                : "Lỗi không xác định khi lưu kết quả cận lâm sàng.";
                        if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                            JOptionPane.showMessageDialog(CanLamSangKetQuaNhapLieuHopThoai.this,
                                    "Không thể lưu kết quả: " + msg,
                                    "Thông Báo Lỗi",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } catch (Exception ex) {
                    if (isShowing() && !GraphicsEnvironment.isHeadless()) {
                        JOptionPane.showMessageDialog(CanLamSangKetQuaNhapLieuHopThoai.this,
                                "Lỗi hệ thống: " + ex.getMessage(),
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
        worker.execute();
    }

    /** Hàm `getOrderId`: lấy yêu cầu mã. */
    public int getOrderId() { return orderId; }
    /** Hàm `getPatientName`: lấy bệnh nhân tên. */
    public String getPatientName() { return patientName; }
    /** Hàm `getServiceName`: lấy dịch vụ tên. */
    public String getServiceName() { return serviceName; }
    /** Hàm `getBtnSave`: lấy `Btn` lưu. */
    public JButton getBtnSave() { return btnSave; }
    /** Hàm `getBtnCancel`: lấy `Btn` hủy. */
    public JButton getBtnCancel() { return btnCancel; }
    /** Hàm `getBtnAddRow`: lấy `Btn` thêm `Row`. */
    public JButton getBtnAddRow() { return btnAddRow; }
    /** Hàm `getBtnDeleteRow`: lấy `Btn` xóa `Row`. */
    public JButton getBtnDeleteRow() { return btnDeleteRow; }
    /** Hàm `getTxtSummary`: lấy `Txt` tonghop. */
    public JTextArea getTxtSummary() { return txtSummary; }
    /** Hàm `getTxtDescription`: lấy `Txt` `Description`. */
    public JTextArea getTxtDescription() { return txtSummary; }
    /** Hàm `getTxtConclusion`: lấy `Txt` `Conclusion`. */
    public JTextArea getTxtConclusion() { return txtConclusion; }
    /** Hàm `getTableModel`: lấy bảng mô hình. */
    public DefaultTableModel getTableModel() { return tableModel; }
    /** Hàm `getTable`: lấy bảng. */
    public JTable getTable() { return table; }
}
