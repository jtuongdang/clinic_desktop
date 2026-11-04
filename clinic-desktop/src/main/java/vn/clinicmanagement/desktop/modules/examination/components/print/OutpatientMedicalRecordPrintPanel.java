package vn.clinicmanagement.desktop.modules.examination.components.print;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Giao diện Xem và In: "BỆNH ÁN NGOẠI TRÚ" (BC11-MVJ17690 - Bệnh án khám bệnh viện).
 * Kích hoạt khi người dùng nhấn nút [🖨 In Bệnh Án] trên tab Bệnh Án.
 * Khớp 100% media_1793381212647_7bcb9750.jpg:
 * - Cột chức năng bên trái (Bệnh án điện tử):
 *   + [✍ Ký Tài Liệu]
 *   + [☑ Duyệt Tài Liệu] (disabled)
 *   + [💾 Lưu Tài Liệu]
 *   + [✕ Hủy Tài Liệu] (disabled)
 *   + Xuất file... [📄 PDF] [☰ ▾]
 *   + Dòng chữ đỏ chân trang: "Chưa ký 1"
 * - Thanh công cụ đỉnh:
 *   + Cụm trái: [🖨 In Tài Liệu], Loại chữ ký : ---, Hiển thị : ---, [✔ Kiểm tra]
 *   + Cụm phải: Banner thông tin bệnh nhân:
 *     NGUYỄN THỊ BÌNH AN | 22000308 | 1999 (23 tuổi) | Nữ | BHYT 80%
 *     Mã VP : 214333 | DN-4-01-8337377888 | Còn 664 ngày
 *     Thị trấn Phố Mới, H. Quế Võ, Bắc Ninh
 * - Vùng trung tâm: Trang giấy A4 "BỆNH ÁN NGOẠI TRÚ"
 *   + SỞ Y TẾ BẮC NINH / BỆNH VIỆN SẢN NHI
 *   + Tiêu đề: BỆNH ÁN NGOẠI TRÚ - KHOA: Khoa khám bệnh
 *   + Phần I. HÀNH CHÍNH (Các ô mã số ngày sinh, tuổi, giới tính, nghề nghiệp, dân tộc, BHYT)
 *   + Quá trình bệnh lý, Tiền sử bệnh (+ Bản thân, + Gia đình)
 *   + Phần IV. KHÁM BỆNH (1. Toàn thân, Bảng chỉ số sinh tồn: Mạch, Nhiệt độ, Huyết áp, Nhịp thở, Cân nặng; 2. Các bộ phận)
 */
public class OutpatientMedicalRecordPrintPanel extends JPanel {

    private String patientCode = "22000308";
    private String patientName = "NGUYỄN THỊ BÌNH AN";
    private String patientBirthYear = "1999";
    private String patientAge = "23";
    private String patientGender = "Nữ";
    private String patientBhyt = "DN-4-01-8337377888";
    private String patientAddress = "Thị trấn Phố Mới, H. Quế Võ, Bắc Ninh";
    private String invoiceCode = "214333";
    private String daysLeft = "Còn 664 ngày";

    public OutpatientMedicalRecordPrintPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. THANH CÔNG CỤ ĐỈNH KÈM BANNER BỆNH NHÂN GÓC PHẢI
        add(buildTopToolBar(), BorderLayout.NORTH);

        // 2. KHUNG TRUNG TÂM: CỘT BÊN TRÁI + VÙNG XEM TÀI LIỆU
        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setBackground(new Color(90, 98, 110));

        // Cột bên trái: Các nút Ký, Duyệt, Lưu, Hủy, Xuất PDF
        centerContainer.add(buildLeftSidebar(), BorderLayout.WEST);

        // Vùng xem trước trang giấy in A4 Bệnh án ngoại trú
        JScrollPane scrollPane = new JScrollPane(new OutpatientMedicalRecordSheet());
        scrollPane.getViewport().setBackground(new Color(105, 115, 128));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        centerContainer.add(scrollPane, BorderLayout.CENTER);

