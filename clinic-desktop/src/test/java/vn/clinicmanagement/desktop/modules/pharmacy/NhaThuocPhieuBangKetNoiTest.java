package vn.clinicmanagement.desktop.modules.pharmacy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocDanhMucCayManHinh;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocDanhMucCayManHinh.CategoryFilter;
import vn.clinicmanagement.desktop.modules.pharmacy.components.NhaThuocPhieuBangManHinh;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.prescription.DonThuoc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PharmacyTicketTable & Integration Wiring Tests (HD-INTEG-02)")
class NhaThuocPhieuBangKetNoiTest {

    @Test
    @DisplayName("PharmacyTicketTablePanel initializes cleanly with default and injected services")
    void tablePanelInitializesCleanly() {
        NhaThuocPhieuBangManHinh defaultPanel = new NhaThuocPhieuBangManHinh();
        assertNotNull(defaultPanel, "Default table panel must not be null");
        assertNotNull(defaultPanel.getServices(), "Services must be initialized via fallback");

        NhaThuocGhepNoiGoc.Services customServices = NhaThuocGhepNoiGoc.createDefault();
        NhaThuocPhieuBangManHinh panel = new NhaThuocPhieuBangManHinh(customServices);
        assertNotNull(panel, "Custom services table panel must not be null");
        assertSame(customServices, panel.getServices(), "Services must match injected instance");

        assertNotNull(panel.getTable(), "JTable must be initialized");
        assertNotNull(panel.getTableModel(), "TableModel must be initialized");
        assertNotNull(panel.getTxtSearch(), "Search field must be initialized");
    }

    @Test
    @DisplayName("PharmacyTicketTablePanel filters by CategoryFilter properly")
    void tablePanelFiltersByCategory() {
        NhaThuocGhepNoiGoc.Services customServices = NhaThuocGhepNoiGoc.createDefault();

        // Seed a prescription in repository
        DonThuoc dt = new DonThuoc();
        dt.setId(10);
        dt.setMaLuotKham(100);
        dt.setMaBacSi(1);
        dt.setTrangThai(TrangThaiDonThuoc.DA_DUYET);
        customServices.prescriptionRepository().add(dt);

        NhaThuocPhieuBangManHinh panel = new NhaThuocPhieuBangManHinh(customServices);
        NhaThuocDanhMucCayManHinh treePanel = new NhaThuocDanhMucCayManHinh();
        panel.setCategoryTreePanel(treePanel);

        assertDoesNotThrow(() -> panel.loadData());

        // Test filtering
        assertDoesNotThrow(() -> panel.applyFilter(new CategoryFilter(null, CategoryFilter.NHAP_KHO)));
        assertEquals(CategoryFilter.NHAP_KHO, panel.getCurrentFilter().ticketType());

        assertDoesNotThrow(() -> panel.applyFilter(new CategoryFilter(null, CategoryFilter.DON_THUOC)));
        assertEquals(CategoryFilter.DON_THUOC, panel.getCurrentFilter().ticketType());

        assertDoesNotThrow(() -> panel.applyFilter(new CategoryFilter(null, CategoryFilter.ALL)));
        assertEquals(CategoryFilter.ALL, panel.getCurrentFilter().ticketType());
    }

    @Test
    @DisplayName("PharmacyTicketTablePanel handles prescription dispensing safely")
    void tablePanelHandlesDispensingSafely() {
        NhaThuocGhepNoiGoc.Services customServices = NhaThuocGhepNoiGoc.createDefault();
        NhaThuocPhieuBangManHinh panel = new NhaThuocPhieuBangManHinh(customServices);

        // Validation guard when no row is selected
        assertDoesNotThrow(() -> panel.dispensePrescription(null));

        // When a non-prescription row is passed
        NhaThuocPhieuBangManHinh.TicketRow nonRxRow = new NhaThuocPhieuBangManHinh.TicketRow(
                1, "NHAP_KHO", "Đã nhập kho", "10:00", "2021-09-14", "PNK01", "", "Nhập kho",
                "Nội bộ", "Khoa Dược", "Kho Tổng", 1, null, false
        );
        assertDoesNotThrow(() -> panel.dispensePrescription(nonRxRow));
    }
}
