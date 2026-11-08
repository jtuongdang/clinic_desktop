package vn.clinicmanagement.desktop.modules.examination.components.dialogs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.modules.examination.ExaminationCompositionRoot;
import vn.clinicmanagement.desktop.modules.examination.components.tabs.LabOrderTabPanel;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("ServiceOrder Wiring Tests (HD-INTEG-01)")
class ServiceOrderWiringTest {

    @Test
    @DisplayName("ServiceOrderPanel initializes cleanly with default services and context")
    void serviceOrderPanelInitializesCleanly() {
        ExaminationCompositionRoot.Services services = ExaminationCompositionRoot.createDefault();
        ServiceOrderPanel panel = new ServiceOrderPanel(() -> {}, services);
        assertNotNull(panel, "ServiceOrderPanel must not be null");
        assertDoesNotThrow(() -> panel.setVisitContext(10, 2, 1));
    }

    @Test
    @DisplayName("LabOrderTabPanel initializes cleanly and accepts visit context")
    void labOrderTabPanelInitializesCleanly() {
        ExaminationCompositionRoot.Services services = ExaminationCompositionRoot.createDefault();
        LabOrderTabPanel panel = new LabOrderTabPanel(() -> {}, services);
        assertNotNull(panel, "LabOrderTabPanel must not be null");
        assertDoesNotThrow(() -> panel.loadServicesForVisit(10));
    }
}
