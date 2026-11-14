package vn.clinicmanagement.desktop.modules.paraclinical;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.cls.CancelDichVuYeuCauCommand;
import vn.clinicmanagement.application.cls.GetDichVuTheoLuotKhamQuery;
import vn.clinicmanagement.application.cls.YeuCauDichVuCommand;
import vn.clinicmanagement.application.cls.RecordDichVuKetQuaCommand;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanLamSangGhepNoiGocTest {

    @Test
    @DisplayName("CreateDefault must assemble all 4 paraclinical handlers and serviceRepository cleanly")
    void testCreateDefaultAssemblesAllHandlersAndRepository() {
        CanLamSangGhepNoiGoc.Services services = CanLamSangGhepNoiGoc.createDefault();

        assertNotNull(services, "Services bundle must not be null");
        assertNotNull(services.recordResult(), "RecordServiceResultHandler must be present");
        assertNotNull(services.getServices(), "GetServicesByVisitHandler must be present");
        assertNotNull(services.orderService(), "OrderServiceHandler must be present");
        assertNotNull(services.cancelOrder(), "CancelServiceOrderHandler must be present");
        assertNotNull(services.serviceRepository(), "IChiDinhDichVuRepository must be present");

        CanLamSangGhepNoiGoc root = new CanLamSangGhepNoiGoc(services);
        assertNotNull(root.getServices());
        assertNotNull(root.getRecordResult());
        assertNotNull(root.getGetServices());
        assertNotNull(root.getOrderService());
        assertNotNull(root.getCancelOrder());
        assertNotNull(root.getServiceRepository());
    }

    @Test
    @DisplayName("RecordServiceResultHandler rejects null command with VALIDATION_ERROR")
    void testRecordResultValidationFailureOnNullCommand() {
        CanLamSangGhepNoiGoc.Services services = CanLamSangGhepNoiGoc.createDefault();

        var result = services.recordResult().handle(null);
        assertFalse(result.isSuccess(), "Null record result command must fail");
        assertEquals("VALIDATION_ERROR", result.getErrorCode().orElse(null));
    }

    @Test
    @DisplayName("RecordServiceResultHandler rejects command with invalid order id with INVALID_ORDER")
    void testRecordResultInvalidOrderOnNonPositiveId() {
        CanLamSangGhepNoiGoc.Services services = CanLamSangGhepNoiGoc.createDefault();

        RecordDichVuKetQuaCommand command = new RecordDichVuKetQuaCommand(-1, null, null, null, null, null, null);
        var result = services.recordResult().handle(command);
        assertFalse(result.isSuccess(), "Command with orderId <= 0 must fail");
        assertEquals("INVALID_ORDER", result.getErrorCode().orElse(null));
    }

    @Test
    @DisplayName("CancelServiceOrderHandler rejects null command with VALIDATION_ERROR")
    void testCancelOrderValidationFailure() {
        CanLamSangGhepNoiGoc.Services services = CanLamSangGhepNoiGoc.createDefault();

        var result = services.cancelOrder().handle(null);
        assertFalse(result.isSuccess(), "Null cancel command must fail");
        assertEquals("VALIDATION_ERROR", result.getErrorCode().orElse(null));
    }

    @Test
    @DisplayName("GetServicesByVisitHandler rejects null query with VALIDATION_ERROR")
    void testGetServicesValidationFailure() {
        CanLamSangGhepNoiGoc.Services services = CanLamSangGhepNoiGoc.createDefault();

        var result = services.getServices().handle(null);
        assertFalse(result.isSuccess(), "Null getServices query must fail");
        assertEquals("VALIDATION_ERROR", result.getErrorCode().orElse(null));
    }

    @Test
    @DisplayName("OrderServiceHandler rejects null command with VALIDATION_ERROR")
    void testOrderServiceValidationFailure() {
        CanLamSangGhepNoiGoc.Services services = CanLamSangGhepNoiGoc.createDefault();

        var result = services.orderService().handle(null);
        assertFalse(result.isSuccess(), "Null orderService command must fail");
        assertEquals("VALIDATION_ERROR", result.getErrorCode().orElse(null));
    }

    @Test
    @DisplayName("InMemoryChiDinhDichVuRepository behaves properly in-memory")
    void testInMemoryRepositoryOperations() {
        CanLamSangGhepNoiGoc.InMemoryChiDinhDichVuRepository repo =
                new CanLamSangGhepNoiGoc.InMemoryChiDinhDichVuRepository();

        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setMaLuotKham(101);
        order.setMaDichVu(201);

        repo.save(order);
        assertNotNull(order.getId());
        assertTrue(order.getId() > 0);

        var retrieved = repo.getById(order.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(101, retrieved.get().getMaLuotKham());

        var byVisit = repo.getByLuotKham(101);
        assertEquals(1, byVisit.size());

        assertEquals(1, repo.getAll().size());
        assertEquals(1, repo.count(null));
        assertTrue(repo.exists(null));

        repo.delete(order);
        assertTrue(repo.getById(order.getId()).isEmpty());
    }
}
