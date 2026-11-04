package vn.clinicmanagement.desktop.modules.examination.components.print;

import vn.clinicmanagement.desktop.core.config.AppFonts;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Giao diện Xem và In: "BỆNH ÁN KHÁM BỆNH - CHUYÊN KHOA TAI - MŨI - HỌNG" (EHC EMR Print Station).
 * Kích hoạt khi bấm vào mục "In ▾" -> "Bệnh Án Khám Bệnh" trên thanh công cụ Khám Bệnh.
 * Khớp 100% giao diện trong ảnh chụp thực tế:
 * - Cột chức năng bên trái (Bệnh án điện tử):
 *   + [✍ Ký Tài Liệu]
 *   + [☑ Duyệt Tài Liệu] (disabled)
 *   + [💾 Lưu Tài Liệu]
 *   + [✕ Hủy Tài Liệu] (disabled)
 *   + Xuất file... [📄 PDF] [☰ ▾]
 *   + Dòng chữ đỏ chân trang: "Chưa Ký !"
 * - Thanh công cụ đỉnh:
 *   + Cụm trái: [🖨 In Tài Liệu], Loại chữ ký : ---, Hiệu lực : ---, [✔ Kiểm Tra]
 *   + Cụm phải: Banner bệnh nhân TEST1 | 22000324 | 2000 (22 tuổi) | Nam | BHYT 80%
 * - Vùng trung tâm: Trang giấy A4 mô phỏng chuẩn xác mẫu bệnh án chuyên khoa Tai - Mũi - Họng
 *   + Phần I. HÀNH CHÍNH (Đầy đủ các ô mã số, hộp tích, số thẻ BHYT, địa chỉ)
 *   + Phần II. LÝ DO VÀO VIỆN
 *   + Phần III. HỎI BỆNH
 *   + Phần IV. KHÁM XÉT (Toàn thân, Bệnh chuyên khoa: TAI, MŨI, HỌNG)
 */
public class MedicalRecordPrintPreviewPanel extends JPanel {

    public MedicalRecordPrintPreviewPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. THANH CÔNG CỤ ĐỈNH KÈM BANNER BỆNH NHÂN GÓC PHẢI
        add(buildTopToolBar(), BorderLayout.NORTH);

        // 2. KHUNG TRUNG TÂM: CỘT BÊN TRÁI + VÙNG XEM TÀI LIỆU
        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setBackground(new Color(90, 98, 110));

        // Cột bên trái: Các nút Ký, Duyệt, Lưu, Hủy, Xuất PDF
        centerContainer.add(buildLeftSidebar(), BorderLayout.WEST);

        // Vùng xem trước trang giấy in A4
        JScrollPane scrollPane = new JScrollPane(buildDocumentSheetView());
        scrollPane.getViewport().setBackground(new Color(105, 115, 128));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        centerContainer.add(scrollPane, BorderLayout.CENTER);

