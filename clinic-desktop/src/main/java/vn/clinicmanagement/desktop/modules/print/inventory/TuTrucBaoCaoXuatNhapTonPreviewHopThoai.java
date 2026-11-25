package vn.clinicmanagement.desktop.modules.print.inventory;


import vn.clinicmanagement.desktop.modules.inventory.*;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.core.session.NguoiDungPhien;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Màn hình toàn màn hình "EHC - Báo cáo xuất nhập tồn - [50004] - TH_005_BaoCaoNhapXuatTonThuoc.xls"
 * mở khi bấm OK ở hộp thoại Tùy Chọn Báo Cáo.
 */
public final class TuTrucBaoCaoXuatNhapTonPreviewHopThoai extends JDialog {
    public enum ReportTemplate { XUAT_NHAP_TON, HAO_PHI_KHOA_PHONG }

    private static final Color BORDER = new Color(195, 199, 205);
    private static final Color BACKDROP = new Color(138, 142, 148);
    private static final Color GREEN_TINT = new Color(226, 240, 217);

    private final ReportTemplate template;
    private final String warehouseName;
    private final String fromText;
    private final String untilText;
    private final String groupLabel;
    private final JPanel paperPanel;
    private final JLabel zoomLabel = new JLabel("100%");
    private final DefaultTableModel dataModel;

    public TuTrucBaoCaoXuatNhapTonPreviewHopThoai(
            Window owner,
            String warehouseName,
            String fromText,
            String untilText,
            String groupLabel) {
        this(owner, ReportTemplate.XUAT_NHAP_TON, warehouseName, fromText, untilText, groupLabel);
    }

    public TuTrucBaoCaoXuatNhapTonPreviewHopThoai(
            Window owner,
            ReportTemplate template,
            String warehouseName,
            String fromText,
            String untilText,
            String groupLabel) {
        super(owner, buildTitle(), ModalityType.MODELESS);
        this.template = template == null ? ReportTemplate.XUAT_NHAP_TON : template;
        this.warehouseName = (warehouseName == null || warehouseName.isBlank()) ? "Kho hóa chất" : warehouseName;
        this.fromText = fromText;
        this.untilText = untilText;
        this.groupLabel = (groupLabel == null || groupLabel.isBlank()) ? "(Không nhóm dữ liệu)" : groupLabel;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(242, 243, 245));

