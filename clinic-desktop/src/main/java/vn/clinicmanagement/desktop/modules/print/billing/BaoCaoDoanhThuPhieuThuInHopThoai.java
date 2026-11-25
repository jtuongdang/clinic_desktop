package vn.clinicmanagement.desktop.modules.print.billing;


import vn.clinicmanagement.desktop.modules.billing.*;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
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
 * Màn hình mẫu phiếu in báo cáo doanh thu theo phiếu thu (mở toàn màn hình khi bấm OK ở Tùy Chọn Báo Cáo).
 * Đồng bộ giao diện EHC: [8018] - BC_008_BaoCaoDoanhThu_PhieuThu.xls.
 */
public final class BaoCaoDoanhThuPhieuThuInHopThoai extends JDialog {
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0");
    private static final DateTimeFormatter OUT_DT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter OUT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public record ReceiptPrintRow(
            String cashierName,
            String receiptCode,
            LocalDateTime receiptTime,
            LocalDateTime visitDate,
            String patientName,
            String birthMale,
            String birthFemale,
            String address,
            String receiptType,
            String feeCode,
            String paymentMethod,
            String note,
            BigDecimal cancelAmount,
            String cancelReason,
            BigDecimal collectAmount,
            BigDecimal refundAmount,
            BigDecimal advanceAmount,
            BigDecimal advanceRefundAmount,
            BigDecimal discountAmount,
            String discountReason
    ) {}

    private static final String[] COLUMNS = {
            "STT", "Số phiếu thu", "Ngày phiếu thu", "Ngày đến khám", "Tên bệnh nhân",
            "Năm sinh (Nam)", "Năm sinh (Nữ)", "Địa chỉ", "Loại phiếu", "Mã viện phí",
            "Hình thức phiếu thu", "Ghi chú",
            "Hủy phiếu - Số tiền", "Hủy phiếu - Lý do hủy",
            "Thu tiền", "Hoàn tiền", "Tạm ứng", "Hoàn ứng", "Miễn giảm", "Lý do miễn giảm"
    };
    private static final int[] COL_WIDTHS = {
            42, 115, 110, 90, 165,
            60, 60, 200, 75, 75,
            105, 220,
            75, 110,
            90, 80, 80, 80, 80, 120
    };

    private final List<ReceiptPrintRow> rows;
    private final String groupMode;
    private final boolean groupOnly;
    private final LocalDateTime from;
    private final LocalDateTime to;
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel zoomLabel = new JLabel("100%");

