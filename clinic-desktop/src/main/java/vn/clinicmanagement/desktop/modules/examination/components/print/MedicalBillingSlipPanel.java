package vn.clinicmanagement.desktop.modules.examination.components.print;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Giao diện xem trước và in: "BẢNG KÊ CHI PHÍ KHÁM BỆNH" (Mẫu 01/KCCB chuẩn Bộ Y Tế).
 * Cỡ chữ được nâng cấp to rõ, sắc nét, dễ đọc trên mọi độ phân giải màn hình.
 * Phân trang Trang 1 & Trang 2, đầy đủ Phần II, hàng số cột (1)..(13) và nút in nổi.
 */
public class MedicalBillingSlipPanel extends JPanel {

    public MedicalBillingSlipPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        // 1. THANH CÔNG CỤ ĐỈNH & BANNER BỆNH NHÂN GÓC PHẢI
        add(buildTopToolBar(), BorderLayout.NORTH);

        // 2. KHUNG HIỂN THỊ CÁC TRANG GIẤY A4 TRÊN NỀN XÁM ĐẬM (#696E73)
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);

        JScrollPane scrollPane = new JScrollPane(buildMultiPageDocumentView());
        scrollPane.getViewport().setBackground(new Color(105, 110, 115));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Nút In nổi (Floating Print Button) ở góc trên bên phải
        JButton btnFloatingPrint = createFloatingPrintButton();

        layeredPane.add(scrollPane, Integer.valueOf(1));
        layeredPane.add(btnFloatingPrint, Integer.valueOf(2));

        layeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = layeredPane.getWidth();
                int h = layeredPane.getHeight();
                scrollPane.setBounds(0, 0, w, h);
                btnFloatingPrint.setBounds(w - 65, 20, 40, 40);
            }
        });

        add(layeredPane, BorderLayout.CENTER);
    }

    private JPanel buildTopToolBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(238, 241, 245));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(195, 205, 218)));
        bar.setPreferredSize(new Dimension(1000, 46));

        // Cụm trái: Loại chữ ký, Hiệu lực, Nút [Kiểm Tra]
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        left.setOpaque(false);

        JLabel lblSigType = new JLabel("Loại chữ ký : ---");
        lblSigType.setFont(AppFonts.BODY);

        JLabel lblValidity = new JLabel("Hiệu lực : ---");
        lblValidity.setFont(AppFonts.BODY);

        JButton btnVerify = new JButton(" Kiểm Tra", new CheckmarkIcon());
        btnVerify.setFont(AppFonts.BODY_BOLD);
        btnVerify.setBackground(Color.WHITE);
        btnVerify.setFocusPainted(false);
        btnVerify.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVerify.setPreferredSize(new Dimension(96, 26));
        btnVerify.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(175, 185, 198), 1),
                new EmptyBorder(1, 4, 1, 4)
        ));

        left.add(lblSigType);
        left.add(lblValidity);
        left.add(btnVerify);

        // Cụm phải: Banner thông tin bệnh nhân (Màu đỏ sẫm)
        JPanel right = new JPanel(new BorderLayout(8, 0));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(2, 6, 2, 12));

        JPanel textPanel = new JPanel(new GridLayout(3, 1, 0, 0));
        textPanel.setOpaque(false);

        JLabel l1 = new JLabel("NGUYỄN THỊ BÌNH AN | 22000308 | 1995 (23 tuổi) | Nữ", SwingConstants.RIGHT);
        l1.setFont(AppFonts.SECTION);
        l1.setForeground(new Color(125, 20, 20));

        JLabel l2 = new JLabel("Mã VP : 214332 | DN-4-01-8337377888 | Còn 874 ngày", SwingConstants.RIGHT);
        l2.setFont(AppFonts.BODY);
        l2.setForeground(new Color(125, 20, 20));

        JLabel l3 = new JLabel("Thị trấn Phố Mới, H. Quế Võ, Bắc Ninh", SwingConstants.RIGHT);
        l3.setFont(AppFonts.BODY);
        l3.setForeground(new Color(125, 20, 20));

        textPanel.add(l1);
        textPanel.add(l2);
        textPanel.add(l3);

        JLabel lblBhyt = new JLabel("<html><center><font color='#800000'><b>BHYT</b></font><br><font color='#0044CC'><b>80%</b></font></center></html>", SwingConstants.CENTER);
        lblBhyt.setFont(AppFonts.SECTION);
        lblBhyt.setBorder(new EmptyBorder(0, 10, 0, 4));

        right.add(textPanel, BorderLayout.CENTER);
        right.add(lblBhyt, BorderLayout.EAST);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JButton createFloatingPrintButton() {
        JButton btn = new JButton(new PrinterVectorIcon(20, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(30, 30, 30, 200));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(80, 80, 80));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("In phiếu khám bệnh");
        btn.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Đang gửi lệnh in phiếu:\n" +
                    "▶ Mẫu 01/KCCB - BẢNG KÊ CHI PHÍ KHÁM BỆNH\n" +
                    "Bệnh nhân: NGUYỄN THỊ BÌNH AN | Mã VP: 214332",
                    "In Bảng Kê Chi Phí", JOptionPane.INFORMATION_MESSAGE);
        });
        return btn;
    }

    /**
     * Vùng chứa đa trang: Trang 1 và Trang 2 nối tiếp nhau với khoảng cách và đổ bóng
     */
    private JPanel buildMultiPageDocumentView() {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(15, 20, 30, 20));

        // TRANG 1: Tăng chiều cao để chứa font chữ to thoải mái
        JPanel page1 = createPaperContainer(630);
        page1.add(buildPage1Content());

        // TRANG 2
        JPanel page2 = createPaperContainer(420);
        page2.add(buildPage2Content());

        wrapper.add(wrapInCenter(page1));
        wrapper.add(Box.createVerticalStrut(22)); // Khoảng cách giữa 2 trang A4
        wrapper.add(wrapInCenter(page2));

        return wrapper;
    }

    private JPanel wrapInCenter(JComponent comp) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        p.setOpaque(false);
        p.add(comp);
        return p;
    }

    private JPanel createPaperContainer(int height) {
        JPanel paper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(30, 30, 30, 110)); // Drop shadow
                g2.fillRect(3, 3, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth() - 3, getHeight() - 3);
                g2.setColor(new Color(195, 195, 195));
                g2.drawRect(0, 0, getWidth() - 3, getHeight() - 3);
                g2.dispose();
            }
        };
        paper.setOpaque(false);
        paper.setPreferredSize(new Dimension(880, height));
        paper.setBorder(new EmptyBorder(14, 20, 14, 20));
        return paper;
    }

    /**
     * Nội dung TRANG 1:
     * - Header Bộ Y Tế, Mẫu 01/KCCB, Barcode, Số khám bệnh, ô vuông [1]
     * - Tiêu đề BẢNG KÊ CHI PHÍ KHÁM BỆNH
     * - I. Phần hành chính
     * - II. Phần chi phí khám bệnh, chữa bệnh
     * - Bảng chi phí phần 1 (4 dòng đầu)
     */
    private JPanel buildPage1Content() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Header
        content.add(buildPaperHeader());
        content.add(Box.createVerticalStrut(6));

        // Tiêu đề lớn
        JLabel lblDocTitle = new JLabel("BẢNG KÊ CHI PHÍ KHÁM BỆNH", SwingConstants.CENTER);
        lblDocTitle.setFont(new Font(AppFonts.FAMILY, Font.BOLD, 21));
        lblDocTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(lblDocTitle);
        content.add(Box.createVerticalStrut(8));

        // I. Phần hành chính
        content.add(buildAdminSection());
        content.add(Box.createVerticalStrut(6));

        // II. Phần chi phí khám bệnh, chữa bệnh
        content.add(buildCostSectionHeader());
        content.add(Box.createVerticalStrut(6));

        // Bảng chi phí Trang 1
        content.add(new CostTableCustomView(true));

        return content;
    }

    /**
     * Nội dung TRANG 2:
     * - Lặp lại Bảng chi phí với Header 2 tầng + Hàng số cột (1)..(13)
     * - Các dòng dịch vụ tiếp theo + Dòng "Cộng"
     */
    private JPanel buildPage2Content() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(10, 0, 0, 0));

        // Bảng chi phí Trang 2
        content.add(new CostTableCustomView(false));

        return content;
    }

    private JPanel buildPaperHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        // Góc trái
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel l1 = new JLabel("Bộ Y tế/Sở Y tế/ Y tế ngành: SỞ Y TẾ BẮC NINH");
        l1.setFont(new Font("Times New Roman", Font.BOLD, 13));

        JLabel l2 = new JLabel("Cơ sở khám, chữa bệnh: BỆNH VIỆN SẢN NHI");
        l2.setFont(new Font("Times New Roman", Font.BOLD, 13));

        JLabel l3 = new JLabel("Khoa: Khoa khám bệnh - PK Nhi 1");
        l3.setFont(new Font("Times New Roman", Font.PLAIN, 13));

        JLabel l4 = new JLabel("Mã khoa: K01");
        l4.setFont(new Font("Times New Roman", Font.PLAIN, 13));

        left.add(l1);
        left.add(l2);
        left.add(l3);
        left.add(l4);

        // Góc phải
        JPanel right = new JPanel(new BorderLayout(10, 0));
        right.setOpaque(false);

        JPanel rightText = new JPanel();
        rightText.setLayout(new BoxLayout(rightText, BoxLayout.Y_AXIS));
        rightText.setOpaque(false);

        JLabel r1 = new JLabel("Mẫu số: 01/KCCB");
        r1.setFont(new Font("Times New Roman", Font.BOLD, 13));

        JLabel r2 = new JLabel("Mã số người bệnh: 22000308");
        r2.setFont(new Font("Times New Roman", Font.PLAIN, 13));

        JLabel r3 = new JLabel("Số khám bệnh: 214332");
        r3.setFont(new Font("Times New Roman", Font.PLAIN, 13));

        JLabel lblBarcode = new JLabel(new BarcodeVectorIcon(96, 24));

        rightText.add(r1);
        rightText.add(r2);
        rightText.add(r3);
        rightText.add(Box.createVerticalStrut(3));
        rightText.add(lblBarcode);

        JLabel box1 = new JLabel("1", SwingConstants.CENTER);
        box1.setFont(new Font("Times New Roman", Font.BOLD, 16));
        box1.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        box1.setPreferredSize(new Dimension(32, 52));

        right.add(rightText, BorderLayout.CENTER);
        right.add(box1, BorderLayout.EAST);

        p.add(left, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel buildAdminSection() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        p.add(createDocText("I. Phần hành chính:", true));

        // (1) Họ tên, Ngày sinh, Giới tính, [2]
        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r1.setOpaque(false);
        r1.add(createDocText("(1) Họ tên người bệnh: ", false));
        r1.add(createDocText("NGUYỄN THỊ BÌNH AN", true));
        r1.add(Box.createHorizontalStrut(25));
        r1.add(createDocText("Ngày, tháng, năm sinh: 1999", false));
        r1.add(Box.createHorizontalStrut(25));
        r1.add(createDocText("Giới tính: Nữ", false));
        r1.add(Box.createHorizontalStrut(40));
        r1.add(createNumberedBox("2"));
        p.add(r1);

        // (2) Địa chỉ, (3) Mã khu vực [ K2 ]
        JPanel r2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r2.setOpaque(false);
        r2.add(createDocText("(2) Địa chỉ hiện tại: Thị trấn Phố Mới, H. Quế Võ, Bắc Ninh", false));
        r2.add(Box.createHorizontalStrut(45));
        r2.add(createDocText("(3) Mã khu vực (K1/K2/K3):", false));
        r2.add(createBoxText("K2", 44));
        p.add(r2);

        // (4) Mã thẻ BHYT phân đoạn [DN][4][01][83][373][77888]
        JPanel r3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r3.setOpaque(false);
        r3.add(createDocText("(4) Mã thẻ BHYT: ", false));
        r3.add(createBoxText("DN", 28));
        r3.add(createBoxText("4", 18));
        r3.add(createBoxText("01", 26));
        r3.add(createBoxText("83", 26));
        r3.add(createBoxText("373", 34));
        r3.add(createBoxText("77888", 48));
        r3.add(Box.createHorizontalStrut(10));
        r3.add(createDocText("Giá trị từ 01/01/2021 đến 01/01/2025", false));
        p.add(r3);

        // (5) Cơ sở ĐK KCB ban đầu, (6) Mã [01 - 012]
        JPanel r4 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r4.setOpaque(false);
        r4.add(createDocText("(5) Cơ sở ĐK KCB ban đầu: Phòng khám đa khoa khu vực Đông Mỹ", false));
        r4.add(Box.createHorizontalStrut(50));
        r4.add(createDocText("(6) Mã:", false));
        r4.add(createBoxText("01 - 012", 66));
        p.add(r4);

        // (7) Đến khám
        JPanel r5 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r5.setOpaque(false);
        r5.add(createDocText("(7) Đến khám: 15 giờ 39 phút, ngày 11 / 08 / 2022", false));
        p.add(r5);

        // (8) Điều trị ngoại trú/nội trú từ, Tổng số ngày, (10) Tình trạng ra viện
        JPanel r6 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r6.setOpaque(false);
        r6.add(createDocText("(8) Điều trị ngoại trú/nội trú từ: ---- giờ ---- phút, ngày ----/----/----", false));
        r6.add(Box.createHorizontalStrut(15));
        r6.add(createDocText("Tổng số ngày điều trị: 1", false));
        r6.add(Box.createHorizontalStrut(25));
        r6.add(createDocText("(10) Tình trạng ra viện:", false));
        r6.add(createBoxText("", 40));
        p.add(r6);

        // (9) Kết thúc khám/điều trị, (13) Thông tuyến, (14) Trái tuyến
        JPanel r7 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r7.setOpaque(false);
        r7.add(createDocText("(9) Kết thúc khám/điều trị: ---- giờ ---- phút, ngày ----/----/----", false));
        r7.add(Box.createHorizontalStrut(10));
        r7.add(createDocText("(13) Thông tuyến ☐  (14) Trái tuyến", false));
        r7.add(createBoxText("", 34));
        p.add(r7);

        // (11) Cấp cứu, (12) Đúng tuyến [X] Nơi chuyển, (16) Mã bệnh (ICD-10)
        JPanel r8 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r8.setOpaque(false);
        r8.add(createDocText("(11) Cấp cứu ☐   (12) Đúng tuyến", false));
        r8.add(createBoxText("X", 20));
        r8.add(createDocText("Nơi chuyển: Nơi chuyển: d2", false));
        r8.add(Box.createHorizontalStrut(30));
        r8.add(createDocText("(16) Mã bệnh (ICD-10):", false));
        r8.add(createBoxText("", 60));
        p.add(r8);

        // (15) Chẩn đoán xác định
        JPanel r9 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r9.setOpaque(false);
        r9.add(createDocText("(15) Chẩn đoán xác định:", false));
        p.add(r9);

        // (17) Bệnh kèm theo, (18) Mã bệnh kèm theo
        JPanel r10 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r10.setOpaque(false);
        r10.add(createDocText("(17) Bệnh kèm theo:", false));
        r10.add(Box.createHorizontalStrut(200));
        r10.add(createDocText("(18) Mã bệnh kèm theo:", false));
        r10.add(createBoxText("", 165));
        p.add(r10);

        // (19) Thời điểm đủ 5 năm, (20) Miễn cùng chi trả trong năm
        JPanel r11 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r11.setOpaque(false);
        r11.add(createDocText("(19) Thời điểm đủ 5 năm liên tục ngày: ....../....../......", false));
        r11.add(Box.createHorizontalStrut(20));
        r11.add(createDocText("(20) Miễn cùng chi trả trong năm từ ngày: ....../....../......", false));
        p.add(r11);

        return p;
    }

    /**
     * II. Phần chi phí khám bệnh, chữa bệnh:
     * Mã thẻ BHYT + Giá trị + Mức hưởng: [ 100% ]
     * (Chi phí KCB tính từ ngày 11 / 08 / 2022 đến ngày ....../....../......)
     */
    private JPanel buildCostSectionHeader() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        p.add(createDocText("II. Phần chi phí khám bệnh, chữa bệnh:", true));

        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r1.setOpaque(false);
        r1.add(createDocText("Mã thẻ BHYT: ", false));
        r1.add(createBoxText("DN", 28));
        r1.add(createBoxText("4", 18));
        r1.add(createBoxText("01", 26));
        r1.add(createBoxText("83", 26));
        r1.add(createBoxText("373", 34));
        r1.add(createBoxText("77888", 48));
        r1.add(Box.createHorizontalStrut(10));
        r1.add(createDocText("Giá trị từ 01/01/2021 đến 01/01/2025", false));
        r1.add(Box.createHorizontalStrut(40));
        r1.add(createDocText("Mức hưởng", false));
        r1.add(createBoxText("100%", 64));
        p.add(r1);

        JPanel r2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        r2.setOpaque(false);
        r2.add(createDocText("(Chi phí KCB tính từ ngày 11 / 08 / 2022 đến ngày ....../....../......)", false));
        p.add(r2);

        return p;
    }

    /**
     * Bảng chi phí custom render trực tiếp với đầy đủ:
     * - Cỡ chữ to rõ (13pt cho dữ liệu, 11.5pt cho tiêu đề cột)
     * - Tiêu đề 2 tầng (2-level header)
     * - Dòng đánh số cột (1)..(13) chuẩn Bộ Y Tế
     * - Dữ liệu chia 2 trang khớp 100% ảnh chụp
     */
    private static class CostTableCustomView extends JComponent {
        private final boolean isPage1;
        private final int[] colWidths = {200, 38, 28, 58, 58, 48, 68, 48, 68, 58, 58, 34, 76};
        private final int totalWidth = 840;
        private final int headerHeight = 58;
        private final int tier1H = 22;
        private final int tier2H = 36;
        private final int colNumRowHeight = 18;
        private final int rowHeight = 22;

        // Dữ liệu TRANG 1: 4 dòng
        private final String[][] page1Rows = {
                {"Khoa khám bệnh", "", "", "", "", "", "277,900", "", "38,700", "38,700", "0", "0", "239,200"},
                {"1. Khám bệnh:", "", "", "", "", "", "38,700", "", "38,700", "38,700", "0", "0", "0"},
                {"Khám Ngoại", "Lần", "1", "38,700", "38,700", "100", "38,700", "100", "38,700", "38,700", "0", "0", "0"},
                {"3. Xét nghiệm:", "", "", "", "", "", "86,200", "", "0", "0", "0", "0", "86,200"}
        };

        // Dữ liệu TRANG 2: 4 dòng tiếp theo
        private final String[][] page2Rows = {
                {"Định lượng Folate [Máu]", "Lần", "1", "86,200", "86,200", "100", "86,200", "0", "0", "0", "0", "0", "86,200"},
                {"4. Chẩn đoán hình ảnh:", "", "", "", "", "", "153,000", "", "0", "0", "0", "0", "153,000"},
                {"Siêu âm 2D [YC - Siêu âm thai nhi trong 3 tháng giữa]", "LẦN", "1", "153,000", "0", "100", "153,000", "0", "0", "0", "0", "0", "153,000"},
                {"Cộng", "", "", "", "", "", "277,900", "", "38,700", "38,700", "0", "0", "239,200"}
        };

        CostTableCustomView(boolean isPage1) {
            this.isPage1 = isPage1;
            int rowCount = isPage1 ? page1Rows.length : page2Rows.length;
            int totalH = headerHeight + colNumRowHeight + rowCount * rowHeight;
            setPreferredSize(new Dimension(totalWidth, totalH));
            setMinimumSize(new Dimension(totalWidth, totalH));
            setMaximumSize(new Dimension(totalWidth, totalH));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Font fontRegular = new Font("Times New Roman", Font.PLAIN, 13);
            Font fontBold = new Font("Times New Roman", Font.BOLD, 13);
            Font fontHeader = new Font("Times New Roman", Font.BOLD, 11);
            Font fontColNum = new Font("Times New Roman", Font.PLAIN, 11);

            // Nền trắng
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, totalWidth, getHeight());

            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1.0f));

            // Tính tọa độ X của từng cột
            int[] colX = new int[colWidths.length + 1];
            colX[0] = 0;
            for (int i = 0; i < colWidths.length; i++) {
                colX[i + 1] = colX[i] + colWidths[i];
            }

            // 1. VẼ HEADER 2 TẦNG
            String[] topTitles = {
                    "Nội dung",
                    "Đơn\nvị\ntính",
                    "Số\nlượng",
                    "Đơn giá\nBV\n(đồng)",
                    "Đơn giá\nBH\n(đồng)",
                    "Tỷ lệ\nthanh\ntoán\ntheo\ndịch vụ\n%",
                    "Thành\ntiền\nBV\n(đồng)",
                    "Tỷ lệ\nthanh\ntoán\nBHYT\n%",
                    "Thành\ntiền\nBH\n(đồng)"
            };

            for (int i = 0; i <= 8; i++) {
                g2.drawRect(colX[i], 0, colWidths[i], headerHeight);
                drawCenteredMultiLineText(g2, topTitles[i], colX[i], 0, colWidths[i], headerHeight, fontHeader);
            }

            // Cột 9..12: Tầng trên gộp "Nguồn thanh toán (đồng)"
            int sourceX = colX[9];
            int sourceW = colX[13] - colX[9];

            g2.drawRect(sourceX, 0, sourceW, tier1H);
            drawCenteredMultiLineText(g2, "Nguồn thanh toán (đồng)", sourceX, 0, sourceW, tier1H, fontHeader);

            // Tầng dưới: Quỹ BHYT | Người bệnh cùng chi trả | Khác | Người bệnh tự trả
            String[] subTitles = {"Quỹ\nBHYT", "Người\nbệnh\ncùng\nchi trả", "Khác", "Người\nbệnh tự\ntrả"};
            for (int i = 0; i < 4; i++) {
                int cIdx = 9 + i;
                g2.drawRect(colX[cIdx], tier1H, colWidths[cIdx], tier2H);
                drawCenteredMultiLineText(g2, subTitles[i], colX[cIdx], tier1H, colWidths[cIdx], tier2H, fontHeader);
            }

            // 2. VẼ HÀNG ĐÁNH SỐ CỘT (1)..(13) CHUẨN BỘ Y TẾ
            int colNumY = headerHeight;
            g2.setFont(fontColNum);
            for (int c = 0; c < colWidths.length; c++) {
                g2.drawRect(colX[c], colNumY, colWidths[c], colNumRowHeight);
                String numStr = "(" + (c + 1) + ")";
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(numStr);
                int th = colNumY + ((colNumRowHeight - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(numStr, colX[c] + (colWidths[c] - tw) / 2, th);
            }

            // 3. VẼ CÁC DÒNG DỮ LIỆU
            String[][] curRows = isPage1 ? page1Rows : page2Rows;
            int curY = headerHeight + colNumRowHeight;
            Shape origClip = g2.getClip();

            for (int r = 0; r < curRows.length; r++) {
                boolean isBoldRow;
                if (isPage1) {
                    isBoldRow = (r == 0 || r == 1 || r == 3); // Khoa khám bệnh, 1. Khám bệnh:, 3. Xét nghiệm:
                } else {
                    isBoldRow = (r == 1 || r == 3); // 4. Chẩn đoán hình ảnh:, Cộng
                }
                g2.setFont(isBoldRow ? fontBold : fontRegular);

                for (int c = 0; c < colWidths.length; c++) {
                    int cellX = colX[c];
                    int cellW = colWidths[c];
                    g2.drawRect(cellX, curY, cellW, rowHeight);

                    String val = curRows[r][c];
                    if (val == null || val.isEmpty()) continue;

                    g2.setClip(cellX + 1, curY + 1, cellW - 2, rowHeight - 2);

                    FontMetrics fm = g2.getFontMetrics();
                    int textY = curY + ((rowHeight - fm.getHeight()) / 2) + fm.getAscent();

                    if (c == 0) {
                        int pad = (!isPage1 && (r == 0 || r == 2)) || (isPage1 && r == 2) ? 8 : 4;
                        if (!isPage1 && r == 3) {
                            // "Cộng" căn giữa
                            int tw = fm.stringWidth(val);
                            g2.drawString(val, cellX + (cellW - tw) / 2, textY);
                        } else {
                            g2.drawString(val, cellX + pad, textY);
                        }
                    } else if (c == 1 || c == 2) {
                        int tw = fm.stringWidth(val);
                        g2.drawString(val, cellX + (cellW - tw) / 2, textY);
                    } else {
                        int tw = fm.stringWidth(val);
                        g2.drawString(val, cellX + cellW - tw - 4, textY);
                    }

                    g2.setClip(origClip);
                }
                curY += rowHeight;
            }

            g2.dispose();
        }

        private void drawCenteredMultiLineText(Graphics2D g2, String text, int x, int y, int w, int h, Font font) {
            Shape orig = g2.getClip();
            g2.setClip(x + 1, y + 1, w - 2, h - 2);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            String[] lines = text.split("\n");
            int totalTextHeight = lines.length * (fm.getHeight() - 3);
            int startY = y + (h - totalTextHeight) / 2 + fm.getAscent() - 1;

            for (int i = 0; i < lines.length; i++) {
                int strW = fm.stringWidth(lines[i]);
                int strX = x + (w - strW) / 2;
                g2.drawString(lines[i], strX, startY + i * (fm.getHeight() - 3));
            }
            g2.setClip(orig);
        }
    }

    private JLabel createDocText(String text, boolean isBold) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Times New Roman", isBold ? Font.BOLD : Font.PLAIN, 13));
        l.setForeground(Color.BLACK);
        return l;
    }

    private JLabel createNumberedBox(String num) {
        JLabel l = new JLabel(num, SwingConstants.CENTER);
        l.setFont(new Font("Times New Roman", Font.BOLD, 13));
        l.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        l.setPreferredSize(new Dimension(38, 18));
        return l;
    }

    private JLabel createBoxText(String text, int width) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Times New Roman", Font.BOLD, 12));
        l.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        l.setPreferredSize(new Dimension(width, 18));
        return l;
    }

    /** Icon dấu tích xanh lá chuẩn */
    private static class CheckmarkIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(40, 165, 70));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 2, y + 7, x + 5, y + 11);
            g2.drawLine(x + 5, y + 11, x + 12, y + 2);
            g2.dispose();
        }
    }

    /** Vẽ Barcode vector */
    private static class BarcodeVectorIcon implements Icon {
        private final int width, height;
        BarcodeVectorIcon(int w, int h) { this.width = w; this.height = h; }
        @Override public int getIconWidth() { return width; }
        @Override public int getIconHeight() { return height; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Color.BLACK);
            int[] barWidths = {2, 1, 3, 1, 2, 4, 1, 2, 1, 3, 2, 1, 4, 1, 2, 3, 1, 2, 4, 1, 2, 1, 3, 2, 1, 4, 2, 1, 3, 1, 2};
            int curX = x;
            for (int i = 0; i < barWidths.length && curX < x + width; i++) {
                int w = barWidths[i];
                if (i % 2 == 0) {
                    g2.fillRect(curX, y, w, height);
                }
                curX += w;
            }
            g2.dispose();
        }
    }

    /** Icon Máy In vector */
    private static class PrinterVectorIcon implements Icon {
        private final int width, height;
        PrinterVectorIcon(int w, int h) { this.width = w; this.height = h; }
        @Override public int getIconWidth() { return width; }
        @Override public int getIconHeight() { return height; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            // Giấy trên
            g2.fillRect(x + 4, y + 1, width - 8, 4);
            // Thân máy in
            g2.fillRoundRect(x + 1, y + 4, width - 2, 9, 3, 3);
            // Giấy ra dưới
            g2.setColor(new Color(230, 230, 230));
            g2.fillRect(x + 4, y + 10, width - 8, 6);
            g2.setColor(new Color(80, 80, 80));
            g2.drawRect(x + 4, y + 10, width - 8, 6);
            g2.dispose();
        }
    }
}
