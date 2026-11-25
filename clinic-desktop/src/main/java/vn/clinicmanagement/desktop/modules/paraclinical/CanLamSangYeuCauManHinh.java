package vn.clinicmanagement.desktop.modules.paraclinical;

import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.core.components.CoKieuBang;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.config.GiaoDienHangSo;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;
import vn.clinicmanagement.infrastructure.persistence.CoSoDuLieuKetNoi;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Phân hệ Cận Lâm Sàng & Chẩn Đoán Hình Ảnh (HD-INTEG-03).
 * Nâng cấp từ bảng tĩnh sang phân hệ điều hành cận lâm sàng hoàn chỉnh:
 * - Tiếp nhận chỉ định cận lâm sàng theo y lệnh bác sĩ.
 * - Bộ lọc nâng cao: Tìm kiếm bệnh nhân / mã chỉ định, lọc theo trạng thái phiếu.
 * - Mở hộp thoại nhập kết quả CLS (ParaclinicalResultEntryDialog) cập nhật vào CSDL.
 * - Đấu nối Clean Architecture qua ParaclinicalCompositionRoot.Services.
 */
public class CanLamSangYeuCauManHinh extends CoSoManHinhManHinh {

    public static class ParaclinicalOrderRow {
        public final int id;
        public final String patientName;
        public final String serviceName;
        public final String doctorName;
        public final String status;
        public final String statusDisplay;
        public final String paymentStatus;

        public ParaclinicalOrderRow(int id, String patientName, String serviceName,
                                   String doctorName, String status, String statusDisplay) {
            this(id, patientName, serviceName, doctorName, status, statusDisplay, "Chưa có thông tin");
        }

        public ParaclinicalOrderRow(int id, String patientName, String serviceName,
                String doctorName, String status, String statusDisplay, String paymentStatus) {
            this.paymentStatus = paymentStatus;
            this.id = id;
            this.patientName = patientName;
            this.serviceName = serviceName;
            this.doctorName = doctorName;
            this.status = status;
            this.statusDisplay = statusDisplay;
        }
    }

    private final CanLamSangGhepNoiGoc.Services services;
    private final List<ParaclinicalOrderRow> allOrders = new ArrayList<>();
    private final List<ParaclinicalOrderRow> displayedOrders = new ArrayList<>();

    private DefaultTableModel tableModel;
    private CoKieuBang table;
    private JTextField txtSearch;
    private JComboBox<String> cboStatusFilter;
    private JButton btnEnterResult;
    private JButton btnRefresh;

    public CanLamSangYeuCauManHinh() {
        this(GraphicsEnvironment.isHeadless() || System.getProperty("surefire.test.class.path") != null
                ? CanLamSangGhepNoiGoc.createDefault() : CanLamSangGhepNoiGoc.createProduction());
    }