    public BaoCaoDoanhThuPhieuThuInHopThoai(Window owner,
                                            List<ReceiptPrintRow> rows,
                                            String groupMode,
                                            boolean groupOnly,
                                            LocalDateTime from,
                                            LocalDateTime to) {
        super(owner, buildTitle(), ModalityType.APPLICATION_MODAL);
        this.rows = rows == null ? List.of() : List.copyOf(rows);
        this.groupMode = groupMode == null ? "NONE" : groupMode;
        this.groupOnly = groupOnly;
        this.from = from;
        this.to = to;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(
                e -> printTable(),
                KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        this.model = buildModel();
        this.table = buildTable();

        add(buildLeftSidebar(), BorderLayout.WEST);
        add(buildRightReportArea(), BorderLayout.CENTER);
        add(buildBottomStatusBar(), BorderLayout.SOUTH);
    }

    private DefaultTableModel buildModel() {
        DefaultTableModel tm = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        if (!"NONE".equals(groupMode)) {
            Map<String, List<ReceiptPrintRow>> groups = new LinkedHashMap<>();
            for (ReceiptPrintRow r : rows) {
                String k = "DATE".equals(groupMode)
                        ? (r.receiptTime() == null ? "Không rõ ngày" : r.receiptTime().toLocalDate().format(OUT_DATE))
                        : (r.cashierName() == null || r.cashierName().isBlank()) ? "Thu ngân" : r.cashierName();
                groups.computeIfAbsent(k, x -> new ArrayList<>()).add(r);
            }
            for (var entry : groups.entrySet()) {
                BigDecimal grpCollect = BigDecimal.ZERO;
                BigDecimal grpCancel = BigDecimal.ZERO;
                BigDecimal grpRefund = BigDecimal.ZERO;
                BigDecimal grpAdv = BigDecimal.ZERO;
                BigDecimal grpAdvRef = BigDecimal.ZERO;
                BigDecimal grpDisc = BigDecimal.ZERO;
                for (ReceiptPrintRow r : entry.getValue()) {
                    grpCollect = grpCollect.add(nz(r.collectAmount()));
                    grpCancel = grpCancel.add(nz(r.cancelAmount()));
                    grpRefund = grpRefund.add(nz(r.refundAmount()));
                    grpAdv = grpAdv.add(nz(r.advanceAmount()));
                    grpAdvRef = grpAdvRef.add(nz(r.advanceRefundAmount()));
                    grpDisc = grpDisc.add(nz(r.discountAmount()));
                }
                tm.addRow(new Object[]{
                        entry.getKey(), "", "", "", "", "", "", "", "", "", "", "",
                        fmtDash(grpCancel), "",
                        fmtDash(grpCollect), fmtDash(grpRefund), fmtDash(grpAdv), fmtDash(grpAdvRef), fmtDash(grpDisc), ""
                });
                if (groupOnly) continue;
                int stt = 1;
                for (ReceiptPrintRow r : entry.getValue()) {
                    tm.addRow(toRowArray(stt++, r));
                }
            }
        } else {
            int stt = 1;
            for (ReceiptPrintRow r : rows) {
                tm.addRow(toRowArray(stt++, r));
            }
        }
        return tm;
    }

    private static Object[] toRowArray(int stt, ReceiptPrintRow r) {
        return new Object[]{
                stt,
                Objects.toString(r.receiptCode(), ""),
                r.receiptTime() == null ? "" : r.receiptTime().format(OUT_DT),
                r.visitDate() == null ? "" : r.visitDate().format(OUT_DATE),
                Objects.toString(r.patientName(), ""),
                Objects.toString(r.birthMale(), ""),
                Objects.toString(r.birthFemale(), ""),
                Objects.toString(r.address(), ""),
                Objects.toString(r.receiptType(), "Thu tiền"),
                Objects.toString(r.feeCode(), ""),
                Objects.toString(r.paymentMethod(), "Tiền mặt"),
                Objects.toString(r.note(), ""),
                fmtDash(r.cancelAmount()),
                Objects.toString(r.cancelReason(), ""),
                fmtDash(r.collectAmount()),
                fmtDash(r.refundAmount()),
                fmtDash(r.advanceAmount()),
                fmtDash(r.advanceRefundAmount()),
                fmtDash(r.discountAmount()),
                Objects.toString(r.discountReason(), "")
        };
    }

    private JTable buildTable() {
        JTable t = new JTable(model);
        t.setFont(UngDungPhongChu.BODY);
        t.setRowHeight(24);
        t.setShowGrid(true);
        t.setGridColor(new Color(170, 175, 182));
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        t.getTableHeader().setFont(UngDungPhongChu.BODY_BOLD);
        t.getTableHeader().setReorderingAllowed(false);

        for (int i = 0; i < COL_WIDTHS.length; i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(COL_WIDTHS[i]);
        }

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel,
                                                                     boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                setBorder(new EmptyBorder(0, 5, 0, 5));
                Object first = tbl.getValueAt(row, 0);
                boolean isGroupRow = first instanceof String s && !(s.matches("\\d+"));
                if (isGroupRow) {
                    setFont(UngDungPhongChu.BODY_BOLD);
                    setBackground(sel ? new Color(0, 120, 215) : new Color(242, 245, 249));
                } else {
                    setFont(UngDungPhongChu.BODY);
                    setBackground(sel ? new Color(0, 120, 215) : Color.WHITE);
                }
                setForeground(sel ? Color.WHITE : Color.BLACK);

                if (col == 0 && !isGroupRow) setHorizontalAlignment(SwingConstants.CENTER);
                else if (col == 2 || col == 3 || col == 5 || col == 6 || col == 9) setHorizontalAlignment(SwingConstants.CENTER);
                else if (col == 12 || col >= 14 && col <= 18) setHorizontalAlignment(SwingConstants.RIGHT);
                else setHorizontalAlignment(SwingConstants.LEFT);
                return this;
            }
        });
        return t;
    }

    private JPanel buildLeftSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(190, 100));
        side.setBackground(new Color(238, 242, 247));
        side.setBorder(new MatteBorder(0, 0, 0, 1, new Color(185, 190, 198)));
        side.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 0, 1, new Color(185, 190, 198)),
                new EmptyBorder(8, 8, 8, 8)));

        // Top Print button + copies
        JPanel printRow = new JPanel(new BorderLayout(6, 0));
        printRow.setOpaque(false);
        printRow.setMaximumSize(new Dimension(174, 72));
        printRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton printBtn = new JButton("<html><center>In<br>(Ctrl+P)</center></html>", printerIcon(24));
        printBtn.setFont(UngDungPhongChu.BODY);
        printBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        printBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        printBtn.setPreferredSize(new Dimension(78, 66));
        printBtn.setFocusPainted(false);
        printBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        printBtn.addActionListener(e -> printTable());
        printRow.add(printBtn, BorderLayout.WEST);

        JPanel copyPanel = new JPanel();
        copyPanel.setLayout(new BoxLayout(copyPanel, BoxLayout.Y_AXIS));
        copyPanel.setOpaque(false);
        JPanel spinRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        spinRow.setOpaque(false);
        JLabel copyLbl = new JLabel("Bản in");
        copyLbl.setFont(UngDungPhongChu.SMALL);
        JSpinner copies = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        copies.setPreferredSize(new Dimension(42, 22));
        spinRow.add(copyLbl);
        spinRow.add(copies);
        JCheckBox chkAuto = new JCheckBox("In tự động");
        chkAuto.setOpaque(false);
        chkAuto.setFont(UngDungPhongChu.SMALL);
        JCheckBox chkNext = new JCheckBox("In tiếp");
        chkNext.setOpaque(false);
        chkNext.setFont(UngDungPhongChu.SMALL);
        copyPanel.add(spinRow);
        copyPanel.add(chkAuto);
        copyPanel.add(chkNext);
        printRow.add(copyPanel, BorderLayout.CENTER);
        side.add(printRow);
        side.add(Box.createVerticalStrut(8));

        // Máy In
        side.add(sectionTitle("Máy In"));
        side.add(combo(new String[]{"Microsoft Print to PDF", "Default Printer"}));
        side.add(Box.createVerticalStrut(4));
        side.add(combo(new String[]{"Automatically Select", "Khay 1"}));
        JLabel cfgLink = new JLabel("<html><u>Cấu hình máy in</u></html>");
        cfgLink.setFont(UngDungPhongChu.SMALL);
        cfgLink.setForeground(new Color(0, 78, 178));
        cfgLink.setAlignmentX(Component.LEFT_ALIGNMENT);
        cfgLink.setBorder(new EmptyBorder(4, 38, 8, 0));
        side.add(cfgLink);

        // Tùy Chọn
        side.add(sectionTitle("Tùy Chọn"));
        side.add(combo(new String[]{"In tất cả các trang", "Trang hiện tại"}));
        side.add(Box.createVerticalStrut(4));
        JPanel pageRow = new JPanel(new BorderLayout(4, 0));
        pageRow.setOpaque(false);
        pageRow.setMaximumSize(new Dimension(174, 24));
        pageRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel trLbl = new JLabel("Trang in :");
        trLbl.setFont(UngDungPhongChu.SMALL);
        pageRow.add(trLbl, BorderLayout.WEST);
        pageRow.add(new JTextField(), BorderLayout.CENTER);
        side.add(pageRow);
        JCheckBox chkDuplex = new JCheckBox("In 2 mặt");
        chkDuplex.setOpaque(false);
        chkDuplex.setFont(UngDungPhongChu.SMALL);
        chkDuplex.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(chkDuplex);
        side.add(Box.createVerticalStrut(6));

        // Chiều giấy & Khổ giấy
        side.add(sectionTitle("Chiều giấy"));
        side.add(combo(new String[]{"Chiều ngang", "Chiều dọc"}));
        side.add(Box.createVerticalStrut(6));
        side.add(sectionTitle("Khổ giấy"));
        side.add(combo(new String[]{"A4 (210mm x 297mm)", "A5 (148mm x 210mm)"}));
        side.add(Box.createVerticalStrut(10));

        // Xuất File...
        side.add(sectionTitle("Xuất File..."));
        side.add(Box.createVerticalStrut(4));
        JPanel exportRow = new JPanel(new GridLayout(1, 2, 8, 0));
        exportRow.setOpaque(false);
        exportRow.setMaximumSize(new Dimension(174, 58));
        exportRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton pdfBtn = new JButton("Pdf", docIcon(new Color(180, 40, 40)));
        pdfBtn.setFont(UngDungPhongChu.BODY);
        pdfBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        pdfBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        pdfBtn.addActionListener(e -> printTable());

        JButton xlsBtn = new JButton("Excel", docIcon(new Color(28, 125, 65)));
        xlsBtn.setFont(UngDungPhongChu.BODY);
        xlsBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        xlsBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        xlsBtn.addActionListener(e -> exportExcelCsv());

        exportRow.add(pdfBtn);
        exportRow.add(xlsBtn);
        side.add(exportRow);
        side.add(Box.createVerticalGlue());
        return side;
    }

    private JPanel buildRightReportArea() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Color.WHITE);

        JTabbedPane viewTabs = new JTabbedPane();
        viewTabs.setFont(UngDungPhongChu.BODY);

        JPanel sheet = buildSheetPanel();
        viewTabs.addTab("Bản In", sheet);
        viewTabs.addTab("Dữ Liệu", buildSheetPanel());
        viewTabs.setSelectedIndex(1);

        wrap.add(viewTabs, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildSheetPanel() {
        JPanel sheet = new JPanel(new BorderLayout());
        sheet.setBackground(Color.WHITE);

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setBackground(Color.WHITE);
        titleBox.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel t1 = new JLabel("BÁO CÁO DOANH THU THEO PHIẾU THU", SwingConstants.CENTER);
        t1.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        t1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel t2 = new JLabel(from == null || to == null ? "Toàn bộ thời gian"
                : "Từ " + from.format(OUT_DT) + " đến " + to.format(OUT_DT), SwingConstants.CENTER);
        t2.setFont(UngDungPhongChu.BODY_BOLD);
        t2.setAlignmentX(Component.CENTER_ALIGNMENT);

        titleBox.add(t1);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(t2);
        sheet.add(titleBox, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.setBorder(new LineBorder(new Color(180, 185, 192)));
        sp.getViewport().setBackground(Color.WHITE);
        sheet.add(sp, BorderLayout.CENTER);

        JPanel sheetTabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        sheetTabBar.setBackground(new Color(240, 242, 245));
        sheetTabBar.setBorder(new MatteBorder(1, 0, 0, 0, new Color(195, 200, 206)));
        JLabel sTab = new JLabel("1.TongHopPhieuThu");
        sTab.setFont(UngDungPhongChu.SMALL_BOLD);
        sTab.setOpaque(true);
        sTab.setBackground(Color.WHITE);
        sTab.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(180, 185, 192)),
                new EmptyBorder(2, 8, 2, 8)));
        sheetTabBar.add(sTab);
        sheet.add(sheetTabBar, BorderLayout.SOUTH);

        return sheet;
    }

    private JPanel buildBottomStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(240, 242, 245));
        bar.setBorder(new MatteBorder(1, 0, 0, 0, new Color(190, 194, 200)));
        bar.setPreferredSize(new Dimension(100, 32));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        left.setOpaque(false);
        JLabel menuLbl = new JLabel("☰ ▾");
        menuLbl.setFont(UngDungPhongChu.BODY);
        left.add(menuLbl);
        bar.add(left, BorderLayout.WEST);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        center.setOpaque(false);
        center.add(new JLabel("←"));
        JTextField pageBox = new JTextField("1", 3);
        pageBox.setHorizontalAlignment(SwingConstants.CENTER);
        center.add(pageBox);
        center.add(new JLabel("/ 1 →"));
        bar.add(center, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        right.setOpaque(false);
        JSlider slider = new JSlider(60, 150, 100);
        slider.setOpaque(false);
        slider.setPreferredSize(new Dimension(130, 20));
        slider.addChangeListener(e -> {
            int z = slider.getValue();
            zoomLabel.setText(z + "%");
            table.setRowHeight(Math.max(18, 24 * z / 100));
        });
        right.add(zoomLabel);
        right.add(slider);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void printTable() {
        try {
            table.print(JTable.PrintMode.FIT_WIDTH);
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Không thể in: " + ex.getMessage(), "In Báo Cáo", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportExcelCsv() {
        JFileChooser picker = new JFileChooser();
        picker.setSelectedFile(new File("BC_008_BaoCaoDoanhThu_PhieuThu.csv"));
        if (picker.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (var writer = new OutputStreamWriter(new FileOutputStream(picker.getSelectedFile()), StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            for (int r = -1; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (c > 0) writer.write(',');
                    String cell = String.valueOf(r < 0 ? model.getColumnName(c) : model.getValueAt(r, c));
                    if (cell.matches("^[=+@\\-].*")) cell = "'" + cell;
                    writer.write('"' + cell.replace("\"", "\"\"") + '"');
                }
                writer.write("\r\n");
            }
            JOptionPane.showMessageDialog(this, "Đã xuất tệp Excel/CSV thành công.", "Xuất Excel", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không ghi được tệp.", "Xuất Excel", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static String fmtDash(BigDecimal v) {
        if (v == null || v.compareTo(BigDecimal.ZERO) == 0) return "-";
        return MONEY_FMT.format(v);
    }

    private static JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UngDungPhongChu.SMALL_BOLD);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JComboBox<String> combo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(UngDungPhongChu.SMALL);
        cb.setMaximumSize(new Dimension(174, 24));
        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
        return cb;
    }

    private static Icon printerIcon(int size) {
        return new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.translate(x, y);
                g.setColor(new Color(85, 95, 110));
                g.fillRect(5, 2, size - 10, 6);
                g.setColor(new Color(70, 125, 190));
                g.fillRoundRect(2, 7, size - 4, 10, 3, 3);
                g.setColor(Color.WHITE);
                g.fillRect(6, 13, size - 12, 9);
                g.setColor(Color.DARK_GRAY);
                g.drawRect(6, 13, size - 12, 9);
                g.dispose();
            }
        };
    }

    private static Icon docIcon(Color accent) {
        return new Icon() {
            @Override public int getIconWidth() { return 22; }
            @Override public int getIconHeight() { return 22; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.translate(x, y);
                g.setColor(Color.WHITE);
                g.fillRect(3, 1, 16, 20);
                g.setColor(accent);
                g.fillRect(3, 8, 12, 8);
                g.setColor(new Color(90, 95, 105));
                g.drawRect(3, 1, 16, 20);
                g.dispose();
            }
        };
    }

    private static String buildTitle() {
        String user = Objects.toString(NguoiDungPhien.getInstance().getFullName(), "").trim();
        if (user.isEmpty()) user = "Quản Trị Hệ Thống";
        return "EHC - Báo cáo doanh thu theo phiếu thu - [8018] - BC_008_BaoCaoDoanhThu_PhieuThu.xls   --- Người đăng nhập : " + user + " ---";
    }
}
