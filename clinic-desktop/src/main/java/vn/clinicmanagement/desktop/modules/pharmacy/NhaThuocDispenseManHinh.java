package vn.clinicmanagement.desktop.modules.pharmacy;

import vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocDanhMucCayManHinh;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocPhuThanhCongCu;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocPhieuBangManHinh;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Phân hệ Quản lý Nhà Thuốc / Tủ Trực Vật Tư (Khớp 100% ảnh thiết kế EHC HIS).
 * Kế thừa BaseViewPanel để tích hợp vòng đời MDI Tab chuẩn hệ thống.
 * Đấu nối Clean Architecture qua PharmacyCompositionRoot.Services.
 */
public class NhaThuocDispenseManHinh extends CoSoManHinhManHinh {

    /** Trường `services`: dịch vụ. */
    private final NhaThuocGhepNoiGoc.Services services;
    /** Trường `subToolbar`: phu thanhcongcu. */
    private final NhaThuocPhuThanhCongCu subToolbar;
    /** Trường `treePanel`: cay màn hình. */
    private final NhaThuocDanhMucCayManHinh treePanel;
    /** Trường `tablePanel`: bảng màn hình. */
    private final NhaThuocPhieuBangManHinh tablePanel;

    public NhaThuocDispenseManHinh() {
        this(NhaThuocGhepNoiGoc.createDefault());
    }

    public NhaThuocDispenseManHinh(NhaThuocGhepNoiGoc.Services services) {
        super("pharmacy", "Tủ Trực Vật Tư", "💊");
        this.services = Objects.requireNonNull(services, "services");
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 1. Thanh công cụ phụ (Sub-Toolbar) trên đầu
        subToolbar = new NhaThuocPhuThanhCongCu();
        add(subToolbar, BorderLayout.NORTH);

        // 2. Khu vực chính: Chia 2 cột (Cây phân loại bên trái + Bảng dữ liệu phiếu bên phải)
        treePanel = new NhaThuocDanhMucCayManHinh();
        tablePanel = new NhaThuocPhieuBangManHinh(services);
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

    /** Hàm `getServices`: lấy dịch vụ. */
    public NhaThuocGhepNoiGoc.Services getServices() {
        return services;
    }

    /** Hàm `getSubToolbar`: lấy phu thanhcongcu. */
    public NhaThuocPhuThanhCongCu getSubToolbar() {
        return subToolbar;
    }

    /** Hàm `getTreePanel`: lấy cay màn hình. */
    public NhaThuocDanhMucCayManHinh getTreePanel() {
        return treePanel;
    }

    /** Hàm `getTablePanel`: lấy bảng màn hình. */
    public NhaThuocPhieuBangManHinh getTablePanel() {
        return tablePanel;
    }

    /** Hàm `onShow`: tải `Show`. Gốc khai báo tại `vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh#onShow`. */
    @Override
    public void onShow() {
        refreshData();
    }

    /** Hàm `refreshData`: làm mới dữ liệu. Gốc khai báo tại `vn.clinicmanagement.desktop.core.base.CoSoManHinhManHinh#refreshData`. */
    @Override
    public void refreshData() {
        if (tablePanel != null) {
            tablePanel.loadData();
        }
    }
}
