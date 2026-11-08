package vn.clinicmanagement.desktop.modules.examination.components.tabs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.desktop.modules.examination.ExaminationCompositionRoot;
import vn.clinicmanagement.desktop.modules.examination.ExaminationPanel;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("TreatmentResolution & MedicalRecord Wiring Tests (Task 1.4 & 1.5 - HD-INTEG-01)")
class TreatmentResolutionWiringTest {

    @Test
    @DisplayName("TreatmentResolutionTabPanel initializes cleanly with default constructor and services overload")
    void treatmentResolutionTabPanelInitializesCleanly() {
        TreatmentResolutionTabPanel defaultPanel = new TreatmentResolutionTabPanel();
        assertNotNull(defaultPanel, "TreatmentResolutionTabPanel default instance must not be null");
        assertNotNull(defaultPanel.getServices(), "Services must be initialized via fallback");

        ExaminationCompositionRoot.Services customServices = ExaminationCompositionRoot.createDefault();
        TreatmentResolutionTabPanel panel = new TreatmentResolutionTabPanel(customServices);
        assertNotNull(panel, "TreatmentResolutionTabPanel custom services instance must not be null");
        assertSame(customServices, panel.getServices(), "Services must match injected instance");

        assertDoesNotThrow(() -> panel.setVisitContext(105, 12));
        assertEquals(105, panel.getCurrentVisitId(), "Visit ID must be updated");
        assertEquals(12, panel.getCurrentDoctorId(), "Doctor ID must be updated");
    }

    @Test
    @DisplayName("MedicalRecordTabPanel initializes cleanly and updates visit context")
    void medicalRecordTabPanelInitializesCleanly() {
        MedicalRecordTabPanel defaultPanel = new MedicalRecordTabPanel();
        assertNotNull(defaultPanel, "MedicalRecordTabPanel default instance must not be null");
        assertNotNull(defaultPanel.getServices(), "Services must be initialized via fallback");

        ExaminationCompositionRoot.Services customServices = ExaminationCompositionRoot.createDefault();
        MedicalRecordTabPanel panel = new MedicalRecordTabPanel(customServices);
        assertNotNull(panel, "MedicalRecordTabPanel custom services instance must not be null");
        assertSame(customServices, panel.getServices(), "Services must match injected instance");

        assertDoesNotThrow(() -> panel.setVisitContext(202, 18));
        assertEquals(202, panel.getCurrentVisitId(), "Visit ID must be updated");
        assertEquals(18, panel.getCurrentDoctorId(), "Doctor ID must be updated");
    }

    @Test
    @DisplayName("ExaminationPanel properly wires services into TreatmentResolution and MedicalRecord tabs")
    void examinationPanelWiresServicesIntoTabs() {
        ExaminationCompositionRoot.Services services = ExaminationCompositionRoot.createDefault();
        ExaminationPanel examPanel = new ExaminationPanel(services);
        assertNotNull(examPanel, "ExaminationPanel must not be null");

        TreatmentResolutionTabPanel trPanel = examPanel.getTreatmentResolutionTabPanel();
        assertNotNull(trPanel, "TreatmentResolutionTabPanel in ExaminationPanel must not be null");
        assertSame(services, trPanel.getServices(), "TreatmentResolutionTabPanel must receive examination services");

        MedicalRecordTabPanel mrPanel = examPanel.getMedicalRecordTabPanel();
        assertNotNull(mrPanel, "MedicalRecordTabPanel in ExaminationPanel must not be null");
        assertSame(services, mrPanel.getServices(), "MedicalRecordTabPanel must receive examination services");

        assertDoesNotThrow(() -> examPanel.selectPatientByIndex(0));
        assertNotNull(trPanel.getTxtChanDoanXacDinh());
        assertNotNull(mrPanel.getTxtDienBienLamSang());
    }
}
