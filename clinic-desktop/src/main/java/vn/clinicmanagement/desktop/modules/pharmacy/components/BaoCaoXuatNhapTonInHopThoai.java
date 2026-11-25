package vn.clinicmanagement.desktop.modules.pharmacy.components;

import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.XuatNhapTonPrintData;
import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.XuatNhapTonPrintRow;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Màn hình xem trước bản in toàn màn hình cho "Báo Cáo Xuất Nhập Tồn"
 * ([50004] - TH_005_BaoCaoNhapXuatTonThuoc.xls).
 */
public final class BaoCaoXuatNhapTonInHopThoai extends JDialog {

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final Color LINK_BLUE = new Color(0, 78, 178);

    private static final String[] DATA_COLUMNS = {
            "Nhóm", "Mã thuốc", "Tên thuốc", "Hàm lượng", "Hoạt chất", "Đơn vị tính", "Hạn sử dụng",
            "Giá nhập", "Giá xuất", "Tồn đầu SL", "Tồn đầu TT", "Nhập SL", "Nhập TT",
            "Xuất SL", "Xuất TT", "Tồn SL", "Tồn TT"
    };

    private final XuatNhapTonPrintData printData;
    private final LocalDateTime from;
    private final LocalDateTime to;
    private final DefaultTableModel dataModel;
    private final JTable dataTable;
    private final JPanel printSheetPanel;
    private final JLabel zoomLabel = new JLabel("100%");
    private final JComboBox<String> orientationBox = new JComboBox<>(new String[]{"Chiều ngang", "Chiều dọc"});

