package vn.clinicmanagement.desktop.modules.pharmacy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.inventory.DispenseDonThuocCommand;
import vn.clinicmanagement.application.inventory.GetTonKhoKhaDungQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NhaThuocGhepNoiGocTest {

    @Test
    @DisplayName("CreateDefault must assemble all 4 pharmacy handlers and 2 repositories cleanly")
    void testCreateDefaultAssemblesAllHandlersAndRepositories() {
        NhaThuocGhepNoiGoc.Services services = NhaThuocGhepNoiGoc.createDefault();

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
        NhaThuocGhepNoiGoc.Services services = NhaThuocGhepNoiGoc.createDefault();

        var result = services.dispensePrescription().handle(new DispenseDonThuocCommand(-1, -1, -1, null));
        assertFalse(result.isSuccess(), "Invalid dispense command must fail");
        assertEquals("VALIDATION_ERROR", result.getErrorCode().orElse(null));
    }

    @Test
    @DisplayName("GetStockAvailabilityHandler rejects invalid query with VALIDATION_ERROR")
    void testCheckStockValidationFailure() {
        NhaThuocGhepNoiGoc.Services services = NhaThuocGhepNoiGoc.createDefault();

        var result = services.checkStock().handle(new GetTonKhoKhaDungQuery(-1, -1, null));
        assertFalse(result.isSuccess(), "Invalid stock check query must fail");
        assertEquals("VALIDATION_ERROR", result.getErrorCode().orElse(null));
    }
}
