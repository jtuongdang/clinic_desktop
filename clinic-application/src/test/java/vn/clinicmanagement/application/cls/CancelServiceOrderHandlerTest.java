package vn.clinicmanagement.application.cls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.cls.OrderServiceHandlerTest.InMemoryServiceRepo;
import vn.clinicmanagement.application.cls.OrderServiceHandlerTest.RecordingAuditPort;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.common.enums.TrangThaiChiDinhDichVu;
import vn.clinicmanagement.domain.service.ChiDinhDichVu;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CancelServiceOrderHandlerTest {

    private InMemoryServiceRepo serviceRepo;
    private RecordingAuditPort auditPort;
    private CancelServiceOrderHandler handler;

    @BeforeEach
    void setUp() {
        serviceRepo = new InMemoryServiceRepo();
        auditPort = new RecordingAuditPort();
        handler = new CancelServiceOrderHandler(serviceRepo, auditPort);
    }

    @Test
    @DisplayName("Doctor successfully cancels pending service order")
    void doctorCancelsPendingOrderSuccessfully() {
        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setId(300);
        order.setMaLuotKham(100);
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_CHI_DINH);
        serviceRepo.save(order);

        AuthorizationContext auth = new AuthorizationContext(
                10, 1, "DOCTOR", 1, 1, UUID.randomUUID());
        AuditActorContext actor = new AuditActorContext(
                10, 1, "DOCTOR", 1, 1, "WS-01", UUID.randomUUID());

        CancelServiceOrderCommand command = new CancelServiceOrderCommand(
                300, "Bệnh nhân từ chối thực hiện", auth, actor);

        Result<CancelServiceOrderReceipt> result = handler.handle(command);

        assertTrue(result.isSuccess());
        CancelServiceOrderReceipt receipt = result.getValue().orElseThrow();
        assertEquals(300, receipt.orderId());
        assertEquals(TrangThaiChiDinhDichVu.DA_HUY.name(), receipt.status());
        assertEquals("Bệnh nhân từ chối thực hiện", receipt.reason());

        ChiDinhDichVu updated = serviceRepo.getById(300).orElseThrow();
        assertEquals(TrangThaiChiDinhDichVu.DA_HUY, updated.getTrangThai());

        assertEquals(1, auditPort.independentEntries.size());
        SystemAuditEntry entry = auditPort.independentEntries.get(0);
        assertEquals(AuditActionCode.SERVICE_CANCELLED, entry.actionCode());
        assertEquals(AuditResourceType.SERVICE_ORDER, entry.resourceType());
        assertEquals(AuditOutcome.SUCCESS, entry.outcome());
    }

    @Test
    @DisplayName("Cannot cancel order that already has results")
    void cannotCancelCompletedOrder() {
        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setId(301);
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_CO_KET_QUA);
        serviceRepo.save(order);

        AuthorizationContext auth = new AuthorizationContext(
                10, 1, "DOCTOR", 1, 1, UUID.randomUUID());

        CancelServiceOrderCommand command = new CancelServiceOrderCommand(
                301, "Hủy", auth, null);

        Result<CancelServiceOrderReceipt> result = handler.handle(command);
        assertFalse(result.isSuccess());
        assertEquals("ORDER_ALREADY_COMPLETED", result.getErrorCode().orElse(""));
    }

    @Test
    @DisplayName("Cannot cancel order that is already cancelled")
    void cannotCancelAlreadyCancelledOrder() {
        ChiDinhDichVu order = new ChiDinhDichVu();
        order.setId(302);
        order.setTrangThai(TrangThaiChiDinhDichVu.DA_HUY);
        serviceRepo.save(order);

        AuthorizationContext auth = new AuthorizationContext(
                10, 1, "ADMIN", 1, 1, UUID.randomUUID());

        CancelServiceOrderCommand command = new CancelServiceOrderCommand(
                302, "Hủy tiếp", auth, null);

        Result<CancelServiceOrderReceipt> result = handler.handle(command);
        assertFalse(result.isSuccess());
        assertEquals("ORDER_ALREADY_CANCELLED", result.getErrorCode().orElse(""));
    }
}