        add(buildLeftControls(), BorderLayout.WEST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(UngDungPhongChu.BODY);

        paperPanel = buildPaperPanel();
        JPanel canvas = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 18));
        canvas.setBackground(BACKDROP);
        canvas.add(paperPanel);
        JScrollPane previewScroll = new JScrollPane(canvas,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        previewScroll.setBorder(null);
        tabs.addTab("Bản In", previewScroll);

        String[] cols = this.template == ReportTemplate.HAO_PHI_KHOA_PHONG
                ? new String[]{"STT", "Mã thuốc/vật tư", "Tên thuốc - Vật tư", "Hãng SX, Nước SX", "Đơn Vị", "Số lượng", "Đơn giá", "Thành tiền"}
                : new String[]{"STT", "Mã thuốc", "Tên thuốc", "Đơn vị tính", "Số lô", "Hạn sử dụng",
                "Tồn đầu kỳ", "SL Nhập", "Thành tiền Nhập", "SL Xuất", "Thành tiền Xuất", "Tồn cuối kỳ"};
        dataModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable dataTable = new JTable(dataModel);
        dataTable.setRowHeight(24);
        dataTable.setFont(UngDungPhongChu.BODY);
        dataTable.getTableHeader().setFont(UngDungPhongChu.BODY_BOLD);
        JScrollPane dataScroll = new JScrollPane(dataTable);
        tabs.addTab("Dữ Liệu", dataScroll);

        add(tabs, BorderLayout.CENTER);
        add(buildBottomBar(), BorderLayout.SOUTH);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("control P"), "print");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("meta P"), "print");
        getRootPane().getActionMap().put("print", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { doPrint(); }
        });
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "closeWindow");
        getRootPane().getActionMap().put("closeWindow", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });
    }

    private JPanel buildLeftControls() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(new Color(242, 243, 245));
        side.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 0, 1, BORDER),
                new EmptyBorder(8, 8, 8, 8)));
        side.setPreferredSize(new Dimension(176, 100));

        // Top print button + copies
        JPanel printTop = new JPanel(new BorderLayout(6, 0));
        printTop.setOpaque(false);
        printTop.setAlignmentX(Component.LEFT_ALIGNMENT);
        printTop.setMaximumSize(new Dimension(160, 68));

        JButton printBtn = new JButton("<html><center>In<br>(Ctrl+P)</center></html>", printerIcon());
        printBtn.setFont(UngDungPhongChu.SMALL);
        printBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        printBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        printBtn.setPreferredSize(new Dimension(70, 64));
        printBtn.setFocusPainted(false);
        printBtn.addActionListener(e -> doPrint());
        printTop.add(printBtn, BorderLayout.WEST);

        JPanel copiesPanel = new JPanel();
        copiesPanel.setLayout(new BoxLayout(copiesPanel, BoxLayout.Y_AXIS));
        copiesPanel.setOpaque(false);
        JPanel copiesRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        copiesRow.setOpaque(false);
        JLabel lblBanIn = new JLabel("Bản in");
        lblBanIn.setFont(UngDungPhongChu.TINY);
        JSpinner copiesSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
        copiesSpinner.setFont(UngDungPhongChu.SMALL);
        copiesSpinner.setPreferredSize(new Dimension(42, 20));
        copiesRow.add(lblBanIn);
        copiesRow.add(copiesSpinner);

        JCheckBox chkAuto = new JCheckBox("In tự động");
        chkAuto.setFont(UngDungPhongChu.TINY);
        chkAuto.setOpaque(false);
        JCheckBox chkCont = new JCheckBox("In tiếp");
        chkCont.setFont(UngDungPhongChu.TINY);
        chkCont.setOpaque(false);

        copiesPanel.add(copiesRow);
        copiesPanel.add(chkAuto);
        copiesPanel.add(chkCont);
        printTop.add(copiesPanel, BorderLayout.CENTER);
        side.add(printTop);
        side.add(Box.createVerticalStrut(10));

        // Máy In
        side.add(sectionLabel("Máy In"));
        JComboBox<String> printerCombo = new JComboBox<>();
        printerCombo.setFont(UngDungPhongChu.SMALL);
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        if (services.length == 0) printerCombo.addItem("Máy in mặc định");
        else for (PrintService ps : services) printerCombo.addItem(ps.getName());
        sizeCombo(printerCombo);
        side.add(printerCombo);
        side.add(Box.createVerticalStrut(4));

        JComboBox<String> trayCombo = new JComboBox<>(new String[]{"Tự động chọn khay"});
        trayCombo.setFont(UngDungPhongChu.SMALL);
        sizeCombo(trayCombo);
        side.add(trayCombo);
        side.add(Box.createVerticalStrut(4));

        JLabel cfgLink = new JLabel("<html><u>Cấu hình máy in</u></html>");
        cfgLink.setFont(UngDungPhongChu.SMALL);
        cfgLink.setForeground(new Color(0, 85, 200));
        cfgLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cfgLink.setAlignmentX(Component.LEFT_ALIGNMENT);
        cfgLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { PrinterJob.getPrinterJob().pageDialog(PrinterJob.getPrinterJob().defaultPage()); }
        });
        side.add(cfgLink);
        side.add(Box.createVerticalStrut(10));

        // Tùy Chọn
        side.add(sectionLabel("Tùy Chọn"));
        JComboBox<String> pageRangeCombo = new JComboBox<>(new String[]{"In tất cả các trang", "Trang hiện tại", "Chọn trang"});
        pageRangeCombo.setFont(UngDungPhongChu.SMALL);
        sizeCombo(pageRangeCombo);
        side.add(pageRangeCombo);
        side.add(Box.createVerticalStrut(4));

        JPanel pageRow = new JPanel(new BorderLayout(4, 0));
        pageRow.setOpaque(false);
        pageRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        pageRow.setMaximumSize(new Dimension(160, 24));
        JLabel lblTrang = new JLabel("Trang in :");
        lblTrang.setFont(UngDungPhongChu.SMALL);
        JTextField pageField = new JTextField();
        pageField.setFont(UngDungPhongChu.SMALL);
        pageRow.add(lblTrang, BorderLayout.WEST);
        pageRow.add(pageField, BorderLayout.CENTER);
        side.add(pageRow);
        side.add(Box.createVerticalStrut(4));

        JCheckBox chkDuplex = new JCheckBox("In 2 mặt");
        chkDuplex.setFont(UngDungPhongChu.SMALL);
        chkDuplex.setOpaque(false);
        chkDuplex.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(chkDuplex);
        side.add(Box.createVerticalStrut(10));

        // Chiều giấy
        side.add(sectionLabel("Chiều giấy"));
        JComboBox<String> orientCombo = new JComboBox<>(new String[]{"Chiều ngang", "Chiều dọc"});
        orientCombo.setFont(UngDungPhongChu.SMALL);
        sizeCombo(orientCombo);
        side.add(orientCombo);
        side.add(Box.createVerticalStrut(8));

        // Khổ giấy
        side.add(sectionLabel("Khổ giấy"));
        JComboBox<String> sizeCombo = new JComboBox<>(new String[]{"A4 (210mm x 297mm)", "A5 (148mm x 210mm)", "Letter"});
        sizeCombo.setFont(UngDungPhongChu.SMALL);
        sizeCombo(sizeCombo);
        side.add(sizeCombo);
        side.add(Box.createVerticalStrut(12));

        // Xuất File...
        side.add(sectionLabel("Xuất File..."));
        side.add(Box.createVerticalStrut(4));
        JPanel exportRow = new JPanel(new GridLayout(1, 2, 6, 0));
        exportRow.setOpaque(false);
        exportRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        exportRow.setMaximumSize(new Dimension(156, 56));

        JButton pdfBtn = new JButton("Pdf", fileBadgeIcon(new Color(190, 35, 35), "PDF"));
        pdfBtn.setFont(UngDungPhongChu.SMALL);
        pdfBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        pdfBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        pdfBtn.setFocusPainted(false);
        pdfBtn.addActionListener(e -> doPrint());

        JButton excelBtn = new JButton("Excel", fileBadgeIcon(new Color(33, 115, 70), "XLS"));
        excelBtn.setFont(UngDungPhongChu.SMALL);
        excelBtn.setHorizontalTextPosition(SwingConstants.CENTER);
        excelBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
        excelBtn.setFocusPainted(false);
        excelBtn.addActionListener(e -> exportExcelCsv());

        exportRow.add(pdfBtn);
        exportRow.add(excelBtn);
        side.add(exportRow);
        side.add(Box.createVerticalGlue());

        return side;
    }

    private JPanel buildPaperPanel() {
        JPanel paper = new JPanel();
        paper.setLayout(new BoxLayout(paper, BoxLayout.Y_AXIS));
        paper.setBackground(Color.WHITE);
        paper.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(90, 90, 90)),
                new EmptyBorder(24, 24, 24, 24)));
        paper.setPreferredSize(new Dimension(1020, 660));

        if (template == ReportTemplate.HAO_PHI_KHOA_PHONG) {
            JPanel headerRow = new JPanel(new BorderLayout());
            headerRow.setOpaque(false);
            headerRow.setMaximumSize(new Dimension(970, 60));

            JLabel agency = new JLabel("<html><b>SỞ Y TẾ<br/>PHÒNG KHÁM / BỆNH VIỆN</b></html>");
            agency.setFont(new Font(Font.SERIF, Font.BOLD, 12));
            headerRow.add(agency, BorderLayout.WEST);

            JPanel titleBox = new JPanel();
            titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
            titleBox.setOpaque(false);
            JLabel title = new JLabel("BÁO CÁO LĨNH HAO PHÍ THEO KHOA", SwingConstants.CENTER);
            title.setFont(new Font(Font.SERIF, Font.BOLD, 18));
            title.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel sub = new JLabel("Từ " + fromText + " đến " + untilText, SwingConstants.CENTER);
            sub.setFont(new Font(Font.SERIF, Font.BOLD, 13));
            sub.setAlignmentX(Component.CENTER_ALIGNMENT);
            titleBox.add(title);
            titleBox.add(sub);
            headerRow.add(titleBox, BorderLayout.CENTER);
            paper.add(headerRow);
            paper.add(Box.createVerticalStrut(12));

            String html = "<html><body style='font-family:Serif; font-size:11px; color:#000;'>"
                    + "<table width='100%' cellspacing='0' cellpadding='4' border='1' style='border-collapse:collapse; border-color:#333;'>"
                    + "<tr style='background:#f9f9f9; font-weight:bold; text-align:center;'>"
                    + "<td width='40'>STT</td><td width='90'>Mã thuốc/vật tư</td><td>Tên thuốc - Vật tư</td>"
                    + "<td width='160'>Hãng SX, Nước SX</td><td width='60'>Đơn Vị</td>"
                    + "<td width='70'>Số lượng</td><td width='80'>Đơn giá</td><td width='90'>Thành tiền</td>"
                    + "</tr>"
                    + "<tr><td colspan='8' style='font-weight:bold; padding:4px;'>Thuốc vật tư đánh hao phí trên bệnh nhân " + groupLabel + "</td></tr>"
                    + "<tr><td colspan='8' style='text-align:center; color:#555; padding:10px;'>(Chưa có dữ liệu phát sinh trong kỳ báo cáo)</td></tr>"
                    + "<tr style='font-weight:bold; font-style:italic;'>"
                    + "<td colspan='7' align='right'>Tổng:</td><td align='right'>0</td>"
                    + "</tr>"
                    + "</table></body></html>";
            JLabel tablePreview = new JLabel(html);
            tablePreview.setAlignmentX(Component.CENTER_ALIGNMENT);
            paper.add(tablePreview);
            return paper;
        }

        JLabel title = new JLabel("THỐNG KÊ NHẬP XUẤT TỒN THUỐC - " + warehouseName, SwingConstants.CENTER);
        title.setFont(new Font(Font.SERIF, Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        paper.add(title);

        JLabel sub = new JLabel("Từ " + fromText + " đến " + untilText, SwingConstants.CENTER);
        sub.setFont(new Font(Font.SERIF, Font.BOLD, 13));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        sub.setBorder(new EmptyBorder(4, 0, 14, 0));
        paper.add(sub);

        String html = "<html><body style='font-family:Serif; font-size:10px; color:#000;'>"
                + "<table width='100%' cellspacing='0' cellpadding='3' border='1' style='border-collapse:collapse; border-color:#333;'>"
                + "<tr style='background:#f9f9f9; font-weight:bold; text-align:center;'>"
                + "<td>STT</td><td>Mã thuốc</td><td>Tên thuốc</td><td>Hoạt chất</td><td>Hàm lượng</td>"
                + "<td>Đơn vị tính</td><td>Đường dùng</td><td>Hãng sản xuất</td><td>Nước sản xuất</td>"
                + "<td>Mã ATC</td><td>Số đăng ký</td><td>Giá nhập</td><td>Giá xuất</td><td>Giá BHYT</td>"
                + "<td>Giá dịch vụ</td><td>Số lô</td><td>Quyết định trúng thầu</td><td>Hạn sử dụng</td>"
                + "</tr>"
                + "<tr><td colspan='18' style='font-weight:bold; padding:4px;'>" + groupLabel + "</td></tr>"
                + "<tr><td colspan='18' style='text-align:center; color:#555; padding:8px;'>(Chưa có dữ liệu phát sinh trong kỳ báo cáo)</td></tr>"
                + "</table><br/>"
                + "<table width='100%' cellspacing='0' cellpadding='4' border='1' style='border-collapse:collapse; border-color:#333;'>"
                + "<tr style='font-weight:bold; text-align:center; background:#f9f9f9;'>"
                + "<td>Tồn đầu kỳ</td><td colspan='3'>Nhập</td><td colspan='3'>Xuất</td><td>Tồn cuối kỳ</td>"
                + "</tr>"
                + "<tr style='font-weight:bold; text-align:center; background:#f9f9f9;'>"
                + "<td>Số lượng</td><td>Số lượng</td><td>Đơn giá</td><td>Thành tiền</td>"
                + "<td>Số lượng</td><td>Đơn giá</td><td>Thành tiền</td><td>Số lượng</td>"
                + "</tr>"
                + "<tr style='text-align:center;'>"
                + "<td>-</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td>"
                + "</tr>"
                + "<tr style='background:#e2f0d9; font-weight:bold;'>"
                + "<td></td><td colspan='2' align='right'>Tổng cộng:</td><td align='right'>0</td>"
                + "<td colspan='2' align='right'>Tổng cộng:</td><td align='right'>0</td><td></td>"
                + "</tr>"
                + "</table></body></html>";
        JLabel tablePreview = new JLabel(html);
        tablePreview.setAlignmentX(Component.CENTER_ALIGNMENT);
        paper.add(tablePreview);

        return paper;
    }

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(242, 243, 245));
        bar.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));
        bar.setPreferredSize(new Dimension(100, 32));

        JButton menuBtn = new JButton("☰ ▾");
        menuBtn.setFont(UngDungPhongChu.BODY);
        menuBtn.setFocusPainted(false);
        menuBtn.setContentAreaFilled(false);
        bar.add(menuBtn, BorderLayout.WEST);

        JPanel pager = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        pager.setOpaque(false);
        JButton prev = new JButton("←");
        prev.setFont(UngDungPhongChu.SMALL);
        JSpinner pageSpin = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
        pageSpin.setPreferredSize(new Dimension(44, 22));
        JLabel totalPages = new JLabel("/ 1");
        totalPages.setFont(UngDungPhongChu.BODY);
        JButton next = new JButton("→");
        next.setFont(UngDungPhongChu.SMALL);
        pager.add(prev);
        pager.add(pageSpin);
        pager.add(totalPages);
        pager.add(next);
        bar.add(pager, BorderLayout.CENTER);

        JPanel zoom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        zoom.setOpaque(false);
        zoomLabel.setFont(UngDungPhongChu.SMALL);
        JSlider slider = new JSlider(50, 150, 100);
        slider.setPreferredSize(new Dimension(120, 20));
        slider.setOpaque(false);
        slider.addChangeListener(e -> zoomLabel.setText(slider.getValue() + "%"));
        zoom.add(zoomLabel);
        zoom.add(slider);
        bar.add(zoom, BorderLayout.EAST);

        return bar;
    }

    private void doPrint() {
        PrinterJob job = PrinterJob.getPrinterJob();
        if (job.printDialog()) {
            try {
                job.print();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không in được báo cáo.", "In", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void exportExcelCsv() {
        JFileChooser picker = new JFileChooser();
        picker.setSelectedFile(new File("TH_005_BaoCaoNhapXuatTonThuoc.csv"));
        if (picker.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = picker.getSelectedFile();
        try (var writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            writer.write("THỐNG KÊ NHẬP XUẤT TỒN THUỐC - " + warehouseName + "\r\n");
            writer.write("Từ " + fromText + " đến " + untilText + "\r\n");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không ghi được tệp.", "Xuất Excel", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UngDungPhongChu.BODY_BOLD);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private static void sizeCombo(JComboBox<String> combo) {
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        combo.setMaximumSize(new Dimension(160, 24));
        combo.setPreferredSize(new Dimension(160, 24));
    }

    private static String buildTitle() {
        String user = Objects.toString(NguoiDungPhien.getInstance().getFullName(), "").trim();
        if (user.isEmpty()) user = "Quản Trị Hệ Thống";
        return "EHC - Báo cáo xuất nhập tồn - [50004] - TH_005_BaoCaoNhapXuatTonThuoc.xls   ----- Người đăng nhập : " + user + " -----";
    }

    private static Icon printerIcon() {
        return new Icon() {
            @Override public int getIconWidth() { return 22; }
            @Override public int getIconHeight() { return 22; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.translate(x, y);
                g.setColor(new Color(90, 95, 105));
                g.drawRect(5, 2, 12, 5);
                g.setColor(new Color(70, 100, 140));
                g.fillRect(2, 7, 18, 8);
                g.setColor(Color.WHITE);
                g.fillRect(5, 12, 12, 7);
                g.setColor(new Color(90, 95, 105));
                g.drawRect(5, 12, 12, 7);
                g.dispose();
            }
        };
    }

    private static Icon fileBadgeIcon(Color badgeColor, String text) {
        return new Icon() {
            @Override public int getIconWidth() { return 24; }
            @Override public int getIconHeight() { return 24; }
            @Override public void paintIcon(Component c, Graphics gr, int x, int y) {
                Graphics2D g = (Graphics2D) gr.create();
                g.translate(x, y);
                g.setColor(Color.WHITE);
                g.fillRect(3, 1, 17, 21);
                g.setColor(new Color(120, 125, 130));
                g.drawRect(3, 1, 17, 21);
                g.setColor(badgeColor);
                g.fillRect(2, 11, 20, 9);
                g.setColor(Color.WHITE);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 7));
                g.drawString(text, 4, 18);
                g.dispose();
            }
        };
    }
}
