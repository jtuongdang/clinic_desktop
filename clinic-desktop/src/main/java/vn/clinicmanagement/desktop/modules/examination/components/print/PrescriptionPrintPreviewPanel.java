package vn.clinicmanagement.desktop.modules.examination.components.print;

import vn.clinicmanagement.desktop.core.config.AppFonts;

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
public class PrescriptionPrintPreviewPanel extends JPanel {

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

    public PrescriptionPrintPreviewPanel() {
        rebuildUi();
    }

    public PrescriptionPrintPreviewPanel(PrescriptionData data) {
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
        centerContainer.setBackground(new Color(90, 98, 110));

        // Cột bên trái: Các nút Ký, Duyệt, Lưu, Hủy, Xuất PDF
        centerContainer.add(buildLeftSidebar(), BorderLayout.WEST);

        // Vùng xem trước trang giấy in A4 Đơn Thuốc
        JScrollPane scrollPane = new JScrollPane(new PrescriptionSheet());
        scrollPane.getViewport().setBackground(new Color(105, 115, 128));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
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
                    hasPrescriptionData() ? "Đơn thuốc đã sẵn sàng để in." : "Chưa có dữ liệu đơn thuốc để in.",
                    "In Đơn Thuốc", JOptionPane.INFORMATION_MESSAGE);
        });

        JLabel lblSigType = new JLabel("Loại chữ ký : ---");
        lblSigType.setFont(AppFonts.SMALL);
        lblSigType.setForeground(new Color(40, 45, 55));

        JLabel lblValidity = new JLabel("Hiệu lực : ---");
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

        // Cụm phải: Banner thông tin bệnh nhân góc phải khớp 100% media_1793381314310_56e47612.jpg
        JPanel right = new JPanel(new BorderLayout(0, 0));
        right.setOpaque(false);
        right.setBorder(new EmptyBorder(2, 6, 2, 8));

        JPanel line1 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        line1.setOpaque(false);
        JLabel lblPtName = new JLabel(patientBannerText());
        lblPtName.setFont(AppFonts.SMALL_BOLD);
        lblPtName.setForeground(new Color(140, 20, 20));

        JLabel lblBhyt = new JLabel(value(data == null ? null : data.bhyt()));
        lblBhyt.setFont(AppFonts.SMALL_BOLD);
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
                    hasPrescriptionData() ? "Đơn thuốc đã sẵn sàng để xuất PDF." : "Chưa có dữ liệu đơn thuốc để xuất PDF.",
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

    private boolean hasPrescriptionData() {
        return data != null && (!value(data.patientName()).isEmpty()
                || !value(data.patientCode()).isEmpty() || !data.medicines().isEmpty());
    }

    private String patientBannerText() {
        if (data == null) return "";
        return String.join(" | ", List.of(
                value(data.patientName()),
                value(data.patientCode()),
                value(data.birthYear()),
                value(data.gender())
        ).stream().filter(text -> !text.isEmpty()).toList());
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
    private class PrescriptionSheet extends JPanel {

        private final int PAGE_W = 750;
        private final int PAGE_H = 920;

        public PrescriptionSheet() {
            setOpaque(true);
            setBackground(new Color(105, 115, 128));
            setPreferredSize(new Dimension(PAGE_W + 60, PAGE_H + 50));
            getAccessibleContext().setAccessibleName(prescriptionAccessibleText());
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

            int padX = x + 40;
            int topY = y + 30;
            if (!hasPrescriptionData()) {
                g2.setColor(new Color(90, 98, 110));
                g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
                g2.drawString("Chưa có dữ liệu đơn thuốc.", padX, topY + 40);
                g2.dispose();
                return;
            }

            // --- HEADER ---
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Times New Roman", Font.BOLD, 12));
            g2.drawString("BỆNH VIỆN SẢN NHI", padX, topY + 12);

            g2.setFont(new Font("Times New Roman", Font.PLAIN, 11));
            g2.drawString("", padX, topY + 28);

            int rightX = x + PAGE_W - 160;
            g2.setFont(new Font("Times New Roman", Font.BOLD, 11));
            g2.drawString("Mã BN: " + value(data.patientCode()), rightX, topY + 14);

            // Tiêu đề giữa: ĐƠN THUỐC
            int curY = topY + 80;
            g2.setFont(new Font("Times New Roman", Font.BOLD, 16));
            String title = "ĐƠN THUỐC";
            FontMetrics fmt = g2.getFontMetrics();
            int titleX = x + (PAGE_W - fmt.stringWidth(title)) / 2;
            g2.drawString(title, titleX, curY);

            // --- THÔNG TIN HÀNH CHÍNH & LÂM SÀNG ---
            curY += 28;
            g2.setFont(new Font("Times New Roman", Font.PLAIN, 11));
            int lineH = 18;

            // Dòng 1: Họ tên, Tuổi, Cân nặng, Giới tính
            g2.drawString("- Họ tên: " + value(data.patientName()), padX, curY);
            g2.drawString("Năm sinh: " + value(data.birthYear()), padX + 260, curY);
            g2.drawString("Cân nặng:", padX + 360, curY);
            g2.drawString(value(data.gender()), padX + 480, curY);

            // Dòng 2: Đối tượng
            curY += lineH;
            g2.drawString("- Đối tượng: Bảo hiểm", padX, curY);

            // Dòng 3: Số thẻ BHYT, Hạn thẻ
            curY += lineH;
            g2.drawString("- Số thẻ BHYT: " + value(data.bhyt()), padX, curY);

            curY += lineH;
            g2.drawString("- Chẩn đoán: " + value(data.diagnosis()), padX, curY);

            // --- DANH SÁCH THUỐC KÊ ĐƠN ---
            curY += 28;
            int rightColX = x + PAGE_W - 110;

            g2.setFont(new Font("Times New Roman", Font.PLAIN, 11));
            if (data.medicines().isEmpty()) {
                g2.drawString("Chưa có thuốc được kê.", padX, curY);
                curY += 18;
            } else {
                for (int i = 0; i < data.medicines().size(); i++) {
                    PrescriptionItem item = data.medicines().get(i);
                    g2.setFont(new Font("Times New Roman", Font.PLAIN, 11));
                    g2.drawString((i + 1) + ".     " + value(item.name()), padX, curY);
                    g2.setFont(new Font("Times New Roman", Font.BOLD, 11));
                    g2.drawString(value(item.quantity()), rightColX - 35, curY);
                    g2.drawString(value(item.unit()), rightColX, curY);
                    curY += 16;
                    g2.setFont(new Font("Times New Roman", Font.ITALIC, 11));
                    g2.drawString(value(item.instructions()), padX + 30, curY);
                    curY += 22;
                }
            }

            // --- TỔNG KHOẢN & LỜI DẶN ---
            curY += 6;
            g2.setFont(new Font("Times New Roman", Font.PLAIN, 11));
            g2.drawString("* Tổng khoản: " + data.medicines().size(), padX, curY);

            curY += 18;
            g2.drawString("Lời dặn:", padX, curY);

            // --- KHỐI BÁC SĨ KÝ TÊN (GÓC DƯỚI BÊN PHẢI) ---
            int docX = x + PAGE_W - 250;
            int docY = curY + 45;

            g2.setFont(new Font("Times New Roman", Font.ITALIC, 11));
            docY += 18;
            g2.setFont(new Font("Times New Roman", Font.BOLD, 12));
            g2.drawString("BÁC SỸ KHÁM", docX + 30, docY);

            docY += 14;
            g2.setFont(new Font("Times New Roman", Font.ITALIC, 10));
            g2.drawString("(Ký, Họ tên)", docX + 42, docY);

            // VẼ CHỮ KÝ MỰC XANH VECTOR CHUẨN XÁC
            docY += 10;
            paintBlueSignature(g2, docX + 25, docY, 85, 35);

            docY += 45;
            g2.setFont(new Font("Times New Roman", Font.PLAIN, 11));
            g2.drawString(value(data.doctorName()), docX + 50, docY);

            // --- GHI CHÚ CHÂN TRANG ---
            int footY = y + PAGE_H - 60;
            g2.setFont(new Font("Times New Roman", Font.ITALIC, 10));
            g2.dispose();
        }

        private void paintBlueSignature(Graphics2D g2, int sx, int sy, int sw, int sh) {
            Graphics2D sg = (Graphics2D) g2.create();
            sg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            sg.setColor(new Color(25, 65, 160));
            sg.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Nét chữ ký lượn tự nhiên
            Path2D.Double path = new Path2D.Double();
            path.moveTo(sx + 5, sy + 18);
            path.curveTo(sx + 15, sy - 5, sx + 25, sy + 28, sx + 35, sy + 5);
            path.curveTo(sx + 40, sy - 2, sx + 45, sy + 25, sx + 52, sy + 12);
            path.lineTo(sx + 60, sy + 20);

            // Nét gạch chân chữ ký cong dài
            path.moveTo(sx, sy + 25);
            path.curveTo(sx + 20, sy + 24, sx + 50, sy + 20, sx + 75, sy + 18);

            sg.draw(path);
            sg.dispose();
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
