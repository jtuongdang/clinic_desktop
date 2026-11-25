package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.modules.examination.KhamBenhGhepNoiGoc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("PrescriptionOrder Wiring Tests (HD-INTEG-01)")
class DonThuocYeuCauKetNoiTest {

    @Test
    @DisplayName("PrescriptionOrderPanel initializes cleanly with default services")
    void prescriptionOrderPanelInitializesCleanly() {
        KhamBenhGhepNoiGoc.Services services = KhamBenhGhepNoiGoc.createDefault();
        DonThuocYeuCauManHinh panel = new DonThuocYeuCauManHinh(() -> {}, services);
        assertNotNull(panel, "PrescriptionOrderPanel must not be null");
        assertDoesNotThrow(() -> panel.setVisitContext(10, 2, 1));
    }

    @Test
    @DisplayName("PrescriptionOrderTabPanel initializes cleanly and accepts visit context")
    void prescriptionOrderTabPanelInitializesCleanly() {
        KhamBenhGhepNoiGoc.Services services = KhamBenhGhepNoiGoc.createDefault();
        DonThuocYeuCauTheManHinh panel = new DonThuocYeuCauTheManHinh(services);
        assertNotNull(panel, "PrescriptionOrderTabPanel must not be null");
        assertDoesNotThrow(() -> panel.setVisitContext(10, 2));
    }
}