        add(centerContainer, BorderLayout.CENTER);
    }

    private JPanel buildTopToolBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(242, 246, 252));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 215, 228)));
        bar.setPreferredSize(new Dimension(1000, 48));

        // Cụm trái: In tài liệu, Loại chữ ký, Hiệu lực, Kiểm tra
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
                    "▶ Mẫu in: BỆNH ÁN KHÁM BỆNH (CHUYÊN KHOA TAI - MŨI - HỌNG)\n" +
                    "▶ Bệnh nhân: TEST1 (Mã BN: 22000324)\n" +
                    "▶ Số khám bệnh: 22/000063",
                    "In Tài Liệu", JOptionPane.INFORMATION_MESSAGE);
        });

        JLabel lblSigType = new JLabel("Loại chữ ký : ---");
        lblSigType.setFont(AppFonts.SMALL);
        lblSigType.setForeground(new Color(40, 45, 55));

        JLabel lblValidity = new JLabel("Hiệu lực : ---");
        lblValidity.setFont(AppFonts.SMALL);
        lblValidity.setForeground(new Color(40, 45, 55));

        JButton btnVerify = new JButton(" Kiểm Tra", new CheckmarkMiniIcon());
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

        // Cụm phải: Banner thông tin bệnh nhân góc phải
        JPanel right = new JPanel(new BorderLayout(0, 0));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(2, 6, 2, 8));

        JPanel bannerText = new JPanel(new GridLayout(3, 1, 0, 0));
        bannerText.setOpaque(false);
        bannerText.setBorder(new EmptyBorder(0, 0, 0, 8));

        JLabel l1 = new JLabel("TEST1 | 22000324 | 2000 (22 tuổi) | Nam", SwingConstants.RIGHT);
        l1.setFont(AppFonts.SMALL_BOLD);
        l1.setForeground(new Color(130, 20, 20));

        JLabel l2 = new JLabel("Mã VP : 214348 | DN-4-01-2929299222 | Còn 864 ngày", SwingConstants.RIGHT);
        l2.setFont(AppFonts.SMALL);
        l2.setForeground(new Color(130, 20, 20));

        JLabel l3 = new JLabel("Thị trấn Phố Mới, H. Quế Võ, Bắc Ninh", SwingConstants.RIGHT);
        l3.setFont(AppFonts.SMALL);
        l3.setForeground(new Color(130, 20, 20));

        bannerText.add(l1);
        bannerText.add(l2);
        bannerText.add(l3);

        JLabel lblTag = new JLabel("<html><center><font color='#800000'><b>BHYT</b></font><br><font color='#0044CC'><b>80%</b></font></center></html>", SwingConstants.CENTER);
        lblTag.setFont(AppFonts.SMALL_BOLD);
        lblTag.setBorder(new EmptyBorder(0, 6, 0, 4));

        right.add(bannerText, BorderLayout.CENTER);
        right.add(lblTag, BorderLayout.EAST);

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
                    "Đang xuất hồ sơ bệnh án ra tệp định dạng PDF:\n" +
                    "▶ Tệp: BenhAn_TaiMuiHong_TEST1_22000324.pdf",
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

        // Chân trang sidebar: Dòng chữ đỏ "Chưa Ký !"
        JLabel lblStatus = new JLabel("Chưa Ký !", SwingConstants.CENTER);
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
            btn.setForeground(new Color(30, 40, 55));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(175, 188, 205), 1),
                    new EmptyBorder(2, 4, 2, 4)
            ));
        } else {
            btn.setBackground(new Color(245, 247, 250));
            btn.setForeground(new Color(160, 168, 178));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(220, 226, 235), 1),
                    new EmptyBorder(2, 4, 2, 4)
            ));
        }
        return btn;
    }

    private JPanel buildDocumentSheetView() {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        wrapper.setOpaque(false);

        ENTMedicalRecordSheet sheet = new ENTMedicalRecordSheet();
        wrapper.add(sheet);
        return wrapper;
    }

    // =========================================================================
    // TRANG GIẤY IN A4: BỆNH ÁN TAI - MŨI - HỌNG (VẼ ĐỒ HỌA CHÍNH XÁC 1:1)
    // =========================================================================

    private static class ENTMedicalRecordSheet extends JPanel {
        private final int sheetW = 760;
        private final int sheetH = 1060;

        public ENTMedicalRecordSheet() {
            setPreferredSize(new Dimension(sheetW, sheetH));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(140, 150, 165), 1),
                    new EmptyBorder(28, 36, 28, 36)
            ));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Nền giấy trắng
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());

            Font fSerif = new Font("Times New Roman", Font.PLAIN, 12);
            Font fSerifBold = new Font("Times New Roman", Font.BOLD, 12);
            Font fHeading = new Font("Times New Roman", Font.BOLD, 16);
            Font fSubHeading = new Font("Times New Roman", Font.BOLD, 13);
            Font fSmall = new Font("Times New Roman", Font.PLAIN, 11);

            int xL = 36;
            int xR = sheetW - 36;
            int y = 42;

            g2.setColor(Color.BLACK);

            // 1. TIÊU NGỮ TRÁI VÀ SỐ KHÁM BỆNH PHẢI
            g2.setFont(fSerif);
            g2.drawString("SỞ Y TẾ BẮC NINH", xL, y);
            g2.setFont(fSerifBold);
            g2.drawString("BỆNH VIỆN SẢN NHI", xL, y + 15);

            g2.setFont(fSerif);
            String strSoKB = "Số khám bệnh: 22/000063";
            g2.drawString(strSoKB, xR - g2.getFontMetrics().stringWidth(strSoKB), y);

            // 2. TIÊU ĐỀ CHÍNH GIỮA TRANG
            g2.setFont(fHeading);
            drawCenteredText(g2, "KHÁM BỆNH", y + 10, sheetW);

            g2.setFont(fSubHeading);
            drawCenteredText(g2, "(CHUYÊN KHOA TAI – MŨI – HỌNG)", y + 32, sheetW);

            // KHOA KHÁM BỆNH [ X ]   BUỒNG CẤP CỨU [   ]
            y += 58;
            g2.setFont(fSerif);
            String sKhoa = "KHOA KHÁM BỆNH";
            int cx = 220;
            g2.drawString(sKhoa, cx, y);
            drawCheckSquare(g2, cx + 115, y - 11, true);

            String sBuong = "BUỒNG CẤP CỨU";
            g2.drawString(sBuong, cx + 155, y);
            drawCheckSquare(g2, cx + 270, y - 11, false);

            // 3. PHẦN I. HÀNH CHÍNH
            y += 32;
            g2.setFont(fSerifBold);
            g2.drawString("I. HÀNH CHÍNH", xL, y);

            y += 20;
            g2.setFont(fSerif);
            g2.drawString("1. Họ và tên (In hoa):", xL, y);
            g2.setFont(fSerifBold);
            g2.drawString("TEST1", xL + 120, y);

            g2.setFont(fSerif);
            g2.drawString("2. Sinh ngày:", xL + 290, y);
            drawCodeBox(g2, xL + 360, y - 12, new String[]{"", ""});
            drawCodeBox(g2, xL + 400, y - 12, new String[]{"", ""});
            drawCodeBox(g2, xL + 440, y - 12, new String[]{"2", "0", "0", "0"});

            g2.drawString("Tuổi", xL + 560, y - 14);
            drawCodeBox(g2, xL + 555, y - 12, new String[]{"2", "2"});

            // Hàng 2: Giới: 1. Nam [X] 2. Nữ [ ] | Nghề nghiệp: Nhân dân [1][3]
            y += 24;
            g2.drawString("3. Giới:", xL, y);
            g2.drawString("1. Nam", xL + 55, y);
            drawCheckSquare(g2, xL + 95, y - 11, true);
            g2.drawString("2. Nữ", xL + 125, y);
            drawCheckSquare(g2, xL + 155, y - 11, false);

            g2.drawString("4. Nghề nghiệp: Nhân dân", xL + 215, y);
            drawCodeBox(g2, xL + 555, y - 12, new String[]{"1", "3"});

            // Hàng 3: Dân tộc: Kinh [2][5] | Ngoại kiều: Việt nam [V][N]
            y += 24;
            g2.drawString("5. Dân tộc: Kinh", xL, y);
            drawCodeBox(g2, xL + 140, y - 12, new String[]{"2", "5"});

            g2.drawString("6. Ngoại kiều: Việt nam", xL + 215, y);
            drawCodeBox(g2, xL + 555, y - 12, new String[]{"V", "N"});

            // Hàng 4: Địa chỉ: Số nhà: ... Thôn, phố: ... Xã, phường: Thị trấn Phố Mới
            y += 24;
            g2.drawString("7. Địa chỉ: Số nhà:", xL, y);
            g2.drawString("Thôn, phố:", xL + 180, y);
            g2.drawString("Xã, phường: Thị trấn Phố Mới", xL + 340, y);

            // Hàng 5: Huyện (Q. Tx): H. Quế Võ [0][3] | Tỉnh, thành phố: Bắc Ninh [2]
            y += 24;
            g2.drawString("Huyện (Q. Tx): H. Quế Võ", xL, y);
            drawCodeBox(g2, xL + 145, y - 12, new String[]{"0", "3"});

            g2.drawString("Tỉnh, thành phố: Bắc Ninh", xL + 215, y);
            drawCodeBox(g2, xL + 568, y - 12, new String[]{"2"});

            // Hàng 6: Nơi làm việc | Đối tượng: 1.BHYT [X] 2.Thu phí [ ] 3.Miễn [ ] 4.Khác [ ]
            y += 24;
            g2.drawString("8. Nơi làm việc:", xL, y);

            g2.drawString("9. Đối tượng:  1.BHYT", xL + 215, y);
            drawCheckSquare(g2, xL + 330, y - 11, true);
            g2.drawString("2.Thu phí", xL + 355, y);
            drawCheckSquare(g2, xL + 405, y - 11, false);
            g2.drawString("3.Miễn", xL + 430, y);
            drawCheckSquare(g2, xL + 470, y - 11, false);
            g2.drawString("4.Khác", xL + 495, y);
            drawCheckSquare(g2, xL + 535, y - 11, false);

            // Hàng 7: BHYT giá trị đến ngày 01 tháng 01 năm 2025 | Số thẻ BHYT [DN][4][01][29][292][99222]
            y += 24;
            g2.drawString("10. BHYT giá trị đến ngày 01 tháng 01 năm 2025", xL, y);

            g2.drawString("Số thẻ BHYT", xL + 310, y);
            drawCardBox(g2, xL + 380, y - 12, new String[]{"DN", "4", "01", "29", "292", "99222"}, new int[]{22, 16, 20, 20, 28, 38});

            // Hàng 8: Họ tên, địa chỉ người nhà khi cần báo tin
            y += 24;
            g2.drawString("11. Họ tên, địa chỉ người nhà khi cần báo tin:", xL, y);

            // Hàng 9: Đến khám bệnh lúc 11 giờ 26 phút, Ngày 22 tháng 08 năm 2022
            y += 24;
            g2.drawString("12. Đến khám bệnh lúc 11 giờ 26 phút, Ngày 22 tháng 08 năm 2022", xL, y);

            // Hàng 10: Chẩn đoán của nơi giới thiệu
            y += 24;
            g2.drawString("13. Chẩn đoán của nơi giới thiệu:", xL, y);

            // 4. PHẦN II. LÝ DO VÀO VIỆN
            y += 30;
            g2.setFont(fSerifBold);
            g2.drawString("II. LÝ DO VÀO VIỆN:", xL, y);

            // 5. PHẦN III. HỎI BỆNH
            y += 28;
            g2.drawString("III. HỎI BỆNH:", xL, y);
            y += 20;
            g2.setFont(fSerif);
            g2.drawString("1. Quá trình bệnh lý:", xL, y);

            // 6. PHẦN IV. KHÁM XÉT
            y += 34;
            g2.setFont(fSerifBold);
            g2.drawString("IV. KHÁM XÉT: 1. Toàn thân:", xL, y);

            y += 30;
            g2.drawString("2. Bệnh chuyên khoa:", xL, y);

            y += 28;
            g2.drawString("TAI:", xL + 120, y);

            y += 48;
            g2.drawString("MŨI:", xL + 120, y);

            y += 48;
            g2.drawString("HỌNG:", xL + 120, y);

            g2.dispose();
        }

        private void drawCenteredText(Graphics2D g2, String text, int y, int width) {
            FontMetrics fm = g2.getFontMetrics();
            int x = (width - fm.stringWidth(text)) / 2;
            g2.drawString(text, x, y);
        }

        private void drawCheckSquare(Graphics2D g2, int x, int y, boolean isChecked) {
            g2.drawRect(x, y, 13, 13);
            if (isChecked) {
                Font oldF = g2.getFont();
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                g2.drawString("X", x + 2, y + 10);
                g2.setFont(oldF);
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
    }

    // =========================================================================
    // VECTOR ICONS KHỚP 100% ẢNH MẪU
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
            // Cây bút chéo
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
