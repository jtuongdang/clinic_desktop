package vn.clinicmanagement.desktop.modules.pharmacy;

import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.core.config.UIConstants;
import vn.clinicmanagement.desktop.modules.pharmacy.components.PharmacyCategoryTreePanel;
import vn.clinicmanagement.desktop.modules.pharmacy.components.PharmacySubToolbar;
import vn.clinicmanagement.desktop.modules.pharmacy.components.PharmacyTicketTablePanel;

import javax.swing.*;
import java.awt.*;

/**
 * Phân hệ Quản lý Nhà Thuốc / Tủ Trực Vật Tư (Khớp 100% ảnh thiết kế EHC HIS).
 * Kế thừa BaseViewPanel để tích hợp vòng đời MDI Tab chuẩn hệ thống.
 */
public class PharmacyDispensePanel extends BaseViewPanel {

    private final PharmacySubToolbar subToolbar;
    private final PharmacyCategoryTreePanel treePanel;
    private final PharmacyTicketTablePanel tablePanel;

    public PharmacyDispensePanel() {
        super("pharmacy", "Tủ Trực Vật Tư", "💊");
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. Thanh công cụ phụ (Sub-Toolbar) trên đầu
        subToolbar = new PharmacySubToolbar();
        add(subToolbar, BorderLayout.NORTH);

        // 2. Khu vực chính: Chia 2 cột (Cây phân loại bên trái + Bảng dữ liệu phiếu bên phải)
        treePanel = new PharmacyCategoryTreePanel();
        tablePanel = new PharmacyTicketTablePanel();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, tablePanel);
        splitPane.setDividerLocation(215);
        splitPane.setDividerSize(4);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        add(splitPane, BorderLayout.CENTER);
    }

    @Override
    public void onShow() {
        refreshData();
    }

    @Override
    public void refreshData() {
        if (tablePanel != null) {
            tablePanel.loadSampleData();
        }
    }
}
