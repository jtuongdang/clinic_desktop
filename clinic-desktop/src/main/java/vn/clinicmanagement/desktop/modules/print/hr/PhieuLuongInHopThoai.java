package vn.clinicmanagement.desktop.modules.print.hr;


import vn.clinicmanagement.desktop.modules.hr.*;
import vn.clinicmanagement.application.hr.NhanSuReadPort.PayrollRow;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;

/**
 * Hộp thoại xem trước và in Phiếu Lương Nhân Viên theo chuẩn phiếu in của hệ thống (tờ giấy trắng trên nền xám + thanh công cụ in).
 */
public final class PhieuLuongInHopThoai extends JDialog {
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0");
    private static final Font SERIF_TITLE = new Font("Serif", Font.BOLD, 18);
    private static final Font SERIF_SUB = new Font("Serif", Font.BOLD, 12);
    private static final Font SERIF_BODY = new Font("Serif", Font.PLAIN, 12);
    private static final Font SERIF_BOLD = new Font("Serif", Font.BOLD, 12);

    private final JPanel paper;

    private PhieuLuongInHopThoai(Window owner, PayrollRow row, String periodName, int attDays, BigDecimal attHours) {
        this(owner,
                "Xem trước phiếu in — Phiếu Lương " + row.fullName(),
                "  Xem trước bản in — Phiếu Lương Cá Nhân (Khổ A4/A5)",
                "In Phiếu Lương",
                940, 740);
        initPaper(buildPaperSheet(row, periodName, attDays, attHours));
    }

    private PhieuLuongInHopThoai(Window owner, String periodName, List<PayrollRow> rows) {
        this(owner,
                "Xem trước phiếu in — Bảng Tổng Hợp Thanh Toán Lương " + periodName,
                "  Xem trước bản in — Bảng Tổng Hợp Thanh Toán Lương Toàn Kỳ (Khổ A4 Ngang)",
                "In Bảng Lương Toàn Kỳ",
                1080, 760);
        initPaper(buildPeriodSummarySheet(periodName, rows));
    }

    private PhieuLuongInHopThoai(Window owner, String title, String subtitle, JTable sourceTable) {
        this(owner,
                "Xem trước phiếu in — " + title,
                "  Xem trước bản in — " + title + " (Khổ A4 Ngang)",
                "In Bảng",
                1120, 760);
        initPaper(buildGenericTableSheet(title, subtitle, sourceTable));
    }

    private PhieuLuongInHopThoai(Window owner, String title, String hintText, String printBtnText, int w, int h) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.paper = new JPanel(new BorderLayout());
        setSize(w, h);
        setMinimumSize(new Dimension(780, 580));
        if (!GraphicsEnvironment.isHeadless()) {
            setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        } else {
            setLocationRelativeTo(owner);
        }
        setLayout(new BorderLayout());

