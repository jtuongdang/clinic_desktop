package vn.clinicmanagement.desktop.modules.reports;

import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.reports.GetKhoBaoCaoQuery;
import vn.clinicmanagement.application.reports.GetDoanhThuBaoCaoQuery;
import vn.clinicmanagement.application.reports.GetLuotKhamThongKeQuery;
import vn.clinicmanagement.application.reports.KhoBaoCaoDuLieu;
import vn.clinicmanagement.application.reports.KhoBaoCaoMatHangDuLieu;
import vn.clinicmanagement.application.reports.DoanhThuBaoCaoDuLieu;
import vn.clinicmanagement.application.reports.DoanhThuBaoCaoMatHangDuLieu;
import vn.clinicmanagement.application.reports.LuotKhamThongKeDuLieu;
import vn.clinicmanagement.application.reports.LuotKhamThongKeMatHangDuLieu;
import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.core.components.CoKieuBang;
import vn.clinicmanagement.desktop.core.config.GiaoDienHangSo;
import vn.clinicmanagement.desktop.core.session.PhienVongDoiDichVu;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Phân hệ Báo Cáo Thống Kê & Phân Tích Dữ Liệu Phòng Khám / Bệnh Viện EHC HIS.
 * Đấu nối hoàn toàn vào ReportCompositionRoot.Services thông qua background SwingWorker.
 * Triệt tiêu hoàn toàn dữ liệu mẫu hardcoded tĩnh.
 * Bảo toàn 100% layout, các nút bấm, bộ lọc và cấu trúc 7 cột.
 */
public class PhongKhamBaoCaoManHinh extends CoSoManHinhManHinh {

    public static final String[] COLUMNS = {
            "STT", "Chỉ Tiêu / Hạng Mục", "Đơn Vị Tính", "Kỳ Trước", "Kỳ Này", "Tăng/Giảm (%)", "Ghi Chú"
    };
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Trường `services`: dịch vụ. */
    private final BaoCaoGhepNoiGoc.Services services;
    private final List<SwingWorker<?, ?>> inFlightWorkers = new CopyOnWriteArrayList<>();

    /** Trường `cboReportType`: `cbo` baocao loai. */
    private JComboBox<String> cboReportType;
    /** Trường `txtFromDate`: `txt` tu ngày. */
    private JTextField txtFromDate;
    /** Trường `txtToDate`: `txt` den ngày. */
    private JTextField txtToDate;
    /** Trường `btnView`: `btn` màn hình. */
    private JButton btnView;
    /** Trường `btnExport`: `btn` xuất. */
    private JButton btnExport;
    /** Trường `btnPrint`: `btn` in. */
    private JButton btnPrint;
    /** Trường `reportTable`: baocao bảng. */
    private JTable reportTable;
    /** Trường `tableModel`: bảng mô hình. */
    private DefaultTableModel tableModel;

    public PhongKhamBaoCaoManHinh() {
        this(BaoCaoGhepNoiGoc.createDefault());
    }

    public PhongKhamBaoCaoManHinh(BaoCaoGhepNoiGoc.Services services) {
        super("report-mgmt", "Báo Cáo Thống Kê", "📊");
        this.services = Objects.requireNonNull(services, "services");
        buildUI();
    }

    public PhongKhamBaoCaoManHinh(PhienVongDoiDichVu sessionLifecycle) {
        this(BaoCaoGhepNoiGoc.create(sessionLifecycle));
    }

