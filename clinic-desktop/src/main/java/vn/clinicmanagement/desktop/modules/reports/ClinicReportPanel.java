package vn.clinicmanagement.desktop.modules.reports;

import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.core.components.StyledTable;
import vn.clinicmanagement.desktop.core.config.UIConstants;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Phân hệ Báo Cáo Thống Kê & Phân Tích Dữ Liệu Phòng Khám / Bệnh Viện
 */
public class ClinicReportPanel extends BaseViewPanel {

    private JComboBox<String> cboReportType;
    private JTable reportTable;
    private DefaultTableModel tableModel;

    public ClinicReportPanel() {
        super("report-mgmt", "Báo Cáo Thống Kê", "📊");
        buildUI();
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
        filterPanel.add(cboReportType);

        filterPanel.add(new JLabel("Từ ngày:"));
        JTextField txtFromDate = new JTextField("01/10/2026", 8);
        filterPanel.add(txtFromDate);

        filterPanel.add(new JLabel("Đến ngày:"));
        JTextField txtToDate = new JTextField("30/10/2026", 8);
        filterPanel.add(txtToDate);

        JButton btnView = new JButton("🔍 Xem Báo Cáo");
        btnView.setBackground(UIConstants.COLOR_PRIMARY);
        btnView.setForeground(Color.WHITE);
        btnView.setOpaque(true);
        filterPanel.add(btnView);

        JButton btnExport = new JButton("📥 Xuất Excel");
        filterPanel.add(btnExport);

        JButton btnPrint = new JButton("🖨️ In Báo Cáo");
        filterPanel.add(btnPrint);

        add(filterPanel, BorderLayout.NORTH);

        // Report table
        String[] columns = {"STT", "Chỉ Tiêu / Hạng Mục", "Đơn Vị Tính", "Kỳ Trước", "Kỳ Này", "Tăng/Giảm (%)", "Ghi Chú"};
        Object[][] data = {
                {"1", "Tổng lượt tiếp nhận khám bệnh", "Lượt", "1,240", "1,485", "+19.7%", "Tăng khoa Mắt & RHM"},
                {"2", "Tổng số ca chỉ định cận lâm sàng", "Ca", "850", "1,020", "+20.0%", "Xét nghiệm máu & Siêu âm"},
                {"3", "Tổng đơn thuốc phát hành", "Đơn", "1,100", "1,310", "+19.1%", "FEFO kho chẵn"},
                {"4", "Doanh thu dịch vụ khám bệnh", "VNĐ", "248,000,000", "297,000,000", "+19.7%", "Đã thanh toán 100%"},
                {"5", "Doanh thu thuốc & vật tư y tế", "VNĐ", "415,000,000", "512,000,000", "+23.3%", "Tồn kho an toàn"},
                {"6", "Tổng doanh thu viện phí", "VNĐ", "663,000,000", "809,000,000", "+22.0%", "Vượt chỉ tiêu tháng"}
        };

        tableModel = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        reportTable = StyledTable.create(tableModel);

        JScrollPane scrollPane = new JScrollPane(reportTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIConstants.COLOR_BORDER, 1));
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void onShow() {
        refreshData();
    }

    @Override
    public void refreshData() {
        // Ready for service integration
    }
}
