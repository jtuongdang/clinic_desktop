package vn.clinicmanagement.desktop.modules.pharmacy.components;

import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort;
import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.BaoCaoKetQua;
import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.LoaiBaoCao;
import vn.clinicmanagement.application.inventory.NhaThuocBaoCaoReadPort.XuatNhapTonOptions;
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
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Màn hình xem trước, in và xuất CSV cho các báo cáo phân hệ Nhà Thuốc. */
public final class NhaThuocBaoCaoHopThoai extends JPanel {

    private static final DateTimeFormatter RANGE_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "PHARMACIST", "WAREHOUSE_MANAGER");

    private final NhaThuocBaoCaoReadPort reportRead;
    private final LoaiBaoCao reportType;
    private final Integer warehouseId;
    private final LocalDateTime from;
    private final LocalDateTime to;
    private final XuatNhapTonOptions xuatNhapTonOptions;

    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel lblSummary;
    private final JLabel lblStatus;

    public NhaThuocBaoCaoHopThoai(
            NhaThuocBaoCaoReadPort reportRead,
            LoaiBaoCao reportType,
            Integer warehouseId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        this(reportRead, reportType, warehouseId, from, to, null);
    }

    public NhaThuocBaoCaoHopThoai(
            NhaThuocBaoCaoReadPort reportRead,
            LoaiBaoCao reportType,
            Integer warehouseId,
            LocalDateTime from,
            LocalDateTime to,
            XuatNhapTonOptions xuatNhapTonOptions
    ) {
        this.reportRead = reportRead;
        this.reportType = Objects.requireNonNull(reportType, "reportType");
        this.warehouseId = warehouseId;
        this.from = Objects.requireNonNull(from, "from");
        this.to = Objects.requireNonNull(to, "to");
        this.xuatNhapTonOptions = xuatNhapTonOptions;

        setLayout(new BorderLayout(0, 8));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 12, 10, 12));
        setPreferredSize(new Dimension(1120, 640));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout(0, 4));
        headerPanel.setOpaque(false);

        JLabel lblTitle = new JLabel(reportType.title().toUpperCase(Locale.ROOT));
        lblTitle.setFont(UngDungPhongChu.BODY_BOLD.deriveFont(16f));
        lblTitle.setForeground(new Color(10, 86, 179));

        JLabel lblRange = new JLabel("Thời gian: Từ " + from.format(RANGE_FMT) + " đến " + to.format(RANGE_FMT));
        lblRange.setFont(UngDungPhongChu.BODY);
        lblRange.setForeground(new Color(65, 80, 96));

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);
        titleBox.add(lblTitle);
        titleBox.add(lblRange);
        headerPanel.add(titleBox, BorderLayout.WEST);

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionBar.setOpaque(false);
        JButton btnReload = createActionButton("Tải lại");
        JButton btnPrint = createActionButton("In Báo Cáo");
        JButton btnExport = createActionButton("Xuất CSV");
        btnReload.addActionListener(e -> loadData());
        btnPrint.addActionListener(e -> printReport());
        btnExport.addActionListener(e -> exportCsv());
        actionBar.add(btnReload);
        actionBar.add(btnPrint);
        actionBar.add(btnExport);
        headerPanel.add(actionBar, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Table
        model = new DefaultTableModel() {
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
        table.setDefaultRenderer(Object.class, new NumberAwareRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(new Color(189, 202, 215)));
        add(scroll, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new BorderLayout(8, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(4, 2, 0, 2));
        lblStatus = new JLabel("Đang tải dữ liệu...");
        lblStatus.setFont(UngDungPhongChu.BODY);
        lblStatus.setForeground(new Color(75, 90, 106));
        lblSummary = new JLabel("");
        lblSummary.setFont(UngDungPhongChu.BODY_BOLD);
        lblSummary.setForeground(new Color(10, 86, 179));
        footer.add(lblStatus, BorderLayout.WEST);
        footer.add(lblSummary, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        loadData();
    }

    public JTable getTable() {
        return table;
    }

    public JLabel getLblSummary() {
        return lblSummary;
    }

    public void loadData() {
        var auth = KhamBenhPhienNguCanh.current();
        if (reportRead == null || auth == null || !ALLOWED_ROLES.contains(auth.roleCode())) {
            lblStatus.setText("Cần phiên đăng nhập có quyền kho để tải dữ liệu báo cáo.");
            return;
        }
        lblStatus.setText("Đang tải dữ liệu từ CSDL...");
        lblSummary.setText("");
        new SwingWorker<BaoCaoKetQua, Void>() {
            @Override
            protected BaoCaoKetQua doInBackground() {
                if (reportType == LoaiBaoCao.XUAT_NHAP_TON && xuatNhapTonOptions != null) {
                    return reportRead.getXuatNhapTonReport(auth.facilityId(), warehouseId, from, to, xuatNhapTonOptions);
                }
                return reportRead.getReport(reportType, auth.facilityId(), warehouseId, from, to);
            }

            @Override
            protected void done() {
                var current = KhamBenhPhienNguCanh.current();
                if (current == null || !current.sessionId().equals(auth.sessionId())) {
                    lblStatus.setText("Phiên làm việc đã thay đổi.");
                    return;
                }
                try {
                    BaoCaoKetQua result = get();
                    model.setRowCount(0);
                    model.setColumnIdentifiers(result.columns().toArray());
                    for (var row : result.rows()) {
                        model.addRow(row.toArray());
                    }
                    lblStatus.setText("Đã tải xong (" + result.rows().size() + " dòng).");
                    lblSummary.setText(result.summaryText());
                } catch (Exception ex) {
                    lblStatus.setText("Không thể tải dữ liệu báo cáo: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void printReport() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            boolean done = table.print(
                    JTable.PrintMode.FIT_WIDTH,
                    new MessageFormat(reportType.title()),
                    new MessageFormat("Trang {0}")
            );
            lblStatus.setText(done ? "Đã gửi lệnh in." : "Đã hủy lệnh in.");
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Không thể in báo cáo: " + ex.getMessage(), "Lỗi In", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportCsv() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(reportType.name().toLowerCase(Locale.ROOT) + ".csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try (var writer = Files.newBufferedWriter(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8)) {
            writer.write('\ufeff');
            for (int row = -1; row < table.getRowCount(); row++) {
                for (int col = 0; col < table.getColumnCount(); col++) {
                    if (col > 0) {
                        writer.write(',');
                    }
                    String value = Objects.toString(
                            row < 0 ? table.getColumnModel().getColumn(col).getHeaderValue() : table.getValueAt(row, col),
                            ""
                    );
                    if (value.startsWith("=") || value.startsWith("+") || value.startsWith("-") || value.startsWith("@")) {
                        value = "'" + value;
                    }
                    writer.write("\"" + value.replace("\"", "\"\"") + "\"");
                }
                writer.newLine();
            }
            lblStatus.setText("Đã xuất CSV thành công.");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không thể xuất CSV: " + ex.getMessage(), "Lỗi Xuất File", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static JButton createActionButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(UngDungPhongChu.BODY_BOLD);
        btn.setBackground(new Color(242, 246, 250));
        btn.setForeground(new Color(20, 40, 68));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(175, 192, 210)),
                new EmptyBorder(4, 10, 4, 10)
        ));
        return btn;
    }

    private static final class NumberAwareRenderer extends DefaultTableCellRenderer {
        private final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (value instanceof BigDecimal || value instanceof Number) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                setText(numberFormat.format(value));
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }
            setBorder(new EmptyBorder(0, 6, 0, 6));
            return comp;
        }
    }
}
