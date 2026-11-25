package vn.clinicmanagement.desktop.modules.paraclinical;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.cls.XetNghiemKetQuaMatHangDauVao;
import vn.clinicmanagement.application.cls.record.RecordDichVuKetQuaCommand;
import vn.clinicmanagement.application.cls.record.RecordDichVuKetQuaBienNhan;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.service.ThietLapThucHienDichVu;

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

    private final int orderId;
    private final String patientName;
    private final String serviceName;
    private final CanLamSangGhepNoiGoc.Services services;
    private final Runnable onSavedCallback;

    private DefaultTableModel tableModel;
    private JTable table;
    private JTextArea txtSummary;
    private JTextArea txtConclusion;
    private JButton btnSave;
    private JButton btnCancel;
    private JButton btnAddRow;
    private JButton btnDeleteRow;
    private boolean readOnlyMode = false;
    private ThietLapThucHienDichVu executionSettings = ThietLapThucHienDichVu.MAC_DINH;
    private String existingImageAttachments = "";
    private final UUID consumptionOperationKey=UUID.randomUUID();

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

    private void initUI() {
        setSize(780, 560);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        // --- 1. BẢNG ĐẦU TRANG ---
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
        btnAddRow.addActionListener(e -> tableModel.addRow(new Object[]{"", "", "", "", ""}));

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

        txtSummary = new JTextArea();
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

        txtConclusion = new JTextArea();
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

        btnSave = new JButton("💾 Lưu và trả kết quả");
        btnSave.setFont(UngDungPhongChu.BODY_BOLD);
        btnSave.setBackground(new Color(16, 104, 188));
        btnSave.setForeground(Color.WHITE);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.addActionListener(e -> saveResult());

        btnCancel = new JButton("Đóng");
        btnCancel.setFont(UngDungPhongChu.BODY);
        btnCancel.addActionListener(e -> dispose());

        var configuration = new JButton("Cấu hình CLS");
        configuration.addActionListener(event -> showConfiguration(this, executionSettings));
        bottomPanel.add(configuration);
        bottomPanel.add(btnSave);
        bottomPanel.add(btnCancel);
        add(bottomPanel, BorderLayout.SOUTH);

        loadExistingFromRepository();
    }

    private void loadExistingFromRepository() {
        btnSave.setEnabled(false);
        if (orderId <= 0) return;
        try {
            var orderOpt = services.serviceRepository().getById(orderId);
            if (orderOpt.isEmpty()) {
                btnSave.setToolTipText("Không tìm thấy chỉ định dịch vụ.");
                return;
            }
            executionSettings = services.serviceRepository().getExecutionSettings(orderId);
            orderOpt.ifPresent(order -> {
                existingImageAttachments = Objects.toString(order.getDanhSachHinhAnhUrl(), "");
                if (order.getKetQua() != null && !order.getKetQua().isBlank()) {
                    txtSummary.setText(order.getKetQua());
                }
                if (order.getKetLuan() != null && !order.getKetLuan().isBlank()) {
                    txtConclusion.setText(order.getKetLuan());
                }
                if (order.getTrangThai() == TrangThaiChiDinhDichVu.DA_CO_KET_QUA
                        || order.getTrangThai() == TrangThaiChiDinhDichVu.DA_HUY) {
                    applyReadOnlyMode(order.getTrangThai());
                }
            });
            var existingLines = services.serviceRepository().getKetQuaByChiDinh(orderId);
            if (existingLines != null && !existingLines.isEmpty()) {
                tableModel.setRowCount(0);
                for (var line : existingLines) {
                    tableModel.addRow(new Object[]{
                            Objects.toString(line.getTenChiSo(), ""),
                            Objects.toString(line.getGiaTri(), ""),
                            Objects.toString(line.getKhoangThamChieu(), ""),
                            Objects.toString(line.getDonViDo(), ""),
                            Objects.toString(line.getDanhGia(), "")
                    });
                }
            }
            if (tableModel.getRowCount() == 0 && !readOnlyMode) {
                tableModel.addRow(new Object[]{serviceName, "", "", executionSettings.donViXetNghiem(), ""});
            }
            btnSave.setEnabled(!readOnlyMode);
        } catch (RuntimeException ex) {
            btnSave.setToolTipText("Không tải được cấu hình hoặc kết quả. Mở lại chỉ định để thử lại.");
            btnSave.setText("Không tải được chỉ định");
        }
    }

    static void showConfiguration(Component parent, ThietLapThucHienDichVu settings) {
        if (GraphicsEnvironment.isHeadless()) return;
        var entries = new ArrayList<String>();
        addSetting(entries, "Đơn vị xét nghiệm", settings.donViXetNghiem());
        addSetting(entries, "Thông tin tham chiếu", settings.thamChieuXetNghiem());
        addSetting(entries, "Hướng dẫn nhập và trả kết quả", settings.huongDanTraKetQua());
        addSetting(entries, "Mẫu nội dung", settings.mauNoiDungKetQua());
        addSetting(entries, "Mẫu kết luận", settings.mauKetLuan());
        var text = new JTextArea(entries.isEmpty() ? "Dịch vụ chưa có nội dung cấu hình CLS."
                : String.join("\n\n", entries), 12, 54);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setCaretPosition(0);
        JOptionPane.showMessageDialog(parent, new JScrollPane(text), "Cấu hình CLS", JOptionPane.PLAIN_MESSAGE);
    }

    private static void addSetting(List<String> entries, String title, String value) {
        if (!value.isBlank()) entries.add(title + "\n" + value);
    }

    private void applyReadOnlyMode(TrangThaiChiDinhDichVu status) {
        this.readOnlyMode = true;
        setTitle("Xem Kết Quả Cận Lâm Sàng - CLS-" + orderId + " (Chỉ đọc)");
        if (table != null) table.setEnabled(false);
        if (txtSummary != null) txtSummary.setEditable(false);
        if (txtConclusion != null) txtConclusion.setEditable(false);
        if (btnAddRow != null) btnAddRow.setEnabled(false);
        if (btnDeleteRow != null) btnDeleteRow.setEnabled(false);
        if (btnSave != null) {
            btnSave.setEnabled(false);
            btnSave.setText(status == TrangThaiChiDinhDichVu.DA_HUY ? "Đã hủy" : "Đã trả kết quả");
        }
    }

    public void saveResult() {
        if (readOnlyMode || !btnSave.isEnabled()) return;
        if (table != null && table.isEditing() && table.getCellEditor() != null) {
            table.getCellEditor().stopCellEditing();
        }
        btnSave.setEnabled(false);

        List<XetNghiemKetQuaMatHangDauVao> items = new ArrayList<>();
        for (int r = 0; r < tableModel.getRowCount(); r++) {
            String name = Objects.toString(tableModel.getValueAt(r, 0), "");
            String val = Objects.toString(tableModel.getValueAt(r, 1), "");
            String ref = Objects.toString(tableModel.getValueAt(r, 2), "");
            String unit = Objects.toString(tableModel.getValueAt(r, 3), "");
            String eval = Objects.toString(tableModel.getValueAt(r, 4), "");
            if (!name.isBlank() && !val.isBlank()) items.add(new XetNghiemKetQuaMatHangDauVao(name, val, unit, ref, eval));
        }

        if (items.isEmpty() && txtSummary.getText().isBlank() && txtConclusion.getText().isBlank()) {
            btnSave.setEnabled(true);
            if (isShowing()) JOptionPane.showMessageDialog(this, "Nhập kết quả trước khi lưu.");
            return;
        }

        PhanQuyenNguCanh auth = vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh.current();
        if (auth == null) {
            btnSave.setEnabled(true);
            if (isShowing()) JOptionPane.showMessageDialog(this, "Phiên làm việc không hợp lệ. Vui lòng đăng nhập lại.");
            return;
        }
        KiemToanTacNhanNguCanh audit = new KiemToanTacNhanNguCanh(auth.employeeId(), auth.roleId(),
                auth.roleCode(), auth.facilityId(), auth.departmentId(), "DESKTOP", UUID.randomUUID());

        new SwingWorker<List<vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi.CauHinh>,Void>(){
            protected List<vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi.CauHinh> doInBackground(){return services.recordResult().getConsumptionDefinition(auth,orderId);}
            protected void done(){try{List<vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi.CauHinh> definition=get();
                var actual=TieuHaoDichVuNhapLieu.ask(CanLamSangKetQuaNhapLieuHopThoai.this,definition);if(actual==null){btnSave.setEnabled(true);return;}
                persistResult(items,auth,audit,actual);
            }catch(Exception ex){btnSave.setEnabled(true);if(isShowing())JOptionPane.showMessageDialog(CanLamSangKetQuaNhapLieuHopThoai.this,"Không tải được định mức tiêu hao: "+ex.getMessage(),"Lỗi",JOptionPane.ERROR_MESSAGE);}}
        }.execute();
    }

    private void persistResult(List<XetNghiemKetQuaMatHangDauVao> items,PhanQuyenNguCanh auth,KiemToanTacNhanNguCanh audit,
            List<vn.clinicmanagement.application.inventory.TieuHaoDichVuCongKetNoi.SoLuongThucTe> actual){
        RecordDichVuKetQuaCommand cmd=new RecordDichVuKetQuaCommand(orderId,txtSummary.getText().trim(),txtConclusion.getText().trim(),existingImageAttachments,items,auth,audit,actual,consumptionOperationKey,true);
        new SwingWorker<KetQua<RecordDichVuKetQuaBienNhan>,Void>(){
            protected KetQua<RecordDichVuKetQuaBienNhan> doInBackground(){var result=services.recordResult().handle(cmd);if(result!=null&&result.isSuccess())XetNghiemDuLieuNguon.ensureSpecimenTimestampsOnResult(orderId,auth.employeeId());return result;}
            protected void done(){btnSave.setEnabled(true);try{var result=get();if(result!=null&&result.isSuccess()){
                String status=result.getValue().map(RecordDichVuKetQuaBienNhan::consumptionStatus).orElse("PENDING");
                String consumptionMessage=switch(status){
                    case "POSTED" -> " Đã ghi tiêu hao vào kho tủ trực.";
                    case "PENDING" -> " Tiêu hao đang chờ do kho thiếu tồn.";
                    case "NO_DEFINITION" -> " Chưa có định mức tiêu hao cho dịch vụ.";
                    case "NOT_TRIGGERED" -> " Định mức của dịch vụ này dùng thời điểm kê khác.";
                    default -> "";
                };
                if(isShowing()&&!GraphicsEnvironment.isHeadless())JOptionPane.showMessageDialog(CanLamSangKetQuaNhapLieuHopThoai.this,"Đã lưu kết quả CLS-"+orderId+" thành công."+consumptionMessage,"Thành Công",JOptionPane.INFORMATION_MESSAGE);
                if(onSavedCallback!=null)onSavedCallback.run();dispose();
            }else{String message=result==null?"Lỗi không xác định.":result.getError().map(e->e.message()).orElse("Lỗi không xác định.");if(isShowing()&&!GraphicsEnvironment.isHeadless())JOptionPane.showMessageDialog(CanLamSangKetQuaNhapLieuHopThoai.this,"Không thể lưu kết quả: "+message,"Thông Báo Lỗi",JOptionPane.ERROR_MESSAGE);}}
            catch(Exception ex){if(isShowing()&&!GraphicsEnvironment.isHeadless())JOptionPane.showMessageDialog(CanLamSangKetQuaNhapLieuHopThoai.this,"Lỗi hệ thống: "+ex.getMessage(),"Lỗi",JOptionPane.ERROR_MESSAGE);}}
        }.execute();
    }

    public int getOrderId() { return orderId; }
    public String getPatientName() { return patientName; }
    public String getServiceName() { return serviceName; }
    public JButton getBtnSave() { return btnSave; }
    public JButton getBtnCancel() { return btnCancel; }
    public JButton getBtnAddRow() { return btnAddRow; }
    public JButton getBtnDeleteRow() { return btnDeleteRow; }
    public JTextArea getTxtSummary() { return txtSummary; }
    public JTextArea getTxtDescription() { return txtSummary; }
    public JTextArea getTxtConclusion() { return txtConclusion; }
    public DefaultTableModel getTableModel() { return tableModel; }
    public JTable getTable() { return table; }
}
