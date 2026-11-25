package vn.clinicmanagement.desktop.modules.billing;

import vn.clinicmanagement.application.billing.QuanLyDoanhThuReadPort;
import vn.clinicmanagement.application.billing.QuanLyDoanhThuReadPort.ReceiptReportRow;
import vn.clinicmanagement.desktop.core.config.UngDungPhongChu;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhPhienNguCanh;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Màn hình Danh Sách Phiếu Thu kết nối CSDL (dbo.PhieuThu, dbo.HoaDon). */
public final class DanhSachPhieuThuHopThoai extends JPanel {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DecimalFormat MONEY_FMT = new DecimalFormat("#,##0");
    private static final String[] COLUMNS = {
            "STT", "Mã phiếu thu", "Ngày thu", "Bệnh nhân", "Đối tượng", "Khoa / Phòng",
            "Số tiền thu", "Miễn giảm", "Tạm ứng", "Hoàn trả", "Phương thức", "Sổ thu", "Người thu", "Trạng thái"
    };

    private final QuanLyDoanhThuReadPort readPort;
    private final JTextField txtFrom = new JTextField(12);
    private final JTextField txtTo = new JTextField(12);
    private final JTextField txtSearch = new JTextField(16);
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel lblStatus = new JLabel("Đang tải dữ liệu...");
    private final JLabel lblTotals = new JLabel("");
    private List<ReceiptReportRow> loadedRows = List.of();

