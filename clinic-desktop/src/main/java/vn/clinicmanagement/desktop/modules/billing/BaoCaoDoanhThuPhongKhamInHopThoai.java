package vn.clinicmanagement.desktop.modules.billing;

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
import java.util.*;
import java.util.List;

/**
 * Màn hình mẫu phiếu in toàn màn hình cho "Báo Cáo Doanh Thu Theo Phòng Khám".
 */
public final class BaoCaoDoanhThuPhongKhamInHopThoai extends JDialog {
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final Color LINK_BLUE = new Color(0, 78, 178);

    public record ServiceLine(String name, int qty, BigDecimal amount, BigDecimal discount, BigDecimal patientPay) {}

    public record VisitDetail(
            int visitId, String dateStr, String roomName, String patientName,
            String birthMale, String birthFemale, String address,
            BigDecimal amount, BigDecimal discount, BigDecimal patientPay, BigDecimal paid,
            List<ServiceLine> services
    ) {}

    private static final String[] COLUMNS = {
            "STT", "Ngày khám", "Tên bệnh nhân", "Nam", "Nữ", "Địa chỉ",
            "SL bệnh nhân", "Thành tiền", "Miễn giảm", "Số tiền thu của người bệnh", "Số tiền đã thu"
    };
    private static final int[] COL_WIDTHS = {55, 80, 175, 48, 48, 220, 80, 90, 80, 120, 95};

    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel zoomLabel = new JLabel("100%");