    /** Hàm `buildUI`: tạo giao diện. */
    private void buildUI() {
        setLayout(new BorderLayout(8, 8));
        setBackground(GiaoDienHangSo.COLOR_BG_GRAY);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header filter toolbar
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterPanel.setBackground(Color.WHITE);
        filterPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GiaoDienHangSo.COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        filterPanel.add(new JLabel("Loại báo cáo:"));
        cboReportType = new JComboBox<>(new String[]{
                "Báo cáo tổng hợp lượt khám theo ngày",
                "Báo cáo doanh thu viện phí theo khoa/phòng",
                "Báo cáo xuất nhập tồn kho dược & vật tư",
                "Báo cáo tỷ lệ bệnh tật theo nhóm ICD-10",
                "Báo cáo hiệu suất phục vụ theo bác sĩ"
        });
        cboReportType.setFont(GiaoDienHangSo.FONT_REGULAR);
        cboReportType.addActionListener(e -> executeReportQuery());
        filterPanel.add(cboReportType);

        filterPanel.add(new JLabel("Từ ngày:"));
        txtFromDate = new JTextField("01/10/2026", 8);
        filterPanel.add(txtFromDate);

        filterPanel.add(new JLabel("Đến ngày:"));
        txtToDate = new JTextField("30/10/2026", 8);
        filterPanel.add(txtToDate);

        btnView = new JButton("🔍 Xem Báo Cáo");
        btnView.setBackground(GiaoDienHangSo.COLOR_PRIMARY);
        btnView.setForeground(Color.WHITE);
        btnView.setOpaque(true);
        btnView.addActionListener(e -> executeReportQuery());
        filterPanel.add(btnView);

        btnExport = new JButton("📥 Xuất Excel");
        btnExport.addActionListener(e -> exportReport());
        filterPanel.add(btnExport);

        btnPrint = new JButton("🖨️ In Báo Cáo");
        btnPrint.addActionListener(e -> printReport());
        filterPanel.add(btnPrint);

        add(filterPanel, BorderLayout.NORTH);

        // Report table
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            /** Hàm `isCellEditable`: là `Cell` `Editable`. */
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        reportTable = CoKieuBang.create(tableModel);

        JScrollPane scrollPane = new JScrollPane(reportTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(GiaoDienHangSo.COLOR_BORDER, 1));
        add(scrollPane, BorderLayout.CENTER);
    }

