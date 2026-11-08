package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.modules.examination.ExaminationCompositionRoot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("PrescriptionOrder Wiring Tests (HD-INTEG-01)")
class PrescriptionOrderWiringTest {

    @Test
    @DisplayName("PrescriptionOrderPanel initializes cleanly with default services")
    void prescriptionOrderPanelInitializesCleanly() {
        ExaminationCompositionRoot.Services services = ExaminationCompositionRoot.createDefault();
        PrescriptionOrderPanel panel = new PrescriptionOrderPanel(() -> {}, services);
        assertNotNull(panel, "PrescriptionOrderPanel must not be null");
        assertDoesNotThrow(() -> panel.setVisitContext(10, 2, 1));
    }

    @Test
    @DisplayName("PrescriptionOrderTabPanel initializes cleanly and accepts visit context")
    void prescriptionOrderTabPanelInitializesCleanly() {
        ExaminationCompositionRoot.Services services = ExaminationCompositionRoot.createDefault();
        PrescriptionOrderTabPanel panel = new PrescriptionOrderTabPanel(services);
        assertNotNull(panel, "PrescriptionOrderTabPanel must not be null");
        assertDoesNotThrow(() -> panel.setVisitContext(10, 2));
    }
}
