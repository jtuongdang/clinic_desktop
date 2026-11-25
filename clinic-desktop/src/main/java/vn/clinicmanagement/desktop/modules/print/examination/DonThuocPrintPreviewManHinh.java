package vn.clinicmanagement.desktop.modules.print.examination;


import vn.clinicmanagement.desktop.modules.print.billing.*;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Objects;

/**
 * Giao diện Xem và In: "ĐƠN THUỐC" (BC505 DONTHUOC2614 - Đơn Thuốc).
 * Kích hoạt khi người dùng nhấn nút [🖨 Đơn Thuốc (F3)] trên tab Thuốc.
 */
public class DonThuocPrintPreviewManHinh extends JPanel {

    public record PrescriptionItem(String name, String quantity, String unit, String instructions) {}

    public record PrescriptionData(
            String patientName,
            String patientCode,
            String birthYear,
            String gender,
            String bhyt,
            String diagnosis,
            List<PrescriptionItem> medicines,
            String doctorName) {
        public PrescriptionData {
            medicines = medicines == null ? List.of() : List.copyOf(medicines);
        }
    }

    private PrescriptionData data;

    public DonThuocPrintPreviewManHinh() {
        rebuildUi();
    }

    public DonThuocPrintPreviewManHinh(PrescriptionData data) {
        this.data = data;
        rebuildUi();
    }

    public void bindData(PrescriptionData data) {
        this.data = data;
        rebuildUi();
    }

    public PrescriptionData getBoundData() {
        return data;
    }

