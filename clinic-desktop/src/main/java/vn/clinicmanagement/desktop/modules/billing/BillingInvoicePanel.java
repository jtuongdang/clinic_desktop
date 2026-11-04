package vn.clinicmanagement.desktop.modules.billing;

import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.core.components.StyledTable;
import vn.clinicmanagement.desktop.core.config.AppFonts;
import vn.clinicmanagement.desktop.core.config.UIConstants;
import vn.clinicmanagement.desktop.modules.examination.components.print.MedicalBillingSlipDialog;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Phân hệ Quản Lý Viện Phí & Hóa Đơn (Billing & Invoicing)
 * Thiết kế chuẩn EHC HIS, tối ưu cho màn hình 13-inch và đồng bộ phong cách hệ thống.
 */
public class BillingInvoicePanel extends BaseViewPanel {

    private final DecimalFormat currencyFormat = new DecimalFormat("#,##0 'VNĐ'");
    private DefaultTableModel invoiceTableModel;
    private DefaultTableModel detailTableModel;
    private StyledTable invoiceTable;
    private StyledTable detailTable;
    private JTextField txtSearch;
    private JComboBox<String> cboFilterStatus;
    private JLabel lblTotalAmount;
    private JLabel lblPaidAmount;
    private JLabel lblUnpaidAmount;