    public BaoCaoXuatNhapTonInHopThoai(Window owner, XuatNhapTonPrintData printData, LocalDateTime from, LocalDateTime to) {
        super(owner, buildTitle(), ModalityType.APPLICATION_MODAL);
        this.printData = Objects.requireNonNull(printData, "printData");
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        this.dataModel = buildDataModel(printData.rows());
        this.dataTable = new JTable(dataModel);
        this.dataTable.setFont(UngDungPhongChu.BODY);
        this.dataTable.setRowHeight(22);
        this.dataTable.setAutoCreateRowSorter(true);
        this.printSheetPanel = buildPrintSheet();

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> printSheet(),
                KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        add(buildSidebar(), BorderLayout.WEST);
        add(buildReportArea(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private static String buildTitle() {
        String user = NguoiDungPhien.getInstance().getFullName();
        if (user == null || user.isBlank()) user = "EHC";
        return "EHC - Báo cáo xuất nhập tồn - [50004] - TH_005_BaoCaoNhapXuatTonThuoc.xls   --- Người đăng nhập : EHC "
                + user + " ---   --- Version : 2023.2.15.1 ---";
    }

    private static DefaultTableModel buildDataModel(List<XuatNhapTonPrintRow> rows) {
        DefaultTableModel tm = new DefaultTableModel(DATA_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (XuatNhapTonPrintRow r : rows) {
            tm.addRow(new Object[]{
                    r.groupName(), r.itemCode(), r.itemName(), r.strength(), r.activeIngredient(),
                    r.unit(), r.expiryDate(), fmt(r.importPrice()), fmt(r.exportPrice()),
                    fmt(r.openingQty()), fmt(r.openingAmount()), fmt(r.importQty()), fmt(r.importAmount()),
                    fmt(r.exportQty()), fmt(r.exportAmount()), fmt(r.closingQty()), fmt(r.closingAmount())
            });
        }
        return tm;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(212, 0));
        side.setBackground(new Color(240, 240, 240));
        side.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 0, 1, new Color(180, 180, 180)),
                new EmptyBorder(8, 8, 8, 8)));

        JPanel top = new JPanel(new BorderLayout(6, 0));
        top.setOpaque(false);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JButton btnPrint = new JButton("<html><center>In<br>(Ctrl+P)</center></html>");
        btnPrint.setPreferredSize(new Dimension(88, 66));
        btnPrint.setFont(new Font("Arial", Font.PLAIN, 15));
        btnPrint.setBackground(new Color(248, 250, 252));
        btnPrint.setBorder(new LineBorder(new Color(145, 165, 195)));
        btnPrint.setFocusPainted(false);
        btnPrint.addActionListener(e -> printSheet());
        top.add(btnPrint, BorderLayout.WEST);

        JPanel copies = new JPanel();
        copies.setLayout(new BoxLayout(copies, BoxLayout.Y_AXIS));
        copies.setOpaque(false);
        JPanel spinRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        spinRow.setOpaque(false);
        spinRow.add(new JLabel("Bản in"));
        JSpinner spCopies = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        spCopies.setPreferredSize(new Dimension(44, 22));
        spinRow.add(spCopies);
        copies.add(spinRow);
        JCheckBox chkAuto = new JCheckBox("In tự động");
        chkAuto.setOpaque(false);
        JCheckBox chkCont = new JCheckBox("In tiếp");
        chkCont.setOpaque(false);
        copies.add(chkAuto);
        copies.add(chkCont);
        top.add(copies, BorderLayout.CENTER);
        side.add(top);
        side.add(Box.createVerticalStrut(10));

        side.add(sectionLabel("Máy In"));
        side.add(fullCombo(new String[]{"Snagit 13", "Microsoft Print to PDF", "Default Printer"}));
        side.add(Box.createVerticalStrut(4));
        side.add(fullCombo(new String[]{"Automatically Select", "Tray 1", "Manual Feed"}));
        JLabel cfgLink = linkLabel("Cấu hình máy in");
        cfgLink.setAlignmentX(RIGHT_ALIGNMENT);
        cfgLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { printSheet(); }
        });
        side.add(cfgLink);
        side.add(Box.createVerticalStrut(10));

        side.add(sectionLabel("Tùy Chọn"));
        side.add(fullCombo(new String[]{"In tất cả các trang", "Trang hiện tại", "Trang chọn"}));
        side.add(Box.createVerticalStrut(4));
        JPanel pageRow = new JPanel(new BorderLayout(6, 0));
        pageRow.setOpaque(false);
        pageRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        pageRow.add(new JLabel("Trang in :"), BorderLayout.WEST);
        pageRow.add(new JTextField(), BorderLayout.CENTER);
        side.add(pageRow);
        JCheckBox chkDuplex = new JCheckBox("In 2 mặt");
        chkDuplex.setOpaque(false);
        side.add(chkDuplex);
        side.add(Box.createVerticalStrut(8));

        side.add(labelOnly("Chiều giấy"));
        orientationBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        side.add(orientationBox);
        side.add(Box.createVerticalStrut(6));
        side.add(labelOnly("Khổ giấy"));
        side.add(fullCombo(new String[]{"A4 (210mm x 297mm)", "A5 (148mm x 210mm)", "Letter"}));
        side.add(Box.createVerticalStrut(12));

        side.add(sectionLabel("Xuất File..."));
        JPanel exportRow = new JPanel(new GridLayout(1, 2, 8, 0));
        exportRow.setOpaque(false);
        exportRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        JButton btnPdf = exportTileButton("Pdf", new Color(46, 125, 50));
        btnPdf.addActionListener(e -> printSheet());
        JButton btnExcel = exportTileButton("Excel", new Color(33, 115, 70));
        btnExcel.addActionListener(e -> exportCsv("TH_005_BaoCaoNhapXuatTonThuoc.csv"));
        exportRow.add(btnPdf);
        exportRow.add(btnExcel);
        side.add(exportRow);
        side.add(Box.createVerticalGlue());
        return side;
    }

    private JComponent buildReportArea() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UngDungPhongChu.BODY);

        JPanel printBg = new JPanel(new GridBagLayout());
        printBg.setBackground(new Color(128, 128, 128));
        printBg.setBorder(new EmptyBorder(14, 14, 14, 14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weighty = 1.0;
        printBg.add(printSheetPanel, gbc);

        JScrollPane printScroll = new JScrollPane(printBg);
        printScroll.getVerticalScrollBar().setUnitIncrement(18);
        tabs.addTab("Bản In", printScroll);
        tabs.addTab("Dữ Liệu", new JScrollPane(dataTable));
        return tabs;
    }

    private JPanel buildPrintSheet() {
        JPanel sheet = new JPanel(new BorderLayout(0, 8));
        sheet.setBackground(Color.WHITE);
        sheet.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(70, 70, 70)),
                new EmptyBorder(26, 26, 28, 26)));
        sheet.setPreferredSize(new Dimension(1060, 720));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel title = new JLabel("THỐNG KÊ NHẬP XUẤT TỒN THUỐC - " + printData.warehouseName(), SwingConstants.CENTER);
        title.setFont(new Font("Times New Roman", Font.BOLD, 16));
        title.setAlignmentX(CENTER_ALIGNMENT);
        JLabel sub = new JLabel("Từ " + from.format(DT_FMT) + " đến " + to.minusMinutes(1).format(DT_FMT), SwingConstants.CENTER);
        sub.setFont(new Font("Times New Roman", Font.BOLD, 12));
        sub.setAlignmentX(CENTER_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(3));
        header.add(sub);
        header.add(Box.createVerticalStrut(10));
        sheet.add(header, BorderLayout.NORTH);

        // Grouped HTML-like custom table using GridBagLayout for exact 2-row header matching reference
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weightx = 0.0;

        Font hdrFont = new Font("Times New Roman", Font.BOLD, 11);
        Font cellFont = new Font("Times New Roman", Font.PLAIN, 11);
        Font boldCellFont = new Font("Times New Roman", Font.BOLD, 11);
        Color groupBg = new Color(238, 241, 247);
        Color totalBg = new Color(235, 243, 223);
        Color line = new Color(110, 110, 110);

        String[] singleHeaders = {"STT", "Tên thuốc", "Hàm lượng", "Hoạt chất", "Đơn vị tính", "Hạn sử dụng"};
        for (int c = 0; c < singleHeaders.length; c++) {
            g.gridx = c; g.gridy = 0; g.gridwidth = 1; g.gridheight = 2;
            g.weightx = (c == 1) ? 1.0 : 0.0;
            body.add(headerCell(singleHeaders[c], hdrFont, line), g);
        }
        String[] groupHeaders = {"Giá", "Tồn đầu kỳ", "Nhập", "Xuất", "Tồn"};
        String[][] subHeaders = {
                {"Giá nhập", "Giá xuất"},
                {"Số lượng", "Thành tiền"},
                {"Số lượng", "Thành tiền"},
                {"Số lượng", "Thành tiền"},
                {"Số lượng", "Thành tiền"}
        };
        g.weightx = 0.0;
        for (int i = 0; i < groupHeaders.length; i++) {
            int col = 6 + i * 2;
            g.gridx = col; g.gridy = 0; g.gridwidth = 2; g.gridheight = 1;
            body.add(headerCell(groupHeaders[i], hdrFont, line), g);
            g.gridy = 1; g.gridwidth = 1;
            g.gridx = col;
            body.add(headerCell(subHeaders[i][0], hdrFont, line), g);
            g.gridx = col + 1;
            body.add(headerCell(subHeaders[i][1], hdrFont, line), g);
        }

        Map<String, List<XuatNhapTonPrintRow>> byGroup = new LinkedHashMap<>();
        for (XuatNhapTonPrintRow r : printData.rows()) {
            byGroup.computeIfAbsent(r.groupName(), k -> new ArrayList<>()).add(r);
        }

        int rowIdx = 2;
        BigDecimal grandOpenAmt = BigDecimal.ZERO;
        BigDecimal grandImpAmt = BigDecimal.ZERO;
        BigDecimal grandExpAmt = BigDecimal.ZERO;
        BigDecimal grandCloseAmt = BigDecimal.ZERO;

        for (var entry : byGroup.entrySet()) {
            List<XuatNhapTonPrintRow> list = entry.getValue();
            BigDecimal gOpen = list.stream().map(XuatNhapTonPrintRow::openingAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal gImp = list.stream().map(XuatNhapTonPrintRow::importAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal gExp = list.stream().map(XuatNhapTonPrintRow::exportAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal gClose = list.stream().map(XuatNhapTonPrintRow::closingAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            grandOpenAmt = grandOpenAmt.add(gOpen);
            grandImpAmt = grandImpAmt.add(gImp);
            grandExpAmt = grandExpAmt.add(gExp);
            grandCloseAmt = grandCloseAmt.add(gClose);

            // Group summary row
            addSummaryRow(body, g, rowIdx++, entry.getKey(), gOpen, gImp, gExp, gClose, groupBg, boldCellFont, line);

            int stt = 1;
            for (XuatNhapTonPrintRow r : list) {
                addDataRow(body, g, rowIdx++, stt++, r, cellFont, line);
            }
        }

        addSummaryRow(body, g, rowIdx++, "Tổng cộng:", grandOpenAmt, grandImpAmt, grandExpAmt, grandCloseAmt, totalBg, boldCellFont, line);

        JPanel centerWrap = new JPanel(new BorderLayout(0, 10));
        centerWrap.setOpaque(false);
        centerWrap.add(body, BorderLayout.NORTH);

        BigDecimal totalProfit = grandExpAmt.add(grandCloseAmt).subtract(grandImpAmt).subtract(grandOpenAmt);
        JPanel profitRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 4));
        profitRow.setOpaque(false);
        JLabel lblProfitTitle = new JLabel("Tổng lãi:");
        lblProfitTitle.setFont(boldCellFont);
        JLabel lblProfitVal = new JLabel(fmt(totalProfit));
        lblProfitVal.setFont(boldCellFont);
        profitRow.add(lblProfitTitle);
        profitRow.add(lblProfitVal);
        centerWrap.add(profitRow, BorderLayout.CENTER);

        sheet.add(centerWrap, BorderLayout.CENTER);
        return sheet;
    }

    private static void addSummaryRow(
            JPanel body, GridBagConstraints g, int rowIdx, String label,
            BigDecimal openAmt, BigDecimal impAmt, BigDecimal expAmt, BigDecimal closeAmt,
            Color bg, Font font, Color line) {
        g.gridy = rowIdx;
        g.gridheight = 1;
        g.gridx = 0; g.gridwidth = 6; g.weightx = 1.0;
        body.add(dataCell(label, SwingConstants.LEFT, font, bg, line), g);
        g.gridwidth = 1; g.weightx = 0.0;
        for (int c = 6; c <= 15; c++) {
            String text = switch (c) {
                case 9 -> dashIfZero(openAmt);
                case 11 -> dashIfZero(impAmt);
                case 13 -> dashIfZero(expAmt);
                case 15 -> dashIfZero(closeAmt);
                default -> "";
            };
            g.gridx = c;
            body.add(dataCell(text, SwingConstants.RIGHT, font, bg, line), g);
        }
    }

    private static void addDataRow(
            JPanel body, GridBagConstraints g, int rowIdx, int stt,
            XuatNhapTonPrintRow r, Font font, Color line) {
        g.gridy = rowIdx;
        g.gridheight = 1;
        g.gridwidth = 1;
        Object[] vals = {
                String.valueOf(stt), r.itemName(), r.strength(), r.activeIngredient(), r.unit(), r.expiryDate(),
                dashIfZero(r.importPrice()), dashIfZero(r.exportPrice()),
                fmt(r.openingQty()), dashIfZero(r.openingAmount()),
                dashIfZero(r.importQty()), dashIfZero(r.importAmount()),
                dashIfZero(r.exportQty()), dashIfZero(r.exportAmount()),
                fmt(r.closingQty()), dashIfZero(r.closingAmount())
        };
        for (int c = 0; c < vals.length; c++) {
            g.gridx = c;
            g.weightx = (c == 1) ? 1.0 : 0.0;
            int align = (c == 0 || c == 4 || c == 5) ? SwingConstants.CENTER
                    : (c >= 6 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            body.add(dataCell(Objects.toString(vals[c], ""), align, font, Color.WHITE, line), g);
        }
    }

    private static JLabel headerCell(String text, Font font, Color line) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(font);
        l.setOpaque(true);
        l.setBackground(new Color(244, 246, 249));
        l.setBorder(BorderFactory.createCompoundBorder(new LineBorder(line), new EmptyBorder(3, 5, 3, 5)));
        return l;
    }

    private static JLabel dataCell(String text, int align, Font font, Color bg, Color line) {
        JLabel l = new JLabel(text, align);
        l.setFont(font);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setBorder(BorderFactory.createCompoundBorder(new LineBorder(line), new EmptyBorder(2, 5, 2, 5)));
        return l;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(240, 240, 240));
        bar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, new Color(180, 180, 180)),
                new EmptyBorder(3, 10, 3, 12)));

        JPanel pager = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pager.setOpaque(false);
        pager.add(new JLabel("◀"));
        JSpinner spPage = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
        spPage.setPreferredSize(new Dimension(42, 22));
        pager.add(spPage);
        pager.add(new JLabel("/1"));
        pager.add(new JLabel("▶"));
        bar.add(pager, BorderLayout.CENTER);

        JPanel zoom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        zoom.setOpaque(false);
        zoom.add(zoomLabel);
        JSlider slider = new JSlider(50, 150, 100);
        slider.setPreferredSize(new Dimension(110, 18));
        slider.setOpaque(false);
        slider.addChangeListener(e -> zoomLabel.setText(slider.getValue() + "%"));
        zoom.add(slider);
        bar.add(zoom, BorderLayout.EAST);
        return bar;
    }

    private void printSheet() {
        if (GraphicsEnvironment.isHeadless()) return;
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pf = job.defaultPage();
        pf.setOrientation("Chiều ngang".equals(orientationBox.getSelectedItem()) ? PageFormat.LANDSCAPE : PageFormat.PORTRAIT);
        Paper paper = pf.getPaper();
        paper.setImageableArea(20, 20, paper.getWidth() - 40, paper.getHeight() - 40);
        pf.setPaper(paper);
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            double scale = Math.min(
                    pageFormat.getImageableWidth() / printSheetPanel.getWidth(),
                    pageFormat.getImageableHeight() / printSheetPanel.getHeight());
            g2.scale(scale, scale);
            printSheetPanel.printAll(g2);
            return Printable.PAGE_EXISTS;
        }, pf);
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Không thể in báo cáo: " + ex.getMessage(), "Lỗi In", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportCsv(String defaultName) {
        if (GraphicsEnvironment.isHeadless()) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (var w = new OutputStreamWriter(new FileOutputStream(chooser.getSelectedFile()), StandardCharsets.UTF_8)) {
            w.write('\ufeff');
            for (int c = 0; c < dataModel.getColumnCount(); c++) {
                if (c > 0) w.write(',');
                w.write(csvCell(dataModel.getColumnName(c)));
            }
            w.write('\n');
            for (int r = 0; r < dataModel.getRowCount(); r++) {
                for (int c = 0; c < dataModel.getColumnCount(); c++) {
                    if (c > 0) w.write(',');
                    w.write(csvCell(Objects.toString(dataModel.getValueAt(r, c), "")));
                }
                w.write('\n');
            }
            JOptionPane.showMessageDialog(this, "Đã xuất file thành công.", "Xuất File", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không thể xuất file: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String csvCell(String s) {
        if (s.startsWith("=") || s.startsWith("+") || s.startsWith("-") || s.startsWith("@")) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static String fmt(BigDecimal val) {
        return MONEY_FMT.format(val == null ? BigDecimal.ZERO : val);
    }

    private static String dashIfZero(BigDecimal val) {
        return val == null || val.compareTo(BigDecimal.ZERO) == 0 ? "-" : MONEY_FMT.format(val);
    }

    private static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UngDungPhongChu.BODY_BOLD);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel labelOnly(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UngDungPhongChu.BODY);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JComboBox<String> fullCombo(String[] items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        return c;
    }

    private static JLabel linkLabel(String text) {
        JLabel l = new JLabel("<html><u>" + text + "</u></html>");
        l.setForeground(LINK_BLUE);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return l;
    }

    private static JButton exportTileButton(String label, Color accent) {
        JButton b = new JButton("<html><center><b style='color:rgb(" + accent.getRed() + "," + accent.getGreen() + "," + accent.getBlue()
                + ")'>" + label.toUpperCase() + "</b><br>" + label + "</center></html>");
        b.setBackground(Color.WHITE);
        b.setBorder(new LineBorder(new Color(145, 165, 195)));
        b.setFocusPainted(false);
        return b;
    }
}
