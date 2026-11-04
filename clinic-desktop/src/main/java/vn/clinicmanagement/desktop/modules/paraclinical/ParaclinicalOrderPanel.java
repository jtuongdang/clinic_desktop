package vn.clinicmanagement.desktop.modules.paraclinical;

import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.core.components.StyledTable;
import vn.clinicmanagement.desktop.core.config.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class ParaclinicalOrderPanel extends BaseViewPanel {
    private DefaultTableModel tableModel;

    public ParaclinicalOrderPanel() {
        super("SERVICES", "CĐHA & Thăm Dò", "🔬");
        initUI();
    }

    private void initUI() {
        setBackground(UIConstants.BG_PANEL);
        setBorder(new EmptyBorder(15, 18, 15, 18));

        JLabel title = new JLabel("🔬 CHẨN ĐOÁN HÌNH ẢNH & THĂM DÒ CHỨC NĂNG");
        title.setFont(UIConstants.FONT_SECTION);
        title.setForeground(new Color(24, 85, 130));
        add(title, BorderLayout.NORTH);

        String[] cols = {"Mã Chỉ Định", "Bệnh Nhân", "Dịch Vụ Cận Lâm Sàng", "Bác Sĩ Yêu Cầu", "Trạng Thái"};
        tableModel = new DefaultTableModel(cols, 0);
        StyledTable table = new StyledTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshData();
    }

    @Override
    public void refreshData() {
        tableModel.setRowCount(0);
        tableModel.addRow(new Object[]{"CLS-101", "Nguyễn Văn An", "Nội soi Tai Mũi Họng ống mềm", "BS. Nguyễn Văn Hùng", "Đang chờ kết quả"});
    }
}
