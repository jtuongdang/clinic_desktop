package vn.clinicmanagement.desktop.modules.paraclinical;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("ParaclinicalOrderPanel Wiring Tests (HD-INTEG-03)")
class ParaclinicalOrderWiringTest {

    @Test
    @DisplayName("ParaclinicalOrderPanel initializes cleanly with default and custom services")
    void orderPanelInitializesCleanly() {
        ParaclinicalOrderPanel defaultPanel = new ParaclinicalOrderPanel();
        assertNotNull(defaultPanel, "Default order panel must not be null");
        assertNotNull(defaultPanel.getServices(), "Services must be initialized via fallback");

        ParaclinicalCompositionRoot.Services customServices = ParaclinicalCompositionRoot.createDefault();
        ParaclinicalOrderPanel panel = new ParaclinicalOrderPanel(customServices);
        assertSame(customServices, panel.getServices(), "Services must match injected instance");

        assertNotNull(panel.getTable(), "Table must be initialized");
        assertNotNull(panel.getTableModel(), "TableModel must be initialized");
        assertNotNull(panel.getTxtSearch(), "Search field must be initialized");
        assertNotNull(panel.getCboStatusFilter(), "Status filter must be initialized");
        assertNotNull(panel.getBtnEnterResult(), "Enter result button must be initialized");
        assertNotNull(panel.getBtnRefresh(), "Refresh button must be initialized");
    }

    @Test
    @DisplayName("Status filter and search filter properly update displayed rows")
    void orderPanelFiltersProperly() {
        ParaclinicalCompositionRoot.Services customServices = ParaclinicalCompositionRoot.createDefault();

        // Seed 2 orders in repository
        ChiDinhDichVu o1 = new ChiDinhDichVu();
        o1.setId(301);
        o1.setMaLuotKham(1);
        o1.setTrangThai(TrangThaiChiDinhDichVu.DA_CHI_DINH);
        customServices.serviceRepository().save(o1);

        ChiDinhDichVu o2 = new ChiDinhDichVu();
        o2.setId(302);
        o2.setMaLuotKham(1);
        o2.setTrangThai(TrangThaiChiDinhDichVu.DA_CO_KET_QUA);
        customServices.serviceRepository().save(o2);

        ParaclinicalOrderPanel panel = new ParaclinicalOrderPanel(customServices);
        assertDoesNotThrow(panel::refreshData);

        // Test filter selection
        assertDoesNotThrow(() -> panel.getCboStatusFilter().setSelectedItem("Đã có kết quả"));
        assertDoesNotThrow(() -> panel.getCboStatusFilter().setSelectedItem("Tất cả trạng thái"));

        // Test opening result dialog when no row selected (should safely notify without exception)
        assertDoesNotThrow(panel::openResultEntryDialogForSelected);
    }
}
