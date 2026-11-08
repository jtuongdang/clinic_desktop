package vn.clinicmanagement.desktop.modules.pharmacy;

import vn.clinicmanagement.desktop.core.base.BaseViewPanel;
import vn.clinicmanagement.desktop.modules.pharmacy.components.PharmacyCategoryTreePanel;
import vn.clinicmanagement.desktop.modules.pharmacy.components.PharmacySubToolbar;
import vn.clinicmanagement.desktop.modules.pharmacy.components.PharmacyTicketTablePanel;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Phân hệ Quản lý Nhà Thuốc / Tủ Trực Vật Tư (Khớp 100% ảnh thiết kế EHC HIS).
 * Kế thừa BaseViewPanel để tích hợp vòng đời MDI Tab chuẩn hệ thống.
 * Đấu nối Clean Architecture qua PharmacyCompositionRoot.Services.
 */
public class PharmacyDispensePanel extends BaseViewPanel {

    private final PharmacyCompositionRoot.Services services;
    private final PharmacySubToolbar subToolbar;
    private final PharmacyCategoryTreePanel treePanel;
    private final PharmacyTicketTablePanel tablePanel;

    public PharmacyDispensePanel() {
        this(PharmacyCompositionRoot.createDefault());
    }

    public PharmacyDispensePanel(PharmacyCompositionRoot.Services services) {
        super("pharmacy", "Tủ Trực Vật Tư", "💊");
        this.services = Objects.requireNonNull(services, "services");
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. Thanh công cụ phụ (Sub-Toolbar) trên đầu
        subToolbar = new PharmacySubToolbar();
        add(subToolbar, BorderLayout.NORTH);

        // 2. Khu vực chính: Chia 2 cột (Cây phân loại bên trái + Bảng dữ liệu phiếu bên phải)
        treePanel = new PharmacyCategoryTreePanel();
        tablePanel = new PharmacyTicketTablePanel(services);
        tablePanel.setCategoryTreePanel(treePanel);

        // Đấu nối callback bộ lọc từ cây danh mục sang bảng phiếu
        treePanel.setOnCategorySelected(tablePanel::applyFilter);

        // Đấu nối thanh công cụ phụ sang bảng dữ liệu
        subToolbar.setOnSearchListener(tablePanel::filterByTicketCode);
        subToolbar.getBtnList().addActionListener(e -> {
            tablePanel.filterByTicketCode("");
            tablePanel.loadData();
        });
        subToolbar.getBtnReport().addActionListener(e -> tablePanel.loadData());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treePanel, tablePanel);
        splitPane.setDividerLocation(215);
        splitPane.setDividerSize(4);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        add(splitPane, BorderLayout.CENTER);
    }

    public PharmacyCompositionRoot.Services getServices() {
        return services;
    }

    public PharmacySubToolbar getSubToolbar() {
        return subToolbar;
    }

    public PharmacyCategoryTreePanel getTreePanel() {
        return treePanel;
    }

    public PharmacyTicketTablePanel getTablePanel() {
        return tablePanel;
    }

    @Override
    public void onShow() {
        refreshData();
    }

    @Override
    public void refreshData() {
        if (tablePanel != null) {
            tablePanel.loadData();
        }
    }
}
