package vn.clinicmanagement.desktop.modules.pharmacy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.inventory.DispensePrescriptionCommand;
import vn.clinicmanagement.application.inventory.GetStockAvailabilityQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PharmacyCompositionRootTest {

    @Test
    @DisplayName("CreateDefault must assemble all 4 pharmacy handlers and 2 repositories cleanly")
    void testCreateDefaultAssemblesAllHandlersAndRepositories() {
        PharmacyCompositionRoot.Services services = PharmacyCompositionRoot.createDefault();

        assertNotNull(services, "Services bundle must not be null");
        assertNotNull(services.dispensePrescription(), "DispensePrescriptionHandler must be present");
        assertNotNull(services.receiveStock(), "ReceiveWarehouseStockHandler must be present");
        assertNotNull(services.checkStock(), "GetStockAvailabilityHandler must be present");
        assertNotNull(services.approveStocktake(), "ApproveStocktakeHandler must be present");
        assertNotNull(services.khoRepository(), "IKhoRepository must be present");
        assertNotNull(services.prescriptionRepository(), "IDonThuocRepository must be present");
    }

    @Test
    @DisplayName("DispensePrescriptionHandler rejects invalid command with VALIDATION_ERROR")
    void testDispensePrescriptionValidationFailure() {
        PharmacyCompositionRoot.Services services = PharmacyCompositionRoot.createDefault();

        var result = services.dispensePrescription().handle(new DispensePrescriptionCommand(-1, -1, -1, null));
        assertFalse(result.isSuccess(), "Invalid dispense command must fail");
        assertEquals("VALIDATION_ERROR", result.getErrorCode().orElse(null));
    }

    @Test
    @DisplayName("GetStockAvailabilityHandler rejects invalid query with VALIDATION_ERROR")
    void testCheckStockValidationFailure() {
        PharmacyCompositionRoot.Services services = PharmacyCompositionRoot.createDefault();

        var result = services.checkStock().handle(new GetStockAvailabilityQuery(-1, -1, null));
        assertFalse(result.isSuccess(), "Invalid stock check query must fail");
        assertEquals("VALIDATION_ERROR", result.getErrorCode().orElse(null));
    }
}