    private void rebuildUi() {
        removeAll();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. THANH CÔNG CỤ ĐỈNH KÈM BANNER BỆNH NHÂN GÓC PHẢI
        add(buildTopToolBar(), BorderLayout.NORTH);

        // 2. KHUNG TRUNG TÂM: CỘT BÊN TRÁI + VÙNG XEM TÀI LIỆU
        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setBackground(new Color(56, 56, 56));

        // Cột bên trái: Các nút Ký, Duyệt, Lưu, Hủy, Xuất PDF
        centerContainer.add(buildLeftSidebar(), BorderLayout.WEST);

        // Vùng xem trước trang giấy in A4 Đơn Thuốc
        JScrollPane scrollPane = new JScrollPane(new PrescriptionSheet());
        scrollPane.getViewport().setBackground(new Color(56, 56, 56));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        centerContainer.add(scrollPane, BorderLayout.CENTER);

        add(centerContainer, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JPanel buildTopToolBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(242, 246, 252));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(205, 215, 228)));
        bar.setPreferredSize(new Dimension(1000, 95));

        // Cụm trái: In tài liệu, Loại chữ ký, Hiệu lực, Kiểm tra
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 18));
        left.setOpaque(false);

        JButton btnPrint = new JButton(" In Tài Liệu", new PrinterVectorIcon(18, 18));
        btnPrint.setFont(UngDungPhongChu.SMALL_BOLD);
        btnPrint.setBackground(Color.WHITE);
        btnPrint.setFocusPainted(false);
        btnPrint.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPrint.setPreferredSize(new Dimension(94, 58));
        btnPrint.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(185, 195, 210), 1),
                new EmptyBorder(2, 6, 2, 6)
        ));
        btnPrint.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    hasPrescriptionData() ? "Đơn thuốc đã sẵn sàng để in." : "Chưa có dữ liệu đơn thuốc để in.",
                    "In Đơn Thuốc", JOptionPane.INFORMATION_MESSAGE);
        });

        JLabel lblSigType = new JLabel("Loại chữ ký : ---");
        lblSigType.setFont(UngDungPhongChu.SMALL);
        lblSigType.setForeground(new Color(40, 45, 55));

        JLabel lblValidity = new JLabel("Hiệu lực : ---");
        lblValidity.setFont(UngDungPhongChu.SMALL);
        lblValidity.setForeground(new Color(40, 45, 55));

        JButton btnVerify = new JButton(" Kiểm tra", new CheckmarkMiniIcon());
        btnVerify.setFont(UngDungPhongChu.SMALL_BOLD);
        btnVerify.setBackground(Color.WHITE);
        btnVerify.setFocusPainted(false);
        btnVerify.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVerify.setPreferredSize(new Dimension(150, 58));
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

        // Cụm phải: Banner thông tin bệnh nhân góc phải khớp 100% media_1793381314310_56e47612.jpg
        JPanel right = new JPanel(new BorderLayout(0, 0));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(2, 6, 2, 8));

        JPanel line1 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        line1.setOpaque(false);
        JLabel lblPtName = new JLabel(patientBannerText());
        lblPtName.setFont(UngDungPhongChu.SMALL_BOLD);
        lblPtName.setForeground(new Color(140, 20, 20));

        JLabel lblBhyt = new JLabel(value(data == null ? null : data.bhyt()));
        lblBhyt.setFont(UngDungPhongChu.SMALL_BOLD);
        lblBhyt.setForeground(new Color(180, 20, 20));

        line1.add(lblPtName);
        line1.add(lblBhyt);

        JLabel lblLine2 = new JLabel(value(data == null ? null : data.patientCode()), SwingConstants.RIGHT);
        lblLine2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblLine2.setForeground(new Color(60, 65, 75));

        JLabel lblLine3 = new JLabel(value(data == null ? null : data.birthYear()), SwingConstants.RIGHT);
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
        bar.setPreferredSize(new Dimension(188, 600));
        bar.setBackground(new Color(240, 243, 248));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(205, 215, 226)));

        // Tiêu đề: Bệnh án điện tử
        JLabel lblTitle = new JLabel("Bệnh án điện tử", SwingConstants.CENTER);
        lblTitle.setFont(UngDungPhongChu.SMALL_BOLD);
        lblTitle.setForeground(new Color(30, 35, 45));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTitle.setBorder(new EmptyBorder(4, 4, 2, 4));

        bar.add(lblTitle);
        bar.add(Box.createVerticalStrut(1));

        // 4 nút thao tác chính
        bar.add(createSidebarButton("Ký Tài Liệu", new PenSignatureIcon(), true));
        bar.add(Box.createVerticalStrut(4));
        bar.add(createSidebarButton("Duyệt Tài Liệu", new ApproveCheckIcon(), false));
        bar.add(Box.createVerticalStrut(4));
        bar.add(createSidebarButton("Lưu Tài Liệu", new FloppyDiskMiniIcon(), true));
        bar.add(Box.createVerticalStrut(4));
        bar.add(createSidebarButton("Hủy Tài Liệu", new GreyCrossIcon(), false));

        bar.add(Box.createVerticalStrut(8));

        // Nhãn "Xuất file..."
        JLabel lblExport = new JLabel("Xuất file...");
        lblExport.setFont(UngDungPhongChu.SMALL);
        lblExport.setForeground(new Color(50, 60, 70));
        lblExport.setAlignmentX(Component.CENTER_ALIGNMENT);
        bar.add(lblExport);
        bar.add(Box.createVerticalStrut(4));

        // Cụm nút Xuất file: [PDF] và [☰ ▾]
        JPanel exportGroup = new JPanel();
        exportGroup.setLayout(new BoxLayout(exportGroup, BoxLayout.Y_AXIS));
        exportGroup.setOpaque(false);
        exportGroup.setMaximumSize(new Dimension(172, 124));

        JButton btnPdf = new JButton("PDF", new PdfVectorIcon());
        btnPdf.setFont(new Font(UngDungPhongChu.FAMILY, Font.BOLD, 10));
        btnPdf.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnPdf.setHorizontalTextPosition(SwingConstants.CENTER);
        btnPdf.setPreferredSize(new Dimension(168, 74));
        btnPdf.setBackground(Color.WHITE);
        btnPdf.setFocusPainted(false);
        btnPdf.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPdf.setBorder(new LineBorder(new Color(185, 195, 210), 1));
        btnPdf.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    hasPrescriptionData() ? "Đơn thuốc đã sẵn sàng để xuất PDF." : "Chưa có dữ liệu đơn thuốc để xuất PDF.",
                    "Xuất PDF", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton btnMore = new JButton("☰ ▾");
        btnMore.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        btnMore.setPreferredSize(new Dimension(168, 42));
        btnMore.setBackground(Color.WHITE);
        btnMore.setFocusPainted(false);
        btnMore.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMore.setBorder(new LineBorder(new Color(185, 195, 210), 1));

        btnPdf.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnMore.setAlignmentX(Component.CENTER_ALIGNMENT);
        exportGroup.add(btnPdf);
        exportGroup.add(Box.createVerticalStrut(4));
        exportGroup.add(btnMore);
        bar.add(exportGroup);

        bar.add(Box.createVerticalGlue());

        // Chân trang sidebar: Dòng chữ đỏ "Chưa ký 1"
        JLabel lblStatus = new JLabel("Chưa ký 1", SwingConstants.CENTER);
        lblStatus.setFont(UngDungPhongChu.SMALL_BOLD);
        lblStatus.setForeground(new Color(205, 30, 30));
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblStatus.setBorder(new EmptyBorder(6, 4, 12, 4));

        bar.add(lblStatus);

        return bar;
    }

    private JButton createSidebarButton(String text, Icon icon, boolean isEnabled) {
        JButton btn = new JButton(text, icon);
        btn.setFont(UngDungPhongChu.SMALL_BOLD);
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setIconTextGap(3);
        btn.setPreferredSize(new Dimension(168, 74));
        btn.setMaximumSize(new Dimension(168, 74));
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

    private boolean hasPrescriptionData() {
        return data != null && (!value(data.patientName()).isEmpty()
                || !value(data.patientCode()).isEmpty() || !data.medicines().isEmpty());
    }

    private String patientBannerText() {
        if (data == null) return "";
        return String.join(" | ", List.of(
                value(data.patientName()),
                value(data.patientCode()),
                birthYearAndAge(),
                value(data.gender())
        ).stream().filter(text -> !text.isEmpty()).toList());
    }

    private String birthYearAndAge() {
        if (data == null || value(data.birthYear()).isBlank()) return "";
        String birthYear = value(data.birthYear()).trim();
        try {
            int year = Integer.parseInt(birthYear);
            int age = year > 1900 ? java.time.Year.now().getValue() - year : year;
            return birthYear + " (" + age + " Tuổi)";
        } catch (NumberFormatException ignored) {
            return birthYear;
        }
    }

    private String prescriptionAccessibleText() {
        if (data == null) return "Chưa có dữ liệu đơn thuốc.";
        String medicines = data.medicines().stream()
                .map(item -> value(item.name()) + " " + value(item.quantity()) + " " + value(item.unit())
                        + " " + value(item.instructions()))
                .reduce("", (all, item) -> all + " " + item);
        return (patientBannerText() + " " + value(data.bhyt()) + " "
                + value(data.diagnosis()) + medicines).trim();
    }

    private static String value(String text) {
        return Objects.requireNonNullElse(text, "");
    }

    /**
     * Tờ giấy A4 mô phỏng "ĐƠN THUỐC"
     */
    private class PrescriptionSheet extends JPanel implements Scrollable {
        private static final int PAGE_W = 614;
        private static final int PAGE_H = 875;

        PrescriptionSheet() {
            setOpaque(true);
            setBackground(new Color(56, 56, 56));
            getAccessibleContext().setAccessibleName(prescriptionAccessibleText());
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(PAGE_W * 2, PAGE_H); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 100; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return true; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            double scale = Math.min((double) getWidth() / PAGE_W, (double) getHeight() / PAGE_H);
            int pageW = (int) Math.round(PAGE_W * scale);
            int pageH = (int) Math.round(PAGE_H * scale);
            int x = (getWidth() - pageW) / 2;
            int y = (getHeight() - pageH) / 2;
            g2.setColor(new Color(30, 30, 30, 100));
            g2.fillRect(x + 3, y + 3, pageW, pageH);
            g2.translate(x, y);
            g2.scale(scale, scale);
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, PAGE_W, PAGE_H);
            g2.setColor(new Color(205, 205, 205));
            g2.drawRect(0, 0, PAGE_W - 1, PAGE_H - 1);

            g2.setColor(Color.BLACK);
            if (!hasPrescriptionData()) {
                g2.setFont(new Font("Times New Roman", Font.PLAIN, 17));
                g2.drawString("Chưa có dữ liệu đơn thuốc.", 24, 54);
                g2.dispose();
                return;
            }

            g2.setFont(new Font("Times New Roman", Font.BOLD, 17));
            g2.drawString("BỆNH VIỆN SẢN NHI", 24, 48);
            g2.drawString("Mã BN: " + value(data.patientCode()), 444, 48);
            g2.setFont(new Font("Times New Roman", Font.PLAIN, 17));
            g2.drawString("PK Số: 1", 24, 70);
            g2.drawString("Khoa thuốc ngoại trú", 24, 94);
            g2.setFont(new Font("Times New Roman", Font.BOLD, 22));
            drawCentered(g2, "ĐƠN THUỐC", PAGE_W, 124);

            g2.setFont(new Font("Times New Roman", Font.PLAIN, 17));
            int yLine = 162;
            g2.drawString("- Họ tên: " + value(data.patientName()), 24, yLine);
            g2.drawString("Tuổi: " + ageFromBirthYear(), 310, yLine);
            g2.drawString("Giới tính:", 426, yLine);
            g2.setFont(new Font("Times New Roman", Font.BOLD, 17));
            g2.drawString(value(data.gender()), 538, yLine);
            g2.setFont(new Font("Times New Roman", Font.PLAIN, 17));
            yLine += 24;
            g2.drawString("- Đối tượng: Bảo hiểm", 24, yLine);
            yLine += 24;
            g2.drawString("- Số thẻ BHYT: " + value(data.bhyt()), 24, yLine);
            g2.drawString("- Hạn thẻ đến: -", 360, yLine);
            yLine += 24;
            drawClipped(g2, "- Địa chỉ: -", 24, yLine, PAGE_W - 48);
            yLine += 24;
            drawClipped(g2, "- Chẩn đoán: " + value(data.diagnosis()), 24, yLine, PAGE_W - 48);
            yLine += 24;
            g2.drawString("- Chẩn đoán kèm theo: -", 24, yLine);

            yLine += 32;
            for (int i = 0; i < data.medicines().size(); i++) {
                PrescriptionItem item = data.medicines().get(i);
                g2.setFont(new Font("Times New Roman", Font.BOLD, 17));
                drawClipped(g2, (i + 1) + ".    " + value(item.name()), 24, yLine, 420);
                drawRight(g2, value(item.quantity()), 525, yLine);
                drawRight(g2, value(item.unit()), 590, yLine);
                yLine += 23;
                g2.setFont(new Font("Times New Roman", Font.ITALIC, 16));
                drawClipped(g2, value(item.instructions()), 57, yLine, 475);
                yLine += 27;
            }
            yLine += 7;
            g2.setFont(new Font("Times New Roman", Font.PLAIN, 17));
            g2.drawString("* Tổng thuốc: " + String.format("%02d", data.medicines().size()), 24, yLine);
            yLine += 24;
            g2.setFont(new Font("Times New Roman", Font.BOLD, 17));
            g2.drawString("Lời dặn:", 24, yLine);

            g2.setFont(new Font("Times New Roman", Font.PLAIN, 16));
            String printedAt = java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm 'Ngày' dd 'tháng' MM 'năm' yyyy"));
            drawRight(g2, printedAt, 590, 504);
            g2.setFont(new Font("Times New Roman", Font.BOLD, 17));
            drawRight(g2, "BÁC SỸ KHÁM", 590, 529);
            g2.setFont(new Font("Times New Roman", Font.ITALIC, 16));
            drawRight(g2, "(Ký, họ tên)", 590, 551);
            g2.setColor(new Color(40, 70, 220));
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{8f, 6f}, 0f));
            g2.drawRect(426, 558, 142, 58);
            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            drawCenteredIn(g2, "Nhấp đúp", 426, 587, 142);
            drawCenteredIn(g2, "chuột để ký", 426, 609, 142);
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Times New Roman", Font.BOLD, 16));
            drawRight(g2, "*****", 574, 635);

            g2.setFont(new Font("Times New Roman", Font.ITALIC, 15));
            g2.drawString("* Khám lại xin mang theo đơn này.", 24, 675);
            g2.drawString("- Trẻ bé hoặc mẹ cho trẻ bú: người đưa trẻ đến khám bệnh, chữa bệnh: ”", 24, 699);
            g2.dispose();
        }

        private void drawCentered(Graphics2D g, String text, int width, int y) {
            g.drawString(text, (width - g.getFontMetrics().stringWidth(text)) / 2, y);
        }

        private void drawCenteredIn(Graphics2D g, String text, int x, int y, int width) {
            g.drawString(text, x + (width - g.getFontMetrics().stringWidth(text)) / 2, y);
        }

        private void drawRight(Graphics2D g, String text, int x, int y) {
            g.drawString(text, x - g.getFontMetrics().stringWidth(text), y);
        }

        private void drawClipped(Graphics2D g, String text, int x, int y, int width) {
            String value = text;
            FontMetrics metrics = g.getFontMetrics();
            while (!value.isEmpty() && metrics.stringWidth(value) > width) value = value.substring(0, value.length() - 1);
            g.drawString(value, x, y);
        }

        private String ageFromBirthYear() {
            try {
                int value = Integer.parseInt(value(data.birthYear()).trim());
                return Integer.toString(value > 1900 ? java.time.Year.now().getValue() - value : value);
            } catch (NumberFormatException ignored) {
                return value(data.birthYear());
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