    public CanLamSangYeuCauManHinh(CanLamSangGhepNoiGoc.Services services) {
        super("SERVICES", "CĐHA & Thăm Dò", "🔬");
        this.services = services != null ? services : CanLamSangGhepNoiGoc.createDefault();
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 10));
        setBackground(GiaoDienHangSo.BG_PANEL);
        setBorder(new EmptyBorder(12, 16, 12, 16));

        // --- 1. NORTH: TIÊU ĐỀ & THANH CÔNG CỤ TÌM KIẾM / LỌC TRẠNG THÁI ---
        JPanel northPanel = new JPanel(new BorderLayout(0, 8));
        northPanel.setOpaque(false);

        JLabel title = new JLabel("🔬 CHẨN ĐOÁN HÌNH ẢNH & THĂM DÒ CHỨC NĂNG");
        title.setFont(GiaoDienHangSo.FONT_SECTION);
        title.setForeground(new Color(24, 85, 130));
        northPanel.add(title, BorderLayout.NORTH);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 218, 230), 1),
                new EmptyBorder(4, 8, 4, 8)
        ));

        JLabel lblSearch = new JLabel("Tìm kiếm:");
        lblSearch.setFont(UngDungPhongChu.BODY_BOLD);

        txtSearch = new JTextField(18);
        txtSearch.setFont(UngDungPhongChu.BODY);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tên bệnh nhân, mã CLS...");
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applyFilter();
            }
        });

        JLabel lblFilter = new JLabel("Trạng thái:");
        lblFilter.setFont(UngDungPhongChu.BODY_BOLD);

        cboStatusFilter = new JComboBox<>(new String[]{
                "Tất cả trạng thái",
                "Chờ thực hiện",
                "Đang thực hiện",
                "Đã có kết quả",
                "Đã hủy"
        });
        cboStatusFilter.setFont(UngDungPhongChu.BODY);
        cboStatusFilter.addActionListener(e -> applyFilter());

        btnEnterResult = new JButton("🔬 Nhập Kết Quả / Trả Kết Quả");
        btnEnterResult.setFont(UngDungPhongChu.BODY_BOLD);
        btnEnterResult.setBackground(new Color(16, 104, 188));
        btnEnterResult.setForeground(Color.WHITE);
        btnEnterResult.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnEnterResult.addActionListener(e -> openResultEntryDialogForSelected());

        btnRefresh = new JButton("🔄 Làm Mới");
        btnRefresh.setFont(UngDungPhongChu.BODY);
        btnRefresh.addActionListener(e -> refreshData());

        toolbar.add(lblSearch);
        toolbar.add(txtSearch);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(lblFilter);
        toolbar.add(cboStatusFilter);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(btnEnterResult);
        toolbar.add(btnRefresh);

        northPanel.add(toolbar, BorderLayout.CENTER);
        add(northPanel, BorderLayout.NORTH);

        // --- 2. CENTER: BẢNG CHỈ ĐỊNH DỊCH VỤ CẬN LÂM SÀNG ---
        String[] cols = {"Mã Chỉ Định", "Bệnh Nhân", "Dịch Vụ Cận Lâm Sàng", "Bác Sĩ Yêu Cầu", "Trạng Thái", "Thanh toán lượt khám"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new CoKieuBang(tableModel);
        table.setRowHeight(28);
        table.setFont(UngDungPhongChu.BODY);

        // Renderer cho cột Trạng Thái với badge màu
        table.getColumnModel().getColumn(4).setCellRenderer(new ParaclinicalStatusCellRenderer());

        // Xử lý Double Click và Context Menu
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    openResultEntryDialogForSelected();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowContextMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowContextMenu(e);
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshData();
    }

    public CanLamSangGhepNoiGoc.Services getServices() {
        return services;
    }

    public CoKieuBang getTable() {
        return table;
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public JTextField getTxtSearch() {
        return txtSearch;
    }

    public JComboBox<String> getCboStatusFilter() {
        return cboStatusFilter;
    }

    public JButton getBtnEnterResult() {
        return btnEnterResult;
    }

    public JButton getBtnRefresh() {
        return btnRefresh;
    }

    public List<ParaclinicalOrderRow> getAllOrders() {
        return allOrders;
    }

    public List<ParaclinicalOrderRow> getDisplayedOrders() {
        return displayedOrders;
    }

    private boolean isModalDialogAllowed() {
        if (GraphicsEnvironment.isHeadless()) return false;
        String surefire = System.getProperty("surefire.test.class.path");
        return surefire == null || surefire.isBlank();
    }

    private boolean isDatabaseAllowed() {
        if (GraphicsEnvironment.isHeadless()) return false;
        String surefire = System.getProperty("surefire.test.class.path");
        return surefire == null || surefire.isBlank();
    }

    public void openResultEntryDialogForSelected() {
        int selected = table.getSelectedRow();
        if (selected < 0 || selected >= displayedOrders.size()) {
            if (isModalDialogAllowed()) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng chọn một phiếu chỉ định để nhập kết quả.",
                        "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        ParaclinicalOrderRow row = displayedOrders.get(selected);
        if ("DA_HUY".equalsIgnoreCase(row.status)) {
            if (isModalDialogAllowed()) {
                JOptionPane.showMessageDialog(this,
                        "Chỉ định CLS-" + row.id + " đã bị hủy, không thể nhập kết quả.",
                        "Thông báo",
                        JOptionPane.WARNING_MESSAGE);
            }
            return;
        }
        Window owner = SwingUtilities.getWindowAncestor(this);
        CanLamSangKetQuaNhapLieuHopThoai dialog = new CanLamSangKetQuaNhapLieuHopThoai(
                owner,
                row.id,
                row.patientName,
                row.serviceName,
                services,
                this::refreshData
        );
        if (isModalDialogAllowed()) {
            dialog.setVisible(true);
        }
    }

    private void maybeShowContextMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) return;
        int row = table.rowAtPoint(e.getPoint());
        if (row >= 0 && row < displayedOrders.size()) {
            table.setRowSelectionInterval(row, row);
            ParaclinicalOrderRow order = displayedOrders.get(row);

            JPopupMenu menu = new JPopupMenu();
            boolean isCompleted = "DA_CO_KET_QUA".equalsIgnoreCase(order.status) || "HOAN_THANH".equalsIgnoreCase(order.status);
            boolean isCancelled = "DA_HUY".equalsIgnoreCase(order.status);
            JMenuItem miResult = new JMenuItem((isCompleted ? "🔍 Xem Kết Quả (CLS-" : "🔬 Nhập Kết Quả (CLS-") + order.id + ")");
            miResult.setEnabled(!isCancelled);
            miResult.addActionListener(ev -> openResultEntryDialogForSelected());
            menu.add(miResult);

            JMenuItem miDetail = new JMenuItem("📄 Xem Thông Tin");
            miDetail.addActionListener(ev -> {
                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(this,
                            "Mã chỉ định: CLS-" + order.id + "\nBệnh nhân: " + order.patientName +
                                    "\nDịch vụ: " + order.serviceName + "\nTrạng thái: " + order.statusDisplay,
                            "Thông Tin Chỉ Định",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            });
            menu.add(miDetail);

            menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    private void applyFilter() {
        tableModel.setRowCount(0);
        displayedOrders.clear();

        String kw = txtSearch != null ? txtSearch.getText().trim().toLowerCase() : "";
        String statusFilter = cboStatusFilter != null ? (String) cboStatusFilter.getSelectedItem() : "Tất cả trạng thái";

        for (ParaclinicalOrderRow r : allOrders) {
            // Lọc theo trạng thái
            if (statusFilter != null && !"Tất cả trạng thái".equals(statusFilter)) {
                if (!statusFilter.equalsIgnoreCase(r.statusDisplay) && !statusFilter.equalsIgnoreCase(r.status)) {
                    continue;
                }
            }

            // Lọc theo từ khóa
            if (!kw.isEmpty()) {
                String idStr = "cls-" + r.id;
                boolean match = idStr.contains(kw)
                        || (r.patientName != null && r.patientName.toLowerCase().contains(kw))
                        || (r.serviceName != null && r.serviceName.toLowerCase().contains(kw))
                        || (r.doctorName != null && r.doctorName.toLowerCase().contains(kw));
                if (!match) continue;
            }

            displayedOrders.add(r);
            tableModel.addRow(new Object[]{
                    "CLS-" + r.id,
                    r.patientName,
                    r.serviceName,
                    r.doctorName,
                    r.statusDisplay,
                    r.paymentStatus
            });
        }
    }

    @Override
    public void refreshData() {
        SwingWorker<List<ParaclinicalOrderRow>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ParaclinicalOrderRow> doInBackground() {
                List<ParaclinicalOrderRow> list = new ArrayList<>();
                boolean dbLoaded = false;

                if (isDatabaseAllowed()) {
                    var context = vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh.current();
                    if (context == null) return list;
                    try (Connection conn = CoSoDuLieuKetNoi.getConnection();
                         PreparedStatement ps = conn.prepareStatement(
                                 "SELECT cd.id, bn.ho + ' ' + bn.ten AS tenBenhNhan, dv.tenDichVu, " +
                                 "       ISNULL(nv.ho + ' ' + nv.ten, N'Bác sĩ điều trị') AS bacSiChiDinh, cd.trangThai, " +
                                 "       inv.invoiceId, inv.invoiceStatus, inv.remaining " +
                                 "FROM ChiDinhDichVu cd " +
                                 "JOIN DanhMucDichVu dv ON cd.maDichVu = dv.id " +
                                 "JOIN LuotKham lk ON cd.maLuotKham = lk.id " +
                                 "JOIN BenhNhan bn ON lk.maBenhNhan = bn.id " +
                                 "LEFT JOIN NhanVien nv ON cd.maBacSiChiDinh = nv.id " +
                                 "OUTER APPLY (" +
                                 "    SELECT MAX(h.id) AS invoiceId, " +
                                 "           MAX(h.trangThai) AS invoiceStatus, " +
                                 "           SUM(CASE WHEN h.trangThai <> 'DA_HUY' THEN (h.soTienBenhNhanChiTra - h.daThanhToan) ELSE 0 END) AS remaining " +
                                 "    FROM HoaDon h " +
                                 "    WHERE h.maLuotKham = lk.id AND h.isDeleted = 0 AND h.trangThai <> 'DA_HUY'" +
                                 ") inv " +
                                 "WHERE cd.isDeleted=0 AND lk.isDeleted=0 AND lk.maCoSo=? " +
                                 "AND dv.loaiDichVu IN ('CDHA', 'CHAN_DOAN_HINH_ANH', 'DICH_VU_KY_THUAT') " +
                                 "ORDER BY cd.id DESC")) {
                        ps.setInt(1, context.facilityId());
                        try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String rawStatus = rs.getString("trangThai");
                            String display = formatStatus(rawStatus);
                            list.add(new ParaclinicalOrderRow(
                                    rs.getInt("id"),
                                    rs.getString("tenBenhNhan"),
                                    rs.getString("tenDichVu"),
                                    rs.getString("bacSiChiDinh"),
                                    rawStatus != null ? rawStatus : "CHO_THUC_HIEN",
                                    display,
                                    formatPaymentStatus(rs.getObject("invoiceId") != null,
                                            rs.getString("invoiceStatus"), rs.getBigDecimal("remaining"))
                            ));
                            dbLoaded = true;
                        }
                        }
                        return list;
                    } catch (Exception ex) {
                        throw new IllegalStateException("Không thể tải danh sách CLS từ cơ sở dữ liệu.", ex);
                    }
                }

                // Fallback nếu CSDL chưa có dữ liệu hoặc offline
                if (!dbLoaded || list.isEmpty()) {
                    if (services != null && services.serviceRepository() != null) {
                        List<ChiDinhDichVu> orders = services.serviceRepository().getAll();
                        for (ChiDinhDichVu cd : orders) {
                            String rawSt = cd.getTrangThai() != null ? cd.getTrangThai().name() : "CHO_THUC_HIEN";
                            list.add(new ParaclinicalOrderRow(
                                    cd.getId() != null ? cd.getId() : 1,
                                    "Nguyễn Văn An",
                                    "Tổng phân tích tế bào máu ngoại vi",
                                    "BS. Hoàng Tiến",
                                    rawSt,
                                    formatStatus(rawSt)
                            ));
                        }
                    }

                    if (list.isEmpty()) {
                        list.add(new ParaclinicalOrderRow(
                                101,
                                "NGUYỄN MẠNH HÙNG",
                                "Siêu âm màu tổng quát ổ bụng",
                                "BS. Nguyễn Văn A",
                                "CHO_THUC_HIEN",
                                "Chờ thực hiện"
                        ));
                        list.add(new ParaclinicalOrderRow(
                                102,
                                "TRẦN THỊ MAI",
                                "Chụp X-Quang ngực thẳng tiêu chuẩn",
                                "BS. Lê Thị B",
                                "DA_CO_KET_QUA",
                                "Đã có kết quả"
                        ));
                    }
                }
                return list;
            }

            @Override
            protected void done() {
                try {
                    List<ParaclinicalOrderRow> res = get();
                    allOrders.clear();
                    allOrders.addAll(res);
                    applyFilter();
                } catch (Exception ex) {
                    allOrders.clear();
                    applyFilter();
                    if (isModalDialogAllowed()) JOptionPane.showMessageDialog(CanLamSangYeuCauManHinh.this,
                            "Không thể tải danh sách CLS. Vui lòng thử Làm mới.", "Lỗi tải dữ liệu", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    static String formatPaymentStatus(boolean hasInvoice, String status, java.math.BigDecimal remaining) {
        if (!hasInvoice) return "Chưa có hóa đơn";
        if ("DA_HUY".equals(status)) return "Hóa đơn đã hủy";
        if (remaining == null) return "Chưa có thông tin";
        if (remaining.signum() <= 0) return "Đã thu đủ";
        return "Còn nợ " + new java.text.DecimalFormat("#,##0.##").format(remaining) + " VNĐ";
    }

    private static String formatStatus(String raw) {
        if (raw == null || raw.isBlank()) return "Chờ thực hiện";
        if ("HOAN_THANH".equalsIgnoreCase(raw.trim())) return "Đã có kết quả";
        TrangThaiChiDinhDichVu st = TrangThaiChiDinhDichVu.fromCode(raw);
        if (st.isResultReleased()) return "Đã có kết quả";
        if (st.isExecuting() || st.isApproved() || st == TrangThaiChiDinhDichVu.DA_LAY_MAU) return "Đang thực hiện";
        if (st.isCancelled()) return TrangThaiChiDinhDichVu.DA_HUY.getDescription();
        return "Chờ thực hiện";
    }

    /**
     * CellRenderer đặc biệt cho cột Trạng Thái Chỉ Định CLS.
     */
    private static class ParaclinicalStatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            lbl.setFont(UngDungPhongChu.BODY_BOLD);

            String text = value != null ? value.toString() : "";
            if (!isSelected) {
                if ("Đã có kết quả".equalsIgnoreCase(text)) {
                    lbl.setForeground(new Color(25, 120, 50));
                } else if ("Đang thực hiện".equalsIgnoreCase(text)) {
                    lbl.setForeground(new Color(16, 104, 188));
                } else if ("Chờ thực hiện".equalsIgnoreCase(text)) {
                    lbl.setForeground(new Color(185, 110, 0));
                } else if ("Đã hủy".equalsIgnoreCase(text)) {
                    lbl.setForeground(new Color(120, 120, 120));
                }
            }
            return lbl;
        }
    }
}
