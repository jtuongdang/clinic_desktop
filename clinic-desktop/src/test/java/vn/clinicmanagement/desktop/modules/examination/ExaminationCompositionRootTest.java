package vn.clinicmanagement.desktop.modules.examination;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExaminationCompositionRootTest {

    @Test
    @DisplayName("CreateDefault must assemble all 11 examination handlers cleanly")
    void testCreateDefaultAssemblesAllHandlers() {
        ExaminationCompositionRoot.Services services = ExaminationCompositionRoot.createDefault();

        assertNotNull(services, "Services bundle must not be null");
        assertNotNull(services.startExam(), "StartExamHandler must be present");
        assertNotNull(services.completeExam(), "CompleteExamHandler must be present");
        assertNotNull(services.addDiagnosis(), "AddDiagnosisHandler must be present");
        assertNotNull(services.prescribeMedication(), "PrescribeMedicationHandler must be present");
        assertNotNull(services.getPrescriptionByVisit(), "GetPrescriptionByVisitHandler must be present");
        assertNotNull(services.cancelPrescription(), "CancelPrescriptionHandler must be present");
        assertNotNull(services.orderService(), "OrderServiceHandler must be present");
        assertNotNull(services.getServicesByVisit(), "GetServicesByVisitHandler must be present");
        assertNotNull(services.cancelServiceOrder(), "CancelServiceOrderHandler must be present");
        assertNotNull(services.updateEmrClinicalRecord(), "UpdateEmrClinicalRecordHandler must be present");
        assertNotNull(services.transferVisit(), "TransferVisitHandler must be present");
    }
}
