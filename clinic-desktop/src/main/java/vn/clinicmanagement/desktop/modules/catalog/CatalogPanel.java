package vn.clinicmanagement.desktop.modules.catalog;

import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.core.components.StyledTable;
import vn.clinicmanagement.desktop.core.config.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class CatalogPanel extends BaseViewPanel {
    private DefaultTableModel tableModel;

    public CatalogPanel() {
        super("CATALOG", "Quản Lý Danh Mục", "📑");
        initUI();
    }

    private void initUI() {
        setBackground(UIConstants.BG_PANEL);
        setBorder(new EmptyBorder(15, 18, 15, 18));

        JLabel title = new JLabel("📑 QUẢN LÝ DANH MỤC DỊCH VỤ & THUỐC (ICD-10)");
        title.setFont(UIConstants.FONT_SECTION);
        title.setForeground(new Color(24, 85, 130));
        add(title, BorderLayout.NORTH);

        String[] cols = {"Mã ICD-10", "Tên Bệnh Theo BYT", "Chương Bệnh"};
        tableModel = new DefaultTableModel(cols, 0);
        StyledTable table = new StyledTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        refreshData();
    }

    @Override
    public void refreshData() {
        tableModel.setRowCount(0);
        tableModel.addRow(new Object[]{"J00", "Viêm mũi họng cấp [cảm thường]", "Bệnh hệ hô hấp"});
        tableModel.addRow(new Object[]{"J01", "Viêm xoang cấp", "Bệnh hệ hô hấp"});
        tableModel.addRow(new Object[]{"J03", "Viêm amiđan cấp", "Bệnh hệ hô hấp"});
        tableModel.addRow(new Object[]{"H66", "Viêm tai giữa mủ và không đặc hiệu", "Bệnh về tai và xương chũm"});
    }
}