    public BaoCaoDoanhThuPhongKhamInHopThoai(Window owner,
                                             List<VisitDetail> visits,
                                             boolean groupByDate,
                                             boolean withPatient,
                                             boolean withService,
                                             String selectedRoomName,
                                             LocalDateTime from,
                                             LocalDateTime to) {
        super(owner, buildTitle(), ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        List<Integer> boldRows = new ArrayList<>();
        this.model = buildModel(visits, groupByDate, withPatient, withService, boldRows);
        this.table = buildTable(this.model, boldRows);

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(
                e -> printTable(),
                KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        add(buildSidebar(), BorderLayout.WEST);
        add(buildReportArea(selectedRoomName, from, to), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private static DefaultTableModel buildModel(List<VisitDetail> visits,
                                                boolean groupByDate,
                                                boolean withPatient,
                                                boolean withService,
                                                List<Integer> boldRows) {
        DefaultTableModel tm = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        Map<String, List<VisitDetail>> byGroup = new LinkedHashMap<>();
        for (VisitDetail v : visits) {
            String key = groupByDate ? v.dateStr() : "(Không nhóm dữ liệu)";
            byGroup.computeIfAbsent(key, x -> new ArrayList<>()).add(v);
        }

        int grandCount = 0;
        BigDecimal grandAmt = BigDecimal.ZERO, grandDisc = BigDecimal.ZERO,
                grandPat = BigDecimal.ZERO, grandPaid = BigDecimal.ZERO;

        for (var gEntry : byGroup.entrySet()) {
            List<VisitDetail> gList = gEntry.getValue();
            BigDecimal gAmt = BigDecimal.ZERO, gDisc = BigDecimal.ZERO, gPat = BigDecimal.ZERO, gPaid = BigDecimal.ZERO;
            for (VisitDetail v : gList) {
                gAmt = gAmt.add(v.amount());
                gDisc = gDisc.add(v.discount());
                gPat = gPat.add(v.patientPay());
                gPaid = gPaid.add(v.paid());
            }
            boldRows.add(tm.getRowCount());
            tm.addRow(new Object[]{
                    gEntry.getKey(), "", "", "", "", "",
                    gList.size(), fmtDash(gAmt), fmtDash(gDisc), fmtDash(gPat), fmtDash(gPaid)
            });

            Map<String, List<VisitDetail>> byRoom = new LinkedHashMap<>();
            for (VisitDetail v : gList) byRoom.computeIfAbsent(v.roomName(), x -> new ArrayList<>()).add(v);
            for (var rEntry : byRoom.entrySet()) {
                List<VisitDetail> rList = rEntry.getValue();
                BigDecimal rAmt = BigDecimal.ZERO, rDisc = BigDecimal.ZERO, rPat = BigDecimal.ZERO, rPaid = BigDecimal.ZERO;
                for (VisitDetail v : rList) {
                    rAmt = rAmt.add(v.amount());
                    rDisc = rDisc.add(v.discount());
                    rPat = rPat.add(v.patientPay());
                    rPaid = rPaid.add(v.paid());
                }
                boldRows.add(tm.getRowCount());
                tm.addRow(new Object[]{
                        rEntry.getKey(), "", "", "", "", "",
                        rList.size(), fmtDash(rAmt), fmtDash(rDisc), fmtDash(rPat), fmtDash(rPaid)
                });
                if (withPatient || withService) {
                    int stt = 1;
                    for (VisitDetail v : rList) {
                        if (withPatient) {
                            tm.addRow(new Object[]{
                                    stt++, v.dateStr(), v.patientName(), v.birthMale(), v.birthFemale(), v.address(),
                                    1, fmtDash(v.amount()), fmtDash(v.discount()), fmtDash(v.patientPay()), fmtDash(v.paid())
                            });
                        }
                        if (withService) {
                            for (ServiceLine svc : v.services()) {
                                tm.addRow(new Object[]{
                                        withPatient ? "" : stt++,
                                        withPatient ? "" : v.dateStr(),
                                        (withPatient ? "   • " : "") + svc.name(),
                                        "", "", "",
                                        svc.qty(), fmtDash(svc.amount()), fmtDash(svc.discount()), fmtDash(svc.patientPay()), "-"
                                });
                            }
                        }
                    }
                }
            }
            grandCount += gList.size();
            grandAmt = grandAmt.add(gAmt);
            grandDisc = grandDisc.add(gDisc);
            grandPat = grandPat.add(gPat);
            grandPaid = grandPaid.add(gPaid);
        }

        tm.addRow(new Object[]{
                "", "", "", "", "", "Tổng Cộng",
                grandCount, fmtDash(grandAmt), fmtDash(grandDisc), fmtDash(grandPat), fmtDash(grandPaid)
        });
        return tm;
    }

    private static JTable buildTable(DefaultTableModel tm, List<Integer> boldRows) {
        JTable tbl = new JTable(tm);
        tbl.setFont(new Font(Font.SERIF, Font.PLAIN, 12));
        tbl.setRowHeight(22);
        tbl.setShowGrid(true);
        tbl.setGridColor(Color.BLACK);
        tbl.getTableHeader().setFont(new Font(Font.SERIF, Font.BOLD, 12));
        tbl.getTableHeader().setBackground(Color.WHITE);
        tbl.getTableHeader().setReorderingAllowed(false);
        for (int i = 0; i < COL_WIDTHS.length; i++) tbl.getColumnModel().getColumn(i).setPreferredWidth(COL_WIDTHS[i]);

        Set<Integer> boldSet = new HashSet<>(boldRows);
        tbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                boolean isTotal = r == t.getRowCount() - 1;
                boolean isGroup = boldSet.contains(r);
                setFont(new Font(Font.SERIF, (isTotal || isGroup) ? Font.BOLD : Font.PLAIN, 12));
                setBackground(s ? new Color(0, 120, 215) : (isTotal ? new Color(255, 255, 224) : Color.WHITE));
                setForeground(s ? Color.WHITE : Color.BLACK);
                if (isTotal && c == 5) setHorizontalAlignment(SwingConstants.RIGHT);
                else if (c >= 6) setHorizontalAlignment(SwingConstants.RIGHT);
                else if (c == 0 && !isGroup) setHorizontalAlignment(SwingConstants.CENTER);
                else if (c == 1 || c == 3 || c == 4) setHorizontalAlignment(SwingConstants.CENTER);
                else setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(new EmptyBorder(0, 5, 0, 5));
                return this;
            }
        });
        return tbl;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(190, 100));
        side.setBackground(new Color(238, 242, 247));
        side.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 0, 1, new Color(185, 190, 198)),
                new EmptyBorder(8, 8, 8, 8)));

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

        side.add(sideTitle("Máy In"));
        side.add(sideCombo(new String[]{"Microsoft Print to PDF", "Default Printer"}));
        side.add(Box.createVerticalStrut(4));
        side.add(sideCombo(new String[]{"Automatically Select", "Khay 1"}));
        JLabel cfgLink = new JLabel("<html><u>Cấu hình máy in</u></html>");
        cfgLink.setFont(UngDungPhongChu.SMALL);
        cfgLink.setForeground(LINK_BLUE);
        cfgLink.setAlignmentX(Component.LEFT_ALIGNMENT);
        cfgLink.setBorder(new EmptyBorder(4, 38, 8, 0));
        side.add(cfgLink);

        side.add(sideTitle("Tùy Chọn"));
        side.add(sideCombo(new String[]{"In tất cả các trang", "Trang hiện tại"}));
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

        side.add(sideTitle("Chiều giấy"));
        side.add(sideCombo(new String[]{"Chiều ngang", "Chiều dọc"}));
        side.add(Box.createVerticalStrut(6));
        side.add(sideTitle("Khổ giấy"));
        side.add(sideCombo(new String[]{"A4 (210mm x 297mm)", "A5 (148mm x 210mm)"}));
        side.add(Box.createVerticalStrut(10));

        side.add(sideTitle("Xuất File..."));
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
        xlsBtn.addActionListener(e -> exportCsv());

        exportRow.add(pdfBtn);
        exportRow.add(xlsBtn);
        side.add(exportRow);
        side.add(Box.createVerticalGlue());
        return side;
    }

    private JPanel buildReportArea(String selectedRoomName, LocalDateTime from, LocalDateTime to) {
        JTabbedPane viewTabs = new JTabbedPane();
        viewTabs.setFont(UngDungPhongChu.BODY);

        JPanel printBg = new JPanel(new BorderLayout());
        printBg.setBackground(new Color(210, 214, 220));
        printBg.setBorder(new EmptyBorder(14, 24, 14, 24));
        JPanel paper = new JPanel(new BorderLayout());
        paper.setBackground(Color.WHITE);
        paper.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(145, 150, 158)),
                new EmptyBorder(18, 22, 18, 22)));

        JPanel hdr = new JPanel();
        hdr.setLayout(new BoxLayout(hdr, BoxLayout.Y_AXIS));
        hdr.setBackground(Color.WHITE);
        hdr.setBorder(new EmptyBorder(2, 12, 10, 12));
        JLabel t1 = new JLabel("BÁO CÁO DOANH THU THEO PHÒNG KHÁM");
        t1.setFont(new Font(Font.SERIF, Font.BOLD, 17));
        t1.setAlignmentX(Component.CENTER_ALIGNMENT);
        String roomSub = (selectedRoomName == null || selectedRoomName.isEmpty()) ? "" : " — " + selectedRoomName;
        JLabel t2 = new JLabel("Từ " + (from == null ? "" : from.format(DT_FMT))
                + " đến " + (to == null ? "" : to.format(DT_FMT)) + roomSub);
        t2.setFont(new Font(Font.SERIF, Font.BOLD, 12));
        t2.setAlignmentX(Component.CENTER_ALIGNMENT);
        hdr.add(t1);
        hdr.add(Box.createVerticalStrut(4));
        hdr.add(t2);
        paper.add(hdr, BorderLayout.NORTH);

        JScrollPane spPrint = new JScrollPane(table);
        spPrint.setBorder(new LineBorder(Color.BLACK));
        spPrint.getViewport().setBackground(Color.WHITE);
        paper.add(spPrint, BorderLayout.CENTER);
        printBg.add(paper, BorderLayout.CENTER);

        JTable dataTbl = buildTable(model, List.of());
        JScrollPane spData = new JScrollPane(dataTbl);
        spData.getViewport().setBackground(Color.WHITE);

        viewTabs.addTab("Bản In", printBg);
        viewTabs.addTab("Dữ Liệu", spData);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(viewTabs, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(240, 242, 245));
        bar.setBorder(new MatteBorder(1, 0, 0, 0, new Color(190, 194, 200)));
        bar.setPreferredSize(new Dimension(100, 32));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        left.setOpaque(false);
        left.add(new JLabel("☰ ▾"));
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
            table.setRowHeight(Math.max(16, 22 * z / 100));
        });
        right.add(zoomLabel);
        right.add(slider);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void printTable() {
        try { table.print(JTable.PrintMode.FIT_WIDTH); }
        catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Không thể in: " + ex.getMessage(), "In Báo Cáo", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCsv() {
        JFileChooser picker = new JFileChooser();
        picker.setSelectedFile(new File("PK_020_BaoCaoDoanhThu_PhongKham.csv"));
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
            JOptionPane.showMessageDialog(this, "Đã xuất báo cáo CSV.", "Xuất Excel", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không ghi được tệp CSV.", "Xuất Excel", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String fmtDash(BigDecimal v) {
        if (v == null || v.compareTo(BigDecimal.ZERO) == 0) return "-";
        return MONEY_FMT.format(v);
    }

    private static JLabel sideTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UngDungPhongChu.SMALL_BOLD);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JComboBox<String> sideCombo(String[] items) {
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
        return "EHC - - [0] - PK_020_BaoCaoDoanhThu_PhongKham.xls   --- Người đăng nhập : " + user + " ---";
    }
}
