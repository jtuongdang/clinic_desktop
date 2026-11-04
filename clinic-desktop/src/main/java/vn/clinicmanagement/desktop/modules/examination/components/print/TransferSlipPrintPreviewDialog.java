package vn.clinicmanagement.desktop.modules.examination.components.print;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Cửa sổ xem trước và in Phiếu Khám Bệnh / Phiếu Chuyển Phòng Khám:
 * "In - DT_001_A6_Phieukhambent.doc"
 * Khớp 100% media_1793381633910_5fdd704b.jpg
 */
public class TransferSlipPrintPreviewDialog extends JDialog {

    private final String patientName;
    private final String patientCode;
    private final String targetRoom;

    public TransferSlipPrintPreviewDialog(Window owner, String patientName, String patientCode, String targetRoom) {
        super(owner, "In - DT_001_A6_Phieukhambent.doc", ModalityType.MODELESS);
        this.patientName = patientName != null ? patientName : "NGUYỄN ĐÌNH PHONG";
        this.patientCode = patientCode != null ? patientCode : "22000306";
        this.targetRoom = targetRoom != null ? targetRoom : "Tầng 2 - A223";

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(owner);

        JPanel mainPanel = createPreviewPanel(patientName, patientCode, targetRoom, () -> {
            JOptionPane.showMessageDialog(this, "Đang gửi lệnh in tới máy in Microsoft Print to PDF...", "In Phiếu Khám Bệnh", JOptionPane.INFORMATION_MESSAGE);
        });

        setContentPane(mainPanel);
    }

    /**
     * Tạo Panel hoàn chỉnh cho màn hình xem trước in ấn (dùng cả trong Dialog và Snapshot Render)
     */
    public static JPanel createPreviewPanel(String pName, String pCode, String room, Runnable onPrint) {
        String name = pName != null ? pName : "NGUYỄN ĐÌNH PHONG";
        String code = pCode != null ? pCode : "22000306";
        String targetRoom = room != null ? room : "Tầng 2 - A223";

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(90, 95, 105));

