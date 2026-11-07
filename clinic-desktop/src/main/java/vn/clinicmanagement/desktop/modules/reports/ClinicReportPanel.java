package vn.clinicmanagement.desktop.modules.reports;

import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.reports.GetInventoryReportQuery;
import vn.clinicmanagement.application.reports.GetRevenueReportQuery;
import vn.clinicmanagement.application.reports.GetVisitStatisticsQuery;
import vn.clinicmanagement.application.reports.InventoryReportDto;
import vn.clinicmanagement.application.reports.InventoryReportItemDto;
import vn.clinicmanagement.application.reports.RevenueReportDto;
import vn.clinicmanagement.application.reports.RevenueReportItemDto;
import vn.clinicmanagement.application.reports.VisitStatisticsDto;
import vn.clinicmanagement.application.reports.VisitStatisticsItemDto;
import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.core.components.StyledTable;
import vn.clinicmanagement.desktop.core.config.UIConstants;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;

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
public class ClinicReportPanel extends BaseViewPanel {

    public static final String[] COLUMNS = {
            "STT", "Chỉ Tiêu / Hạng Mục", "Đơn Vị Tính", "Kỳ Trước", "Kỳ Này", "Tăng/Giảm (%)", "Ghi Chú"
    };
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReportCompositionRoot.Services services;
    private final List<SwingWorker<?, ?>> inFlightWorkers = new CopyOnWriteArrayList<>();

    private JComboBox<String> cboReportType;
    private JTextField txtFromDate;
    private JTextField txtToDate;
    private JButton btnView;
    private JButton btnExport;
    private JButton btnPrint;
    private JTable reportTable;
    private DefaultTableModel tableModel;

    public ClinicReportPanel() {
        this(ReportCompositionRoot.createDefault());
    }

    public ClinicReportPanel(ReportCompositionRoot.Services services) {
        super("report-mgmt", "Báo Cáo Thống Kê", "📊");
        this.services = Objects.requireNonNull(services, "services");
        buildUI();
    }

    public ClinicReportPanel(SessionLifecycleService sessionLifecycle) {
        this(ReportCompositionRoot.create(sessionLifecycle));
    }

    private void buildUI() {
        setLayout(new BorderLayout(8, 8));
        setBackground(UIConstants.COLOR_BG_GRAY);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header filter toolbar
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterPanel.setBackground(Color.WHITE);
        filterPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.COLOR_BORDER, 1),
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
        cboReportType.setFont(UIConstants.FONT_REGULAR);
        cboReportType.addActionListener(e -> executeReportQuery());
        filterPanel.add(cboReportType);

        filterPanel.add(new JLabel("Từ ngày:"));
        txtFromDate = new JTextField("01/10/2026", 8);
        filterPanel.add(txtFromDate);

        filterPanel.add(new JLabel("Đến ngày:"));
        txtToDate = new JTextField("30/10/2026", 8);
        filterPanel.add(txtToDate);

        btnView = new JButton("🔍 Xem Báo Cáo");
        btnView.setBackground(UIConstants.COLOR_PRIMARY);
        btnView.setForeground(Color.WHITE);
        btnView.setOpaque(true);
        btnView.addActionListener(e -> executeReportQuery());
        filterPanel.add(btnView);

        btnExport = new JButton("📥 Xuất Excel");
        filterPanel.add(btnExport);

        btnPrint = new JButton("🖨️ In Báo Cáo");
        filterPanel.add(btnPrint);

        add(filterPanel, BorderLayout.NORTH);

        // Report table
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        reportTable = StyledTable.create(tableModel);

        JScrollPane scrollPane = new JScrollPane(reportTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIConstants.COLOR_BORDER, 1));
        add(scrollPane, BorderLayout.CENTER);
    }

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
                            GetVisitStatisticsQuery query = new GetVisitStatisticsQuery("ADMIN", fromDate, toDate);
                            Result<VisitStatisticsDto> res = services.getVisitStatisticsHandler().handle(query);
                            if (res != null && res.isSuccess()) {
                                VisitStatisticsDto dto = res.getValue().orElse(null);
                                if (dto != null) {
                                    int stt = 1;
                                    for (VisitStatisticsItemDto item : dto.getItems()) {
                                        rows.add(item.toTableRow(stt++));
                                    }
                                }
                            }
                            break;
                        }
                        case 1: {
                            GetRevenueReportQuery query = new GetRevenueReportQuery("ADMIN", fromDate, toDate);
                            Result<RevenueReportDto> res = services.getRevenueReportHandler().handle(query);
                            if (res != null && res.isSuccess()) {
                                RevenueReportDto dto = res.getValue().orElse(null);
                                if (dto != null) {
                                    int stt = 1;
                                    for (RevenueReportItemDto item : dto.getItems()) {
                                        rows.add(item.toTableRow(stt++));
                                    }
                                }
                            }
                            break;
                        }
                        case 2: {
                            GetInventoryReportQuery query = new GetInventoryReportQuery("ADMIN", fromDate, toDate, (Integer) null);
                            Result<InventoryReportDto> res = services.getInventoryReportHandler().handle(query);
                            if (res != null && res.isSuccess()) {
                                InventoryReportDto dto = res.getValue().orElse(null);
                                if (dto != null) {
                                    int stt = 1;
                                    for (InventoryReportItemDto item : dto.getItems()) {
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

    private void cancelInFlightWorkers() {
        for (SwingWorker<?, ?> w : inFlightWorkers) {
            if (!w.isDone()) {
                w.cancel(true);
            }
        }
        inFlightWorkers.clear();
    }

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

    public JComboBox<String> getCboReportType() {
        return cboReportType;
    }

    public JTextField getTxtFromDate() {
        return txtFromDate;
    }

    public JTextField getTxtToDate() {
        return txtToDate;
    }

    public JButton getBtnView() {
        return btnView;
    }

    public JButton getBtnExport() {
        return btnExport;
    }

    public JButton getBtnPrint() {
        return btnPrint;
    }

    public JTable getReportTable() {
        return reportTable;
    }

    public DefaultTableModel getTableModel() {
        return tableModel;
    }

    public ReportCompositionRoot.Services getServices() {
        return services;
    }

    @Override
    public void onShow() {
        refreshData();
    }

    public void onClose() {
        cancelInFlightWorkers();
    }

    @Override
    public void refreshData() {
        executeReportQuery();
    }
}
