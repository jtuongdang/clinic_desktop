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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Màn hình mẫu phiếu in toàn màn hình cho "Báo Cáo Theo Dịch Vụ (019)".
 */
public final class BaoCaoTheoDichVu019InHopThoai extends JDialog {
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final Color LINK_BLUE = new Color(0, 78, 178);

    public record Svc019Row(
            String outerGroup,
            String categoryGroup,
            String serviceName,
            String patientCode,
            String patientName,
            int qty
    ) {}

    private static final String[] COLUMNS = {"STT", "Mã BN", "Nội dung", "Số lượng"};
    private static final int[] COL_WIDTHS = {50, 85, 440, 85};

    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel zoomLabel = new JLabel("100%");

    public BaoCaoTheoDichVu019InHopThoai(Window owner,
                                         List<Svc019Row> rows,
                                         boolean withPatient,
                                         LocalDateTime from,
                                         LocalDateTime to) {
        super(owner, buildTitle(), ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        List<Integer> boldRows = new ArrayList<>();
        this.model = buildModel(rows, withPatient, boldRows);
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
        add(buildReportArea(from, to), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private static DefaultTableModel buildModel(List<Svc019Row> rows,
                                                boolean withPatient,
                                                List<Integer> boldRows) {
        DefaultTableModel tm = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        Map<String, List<Svc019Row>> byGroup = new LinkedHashMap<>();
        for (Svc019Row r : rows) {
            byGroup.computeIfAbsent(r.outerGroup(), x -> new ArrayList<>()).add(r);
        }

        int grandQty = 0;
        for (var g : byGroup.entrySet()) {
            boldRows.add(tm.getRowCount());
            tm.addRow(new Object[]{g.getKey(), "", "", ""});

            Map<String, List<Svc019Row>> byType = new LinkedHashMap<>();
            for (Svc019Row r : g.getValue()) byType.computeIfAbsent(r.categoryGroup(), x -> new ArrayList<>()).add(r);
            for (var tEntry : byType.entrySet()) {
                boldRows.add(tm.getRowCount());
                tm.addRow(new Object[]{tEntry.getKey(), "", "", ""});

                int stt = 1;
                if (withPatient) {
                    for (Svc019Row r : tEntry.getValue()) {
                        grandQty += r.qty();
                        tm.addRow(new Object[]{stt++, r.patientCode(), r.serviceName() + " — " + r.patientName(), r.qty()});
                    }
                } else {
                    Map<String, Integer> bySvc = new LinkedHashMap<>();
                    for (Svc019Row r : tEntry.getValue()) {
                        bySvc.merge(r.serviceName(), r.qty(), Integer::sum);
                    }
                    for (var sEntry : bySvc.entrySet()) {
                        grandQty += sEntry.getValue();
                        tm.addRow(new Object[]{stt++, "", sEntry.getKey(), sEntry.getValue()});
                    }
                }
            }
        }

        tm.addRow(new Object[]{"", "", "Tổng Cộng :", grandQty});
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
                setBackground(s ? new Color(0, 120, 215)
                        : (isTotal ? new Color(234, 244, 223) : (isGroup ? new Color(238, 242, 246) : Color.WHITE)));
                setForeground(s ? Color.WHITE : Color.BLACK);
                if (c == 3 || (isTotal && c == 2)) setHorizontalAlignment(SwingConstants.RIGHT);
                else if ((c == 0 || c == 1) && !isGroup) setHorizontalAlignment(SwingConstants.CENTER);
                else setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(new EmptyBorder(0, 6, 0, 6));
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
        side.add(sideCombo(new String[]{"Chiều dọc", "Chiều ngang"}));
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

    private JPanel buildReportArea(LocalDateTime from, LocalDateTime to) {
        JTabbedPane viewTabs = new JTabbedPane();
        viewTabs.setFont(UngDungPhongChu.BODY);

        JPanel printBg = new JPanel(new GridBagLayout());
        printBg.setBackground(new Color(98, 98, 98));
        printBg.setBorder(new EmptyBorder(14, 24, 14, 24));
        JPanel paper = new JPanel(new BorderLayout());
        paper.setBackground(Color.WHITE);
        paper.setPreferredSize(new Dimension(760, 680));
        paper.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(45, 45, 45)),
                new EmptyBorder(24, 28, 24, 28)));

        JPanel hdr = new JPanel();
        hdr.setLayout(new BoxLayout(hdr, BoxLayout.Y_AXIS));
        hdr.setBackground(Color.WHITE);
        hdr.setBorder(new EmptyBorder(2, 12, 12, 12));
        JLabel t1 = new JLabel("BÁO CÁO DOANH THU THEO DỊCH VỤ ĐÃ THU TIỀN");
        t1.setFont(new Font(Font.SERIF, Font.BOLD, 17));
        t1.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel t2 = new JLabel("Từ " + (from == null ? "" : from.format(DT_FMT))
                + " đến " + (to == null ? "" : to.format(DT_FMT)));
        t2.setFont(new Font(Font.SERIF, Font.BOLD, 12));
        t2.setAlignmentX(Component.CENTER_ALIGNMENT);
        hdr.add(t1);
        hdr.add(Box.createVerticalStrut(4));
        hdr.add(t2);
        paper.add(hdr, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new LineBorder(Color.BLACK));
        sp.getViewport().setBackground(Color.WHITE);
        paper.add(sp, BorderLayout.CENTER);

        GridBagConstraints pgc = new GridBagConstraints();
        pgc.fill = GridBagConstraints.BOTH;
        pgc.weightx = 0.0;
        pgc.weighty = 1.0;
        printBg.add(paper, pgc);

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
        picker.setSelectedFile(new File("BC_019_BaoCaoDoanhThu_DichVu_PhieuThu.csv"));
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
        return "EHC - Báo cáo theo dịch vụ (019) - [8032] - BC_019_BaoCaoDoanhThu_DichVu_PhieuThu.xls   --- Người đăng nhập : " + user + " ---";
    }
}