        // Top print-preview toolbar (matching YTeVienPhiPhieuManHinh)
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(new Color(238, 241, 245));
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(195, 205, 218)));
        toolbar.setPreferredSize(new Dimension(100, 42));

        JLabel lblHint = new JLabel(hintText);
        lblHint.setFont(new Font("Tahoma", Font.BOLD, 12));
        lblHint.setForeground(new Color(40, 55, 75));
        toolbar.add(lblHint, BorderLayout.WEST);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        btnGroup.setOpaque(false);
        JButton btnPrint = new JButton(printBtnText);
        btnPrint.setFont(new Font("Tahoma", Font.BOLD, 12));
        btnPrint.setForeground(new Color(18, 92, 166));
        btnPrint.setFocusPainted(false);
        btnPrint.addActionListener(e -> printPaper());

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Tahoma", Font.PLAIN, 12));
        btnClose.setFocusPainted(false);
        btnClose.addActionListener(e -> dispose());

        btnGroup.add(btnPrint);
        btnGroup.add(btnClose);
        toolbar.add(btnGroup, BorderLayout.EAST);
        add(toolbar, BorderLayout.NORTH);
    }

    private void initPaper(JPanel sheet) {
        paper.removeAll();
        paper.setOpaque(false);
        paper.add(sheet, BorderLayout.CENTER);
        JPanel sheetHost = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 24));
        sheetHost.setBackground(new Color(105, 110, 115));
        sheetHost.add(paper);

        JScrollPane scroll = new JScrollPane(sheetHost);
        scroll.getViewport().setBackground(new Color(105, 110, 115));
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildPaperSheet(PayrollRow r, String periodName, int attDays, BigDecimal attHours) {
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
        sheet.setPreferredSize(new Dimension(790, 620));
        sheet.setBorder(new EmptyBorder(26, 34, 28, 38));

        // 1. Header: Facility + National Motto + Title
        JPanel top = new JPanel(new BorderLayout(0, 10));
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
        JLabel lAddr = new JLabel("Phòng Tài Chính - Kế Toán & Nhân Sự");
        lAddr.setFont(SERIF_BODY.deriveFont(Font.ITALIC, 11f));
        orgLeft.add(lDept);
        orgLeft.add(lHosp);
        orgLeft.add(lAddr);

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
        titleBox.setBorder(new EmptyBorder(6, 0, 4, 0));
        JLabel lTitle = new JLabel("PHIẾU THANH TOÁN LƯƠNG NHÂN VIÊN");
        lTitle.setFont(SERIF_TITLE);
        lTitle.setForeground(new Color(125, 20, 20));
        lTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lPeriod = new JLabel(periodName);
        lPeriod.setFont(SERIF_BOLD.deriveFont(13f));
        lPeriod.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleBox.add(lTitle);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(lPeriod);
        top.add(titleBox, BorderLayout.CENTER);

        // Employee Info Grid
        JPanel infoGrid = new JPanel(new GridLayout(3, 2, 18, 4));
        infoGrid.setOpaque(false);
        infoGrid.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(210, 215, 220), 1),
                new EmptyBorder(8, 12, 8, 12)));
        infoGrid.add(infoLine("Mã nhân viên: ", r.maNhanVien()));
        infoGrid.add(infoLine("Họ và tên: ", r.fullName()));
        infoGrid.add(infoLine("Chức vụ: ", r.positionName().isBlank() ? "Nhân viên" : r.positionName()));
        infoGrid.add(infoLine("Ngày công trong kỳ: ", attDays + " ngày (" + attHours + " giờ)"));
        infoGrid.add(infoLine("Hệ số lương: ", String.valueOf(r.factor())));
        infoGrid.add(infoLine("Ngày lập phiếu: ", LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        top.add(infoGrid, BorderLayout.SOUTH);
        sheet.add(top, BorderLayout.NORTH);

        // 2. Center Table of Earnings & Deductions
        BigDecimal basePart = r.baseSalary().multiply(r.factor());
        BigDecimal gross = basePart.add(r.allowance()).add(r.examCommission());

        String[][] rows = {
                {"1", "Lương theo hệ số (" + MONEY_FMT.format(r.baseSalary()) + " x " + r.factor() + ")", "Thu nhập chính", MONEY_FMT.format(basePart)},
                {"2", "Phụ cấp chức vụ / chuyên môn / ăn trưa", "Phụ cấp cố định", MONEY_FMT.format(r.allowance())},
                {"3", "Thù lao khám bệnh & hoa hồng chuyên môn", "Theo lượt thực hiện", MONEY_FMT.format(r.examCommission())},
                {"", "TỔNG THU NHẬP (A)", "", MONEY_FMT.format(gross)},
                {"4", "Khấu trừ đi muộn / về sớm / khác", "Khoản trừ trong kỳ", "-" + MONEY_FMT.format(r.deduction())},
                {"", "THỰC LĨNH CHUYỂN KHOẢN / TIỀN MẶT (B)", "", MONEY_FMT.format(r.netSalary()) + " đ"}
        };

        JPanel tablePanel = new JPanel(new GridBagLayout());
        tablePanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;

        String[] headers = {"STT", "Nội dung khoản thu nhập / khấu trừ", "Diễn giải", "Số tiền (VNĐ)"};
        double[] weights = {0.07, 0.50, 0.23, 0.20};
        for (int c = 0; c < headers.length; c++) {
            gbc.gridx = c;
            gbc.weightx = weights[c];
            JLabel h = new JLabel(headers[c], c == 0 ? SwingConstants.CENTER : c == 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            h.setFont(SERIF_BOLD);
            h.setOpaque(true);
            h.setBackground(new Color(238, 242, 247));
            h.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(160, 170, 180), 1),
                    new EmptyBorder(6, 8, 6, 8)));
            tablePanel.add(h, gbc);
        }

        for (int i = 0; i < rows.length; i++) {
            gbc.gridy = i + 1;
            boolean highlightTotal = i == 3 || i == 5;
            boolean finalNet = i == 5;
            for (int c = 0; c < 4; c++) {
                gbc.gridx = c;
                gbc.weightx = weights[c];
                JLabel cell = new JLabel(rows[i][c], c == 0 ? SwingConstants.CENTER : c == 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                cell.setFont(highlightTotal ? SERIF_BOLD.deriveFont(finalNet ? 13.5f : 12f) : SERIF_BODY);
                if (finalNet) {
                    cell.setForeground(new Color(175, 20, 20));
                }
                cell.setOpaque(highlightTotal);
                if (highlightTotal) {
                    cell.setBackground(finalNet ? new Color(255, 248, 235) : new Color(246, 248, 250));
                }
                cell.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(180, 185, 192), 1),
                        new EmptyBorder(6, 8, 6, 8)));
                tablePanel.add(cell, gbc);
            }
        }
        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setOpaque(false);
        centerWrap.add(tablePanel, BorderLayout.NORTH);
        sheet.add(centerWrap, BorderLayout.CENTER);

        // 3. Footer Signatures
        LocalDate now = LocalDate.now();
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        JLabel lDate = new JLabel("Ngày " + now.getDayOfMonth() + " tháng " + now.getMonthValue() + " năm " + now.getYear(), SwingConstants.RIGHT);
        lDate.setFont(SERIF_BODY.deriveFont(Font.ITALIC));
        footer.add(lDate, BorderLayout.NORTH);

        JPanel signs = new JPanel(new GridLayout(1, 3, 12, 0));
        signs.setOpaque(false);
        signs.add(signBox("NGƯỜI LẬP BIỂU", "(Ký, ghi rõ họ tên)"));
        signs.add(signBox("KẾ TOÁN TRƯỞNG", "(Ký, ghi rõ họ tên)"));
        signs.add(signBox("NGƯỜI NHẬN LƯƠNG", "(Ký, ghi rõ họ tên)"));
        footer.add(signs, BorderLayout.CENTER);
        sheet.add(footer, BorderLayout.SOUTH);

        return sheet;
    }

    private static JPanel infoLine(String label, String value) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(SERIF_BODY);
        JLabel v = new JLabel(value != null ? value : "");
        v.setFont(SERIF_BOLD);
        p.add(l);
        p.add(v);
        return p;
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

    private void printPaper() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("PhieuLuong_" + getTitle());
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
                JOptionPane.showMessageDialog(this, "Lỗi in phiếu lương: " + ex.getMessage(), "Lỗi in", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void showDialog(Window owner, PayrollRow row, String periodName, int attDays, BigDecimal attHours) {
        new PhieuLuongInHopThoai(owner, row, periodName, attDays, attHours).setVisible(true);
    }

    public static void showPeriodSummaryDialog(Window owner, String periodName, List<PayrollRow> rows) {
        new PhieuLuongInHopThoai(owner, periodName, rows).setVisible(true);
    }

    public static void showTablePrintDialog(Window owner, String title, String subtitle, JTable sourceTable) {
        new PhieuLuongInHopThoai(owner, title, subtitle, sourceTable).setVisible(true);
    }

    private JPanel buildGenericTableSheet(String title, String subtitle, JTable sourceTable) {
        int rowsCount = sourceTable.getRowCount();
        int colsCount = sourceTable.getColumnCount();
        int sheetHeight = Math.max(620, 290 + rowsCount * 24);
        int sheetWidth = Math.max(980, colsCount > 15 ? 1140 : 980);
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
        sheet.setPreferredSize(new Dimension(sheetWidth, sheetHeight));
        sheet.setBorder(new EmptyBorder(22, 24, 24, 28));

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
        JLabel lTitle = new JLabel(title.toUpperCase());
        lTitle.setFont(SERIF_TITLE);
        lTitle.setForeground(new Color(125, 20, 20));
        lTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lSub = new JLabel(subtitle);
        lSub.setFont(SERIF_BOLD.deriveFont(12.5f));
        lSub.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleBox.add(lTitle);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(lSub);
        top.add(titleBox, BorderLayout.CENTER);
        sheet.add(top, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;
        Font cellFont = colsCount > 20 ? SERIF_BODY.deriveFont(9.5f) : SERIF_BODY.deriveFont(11f);
        Font headFont = colsCount > 20 ? SERIF_BOLD.deriveFont(9.5f) : SERIF_BOLD.deriveFont(11f);
        for (int c = 0; c < colsCount; c++) {
            gbc.gridx = c;
            gbc.weightx = c == 2 ? 2.8 : c <= 1 ? 1.0 : 0.6;
            JLabel h = new JLabel(String.valueOf(sourceTable.getColumnModel().getColumn(c).getHeaderValue()),
                    c == 2 ? SwingConstants.LEFT : SwingConstants.CENTER);
            h.setFont(headFont);
            h.setOpaque(true);
            h.setBackground(new Color(238, 242, 247));
            h.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(160, 170, 180), 1),
                    new EmptyBorder(3, 2, 3, 2)));
            grid.add(h, gbc);
        }
        for (int r = 0; r < rowsCount; r++) {
            gbc.gridy = r + 1;
            for (int c = 0; c < colsCount; c++) {
                gbc.gridx = c;
                gbc.weightx = c == 2 ? 2.8 : c <= 1 ? 1.0 : 0.6;
                Object val = sourceTable.getValueAt(r, c);
                JLabel cell = new JLabel(val != null ? val.toString() : "",
                        c == 2 ? SwingConstants.LEFT : SwingConstants.CENTER);
                cell.setFont(cellFont);
                cell.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(190, 195, 202), 1),
                        new EmptyBorder(3, 2, 3, 2)));
                grid.add(cell, gbc);
            }
        }
        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setOpaque(false);
        centerWrap.add(grid, BorderLayout.NORTH);
        sheet.add(centerWrap, BorderLayout.CENTER);

        LocalDate now = LocalDate.now();
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        JLabel lDate = new JLabel("Ngày " + now.getDayOfMonth() + " tháng " + now.getMonthValue() + " năm " + now.getYear(), SwingConstants.RIGHT);
        lDate.setFont(SERIF_BODY.deriveFont(Font.ITALIC));
        footer.add(lDate, BorderLayout.NORTH);
        JPanel signs = new JPanel(new GridLayout(1, 3, 12, 0));
        signs.setOpaque(false);
        signs.add(signBox("NGƯỜI LẬP BIỂU", "(Ký, ghi rõ họ tên)"));
        signs.add(signBox("TRƯỞNG PHÒNG HÀNH CHÍNH", "(Ký, ghi rõ họ tên)"));
        signs.add(signBox("BAN GIÁM ĐỐC", "(Ký, đóng dấu)"));
        footer.add(signs, BorderLayout.CENTER);
        sheet.add(footer, BorderLayout.SOUTH);

        return sheet;
    }

    private JPanel buildPeriodSummarySheet(String periodName, List<PayrollRow> rows) {
        int rowHeight = Math.max(620, 320 + rows.size() * 28);
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
        sheet.setPreferredSize(new Dimension(960, rowHeight));
        sheet.setBorder(new EmptyBorder(24, 28, 26, 32));

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
        JLabel lAddr = new JLabel("Phòng Tài Chính - Kế Toán & Nhân Sự");
        lAddr.setFont(SERIF_BODY.deriveFont(Font.ITALIC, 11f));
        orgLeft.add(lDept);
        orgLeft.add(lHosp);
        orgLeft.add(lAddr);

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
        JLabel lTitle = new JLabel("BẢNG TỔNG HỢP THANH TOÁN LƯƠNG TOÀN BỘ KỲ LƯƠNG");
        lTitle.setFont(SERIF_TITLE);
        lTitle.setForeground(new Color(125, 20, 20));
        lTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lPeriod = new JLabel(periodName + "  •  Tổng số nhân sự: " + rows.size());
        lPeriod.setFont(SERIF_BOLD.deriveFont(13f));
        lPeriod.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleBox.add(lTitle);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(lPeriod);
        top.add(titleBox, BorderLayout.CENTER);
        sheet.add(top, BorderLayout.NORTH);

        JPanel tablePanel = new JPanel(new GridBagLayout());
        tablePanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;

        String[] headers = {"STT", "Mã NV", "Họ và tên", "Chức vụ", "Lương CB", "Hệ số", "Phụ cấp", "Thù lao", "Khấu trừ", "Thực lĩnh"};
        double[] weights = {0.04, 0.08, 0.17, 0.15, 0.11, 0.06, 0.09, 0.09, 0.09, 0.12};
        for (int c = 0; c < headers.length; c++) {
            gbc.gridx = c;
            gbc.weightx = weights[c];
            int align = c <= 1 || c == 5 ? SwingConstants.CENTER : c >= 4 ? SwingConstants.RIGHT : SwingConstants.LEFT;
            JLabel h = new JLabel(headers[c], align);
            h.setFont(SERIF_BOLD.deriveFont(11.5f));
            h.setOpaque(true);
            h.setBackground(new Color(238, 242, 247));
            h.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(160, 170, 180), 1),
                    new EmptyBorder(5, 5, 5, 5)));
            tablePanel.add(h, gbc);
        }

        BigDecimal sumBase = BigDecimal.ZERO;
        BigDecimal sumAllow = BigDecimal.ZERO;
        BigDecimal sumComm = BigDecimal.ZERO;
        BigDecimal sumDed = BigDecimal.ZERO;
        BigDecimal sumNet = BigDecimal.ZERO;

        for (int i = 0; i < rows.size(); i++) {
            PayrollRow r = rows.get(i);
            sumBase = sumBase.add(r.baseSalary());
            sumAllow = sumAllow.add(r.allowance());
            sumComm = sumComm.add(r.examCommission());
            sumDed = sumDed.add(r.deduction());
            sumNet = sumNet.add(r.netSalary());

            String[] vals = {
                    String.valueOf(i + 1), r.maNhanVien(), r.fullName(), r.positionName(),
                    MONEY_FMT.format(r.baseSalary()), String.valueOf(r.factor()),
                    MONEY_FMT.format(r.allowance()), MONEY_FMT.format(r.examCommission()),
                    MONEY_FMT.format(r.deduction()), MONEY_FMT.format(r.netSalary())
            };
            gbc.gridy = i + 1;
            for (int c = 0; c < vals.length; c++) {
                gbc.gridx = c;
                gbc.weightx = weights[c];
                int align = c <= 1 || c == 5 ? SwingConstants.CENTER : c >= 4 ? SwingConstants.RIGHT : SwingConstants.LEFT;
                JLabel cell = new JLabel(vals[c], align);
                cell.setFont(c == 9 ? SERIF_BOLD.deriveFont(11.5f) : SERIF_BODY.deriveFont(11.5f));
                cell.setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(new Color(185, 190, 198), 1),
                        new EmptyBorder(4, 5, 4, 5)));
                tablePanel.add(cell, gbc);
            }
        }

        gbc.gridy = rows.size() + 1;
        String[] totalVals = {
                "", "", "TỔNG CỘNG TOÀN KỲ", "",
                MONEY_FMT.format(sumBase), "",
                MONEY_FMT.format(sumAllow), MONEY_FMT.format(sumComm),
                MONEY_FMT.format(sumDed), MONEY_FMT.format(sumNet) + " đ"
        };
        for (int c = 0; c < totalVals.length; c++) {
            gbc.gridx = c;
            gbc.weightx = weights[c];
            int align = c <= 1 || c == 5 ? SwingConstants.CENTER : c >= 4 ? SwingConstants.RIGHT : SwingConstants.LEFT;
            JLabel cell = new JLabel(totalVals[c], align);
            cell.setFont(SERIF_BOLD.deriveFont(12f));
            cell.setOpaque(true);
            cell.setBackground(new Color(255, 248, 235));
            if (c == 9) cell.setForeground(new Color(175, 20, 20));
            cell.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(160, 170, 180), 1),
                    new EmptyBorder(6, 5, 6, 5)));
            tablePanel.add(cell, gbc);
        }

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setOpaque(false);
        centerWrap.add(tablePanel, BorderLayout.NORTH);
        sheet.add(centerWrap, BorderLayout.CENTER);

        LocalDate now = LocalDate.now();
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        JLabel lDate = new JLabel("Ngày " + now.getDayOfMonth() + " tháng " + now.getMonthValue() + " năm " + now.getYear(), SwingConstants.RIGHT);
        lDate.setFont(SERIF_BODY.deriveFont(Font.ITALIC));
        footer.add(lDate, BorderLayout.NORTH);

        JPanel signs = new JPanel(new GridLayout(1, 3, 12, 0));
        signs.setOpaque(false);
        signs.add(signBox("NGƯỜI LẬP BIỂU", "(Ký, ghi rõ họ tên)"));
        signs.add(signBox("KẾ TOÁN TRƯỞNG", "(Ký, ghi rõ họ tên)"));
        signs.add(signBox("GIÁM ĐỐC BỆNH VIỆN", "(Ký, đóng dấu)"));
        footer.add(signs, BorderLayout.CENTER);
        sheet.add(footer, BorderLayout.SOUTH);

        return sheet;
    }
}