        // 1. Cột trái: Thanh công cụ in
        JPanel leftBar = new JPanel();
        leftBar.setLayout(new BoxLayout(leftBar, BoxLayout.Y_AXIS));
        leftBar.setPreferredSize(new Dimension(135, 680));
        leftBar.setBackground(new Color(245, 246, 248));
        leftBar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(205, 210, 220)));

        // Nút In to với icon máy in
        JPanel topPrintRow = new JPanel(new BorderLayout(6, 0));
        topPrintRow.setOpaque(false);
        topPrintRow.setBorder(new EmptyBorder(6, 6, 6, 6));

        JButton btnPrint = new JButton();
        btnPrint.setLayout(new BorderLayout());
        btnPrint.setPreferredSize(new Dimension(72, 70));
        btnPrint.setBackground(Color.WHITE);
        btnPrint.setBorder(new LineBorder(new Color(180, 190, 205), 1));
        btnPrint.setFocusPainted(false);

        JLabel lblPrinterIcon = new JLabel(new PrinterBigIcon());
        lblPrinterIcon.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel lblPrintText = new JLabel("<html><center>In<br>(Ctrl+P)<br>F1</center></html>");
        lblPrintText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblPrintText.setHorizontalAlignment(SwingConstants.CENTER);
        lblPrintText.setForeground(new Color(50, 55, 65));

        btnPrint.add(lblPrinterIcon, BorderLayout.CENTER);
        btnPrint.add(lblPrintText, BorderLayout.SOUTH);
        btnPrint.addActionListener(e -> {
            if (onPrint != null) onPrint.run();
        });
        topPrintRow.add(btnPrint, BorderLayout.WEST);

        // Bản in + Checkbox bên phải nút In
        JPanel printOptions = new JPanel();
        printOptions.setLayout(new BoxLayout(printOptions, BoxLayout.Y_AXIS));
        printOptions.setOpaque(false);

        JPanel pnlCopies = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        pnlCopies.setOpaque(false);
        JLabel lblBanIn = new JLabel("Bản in");
        lblBanIn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        JSpinner spCopies = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        spCopies.setPreferredSize(new Dimension(36, 18));
        spCopies.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        pnlCopies.add(lblBanIn);
        pnlCopies.add(spCopies);
        printOptions.add(pnlCopies);

        JCheckBox chkAuto = new JCheckBox("In tự động", true);
        chkAuto.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        chkAuto.setOpaque(false);
        printOptions.add(chkAuto);

        JCheckBox chkNext = new JCheckBox("In tiếp", false);
        chkNext.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        chkNext.setOpaque(false);
        printOptions.add(chkNext);

        topPrintRow.add(printOptions, BorderLayout.CENTER);
        leftBar.add(topPrintRow);

        // Các thiết lập máy in phía dưới
        JPanel formSettings = new JPanel();
        formSettings.setLayout(new BoxLayout(formSettings, BoxLayout.Y_AXIS));
        formSettings.setOpaque(false);
        formSettings.setBorder(new EmptyBorder(0, 6, 6, 6));

        JLabel lblPrinter = new JLabel("Máy In");
        lblPrinter.setFont(AppFonts.SMALL_BOLD);
        formSettings.add(lblPrinter);

        JComboBox<String> cboPrinter = new JComboBox<>(new String[]{"Microsoft Print to PDF", "Canon LBP 2900"});
        cboPrinter.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        cboPrinter.setMaximumSize(new Dimension(122, 22));
        formSettings.add(cboPrinter);
        formSettings.add(Box.createVerticalStrut(8));

        // Tùy chọn & Cấu hình máy in
        JPanel pnlOpt = new JPanel(new BorderLayout());
        pnlOpt.setOpaque(false);
        JLabel lblOpt = new JLabel("Tùy Chọn");
        lblOpt.setFont(AppFonts.SMALL_BOLD);
        JLabel lblCfg = new JLabel("<html><u><font color='#0066CC'>Cấu hình máy in</font></u></html>");
        lblCfg.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        lblCfg.setCursor(new Cursor(Cursor.HAND_CURSOR));
        pnlOpt.add(lblOpt, BorderLayout.WEST);
        pnlOpt.add(lblCfg, BorderLayout.EAST);
        formSettings.add(pnlOpt);

        JComboBox<String> cboPageScope = new JComboBox<>(new String[]{"In tất cả các trang", "Trang hiện tại"});
        cboPageScope.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        cboPageScope.setMaximumSize(new Dimension(122, 22));
        formSettings.add(cboPageScope);
        formSettings.add(Box.createVerticalStrut(4));

        JLabel lblTrangIn = new JLabel("Trang in");
        lblTrangIn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        formSettings.add(lblTrangIn);

        JPanel pnlOrient = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        pnlOrient.setOpaque(false);
        JLabel lblChieu = new JLabel("Chiều giấy");
        lblChieu.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        JComboBox<String> cboOrient = new JComboBox<>(new String[]{"Chiều dọc", "Chiều ngang"});
        cboOrient.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        cboOrient.setPreferredSize(new Dimension(72, 20));
        pnlOrient.add(lblChieu);
        pnlOrient.add(cboOrient);
        formSettings.add(pnlOrient);
        formSettings.add(Box.createVerticalStrut(4));

        JPanel pnlPaperSize = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        pnlPaperSize.setOpaque(false);
        JLabel lblKho = new JLabel("Khổ giấy");
        lblKho.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        JComboBox<String> cboSize = new JComboBox<>(new String[]{"A4 (210mm x 297", "A5 (148mm x 210", "A6 (105mm x 148"});
        cboSize.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        cboSize.setPreferredSize(new Dimension(78, 20));
        pnlPaperSize.add(lblKho);
        pnlPaperSize.add(cboSize);
        formSettings.add(pnlPaperSize);
        formSettings.add(Box.createVerticalStrut(6));

        JCheckBox chkTwoSided = new JCheckBox("In 2 mặt", false);
        chkTwoSided.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        chkTwoSided.setOpaque(false);
        formSettings.add(chkTwoSided);

        JCheckBox chkA5inA4 = new JCheckBox("In A5 vào khay giấy A4", false);
        chkA5inA4.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        chkA5inA4.setOpaque(false);
        formSettings.add(chkA5inA4);
        formSettings.add(Box.createVerticalStrut(12));

        // Xuất File
        JLabel lblExport = new JLabel("Xuất File...");
        lblExport.setFont(AppFonts.SMALL_BOLD);
        formSettings.add(lblExport);
        formSettings.add(Box.createVerticalStrut(4));

        JPanel pnlExport = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnlExport.setOpaque(false);

        JButton btnPdf = new JButton("Pdf", new PdfMiniIcon());
        btnPdf.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnPdf.setPreferredSize(new Dimension(58, 38));
        btnPdf.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnPdf.setHorizontalTextPosition(SwingConstants.CENTER);
        btnPdf.setBackground(Color.WHITE);

        JButton btnWord = new JButton("Word", new WordMiniIcon());
        btnWord.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnWord.setPreferredSize(new Dimension(58, 38));
        btnWord.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnWord.setHorizontalTextPosition(SwingConstants.CENTER);
        btnWord.setBackground(Color.WHITE);

        pnlExport.add(btnPdf);
        pnlExport.add(btnWord);
        formSettings.add(pnlExport);

        leftBar.add(formSettings);
        panel.add(leftBar, BorderLayout.WEST);

        // 2. Center: Giấy in A6 đặt trên nền xám
        JPanel centerPaperContainer = new JPanel(new GridBagLayout());
        centerPaperContainer.setBackground(new Color(90, 95, 105));

        JPanel paperSlip = createPaperSlip(name, code, targetRoom);
        centerPaperContainer.add(paperSlip);
        panel.add(centerPaperContainer, BorderLayout.CENTER);

        // 3. Bottom: Thanh điều hướng trang & Zoom
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(new Color(238, 240, 244));
        bottomBar.setPreferredSize(new Dimension(1100, 26));
        bottomBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(205, 210, 220)));

        JPanel leftNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        leftNav.setOpaque(false);
        JLabel btnMenu = new JLabel(" ≡ ");
        btnMenu.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JLabel btnFirst = new JLabel("|◀");
        JLabel btnPrev = new JLabel("◀");
        JLabel lblPage = new JLabel(" 1 / 1 ");
        lblPage.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JLabel btnNext = new JLabel("▶");
        JLabel btnLast = new JLabel("▶|");
        leftNav.add(btnMenu);
        leftNav.add(btnFirst);
        leftNav.add(btnPrev);
        leftNav.add(lblPage);
        leftNav.add(btnNext);
        leftNav.add(btnLast);
        bottomBar.add(leftNav, BorderLayout.WEST);

        JPanel rightZoom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        rightZoom.setOpaque(false);
        JLabel lblZoomVal = new JLabel("100%");
        lblZoomVal.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        JSlider zoomSlider = new JSlider(50, 200, 100);
        zoomSlider.setPreferredSize(new Dimension(110, 16));
        zoomSlider.setOpaque(false);
        rightZoom.add(lblZoomVal);
        rightZoom.add(zoomSlider);
        bottomBar.add(rightZoom, BorderLayout.EAST);

        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Tạo phiếu in A6 màu trắng với nội dung chi tiết khớp 100% ảnh mẫu
     */
    private static JPanel createPaperSlip(String patientName, String patientCode, String targetRoom) {
        Color navyColor = new Color(16, 42, 110);

        JPanel paper = new JPanel(new BorderLayout());
        paper.setPreferredSize(new Dimension(210, 440));
        paper.setBackground(Color.WHITE);
        paper.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(50, 50, 50), 1),
                new EmptyBorder(12, 12, 12, 12)
        ));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Header Sở Y Tế & Bệnh viện
        JLabel lblHeader1 = new JLabel("SỞ Y TẾ BẮC NINH");
        lblHeader1.setFont(new Font("Times New Roman", Font.PLAIN, 10));
        lblHeader1.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblHeader1.setForeground(navyColor);

        JLabel lblHeader2 = new JLabel("BỆNH VIỆN SẢN NHI");
        lblHeader2.setFont(new Font("Times New Roman", Font.BOLD, 11));
        lblHeader2.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblHeader2.setForeground(navyColor);

        content.add(lblHeader1);
        content.add(lblHeader2);
        content.add(Box.createVerticalStrut(3));

        // Đường kẻ ngang 1
        content.add(createSolidLine(180, navyColor));
        content.add(Box.createVerticalStrut(2));

        // Tên phiếu
        JLabel lblTitle = new JLabel("PHIẾU KHÁM BỆNH");
        lblTitle.setFont(new Font("Times New Roman", Font.BOLD, 13));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTitle.setForeground(navyColor);
        content.add(lblTitle);

        JLabel lblDate = new JLabel("NGÀY KHÁM: 10:58 11/08/2022");
        lblDate.setFont(new Font("Times New Roman", Font.PLAIN, 9));
        lblDate.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblDate.setForeground(navyColor);
        content.add(lblDate);
        content.add(Box.createVerticalStrut(3));

        // Đường kẻ ngang 2
        content.add(createSolidLine(180, navyColor));
        content.add(Box.createVerticalStrut(8));

        // Phòng khám hẹn
        JLabel lblRoom = new JLabel("PK hẹn: " + targetRoom);
        lblRoom.setFont(new Font("Times New Roman", Font.BOLD, 11));
        lblRoom.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblRoom.setForeground(navyColor);
        content.add(lblRoom);
        content.add(Box.createVerticalStrut(2));

        // SỐ: 1 (Chữ rất to, đậm, navy)
        JLabel lblNumber = new JLabel("SỐ: 1");
        lblNumber.setFont(new Font("Times New Roman", Font.BOLD, 30));
        lblNumber.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblNumber.setForeground(navyColor);
        content.add(lblNumber);
        content.add(Box.createVerticalStrut(6));

        // Nét đứt phân cách (Dashed line)
        content.add(createDashedLine(180, navyColor));
        content.add(Box.createVerticalStrut(8));

        // Khối thông tin bệnh nhân
        JLabel lblCode = new JLabel("MÃ BỆNH NHÂN: " + patientCode);
        lblCode.setFont(new Font("Times New Roman", Font.PLAIN, 10));
        lblCode.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblCode.setForeground(navyColor);

        JLabel lblName = new JLabel("HỌ TÊN: " + patientName);
        lblName.setFont(new Font("Times New Roman", Font.BOLD, 10));
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblName.setForeground(navyColor);

        JLabel lblSexAge = new JLabel("Giới tính NS: Nam / 1989 (33 tuổi)");
        lblSexAge.setFont(new Font("Times New Roman", Font.PLAIN, 10));
        lblSexAge.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblSexAge.setForeground(navyColor);

        content.add(lblCode);
        content.add(Box.createVerticalStrut(2));
        content.add(lblName);
        content.add(Box.createVerticalStrut(2));
        content.add(lblSexAge);
        content.add(Box.createVerticalStrut(6));

        // Mã vạch Barcode
        JLabel lblBarcode = new JLabel(new BarcodeMiniIcon(100, 20));
        lblBarcode.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(lblBarcode);
        content.add(Box.createVerticalStrut(6));

        // Khối chữ ký Lễ tân bên phải
        JPanel pnlSign = new JPanel(new BorderLayout());
        pnlSign.setOpaque(false);
        JPanel pnlSignRight = new JPanel();
        pnlSignRight.setLayout(new BoxLayout(pnlSignRight, BoxLayout.Y_AXIS));
        pnlSignRight.setOpaque(false);

        JLabel lblStaffTitle = new JLabel("LỄ TÂN");
        lblStaffTitle.setFont(new Font("Times New Roman", Font.BOLD, 10));
        lblStaffTitle.setForeground(navyColor);
        lblStaffTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblStaff = new JLabel("tiennv");
        lblStaff.setFont(new Font("Times New Roman", Font.PLAIN, 10));
        lblStaff.setForeground(navyColor);
        lblStaff.setAlignmentX(Component.CENTER_ALIGNMENT);

        pnlSignRight.add(lblStaffTitle);
        pnlSignRight.add(Box.createVerticalStrut(2));
        pnlSignRight.add(lblStaff);
        pnlSign.add(pnlSignRight, BorderLayout.EAST);

        content.add(pnlSign);

        paper.add(content, BorderLayout.NORTH);
        return paper;
    }

    private static JComponent createSolidLine(int w, Color color) {
        JComponent comp = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(color);
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        comp.setPreferredSize(new Dimension(w, 1));
        comp.setMaximumSize(new Dimension(w, 1));
        return comp;
    }

    private static JComponent createDashedLine(int w, Color color) {
        JComponent comp = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(color);
                Stroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{3.0f, 3.0f}, 0.0f);
                g2.setStroke(dashed);
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        comp.setPreferredSize(new Dimension(w, 1));
        comp.setMaximumSize(new Dimension(w, 1));
        return comp;
    }

    // =========================================================================
    // ICONS
    // =========================================================================

    private static class PrinterBigIcon implements Icon {
        @Override public int getIconWidth() { return 24; }
        @Override public int getIconHeight() { return 22; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(110, 115, 125));
            // Khay nạp giấy trên
            g2.fillRect(x + 5, y + 1, 14, 5);
            // Thân máy
            g2.fillRoundRect(x + 2, y + 6, 20, 11, 2, 2);
            // Khay giấy ra dưới
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 5, y + 11, 14, 8);
            g2.setColor(new Color(110, 115, 125));
            g2.drawRect(x + 5, y + 11, 14, 8);
            g2.drawLine(x + 7, y + 14, x + 17, y + 14);
            g2.dispose();
        }
    }

    private static class BarcodeMiniIcon implements Icon {
        private final int width;
        private final int height;
        public BarcodeMiniIcon(int width, int height) {
            this.width = width;
            this.height = height;
        }
        @Override public int getIconWidth() { return width; }
        @Override public int getIconHeight() { return height; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Color.BLACK);
            int curX = x;
            int[] barWidths = {2, 1, 3, 1, 1, 2, 3, 1, 2, 2, 1, 2, 1, 3, 2, 1, 3, 1, 2, 1, 2, 2, 1, 2, 3, 1, 1, 2};
            for (int bw : barWidths) {
                if (curX + bw > x + width) break;
                g2.fillRect(curX, y, bw, height);
                curX += bw + 1;
            }
            g2.dispose();
        }
    }

    private static class PdfMiniIcon implements Icon {
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(220, 50, 45));
            g2.fillRect(x + 2, y + 1, 14, 16);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 7));
            g2.drawString("PDF", x + 3, y + 12);
            g2.dispose();
        }
    }

    private static class WordMiniIcon implements Icon {
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(30, 95, 190));
            g2.fillRect(x + 2, y + 1, 14, 16);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 8));
            g2.drawString("W", x + 5, y + 12);
            g2.dispose();
        }
    }
}