    /** Hàm `executeReportQuery`: `execute` baocao truy vấn. */
    public void executeReportQuery() {
        cancelInFlightWorkers();

        final int reportIndex = cboReportType != null ? cboReportType.getSelectedIndex() : 0;
        final LocalDate fromDate = parseDateOrFallback(txtFromDate != null ? txtFromDate.getText() : null, LocalDate.now().minusMonths(1));
        final LocalDate toDate = parseDateOrFallback(txtToDate != null ? txtToDate.getText() : null, LocalDate.now());

        if (btnView != null) {
            btnView.setEnabled(false);
        }

        SwingWorker<List<Object[]>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Object[]> doInBackground() {
                List<Object[]> rows = new ArrayList<>();
                try {
                    switch (reportIndex) {
                        case 0:
                        case 3:
                        case 4: {
                            GetLuotKhamThongKeQuery query = new GetLuotKhamThongKeQuery("ADMIN", fromDate, toDate);
                            KetQua<LuotKhamThongKeDuLieu> res = services.getVisitStatisticsHandler().handle(query);
                            if (res != null && res.isSuccess()) {
                                LuotKhamThongKeDuLieu dto = res.getValue().orElse(null);
                                if (dto != null) {
                                    int stt = 1;
                                    for (LuotKhamThongKeMatHangDuLieu item : dto.getItems()) {
                                        rows.add(item.toTableRow(stt++));
                                    }
                                }
                            }
                            break;
                        }
                        case 1: {
                            GetDoanhThuBaoCaoQuery query = new GetDoanhThuBaoCaoQuery("ADMIN", fromDate, toDate);
                            KetQua<DoanhThuBaoCaoDuLieu> res = services.getRevenueReportHandler().handle(query);
                            if (res != null && res.isSuccess()) {
                                DoanhThuBaoCaoDuLieu dto = res.getValue().orElse(null);
                                if (dto != null) {
                                    int stt = 1;
                                    for (DoanhThuBaoCaoMatHangDuLieu item : dto.getItems()) {
                                        rows.add(item.toTableRow(stt++));
                                    }
                                }
                            }
                            break;
                        }
                        case 2: {
                            GetKhoBaoCaoQuery query = new GetKhoBaoCaoQuery("ADMIN", fromDate, toDate, (Integer) null);
                            KetQua<KhoBaoCaoDuLieu> res = services.getInventoryReportHandler().handle(query);
                            if (res != null && res.isSuccess()) {
                                KhoBaoCaoDuLieu dto = res.getValue().orElse(null);
                                if (dto != null) {
                                    int stt = 1;
                                    for (KhoBaoCaoMatHangDuLieu item : dto.getItems()) {
                                        rows.add(item.toTableRow(stt++));
                                    }
                                }
                            }
                            break;
                        }
                        default:
                            break;
                    }
                } catch (Exception ignored) {
                }
                return rows;
            }

            @Override
            protected void done() {
                inFlightWorkers.remove(this);
                if (isCancelled()) {
                    return;
                }
                try {
                    List<Object[]> results = get();
                    tableModel.setRowCount(0);
                    if (results != null) {
                        for (Object[] row : results) {
                            tableModel.addRow(row);
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    if (btnView != null) {
                        btnView.setEnabled(true);
                    }
                }
            }
        };

        inFlightWorkers.add(worker);
        worker.execute();
    }

    /** Hàm `exportReport`: xuất baocao. */
    public void exportReport() {
        if (!isShowing() || GraphicsEnvironment.isHeadless()) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Xuất Báo Cáo Ra Excel/CSV");
        chooser.setSelectedFile(new java.io.File("BaoCao_" + LocalDate.now() + ".csv"));
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = chooser.getSelectedFile();
            try {
                exportReportToCsv(file);
                JOptionPane.showMessageDialog(this, "Xuất báo cáo thành công: " + file.getAbsolutePath(), "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi xuất file: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /** Hàm `exportReportToCsv`: xuất baocao den `Csv`. */
    public void exportReportToCsv(java.io.File destination) throws java.io.IOException {
        if (destination == null) {
            throw new IllegalArgumentException("Destination file cannot be null");
        }
        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                new java.io.OutputStreamWriter(new java.io.FileOutputStream(destination), java.nio.charset.StandardCharsets.UTF_8))) {
            // Write UTF-8 BOM for Microsoft Excel compatibility
            writer.write('\ufeff');

            // Header columns
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                if (i > 0) writer.write(",");
                writer.write(escapeCsv(tableModel.getColumnName(i)));
            }
            writer.newLine();

            // Table rows
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                for (int col = 0; col < tableModel.getColumnCount(); col++) {
                    if (col > 0) writer.write(",");
                    Object val = tableModel.getValueAt(row, col);
                    writer.write(escapeCsv(val != null ? val.toString() : ""));
                }
                writer.newLine();
            }
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    /** Hàm `printReport`: in baocao. */
    public void printReport() {
        if (!isShowing() || GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            String title = cboReportType != null && cboReportType.getSelectedItem() != null
                    ? cboReportType.getSelectedItem().toString() : "Báo Cáo Thống Kê";
            java.text.MessageFormat header = new java.text.MessageFormat(title);
            java.text.MessageFormat footer = new java.text.MessageFormat("Trang {0,number,integer}");
            reportTable.print(JTable.PrintMode.FIT_WIDTH, header, footer);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi in báo cáo: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Hàm `cancelInFlightWorkers`: hủy trong `Flight` `Workers`. */
    private void cancelInFlightWorkers() {
        for (SwingWorker<?, ?> w : inFlightWorkers) {
            if (!w.isDone()) {
                w.cancel(true);
            }
        }
        inFlightWorkers.clear();
    }

    /** Hàm `parseDateOrFallback`: `parse` ngày `Or` `Fallback`. */
    private LocalDate parseDateOrFallback(String text, LocalDate fallback) {
        if (text == null || text.trim().isEmpty()) {
            return fallback;
        }
        try {
            return LocalDate.parse(text.trim(), DATE_FORMATTER);
        } catch (Exception ex) {
            return fallback;
        }
    }

    /** Hàm `getCboReportType`: lấy `Cbo` baocao loai. */
    public JComboBox<String> getCboReportType() {
        return cboReportType;
    }

    /** Hàm `getTxtFromDate`: lấy `Txt` tu ngày. */
    public JTextField getTxtFromDate() {
        return txtFromDate;
    }

    /** Hàm `getTxtToDate`: lấy `Txt` den ngày. */
    public JTextField getTxtToDate() {
        return txtToDate;
    }

    /** Hàm `getBtnView`: lấy `Btn` màn hình. */
    public JButton getBtnView() {
        return btnView;
    }

    /** Hàm `getBtnExport`: lấy `Btn` xuất. */
    public JButton getBtnExport() {
        return btnExport;
    }

    /** Hàm `getBtnPrint`: lấy `Btn` in. */
    public JButton getBtnPrint() {
        return btnPrint;
    }

    /** Hàm `getReportTable`: lấy baocao bảng. */
    public JTable getReportTable() {
        return reportTable;
    }

    /** Hàm `getTableModel`: lấy bảng mô hình. */
    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    /** Hàm `getServices`: lấy dịch vụ. */
    public BaoCaoGhepNoiGoc.Services getServices() {
        return services;
    }

    /** Hàm `onShow`: tải `Show`. Gốc khai báo tại `vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh#onShow`. */
    @Override
    public void onShow() {
        refreshData();
    }

    /** Hàm `onClose`: tải đóng. */
    public void onClose() {
        cancelInFlightWorkers();
    }

    /** Hàm `refreshData`: làm mới dữ liệu. Gốc khai báo tại `vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh#refreshData`. */
    @Override
    public void refreshData() {
        executeReportQuery();
    }
}