    public BillingInvoicePanel() {
        super("billing", "Quản Lý Viện Phí", "💳");
        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. THANH CÔNG CỤ ĐỈNH (Top Action Toolbar)
        add(buildToolbar(), BorderLayout.NORTH);

        // 2. KHU VỰC TRUNG TÂM (SplitPane: Danh sách hóa đơn phía trên/trái, Chi tiết phía dưới/phải)
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setDividerLocation(360);
        splitPane.setResizeWeight(0.55);
        splitPane.setDividerSize(5);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        splitPane.setTopComponent(buildInvoiceListSection());
        splitPane.setBottomComponent(buildInvoiceDetailSection());

        add(splitPane, BorderLayout.CENTER);

        // 3. THANH TỔNG KẾT & TRẠNG THÁI (Summary Footer)
        add(buildSummaryFooter(), BorderLayout.SOUTH);

        refreshData();
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setBackground(new Color(245, 248, 252));
        toolbar.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(210, 220, 230)),
                new EmptyBorder(6, 12, 6, 12)
        ));

        // Nhóm tìm kiếm & Bộ lọc
        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftGroup.setOpaque(false);

        JLabel lblTitle = new JLabel("QUẢN LÝ VIỆN PHÍ & THU NGÂN");
        lblTitle.setFont(AppFonts.SECTION);
        lblTitle.setForeground(new Color(24, 85, 130));
        leftGroup.add(lblTitle);

        JSeparator sep1 = new JSeparator(SwingConstants.VERTICAL);
        sep1.setPreferredSize(new Dimension(2, 22));
        leftGroup.add(sep1);

        JLabel lblSearch = new JLabel("Tìm kiếm (Mã BN/Tên):");
        lblSearch.setFont(AppFonts.BODY);
        leftGroup.add(lblSearch);

        txtSearch = new JTextField(12);
        txtSearch.setFont(AppFonts.BODY);
        txtSearch.setBackground(new Color(255, 255, 220));
        leftGroup.add(txtSearch);

        JLabel lblStatus = new JLabel("Trạng thái:");
        lblStatus.setFont(AppFonts.BODY);
        leftGroup.add(lblStatus);

        cboFilterStatus = new JComboBox<>(new String[]{"Tất cả hóa đơn", "Chưa thanh toán", "Đã thanh toán", "Miễn giảm / BHYT"});
        cboFilterStatus.setFont(AppFonts.BODY);
        leftGroup.add(cboFilterStatus);

        // Nhóm nút chức năng bên phải
        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightGroup.setOpaque(false);

        JButton btnCollect = createButton(" Thu Tiền (F2)", new Color(25, 135, 84), Color.WHITE);
        btnCollect.addActionListener(e -> handleCollectPayment());
        rightGroup.add(btnCollect);

        JButton btnPrintSlip = createButton(" In Bảng Kê (F7)", new Color(13, 110, 253), Color.WHITE);
        btnPrintSlip.addActionListener(e -> handleOpenBillingSlip());
        rightGroup.add(btnPrintSlip);

        JButton btnRefresh = createButton("🔄 Làm Mới", new Color(108, 117, 125), Color.WHITE);
        btnRefresh.addActionListener(e -> refreshData());
        rightGroup.add(btnRefresh);

        toolbar.add(leftGroup, BorderLayout.WEST);
        toolbar.add(rightGroup, BorderLayout.EAST);
        return toolbar;
    }

    private JPanel buildInvoiceListSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.COLOR_BORDER),
                new EmptyBorder(4, 8, 4, 8)
        ));

        JLabel title = new JLabel("DANH SÁCH HÓA ĐƠN & PHIẾU THU KHÁM BỆNH");
        title.setFont(AppFonts.BODY_BOLD);
        title.setForeground(new Color(40, 70, 100));
        title.setBorder(new EmptyBorder(4, 4, 6, 4));
        panel.add(title, BorderLayout.NORTH);

        String[] cols = {
                "STT", "Số Phiếu Thu", "Mã BN", "Họ Và Tên Bệnh Nhân", "Ngày Lập",
                "Đối Tượng", "Tổng Tiền", "BHYT Chi Trả", "BN Cần Trả", "Đã Thu", "Còn Nợ", "Trạng Thái"
        };
        invoiceTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        invoiceTable = new StyledTable(invoiceTableModel);
        invoiceTable.setFont(AppFonts.BODY);
        invoiceTable.setRowHeight(26);

        // Căn chỉnh cột số tiền sang phải
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : new int[]{6, 7, 8, 9, 10}) {
            invoiceTable.getColumnModel().getColumn(c).setCellRenderer(rightRenderer);
        }

        invoiceTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedInvoiceDetails();
            }
        });

        JScrollPane scrollPane = new JScrollPane(invoiceTable);
        scrollPane.setBorder(new LineBorder(UIConstants.COLOR_BORDER, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildInvoiceDetailSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(4, 8, 4, 8));

        JLabel title = new JLabel("CHI TIẾT DỊCH VỤ / THUỐC TRONG PHIẾU THU ĐANG CHỌN");
        title.setFont(AppFonts.BODY_BOLD);
        title.setForeground(new Color(40, 70, 100));
        title.setBorder(new EmptyBorder(4, 4, 6, 4));
        panel.add(title, BorderLayout.NORTH);

        String[] cols = {
                "STT", "Khoa/Phòng Thực Hiện", "Tên Dịch Vụ / Thuốc / Vật Tư", "ĐVT",
                "Số Lượng", "Đơn Giá", "Thành Tiền", "Tỷ Lệ BHYT", "BHYT Trả", "Bệnh Nhân Trả"
        };
        detailTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        detailTable = new StyledTable(detailTableModel);
        detailTable.setFont(AppFonts.BODY);
        detailTable.setRowHeight(25);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int c : new int[]{4, 5, 6, 7, 8, 9}) {
            detailTable.getColumnModel().getColumn(c).setCellRenderer(rightRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(detailTable);
        scrollPane.setBorder(new LineBorder(UIConstants.COLOR_BORDER, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildSummaryFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(240, 244, 250));
        footer.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(195, 205, 220)),
                new EmptyBorder(8, 14, 8, 14)
        ));

        JPanel statPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        statPanel.setOpaque(false);

        lblTotalAmount = new JLabel("Tổng doanh thu: 0 VNĐ");
        lblTotalAmount.setFont(AppFonts.BODY_BOLD);
        lblTotalAmount.setForeground(new Color(20, 60, 110));

        lblPaidAmount = new JLabel("Đã thanh toán: 0 VNĐ");
        lblPaidAmount.setFont(AppFonts.BODY_BOLD);
        lblPaidAmount.setForeground(new Color(25, 135, 84));

        lblUnpaidAmount = new JLabel("Còn công nợ: 0 VNĐ");
        lblUnpaidAmount.setFont(AppFonts.BODY_BOLD);
        lblUnpaidAmount.setForeground(new Color(220, 53, 69));

        statPanel.add(lblTotalAmount);
        statPanel.add(new JSeparator(SwingConstants.VERTICAL));
        statPanel.add(lblPaidAmount);
        statPanel.add(new JSeparator(SwingConstants.VERTICAL));
        statPanel.add(lblUnpaidAmount);

        JLabel lblGuide = new JLabel("💡 Mẹo: Nhấn 'In Bảng Kê' để xem trước Mẫu 01/KCCB chuẩn Bộ Y Tế.");
        lblGuide.setFont(AppFonts.SMALL);
        lblGuide.setForeground(new Color(100, 110, 120));

        footer.add(lblGuide, BorderLayout.WEST);
        footer.add(statPanel, BorderLayout.EAST);
        return footer;
    }

    private JButton createButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setFont(AppFonts.BODY_BOLD);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new CompoundBorder(
                new LineBorder(bg.darker(), 1),
                new EmptyBorder(4, 10, 4, 10)
        ));
        return btn;
    }

    private void handleCollectPayment() {
        int row = invoiceTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng chọn một phiếu thu từ danh sách để thực hiện thu tiền!",
                    "Chưa Chọn Hóa Đơn", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String code = (String) invoiceTableModel.getValueAt(row, 1);
        String name = (String) invoiceTableModel.getValueAt(row, 3);
        String unpaid = (String) invoiceTableModel.getValueAt(row, 10);

        if ("0 VNĐ".equals(unpaid) || "0".equals(unpaid)) {
            JOptionPane.showMessageDialog(this,
                    "Hóa đơn " + code + " của bệnh nhân " + name + " đã được thanh toán đầy đủ!",
                    "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Xác nhận thu tiền phiếu [" + code + "] - " + name + "\nSố tiền cần thu: " + unpaid + "\n\nTiến hành xác nhận thanh toán?",
                "Xác Nhận Thu Viện Phí", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            invoiceTableModel.setValueAt(invoiceTableModel.getValueAt(row, 8), row, 9); // Đã thu = Cần trả
            invoiceTableModel.setValueAt("0 VNĐ", row, 10); // Còn nợ = 0
            invoiceTableModel.setValueAt("Đã thanh toán", row, 11);
            JOptionPane.showMessageDialog(this, "Thu tiền thành công cho hóa đơn: " + code, "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            updateSummaryCounters();
        }
    }

    private void handleOpenBillingSlip() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        MedicalBillingSlipDialog dialog = new MedicalBillingSlipDialog(owner);
        dialog.setVisible(true);
    }

    private void loadSelectedInvoiceDetails() {
        detailTableModel.setRowCount(0);
        int row = invoiceTable.getSelectedRow();
        if (row < 0) return;

        String code = (String) invoiceTableModel.getValueAt(row, 1);
        if ("HD26-0001".equals(code)) {
            detailTableModel.addRow(new Object[]{"1", "Phòng Khám Nội 01", "Khám nội tổng quát", "Lượt", "1", "120,000", "120,000", "0%", "0", "120,000"});
            detailTableModel.addRow(new Object[]{"2", "Khoa Xét Nghiệm", "Tổng phân tích tế bào máu ngoại vi bằng máy đếm laser", "Lần", "1", "150,000", "150,000", "0%", "0", "150,000"});
            detailTableModel.addRow(new Object[]{"3", "Khoa CĐHA", "Chụp X-quang tim phổi thẳng", "Phim", "1", "110,000", "110,000", "0%", "0", "110,000"});
            detailTableModel.addRow(new Object[]{"4", "Nhà Thuốc Bệnh Viện", "Augmentin 1g (Amoxicillin/Clavulanate)", "Viên", "14", "18,500", "259,000", "0%", "0", "259,000"});
            detailTableModel.addRow(new Object[]{"5", "Nhà Thuốc Bệnh Viện", "Panadol Extra 500mg", "Viên", "20", "2,000", "40,000", "0%", "0", "40,000"});
        } else if ("HD26-0002".equals(code)) {
            detailTableModel.addRow(new Object[]{"1", "Phòng Khám Tai Mũi Họng", "Nội soi tai mũi họng dải tần hẹp (NBI)", "Lần", "1", "280,000", "280,000", "80%", "224,000", "56,000"});
            detailTableModel.addRow(new Object[]{"2", "Nhà Thuốc Bệnh Viện", "Kháng sinh Cefuroxime 500mg", "Viên", "14", "15,000", "210,000", "80%", "168,000", "42,000"});
            detailTableModel.addRow(new Object[]{"3", "Nhà Thuốc Bệnh Viện", "Thuốc xịt mũi Flixonase", "Lọ", "1", "185,000", "185,000", "0%", "0", "185,000"});
        } else {
            detailTableModel.addRow(new Object[]{"1", "Phòng Khám Đa Khoa", "Khám lâm sàng chuyên khoa", "Lượt", "1", "100,000", "100,000", "0%", "0", "100,000"});
            detailTableModel.addRow(new Object[]{"2", "Khoa Xét Nghiệm", "Định lượng Glucose máu", "Lần", "1", "55,000", "55,000", "0%", "0", "55,000"});
        }
    }

    private void updateSummaryCounters() {
        long total = 0;
        long paid = 0;
        long unpaid = 0;

        for (int r = 0; r < invoiceTableModel.getRowCount(); r++) {
            String strTotal = String.valueOf(invoiceTableModel.getValueAt(r, 6)).replaceAll("[^0-9]", "");
            String strPaid = String.valueOf(invoiceTableModel.getValueAt(r, 9)).replaceAll("[^0-9]", "");
            String strUnpaid = String.valueOf(invoiceTableModel.getValueAt(r, 10)).replaceAll("[^0-9]", "");

            if (!strTotal.isEmpty()) total += Long.parseLong(strTotal);
            if (!strPaid.isEmpty()) paid += Long.parseLong(strPaid);
            if (!strUnpaid.isEmpty()) unpaid += Long.parseLong(strUnpaid);
        }

        lblTotalAmount.setText("Tổng doanh thu: " + currencyFormat.format(total));
        lblPaidAmount.setText("Đã thanh toán: " + currencyFormat.format(paid));
        lblUnpaidAmount.setText("Còn công nợ: " + currencyFormat.format(unpaid));
    }

    @Override
    public void onShow() {
        refreshData();
    }

    @Override
    public void refreshData() {
        invoiceTableModel.setRowCount(0);
        detailTableModel.setRowCount(0);

        invoiceTableModel.addRow(new Object[]{
                "1", "HD26-0001", "21000325", "NGUYỄN MẠNH HÙNG", "31/10/2026 08:30",
                "Viện Phí", "679,000 VNĐ", "0 VNĐ", "679,000 VNĐ", "679,000 VNĐ", "0 VNĐ", "Đã thanh toán"
        });
        invoiceTableModel.addRow(new Object[]{
                "2", "HD26-0002", "21000328", "TRẦN VĂN BÌNH", "31/10/2026 09:15",
                "BHYT (80%)", "675,000 VNĐ", "392,000 VNĐ", "283,000 VNĐ", "0 VNĐ", "283,000 VNĐ", "Chưa thanh toán"
        });
        invoiceTableModel.addRow(new Object[]{
                "3", "HD26-0003", "21000331", "LÊ THỊ MAI", "31/10/2026 09:40",
                "Viện Phí", "155,000 VNĐ", "0 VNĐ", "155,000 VNĐ", "155,000 VNĐ", "0 VNĐ", "Đã thanh toán"
        });
        invoiceTableModel.addRow(new Object[]{
                "4", "HD26-0004", "21000335", "PHẠM HOÀNG LONG", "31/10/2026 10:05",
                "BHYT (100%)", "420,000 VNĐ", "420,000 VNĐ", "0 VNĐ", "0 VNĐ", "0 VNĐ", "Miễn giảm / BHYT"
        });

        if (invoiceTableModel.getRowCount() > 0) {
            invoiceTable.setRowSelectionInterval(0, 0);
        }

        updateSummaryCounters();
    }
}