        add(centerContainer, BorderLayout.CENTER);
    }

    private JPanel buildTopToolBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(242, 246, 252));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 215, 228)));
        bar.setPreferredSize(new Dimension(1000, 48));

        // Cụm trái: In tài liệu, Loại chữ ký, Hiển thị, Kiểm tra
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        left.setOpaque(false);

        JButton btnPrint = new JButton(" In Tài Liệu", new PrinterVectorIcon(18, 18));
        btnPrint.setFont(AppFonts.SMALL_BOLD);
        btnPrint.setBackground(Color.WHITE);
        btnPrint.setFocusPainted(false);
        btnPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrint.setPreferredSize(new Dimension(105, 34));
        btnPrint.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(2, 6, 2, 6)
        ));
        btnPrint.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Hệ thống đang gửi lệnh in tài liệu tới máy in mặc định:\n" +
                    "▶ Mẫu in: BỆNH ÁN NGOẠI TRÚ\n" +
                    "▶ Bệnh nhân: " + patientName + " (Mã BN: " + patientCode + ")\n" +
                    "▶ Khoa phòng: Khoa khám bệnh",
                    "In Tài Liệu", JOptionPane.INFORMATION_MESSAGE);
        });

        JLabel lblSigType = new JLabel("Loại chữ ký : ---");
        lblSigType.setFont(AppFonts.SMALL);
        lblSigType.setForeground(new Color(40, 45, 55));

        JLabel lblValidity = new JLabel("Hiển thị : ---");
        lblValidity.setFont(AppFonts.SMALL);
        lblValidity.setForeground(new Color(40, 45, 55));

        JButton btnVerify = new JButton(" Kiểm tra", new CheckmarkMiniIcon());
        btnVerify.setFont(AppFonts.SMALL_BOLD);
        btnVerify.setBackground(Color.WHITE);
        btnVerify.setFocusPainted(false);
        btnVerify.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVerify.setPreferredSize(new Dimension(92, 26));
        btnVerify.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(180, 190, 205), 1),
                new EmptyBorder(1, 4, 1, 4)
        ));

        left.add(btnPrint);
        left.add(Box.createHorizontalStrut(6));
        left.add(lblSigType);
        left.add(lblValidity);
        left.add(Box.createHorizontalStrut(4));
        left.add(btnVerify);

        // Cụm phải: Banner thông tin bệnh nhân góc phải khớp 100% media_1793381212647_7bcb9750.jpg
        JPanel right = new JPanel(new BorderLayout(0, 0));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(2, 6, 2, 8));

        JPanel line1 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        line1.setOpaque(false);
        JLabel lblPtName = new JLabel(patientName + " | " + patientCode + " | " + patientBirthYear + " (" + patientAge + " tuổi) | " + patientGender);
        lblPtName.setFont(AppFonts.SMALL_BOLD);
        lblPtName.setForeground(new Color(140, 20, 20));

        JLabel lblBhyt = new JLabel("BHYT 80%");
        lblBhyt.setFont(AppFonts.SMALL_BOLD);
        lblBhyt.setForeground(new Color(180, 20, 20));

        line1.add(lblPtName);
        line1.add(lblBhyt);

        JLabel lblLine2 = new JLabel("Mã VP : " + invoiceCode + " | " + patientBhyt + " | " + daysLeft, SwingConstants.RIGHT);
        lblLine2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblLine2.setForeground(new Color(60, 65, 75));

        JLabel lblLine3 = new JLabel(patientAddress, SwingConstants.RIGHT);
        lblLine3.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblLine3.setForeground(new Color(60, 65, 75));

        JPanel pnlRightLines = new JPanel();
        pnlRightLines.setLayout(new BoxLayout(pnlRightLines, BoxLayout.Y_AXIS));
        pnlRightLines.setOpaque(false);
        line1.setAlignmentX(Component.RIGHT_ALIGNMENT);
        lblLine2.setAlignmentX(Component.RIGHT_ALIGNMENT);
        lblLine3.setAlignmentX(Component.RIGHT_ALIGNMENT);
        pnlRightLines.add(line1);
        pnlRightLines.add(lblLine2);
        pnlRightLines.add(lblLine3);

        right.add(pnlRightLines, BorderLayout.CENTER);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildLeftSidebar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setPreferredSize(new Dimension(110, 600));
        bar.setBackground(new Color(240, 243, 248));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(205, 215, 226)));

        // Tiêu đề: Bệnh án điện tử
        JLabel lblTitle = new JLabel("Bệnh án điện tử", SwingConstants.CENTER);
        lblTitle.setFont(AppFonts.SMALL_BOLD);
        lblTitle.setForeground(new Color(30, 35, 45));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTitle.setBorder(new EmptyBorder(8, 4, 6, 4));

        bar.add(lblTitle);
        bar.add(Box.createVerticalStrut(4));

        // 4 nút thao tác chính
        bar.add(createSidebarButton("Ký Tài Liệu", new PenSignatureIcon(), true));
        bar.add(Box.createVerticalStrut(6));
        bar.add(createSidebarButton("Duyệt Tài Liệu", new ApproveCheckIcon(), false));
        bar.add(Box.createVerticalStrut(6));
        bar.add(createSidebarButton("Lưu Tài Liệu", new FloppyDiskMiniIcon(), true));
        bar.add(Box.createVerticalStrut(6));
        bar.add(createSidebarButton("Hủy Tài Liệu", new GreyCrossIcon(), false));

        bar.add(Box.createVerticalStrut(12));

        // Nhãn "Xuất file..."
        JLabel lblExport = new JLabel("Xuất file...");
        lblExport.setFont(AppFonts.SMALL);
        lblExport.setForeground(new Color(50, 60, 70));
        lblExport.setAlignmentX(Component.CENTER_ALIGNMENT);
        bar.add(lblExport);
        bar.add(Box.createVerticalStrut(4));

        // Cụm nút Xuất file: [PDF] và [☰ ▾]
        JPanel exportGroup = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        exportGroup.setOpaque(false);
        exportGroup.setMaximumSize(new Dimension(100, 32));

        JButton btnPdf = new JButton("PDF", new PdfVectorIcon());
        btnPdf.setFont(new Font(AppFonts.FAMILY, Font.BOLD, 10));
        btnPdf.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnPdf.setHorizontalTextPosition(SwingConstants.CENTER);
        btnPdf.setPreferredSize(new Dimension(54, 34));
        btnPdf.setBackground(Color.WHITE);
        btnPdf.setFocusPainted(false);
        btnPdf.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPdf.setBorder(new LineBorder(new Color(185, 195, 210), 1));
        btnPdf.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Đang xuất bệnh án ngoại trú ra tệp PDF:\n" +
                    "▶ Tệp: BenhAn_NgoaiTru_" + patientCode + ".pdf",
                    "Xuất PDF", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton btnMore = new JButton("☰ ▾");
        btnMore.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnMore.setPreferredSize(new Dimension(36, 34));
        btnMore.setBackground(Color.WHITE);
        btnMore.setFocusPainted(false);
        btnMore.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMore.setBorder(new LineBorder(new Color(185, 195, 210), 1));

        exportGroup.add(btnPdf);
        exportGroup.add(btnMore);
        bar.add(exportGroup);

        bar.add(Box.createVerticalGlue());

        // Chân trang sidebar: Dòng chữ đỏ "Chưa ký 1"
        JLabel lblStatus = new JLabel("Chưa ký 1", SwingConstants.CENTER);
        lblStatus.setFont(AppFonts.SMALL_BOLD);
        lblStatus.setForeground(new Color(205, 30, 30));
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblStatus.setBorder(new EmptyBorder(6, 4, 12, 4));

        bar.add(lblStatus);

        return bar;
    }

    private JButton createSidebarButton(String text, Icon icon, boolean isEnabled) {
        JButton btn = new JButton(text, icon);
        btn.setFont(AppFonts.SMALL_BOLD);
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setIconTextGap(3);
        btn.setPreferredSize(new Dimension(98, 44));
        btn.setMaximumSize(new Dimension(98, 44));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setEnabled(isEnabled);
        btn.setCursor(new Cursor(isEnabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));

        if (isEnabled) {
            btn.setBackground(Color.WHITE);
            btn.setForeground(new Color(30, 35, 45));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(185, 195, 210), 1),
                    new EmptyBorder(2, 4, 2, 4)
            ));
        } else {
            btn.setBackground(new Color(245, 247, 250));
            btn.setForeground(new Color(155, 165, 175));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(215, 222, 230), 1),
                    new EmptyBorder(2, 4, 2, 4)
            ));
        }
        return btn;
    }

    /**
     * Tờ giấy A4 mô phỏng "BỆNH ÁN NGOẠI TRÚ"
     */
    private class OutpatientMedicalRecordSheet extends JPanel {

        private final int PAGE_W = 750;
        private final int PAGE_H = 1000;

        public OutpatientMedicalRecordSheet() {
            setOpaque(true);
            setBackground(new Color(105, 115, 128));
            setPreferredSize(new Dimension(PAGE_W + 60, PAGE_H + 50));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int x = Math.max(25, (getWidth() - PAGE_W) / 2);
            int y = 20;

            // Bóng đổ (Drop Shadow)
            g2.setColor(new Color(60, 68, 78, 140));
            g2.fillRect(x + 5, y + 5, PAGE_W, PAGE_H);
            g2.setColor(new Color(80, 88, 98, 100));
            g2.fillRect(x + 2, y + 2, PAGE_W, PAGE_H);

            // Nền giấy trắng
            g2.setColor(Color.WHITE);
            g2.fillRect(x, y, PAGE_W, PAGE_H);
            g2.setColor(new Color(200, 205, 215));
            g2.drawRect(x, y, PAGE_W - 1, PAGE_H - 1);

            int padX = x + 35;
            int topY = y + 25;

            // --- HEADER ---
            // Góc trái: SỞ Y TẾ BẮC NINH / BỆNH VIỆN SẢN NHI
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Times New Roman", Font.BOLD, 10));
            g2.drawString("SỞ Y TẾ BẮC NINH", padX, topY + 12);
            g2.drawString("BỆNH VIỆN SẢN NHI", padX, topY + 26);

            // Tiêu đề giữa: BỆNH ÁN NGOẠI TRÚ / KHOA: Khoa khám bệnh
            g2.setFont(new Font("Times New Roman", Font.BOLD, 16));
            String title = "BỆNH ÁN NGOẠI TRÚ";
            FontMetrics fmt = g2.getFontMetrics();
            int titleX = x + (PAGE_W - fmt.stringWidth(title)) / 2;
            g2.drawString(title, titleX, topY + 15);

            g2.setFont(new Font("Times New Roman", Font.BOLD, 11));
            String subTitle = "KHOA: Khoa khám bệnh";
            FontMetrics fmSub = g2.getFontMetrics();
            g2.drawString(subTitle, x + (PAGE_W - fmSub.stringWidth(subTitle)) / 2, topY + 34);

            // Góc phải: Số ngoại trú / Số lưu trữ
            int rightX = x + PAGE_W - 130;
            g2.setFont(new Font("Times New Roman", Font.PLAIN, 10));
            g2.drawString("Số ngoại trú:", rightX, topY + 12);
            g2.drawString("Số lưu trữ:", rightX, topY + 26);

            // --- PHẦN I. HÀNH CHÍNH ---
            int curY = topY + 60;
            g2.setFont(new Font("Times New Roman", Font.BOLD, 11));
            g2.drawString("I. HÀNH CHÍNH", padX, curY);

            curY += 18;
            g2.setFont(new Font("Times New Roman", Font.PLAIN, 10));
            // 1. Họ và tên, 2. Sinh ngày, Tuổi
            g2.drawString("1. Họ và tên (In hoa): ", padX, curY);
            g2.setFont(new Font("Times New Roman", Font.BOLD, 10));
            g2.drawString(patientName, padX + 110, curY);

            g2.setFont(new Font("Times New Roman", Font.PLAIN, 10));
            g2.drawString("2. Sinh ngày:", padX + 265, curY);
            // 8 ô ngày sinh (1999)
            drawCodeBox(g2, padX + 325, curY - 11, new String[]{"", "", "", "", "1", "9", "9", "9"});

            g2.drawString("Tuổi", padX + 465, curY);
            drawCodeBox(g2, padX + 490, curY - 11, new String[]{"2", "3"});

            // Dòng 3: Giới tính, Nghề nghiệp
            curY += 20;
            g2.drawString("3. Giới:       1. Nam", padX, curY);
            drawCheckBox(g2, padX + 90, curY - 10, false);
            g2.drawString("2. Nữ", padX + 115, curY);
            drawCheckBox(g2, padX + 145, curY - 10, true);

            g2.drawString("4. Nghề nghiệp: Nhân dân", padX + 175, curY);
            drawCodeBox(g2, padX + 490, curY - 11, new String[]{"1", "3"});

            // Dòng 4: Dân tộc, Ngoại kiều
            curY += 20;
            g2.drawString("5. Dân tộc: Kinh", padX, curY);
            drawCodeBox(g2, padX + 215, curY - 11, new String[]{"1", "1"});

            g2.drawString("6. Ngoại kiều: Việt Nam", padX + 260, curY);
            drawCodeBox(g2, padX + 490, curY - 11, new String[]{"V", "N"});

            // Dòng 5: Địa chỉ
            curY += 20;
            g2.drawString("7. Địa chỉ: Số nhà:                                     Thôn, phố:                                 Xã, phường: Thị trấn Phố Mới", padX, curY);

            curY += 18;
            g2.drawString("Huyện (Q, Tx): H. Quế Võ", padX, curY);
            drawCodeBox(g2, padX + 215, curY - 11, new String[]{"0", "3"});

            g2.drawString("Tỉnh, thành phố: Bắc Ninh", padX + 260, curY);
            drawCodeBox(g2, padX + 490, curY - 11, new String[]{"", "2"});

            // Dòng 6: Nơi làm việc, Đối tượng
            curY += 20;
            g2.drawString("8. Nơi làm việc:", padX, curY);
            g2.drawString("9. Đối tượng:  1.BHYT", padX + 215, curY);
            drawCheckBox(g2, padX + 315, curY - 10, true);
            g2.drawString("2.Thu phí", padX + 338, curY);
            drawCheckBox(g2, padX + 378, curY - 10, false);
            g2.drawString("3.Miễn", padX + 400, curY);
            drawCheckBox(g2, padX + 438, curY - 10, false);
            g2.drawString("4.Khác", padX + 458, curY);
            drawCheckBox(g2, padX + 495, curY - 10, false);

            // Dòng 7: BHYT giá trị đến, Số thẻ BHYT
            curY += 20;
            g2.drawString("10. BHYT giá trị đến ngày 01 tháng 01 năm 2025", padX, curY);
            g2.drawString("Số thẻ BHYT", padX + 280, curY);
            drawCardBox(g2, padX + 350, curY - 11, new String[]{"DN", "4", "01", "83", "373", "77888"}, new int[]{24, 15, 20, 20, 32, 48});

            // Dòng 8: BHYT địa chỉ người nhà
            curY += 20;
            g2.drawString("10. BHYT, địa chỉ người nhà khi cần báo tin:", padX, curY);
            curY += 16;
            g2.drawString("ngày 01 tháng 01 năm 2025", padX + 5, curY);

            // Dòng 9: Đến khám bệnh lúc 15 giờ 37 ph, ngày 11/08/2022
            curY += 18;
            g2.drawString("11. Đến khám bệnh lúc 15 giờ 37 ph, ngày 11/08/2022", padX, curY);

            // Dòng 10: Chẩn đoán nơi giới thiệu
            curY += 18;
            g2.drawString("12. Chẩn đoán của nơi giới thiệu:", padX, curY);
            g2.drawString("1.Sở tế", padX + 360, curY);
            drawCheckBox(g2, padX + 400, curY - 10, false);
            g2.drawString("2.Sn dần", padX + 425, curY);
            drawCheckBox(g2, padX + 465, curY - 10, true);

            curY += 18;
            g2.drawString("12. Đến khám bệnh lúc 15 giờ 37 ph, ngày 11/08/2022", padX, curY);

            curY += 18;
            g2.drawString("13. Chẩn đoán của nơi giới thiệu:", padX, curY);

            // --- LÝ DO VÀO VIỆN & TIỀN SỬ BỆNH ---
            curY += 22;
            g2.drawString("1. Quá trình bệnh lý:", padX, curY);

            curY += 36;
            g2.drawString("2. Tiền sử bệnh:", padX, curY);
            curY += 16;
            g2.drawString("+ Bản thân:", padX + 10, curY);
            curY += 16;
            g2.drawString("+ Gia đình:", padX + 10, curY);

            // --- PHẦN IV. KHÁM BỆNH ---
            curY += 24;
            g2.setFont(new Font("Times New Roman", Font.BOLD, 11));
            g2.drawString("IV. KHÁM BỆNH:", padX, curY);

            curY += 18;
            g2.setFont(new Font("Times New Roman", Font.PLAIN, 10));
            g2.drawString("1. Toàn thân:", padX, curY);

            // BẢNG CHỈ SỐ SINH TỒN (VITALS TABLE) BÊN PHẢI KHÁM BỆNH
            int vitalsX = padX + 375;
            int vitalsY = curY - 8;
            paintVitalsBox(g2, vitalsX, vitalsY, 140, 95);

            curY += 75;
            g2.drawString("2. Các bộ phận:", padX, curY);

            g2.dispose();
        }

        private void drawCheckBox(Graphics2D g2, int x, int y, boolean checked) {
            g2.drawRect(x, y, 12, 12);
            if (checked) {
                Font old = g2.getFont();
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                g2.drawString("X", x + 2, y + 10);
                g2.setFont(old);
            }
        }

        private void drawCodeBox(Graphics2D g2, int startX, int y, String[] codes) {
            int boxW = 14;
            int boxH = 15;
            for (int i = 0; i < codes.length; i++) {
                int bx = startX + i * boxW;
                g2.drawRect(bx, y, boxW, boxH);
                if (codes[i] != null && !codes[i].isEmpty()) {
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = bx + (boxW - fm.stringWidth(codes[i])) / 2;
                    int ty = y + fm.getAscent() + 1;
                    g2.drawString(codes[i], tx, ty);
                }
            }
        }

        private void drawCardBox(Graphics2D g2, int startX, int y, String[] segments, int[] widths) {
            int cx = startX;
            int boxH = 15;
            for (int i = 0; i < segments.length; i++) {
                int w = widths[i];
                g2.drawRect(cx, y, w, boxH);
                if (segments[i] != null) {
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = cx + (w - fm.stringWidth(segments[i])) / 2;
                    int ty = y + fm.getAscent() + 1;
                    g2.drawString(segments[i], tx, ty);
                }
                cx += w;
            }
        }

        private void paintVitalsBox(Graphics2D g2, int x, int y, int w, int h) {
            g2.setColor(Color.BLACK);
            g2.drawRect(x, y, w, h);

            int rowH = h / 5;
            int midX = x + 70;

            g2.drawLine(midX, y, midX, y + h);

            String[][] rows = {
                    {"Mạch", "l/m'ph"},
                    {"Nhiệt độ", "°C"},
                    {"Huyết áp", "mmHg"},
                    {"Nhịp thở", "l/m'ph"},
                    {"Cân nặng", "kg"}
            };

            g2.setFont(new Font("Times New Roman", Font.PLAIN, 9));
            for (int i = 0; i < 5; i++) {
                int ry = y + i * rowH;
                if (i > 0) g2.drawLine(x, ry, x + w, ry);
                g2.drawString(rows[i][0], x + 5, ry + 13);
                g2.drawString(rows[i][1], midX + 15, ry + 13);
            }
        }
    }

    // =========================================================================
    // ICONS CHO SIDEBAR & TOOLBAR
    // =========================================================================

    private static class PrinterVectorIcon implements Icon {
        private final int width, height;
        public PrinterVectorIcon(int w, int h) { this.width = w; this.height = h; }
        @Override public int getIconWidth() { return width; }
        @Override public int getIconHeight() { return height; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(60, 65, 75));
            g2.fillRect(x + 3, y + 1, 12, 4);
            g2.fillRoundRect(x + 1, y + 5, 16, 8, 2, 2);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 3, y + 9, 12, 6);
            g2.setColor(new Color(90, 95, 105));
            g2.drawRect(x + 3, y + 9, 12, 6);
            g2.dispose();
        }
    }

    private static class CheckmarkMiniIcon implements Icon {
        @Override public int getIconWidth() { return 14; }
        @Override public int getIconHeight() { return 14; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(40, 160, 75));
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 2, y + 7, x + 5, y + 11);
            g2.drawLine(x + 5, y + 11, x + 12, y + 3);
            g2.dispose();
        }
    }

    private static class PenSignatureIcon implements Icon {
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(235, 180, 50));
            g2.fillRect(x + 2, y + 2, 10, 14);
            g2.setColor(new Color(60, 70, 85));
            g2.drawRect(x + 2, y + 2, 10, 14);
            g2.setColor(new Color(65, 115, 215));
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawLine(x + 15, y + 3, x + 8, y + 13);
            g2.dispose();
        }
    }

    private static class ApproveCheckIcon implements Icon {
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(175, 185, 195));
            g2.drawRect(x + 2, y + 2, 14, 14);
            g2.drawLine(x + 4, y + 9, x + 8, y + 13);
            g2.drawLine(x + 8, y + 13, x + 14, y + 5);
            g2.dispose();
        }
    }

    private static class FloppyDiskMiniIcon implements Icon {
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(125, 45, 140));
            g2.fillRoundRect(x + 2, y + 2, 14, 14, 2, 2);
            g2.setColor(Color.WHITE);
            g2.fillRect(x + 5, y + 3, 8, 5);
            g2.fillRect(x + 4, y + 10, 10, 5);
            g2.dispose();
        }
    }

    private static class GreyCrossIcon implements Icon {
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 18; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(170, 180, 190));
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawLine(x + 4, y + 4, x + 14, y + 14);
            g2.drawLine(x + 14, y + 4, x + 4, y + 14);
            g2.dispose();
        }
    }

    private static class PdfVectorIcon implements Icon {
        @Override public int getIconWidth() { return 18; }
        @Override public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(210, 45, 45));
            g2.fillRoundRect(x + 2, y + 1, 14, 14, 2, 2);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 8));
            g2.drawString("PDF", x + 3, y + 11);
            g2.dispose();
        }
    }
}
