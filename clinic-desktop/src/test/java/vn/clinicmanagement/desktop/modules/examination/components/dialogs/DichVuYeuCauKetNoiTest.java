package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.XetNghiemYeuCauTheManHinh;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("ServiceOrder Wiring Tests (HD-INTEG-01)")
class DichVuYeuCauKetNoiTest {

    @Test
    @DisplayName("ServiceOrderPanel initializes cleanly with default services and context")
    void serviceOrderPanelInitializesCleanly() {
        KhamBenhGhepNoiGoc.Services services = KhamBenhGhepNoiGoc.createDefault();
        DichVuYeuCauManHinh panel = new DichVuYeuCauManHinh(() -> {}, services);
        assertNotNull(panel, "ServiceOrderPanel must not be null");
        assertDoesNotThrow(() -> panel.setVisitContext(10, 2, 1));
    }

    @Test
    @DisplayName("LabOrderTabPanel initializes cleanly and accepts visit context")
    void labOrderTabPanelInitializesCleanly() {
        KhamBenhGhepNoiGoc.Services services = KhamBenhGhepNoiGoc.createDefault();
        XetNghiemYeuCauTheManHinh panel = new XetNghiemYeuCauTheManHinh(() -> {}, services);
        assertNotNull(panel, "LabOrderTabPanel must not be null");
        assertDoesNotThrow(() -> panel.loadServicesForVisit(10));
    }
}
