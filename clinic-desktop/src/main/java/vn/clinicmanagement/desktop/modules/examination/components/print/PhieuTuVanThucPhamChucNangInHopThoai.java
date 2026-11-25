package vn.clinicmanagement.desktop.modules.examination.components.print;

import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.FunctionalFoodConsultData;
import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.FunctionalFoodConsultLine;
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
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Cửa sổ xem trước bản in "Phiếu Tư Vấn Thực Phẩm Chức Năng". */
public final class PhieuTuVanThucPhamChucNangInHopThoai extends JDialog {

    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final String[] COLUMNS = {"STT", "Mã SP", "Tên thực phẩm chức năng", "ĐVT", "SL", "Đơn giá", "Thành tiền", "Cách dùng / Hướng dẫn"};

    private final FunctionalFoodConsultData data;
    private final DefaultTableModel model;
    private final JTable table;
    private final JPanel sheetPanel;

    public PhieuTuVanThucPhamChucNangInHopThoai(Window owner, FunctionalFoodConsultData data) {
        super(owner, buildTitle(), ModalityType.APPLICATION_MODAL);
        this.data = Objects.requireNonNull(data, "data");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        this.model = buildModel(data);
        this.table = new JTable(model);
        this.table.setFont(UngDungPhongChu.BODY);
        this.table.setRowHeight(24);
        this.sheetPanel = buildSheet();

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> printSheet(),
                KeyStroke.getKeyStroke(KeyEvent.VK_P, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        add(buildSidebar(), BorderLayout.WEST);
        add(buildTabs(), BorderLayout.CENTER);
    }

    private static String buildTitle() {
        String user = NguoiDungPhien.getInstance().getFullName();
        return "EHC - Phiếu Tư Vấn Thực Phẩm Chức Năng   --- Người đăng nhập : EHC "
                + (user == null || user.isBlank() ? "Bác sĩ" : user) + " ---";
    }

    private static DefaultTableModel buildModel(FunctionalFoodConsultData data) {
        DefaultTableModel tm = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        int stt = 1;
        for (FunctionalFoodConsultLine line : data.lines()) {
            String guide = (Objects.toString(line.dosage(), "") + " "
                    + Objects.toString(line.frequency(), "") + " "
                    + Objects.toString(line.instruction(), "")).trim();
            tm.addRow(new Object[]{
                    stt++, line.itemCode(), line.itemName(), line.unit(),
                    MONEY_FMT.format(line.qty()), MONEY_FMT.format(line.price()),
                    MONEY_FMT.format(line.amount()), guide
            });
        }
        return tm;
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(200, 0));
        side.setBackground(new Color(240, 240, 240));
        side.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 0, 1, new Color(180, 180, 180)),
                new EmptyBorder(10, 10, 10, 10)));

        JButton btnPrint = new JButton("<html><center>In<br>(Ctrl+P)</center></html>");
        btnPrint.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        btnPrint.setFont(new Font("Arial", Font.PLAIN, 15));
        btnPrint.addActionListener(e -> printSheet());
        side.add(btnPrint);
        side.add(Box.createVerticalStrut(12));

        JButton btnExcel = new JButton("Xuất Excel (CSV)");
        btnExcel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnExcel.addActionListener(e -> exportCsv());
        side.add(btnExcel);
        side.add(Box.createVerticalGlue());
        return side;
    }

    private JComponent buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(new Color(128, 128, 128));
        bg.setBorder(new EmptyBorder(16, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weighty = 1.0;
        bg.add(sheetPanel, gbc);
        tabs.addTab("Bản In", new JScrollPane(bg));
        tabs.addTab("Dữ Liệu", new JScrollPane(table));
        return tabs;
    }

    private JPanel buildSheet() {
        JPanel sheet = new JPanel(new BorderLayout(0, 10));
        sheet.setBackground(Color.WHITE);
        sheet.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(70, 70, 70)),
                new EmptyBorder(28, 30, 30, 30)));
        sheet.setPreferredSize(new Dimension(780, 900));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);

        JLabel title = new JLabel("PHIẾU TƯ VẤN THỰC PHẨM CHỨC NĂNG", SwingConstants.CENTER);
        title.setFont(new Font("Times New Roman", Font.BOLD, 17));
        title.setAlignmentX(CENTER_ALIGNMENT);
        JLabel sub = new JLabel("Mã lượt khám: " + Objects.toString(data.visitCode(), "")
                + "   |   Ngày tư vấn: " + (data.prescribedAt() != null ? data.prescribedAt().format(DT_FMT) : ""), SwingConstants.CENTER);
        sub.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        sub.setAlignmentX(CENTER_ALIGNMENT);

        JPanel info = new JPanel(new GridLayout(2, 2, 12, 4));
        info.setOpaque(false);
        info.setBorder(new EmptyBorder(10, 0, 6, 0));
        info.add(new JLabel("Họ tên người bệnh: " + Objects.toString(data.patientName(), "")));
        info.add(new JLabel("Mã bệnh nhân: " + Objects.toString(data.patientCode(), "")));
        info.add(new JLabel("Giới tính: " + Objects.toString(data.gender(), "")));
        info.add(new JLabel("Bác sĩ tư vấn: " + Objects.toString(data.doctorName(), "")));

        top.add(title);
        top.add(Box.createVerticalStrut(4));
        top.add(sub);
        top.add(info);
        sheet.add(top, BorderLayout.NORTH);

        JTable printTable = new JTable(model);
        printTable.setFont(new Font("Times New Roman", Font.PLAIN, 12));
        printTable.setRowHeight(24);
        printTable.setGridColor(Color.BLACK);
        printTable.setShowGrid(true);
        printTable.getTableHeader().setFont(new Font("Times New Roman", Font.BOLD, 12));
        JScrollPane tablePane = new JScrollPane(printTable);
        tablePane.setBorder(new LineBorder(Color.BLACK));
        sheet.add(tablePane, BorderLayout.CENTER);

        BigDecimal total = data.lines().stream().map(FunctionalFoodConsultLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        bottom.add(new JLabel("Tổng giá trị tham khảo: " + MONEY_FMT.format(total) + " đ"
                + (data.advice() != null && !data.advice().isBlank() ? "   |   Lời dặn: " + data.advice() : "")), BorderLayout.NORTH);

        JPanel sign = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sign.setOpaque(false);
        sign.add(new JLabel("<html><center>Bác sĩ tư vấn<br><br><br><b>" + Objects.toString(data.doctorName(), "") + "</b></center></html>"));
        bottom.add(sign, BorderLayout.SOUTH);
        sheet.add(bottom, BorderLayout.SOUTH);
        return sheet;
    }

    private void printSheet() {
        if (GraphicsEnvironment.isHeadless()) return;
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pf = job.defaultPage();
        Paper p = pf.getPaper();
        p.setImageableArea(24, 24, p.getWidth() - 48, p.getHeight() - 48);
        pf.setPaper(p);
        job.setPrintable((g, format, idx) -> {
            if (idx > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(format.getImageableX(), format.getImageableY());
            double s = Math.min(format.getImageableWidth() / sheetPanel.getWidth(), format.getImageableHeight() / sheetPanel.getHeight());
            g2.scale(s, s);
            sheetPanel.printAll(g2);
            return Printable.PAGE_EXISTS;
        }, pf);
        if (job.printDialog()) {
            try { job.print(); } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Không thể in: " + ex.getMessage(), "Lỗi In", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportCsv() {
        if (GraphicsEnvironment.isHeadless()) return;
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("phieu-tu-van-tpcn.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (var w = new OutputStreamWriter(new FileOutputStream(fc.getSelectedFile()), StandardCharsets.UTF_8)) {
            w.write('\ufeff');
            for (int c = 0; c < model.getColumnCount(); c++) {
                if (c > 0) w.write(',');
                w.write("\"" + model.getColumnName(c).replace("\"", "\"\"") + "\"");
            }
            w.write('\n');
            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (c > 0) w.write(',');
                    String val = Objects.toString(model.getValueAt(r, c), "");
                    if (val.startsWith("=") || val.startsWith("+") || val.startsWith("-") || val.startsWith("@")) val = "'" + val;
                    w.write("\"" + val.replace("\"", "\"\"") + "\"");
                }
                w.write('\n');
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không thể xuất CSV: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