    public DanhSachPhieuThuHopThoai(QuanLyDoanhThuReadPort readPort, LocalDateTime initialFrom, LocalDateTime initialTo) {
        this.readPort = readPort;
        setLayout(new BorderLayout(0, 8));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 12, 10, 12));
        setPreferredSize(new Dimension(1180, 640));

        LocalDate today = LocalDate.now();
        txtFrom.setText((initialFrom != null ? initialFrom : today.atStartOfDay()).format(DT_FMT));
        txtTo.setText((initialTo != null ? initialTo : LocalDateTime.of(today, LocalTime.of(23, 59))).format(DT_FMT));

        add(buildToolbar(), BorderLayout.NORTH);

        model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        table.setFont(UngDungPhongChu.BODY);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.setGridColor(new Color(224, 230, 236));
        table.setSelectionBackground(new Color(214, 234, 255));
        table.setSelectionForeground(Color.BLACK);
        table.getTableHeader().setFont(UngDungPhongChu.BODY_BOLD);
        table.getTableHeader().setBackground(new Color(238, 243, 249));
        table.setDefaultRenderer(Object.class, new ReceiptCellRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(new Color(189, 202, 215)));
        add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        lblStatus.setFont(UngDungPhongChu.BODY);
        lblStatus.setForeground(new Color(75, 90, 106));
        lblTotals.setFont(UngDungPhongChu.BODY_BOLD);
        lblTotals.setForeground(new Color(10, 86, 179));
        footer.add(lblStatus, BorderLayout.WEST);
        footer.add(lblTotals, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        loadData();
    }

    public JTable getTable() {
        return table;
    }

    public JLabel getLblTotals() {
        return lblTotals;
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(8, 6));
        bar.setOpaque(false);

        JLabel title = new JLabel("DANH SÁCH PHIẾU THU VIỆN PHÍ");
        title.setFont(UngDungPhongChu.BODY_BOLD.deriveFont(15f));
        title.setForeground(new Color(10, 86, 179));
        bar.add(title, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controls.setOpaque(false);
        controls.add(new JLabel("Từ ngày:"));
        controls.add(txtFrom);
        controls.add(new JLabel("Đến ngày:"));
        controls.add(txtTo);
        controls.add(new JLabel("Tìm:"));
        controls.add(txtSearch);

        JButton btnLoad = actionBtn("Tìm / Tải lại");
        JButton btnPrint = actionBtn("In");
        JButton btnExport = actionBtn("Xuất CSV");
        btnLoad.addActionListener(e -> loadData());
        txtSearch.addActionListener(e -> applyKeywordFilter());
        btnPrint.addActionListener(e -> printTable());
        btnExport.addActionListener(e -> exportCsv());

        controls.add(btnLoad);
        controls.add(btnPrint);
        controls.add(btnExport);
        bar.add(controls, BorderLayout.CENTER);
        return bar;
    }

    public void loadData() {
        var auth = KhamBenhPhienNguCanh.current();
        if (readPort == null || auth == null) {
            lblStatus.setText("Cần phiên đăng nhập và kết nối CSDL để tải danh sách phiếu thu.");
            return;
        }
        LocalDateTime from;
        LocalDateTime to;
        try {
            from = LocalDateTime.parse(txtFrom.getText().trim(), DT_FMT);
            to = LocalDateTime.parse(txtTo.getText().trim(), DT_FMT).plusMinutes(1);
            if (!from.isBefore(to)) throw new IllegalArgumentException();
        } catch (Exception ex) {
            if (isShowing()) {
                JOptionPane.showMessageDialog(this, "Ngày phải có dạng HH:mm dd/MM/yyyy và Từ ngày phải trước Đến ngày.",
                        "Kiểm tra ngày", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        lblStatus.setText("Đang tải phiếu thu từ CSDL...");
        lblTotals.setText("");
        new SwingWorker<List<ReceiptReportRow>, Void>() {
            @Override
            protected List<ReceiptReportRow> doInBackground() {
                return readPort.listReceiptReport(auth.facilityId(), true, 0, from, to, 0);
            }

            @Override
            protected void done() {
                try {
                    loadedRows = get();
                    applyKeywordFilter();
                } catch (Exception ex) {
                    lblStatus.setText("Không thể tải phiếu thu từ CSDL: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void applyKeywordFilter() {
        String kw = txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        model.setRowCount(0);
        int stt = 1;
        BigDecimal sumAmount = BigDecimal.ZERO;
        BigDecimal sumDiscount = BigDecimal.ZERO;
        BigDecimal sumRefund = BigDecimal.ZERO;

        for (ReceiptReportRow r : loadedRows) {
            String hay = (Objects.toString(r.receiptCode(), "") + " "
                    + Objects.toString(r.patientName(), "") + " "
                    + Objects.toString(r.cashierName(), "") + " "
                    + Objects.toString(r.receiptBook(), "")).toLowerCase(Locale.ROOT);
            if (!kw.isBlank() && !hay.contains(kw)) continue;

            BigDecimal amt = safe(r.amount());
            BigDecimal disc = safe(r.discountAmount());
            BigDecimal adv = safe(r.advanceAmount());
            BigDecimal ref = safe(r.refundAmount());
            sumAmount = sumAmount.add(amt);
            sumDiscount = sumDiscount.add(disc);
            sumRefund = sumRefund.add(ref);

            String deptRoom = (Objects.toString(r.department(), "") + " " + Objects.toString(r.room(), "")).trim();
            model.addRow(new Object[]{
                    stt++,
                    r.receiptCode(),
                    r.receiptTime() != null ? r.receiptTime().format(DT_FMT) : "",
                    r.patientName(),
                    r.subjectType(),
                    deptRoom,
                    MONEY_FMT.format(amt),
                    MONEY_FMT.format(disc),
                    MONEY_FMT.format(adv),
                    MONEY_FMT.format(ref),
                    r.paymentMethod(),
                    r.receiptBook(),
                    r.cashierName(),
                    r.closed() ? "Đã chốt" : "Đang mở"
            });
        }

        lblStatus.setText("Số phiếu thu: " + model.getRowCount());
        lblTotals.setText("Tổng thu: " + MONEY_FMT.format(sumAmount) + " đ   |   Miễn giảm: "
                + MONEY_FMT.format(sumDiscount) + " đ   |   Hoàn trả: " + MONEY_FMT.format(sumRefund) + " đ");
    }

    private void printTable() {
        if (GraphicsEnvironment.isHeadless()) return;
        try {
            table.print(JTable.PrintMode.FIT_WIDTH, new MessageFormat("DANH SÁCH PHIẾU THU VIỆN PHÍ"), new MessageFormat("Trang {0}"));
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Không thể in: " + ex.getMessage(), "Lỗi In", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCsv() {
        if (GraphicsEnvironment.isHeadless()) return;
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("danh-sach-phieu-thu.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (var writer = Files.newBufferedWriter(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            for (int row = -1; row < table.getRowCount(); row++) {
                for (int col = 0; col < table.getColumnCount(); col++) {
                    if (col > 0) writer.write(',');
                    String val = Objects.toString(row < 0 ? table.getColumnModel().getColumn(col).getHeaderValue() : table.getValueAt(row, col), "");
                    if (val.startsWith("=") || val.startsWith("+") || val.startsWith("-") || val.startsWith("@")) val = "'" + val;
                    writer.write("\"" + val.replace("\"", "\"\"") + "\"");
                }
                writer.newLine();
            }
            lblStatus.setText("Đã xuất CSV thành công.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không thể xuất CSV: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static JButton actionBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(UngDungPhongChu.BODY_BOLD);
        b.setBackground(new Color(242, 246, 250));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(175, 192, 210)), new EmptyBorder(4, 10, 4, 10)));
        return b;
    }

    private static final class ReceiptCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int r, int c) {
            Component comp = super.getTableCellRendererComponent(tbl, val, sel, foc, r, c);
            setHorizontalAlignment(c == 0 ? SwingConstants.CENTER : (c >= 6 && c <= 9 ? SwingConstants.RIGHT : SwingConstants.LEFT));
            setBorder(new EmptyBorder(0, 6, 0, 6));
            return comp;
        }
    }
}
