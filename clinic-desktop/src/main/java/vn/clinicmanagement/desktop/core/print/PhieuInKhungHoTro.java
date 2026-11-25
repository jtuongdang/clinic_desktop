package vn.clinicmanagement.desktop.core.print;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.time.LocalDate;

/**
 * Khung tiện ích dùng chung cho các cửa sổ xem trước và in phiếu (khổ giấy đổ bóng, thanh công cụ chuẩn, PrinterJob).
 */
public final class PhieuInKhungHoTro {
    public static final Color PREVIEW_BG = new Color(105, 110, 115);
    public static final Color TOOLBAR_BG = new Color(238, 241, 245);
    public static final Color TOOLBAR_BORDER = new Color(195, 205, 218);

    private static final Font SERIF_TITLE = new Font("Serif", Font.BOLD, 18);
    private static final Font SERIF_SUB = new Font("Serif", Font.BOLD, 12);
    private static final Font SERIF_BODY = new Font("Serif", Font.PLAIN, 12);
    private static final Font SERIF_BOLD = new Font("Serif", Font.BOLD, 12);

    private PhieuInKhungHoTro() {}

    public static void maximizeToScreen(Window window, Window owner) {
        if (!GraphicsEnvironment.isHeadless()) {
            window.setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        } else {
            window.setLocationRelativeTo(owner);
        }
    }

    public static JPanel createPreviewToolbar(String hintText, String printButtonText, Runnable onPrint, Runnable onClose) {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(TOOLBAR_BG);
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, TOOLBAR_BORDER));
        toolbar.setPreferredSize(new Dimension(100, 42));

        JLabel lblHint = new JLabel(hintText);
        lblHint.setFont(new Font("Tahoma", Font.BOLD, 12));
        lblHint.setForeground(new Color(40, 55, 75));
        toolbar.add(lblHint, BorderLayout.WEST);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        btnGroup.setOpaque(false);

        JButton btnPrint = new JButton(printButtonText);
        btnPrint.setFont(new Font("Tahoma", Font.BOLD, 12));
        btnPrint.setForeground(new Color(18, 92, 166));
        btnPrint.setFocusPainted(false);
        btnPrint.addActionListener(e -> { if (onPrint != null) onPrint.run(); });

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnClose.setFocusPainted(false);
        btnClose.addActionListener(e -> { if (onClose != null) onClose.run(); });

        btnGroup.add(btnPrint);
        btnGroup.add(btnClose);
        toolbar.add(btnGroup, BorderLayout.EAST);
        return toolbar;
    }

    public static JScrollPane wrapPaperViewport(JComponent paper) {
        JPanel sheetHost = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 24));
        sheetHost.setBackground(PREVIEW_BG);
        sheetHost.add(paper);

        JScrollPane scroll = new JScrollPane(sheetHost);
        scroll.getViewport().setBackground(PREVIEW_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(null);
        return scroll;
    }

    public static JPanel createShadowPaper(int width, int height, Insets padding) {
        JPanel sheet = new JPanel(new BorderLayout(0, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(30, 30, 30, 90));
                g2.fillRect(4, 4, getWidth() - 4, getHeight() - 4);
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth() - 4, getHeight() - 4);
                g2.setColor(new Color(195, 195, 195));
                g2.drawRect(0, 0, getWidth() - 5, getHeight() - 5);
                g2.dispose();
            }
        };
        sheet.setOpaque(false);
        sheet.setPreferredSize(new Dimension(width, height));
        sheet.setBorder(new EmptyBorder(padding));
        return sheet;
    }

    public static JPanel createHospitalHeader(String titleText, String subtitleText, String deptSubline) {
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);

        JPanel orgRow = new JPanel(new GridLayout(1, 2, 16, 0));
        orgRow.setOpaque(false);

        JPanel orgLeft = new JPanel();
        orgLeft.setLayout(new BoxLayout(orgLeft, BoxLayout.Y_AXIS));
        orgLeft.setOpaque(false);
        JLabel lDept = new JLabel("SỞ Y TẾ TỈNH BẮC NINH");
        lDept.setFont(SERIF_BODY);
        JLabel lHosp = new JLabel("BỆNH VIỆN SẢN NHI BẮC NINH");
        lHosp.setFont(SERIF_SUB);
        orgLeft.add(lDept);
        orgLeft.add(lHosp);
        if (deptSubline != null && !deptSubline.isBlank()) {
            JLabel lAddr = new JLabel(deptSubline);
            lAddr.setFont(SERIF_BODY.deriveFont(Font.ITALIC, 11f));
            orgLeft.add(lAddr);
        }

        JPanel orgRight = new JPanel();
        orgRight.setLayout(new BoxLayout(orgRight, BoxLayout.Y_AXIS));
        orgRight.setOpaque(false);
        JLabel lRep = new JLabel("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM");
        lRep.setFont(SERIF_SUB);
        lRep.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JLabel lMot = new JLabel("Độc lập - Tự do - Hạnh phúc");
        lMot.setFont(SERIF_BODY.deriveFont(Font.BOLD | Font.ITALIC, 12f));
        lMot.setAlignmentX(Component.RIGHT_ALIGNMENT);
        orgRight.add(lRep);
        orgRight.add(lMot);

        orgRow.add(orgLeft);
        orgRow.add(orgRight);
        top.add(orgRow, BorderLayout.NORTH);

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);
        titleBox.setBorder(new EmptyBorder(4, 0, 2, 0));
        JLabel lTitle = new JLabel(titleText);
        lTitle.setFont(SERIF_TITLE);
        lTitle.setForeground(new Color(125, 20, 20));
        lTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lSub = new JLabel(subtitleText);
        lSub.setFont(SERIF_BOLD.deriveFont(12.5f));
        lSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleBox.add(lTitle);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(lSub);
        top.add(titleBox, BorderLayout.CENTER);
        return top;
    }

    public static JPanel createSignatureFooter(String leftTitle, String leftSub, String midTitle, String midSub, String rightTitle, String rightSub) {
        LocalDate now = LocalDate.now();
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        JLabel lDate = new JLabel("Ngày " + now.getDayOfMonth() + " tháng " + now.getMonthValue() + " năm " + now.getYear(), SwingConstants.RIGHT);
        lDate.setFont(SERIF_BODY.deriveFont(Font.ITALIC));
        footer.add(lDate, BorderLayout.NORTH);

        JPanel signs = new JPanel(new GridLayout(1, 3, 12, 0));
        signs.setOpaque(false);
        signs.add(signBox(leftTitle, leftSub));
        signs.add(signBox(midTitle, midSub));
        signs.add(signBox(rightTitle, rightSub));
        footer.add(signs, BorderLayout.CENTER);
        return footer;
    }

    private static JPanel signBox(String title, String sub) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(SERIF_BOLD);
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel s = new JLabel(sub);
        s.setFont(SERIF_BODY.deriveFont(Font.ITALIC, 11f));
        s.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(t);
        p.add(s);
        p.add(Box.createVerticalStrut(48));
        return p;
    }

    public static void printComponent(Component parent, JComponent paper, String jobName) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(jobName);
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            double scaleX = pageFormat.getImageableWidth() / paper.getWidth();
            double scaleY = pageFormat.getImageableHeight() / paper.getHeight();
            double scale = Math.min(1.0, Math.min(scaleX, scaleY));
            g2.scale(scale, scale);
            paper.printAll(g2);
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(parent, "Lỗi khi in: " + ex.getMessage(), "Lỗi in", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
