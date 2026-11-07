package vn.clinicmanagement.application.inventory;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonDatHang;
import vn.clinicmanagement.domain.inventory.ChiTietDonDatHang;
import vn.clinicmanagement.domain.inventory.DonDatHang;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.math.BigDecimal;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class CreatePurchaseOrderHandlerTest {
    private static final UUID SESSION_ID = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");
    private static final UUID CORRELATION_ID = UUID.fromString("0b8a0d60-0cc7-4fc8-9534-754f5f0e0017");

    @Test
    void createsPurchaseOrderForPharmacistAndAuditsIndependently() {
        assertPurchaseOrderCreated("PHARMACIST");
    }

    @Test
    void createsPurchaseOrderForAdmin() {
        assertPurchaseOrderCreated("ADMIN");
    }

    @Test
    void rejectsMissingSupplierEmptyItemsAndNegativeQuantityWithoutSaving() {
        Fixture fixture = new Fixture();
        List<CreatePurchaseOrderCommand> commands = List.of(
                fixture.command("SYN-PO-001", 0, List.of(validItem()), "ADMIN"),
                fixture.command("SYN-PO-001", 5, List.of(), "ADMIN"),
                fixture.command("SYN-PO-001", 5,
                        List.of(new PurchaseOrderItemInput(12, new BigDecimal("-1"), new BigDecimal("2.50"))),
                        "ADMIN"));

        for (CreatePurchaseOrderCommand command : commands) {
            Result<CreatePurchaseOrderReceipt> result = fixture.handler().handle(command);
            assertFailure(result, "VALIDATION_ERROR", "Thông tin đơn đặt hàng không hợp lệ.");
        }

        assertEquals(0, fixture.transactions.calls);
        assertEquals(0, fixture.saveCalls);
        assertNull(fixture.audit.entry);
    }

    @Test
    void rejectsNullCommandAndInvalidItems() {
        Fixture fixture = new Fixture();
        assertFailure(fixture.handler().handle(null), "VALIDATION_ERROR",
                "Thông tin đơn đặt hàng không hợp lệ.");
        assertFailure(fixture.handler().handle(fixture.command(null, 5, List.of(validItem()), "ADMIN")),
                "VALIDATION_ERROR", "Thông tin đơn đặt hàng không hợp lệ.");
        assertFailure(fixture.handler().handle(fixture.command("SYN-PO-001", 5, null, "ADMIN")),
                "VALIDATION_ERROR", "Thông tin đơn đặt hàng không hợp lệ.");
        assertFailure(fixture.handler().handle(fixture.command(
                        "SYN-PO-001", 5, List.of(new PurchaseOrderItemInput(0, BigDecimal.ONE, BigDecimal.ONE)),
                        "ADMIN")),
                "VALIDATION_ERROR", "Thông tin đơn đặt hàng không hợp lệ.");
        assertFailure(fixture.handler().handle(fixture.command(
                        "SYN-PO-001", 5, List.of(new PurchaseOrderItemInput(12, BigDecimal.ONE, null)), "ADMIN")),
                "VALIDATION_ERROR", "Thông tin đơn đặt hàng không hợp lệ.");

        assertEquals(0, fixture.transactions.calls);
        assertEquals(0, fixture.saveCalls);
        assertNull(fixture.audit.entry);
    }

    @Test
    void deniesUnauthorizedRoleAndWritesForbiddenAuditIndependently() {
        Fixture fixture = new Fixture();

        Result<CreatePurchaseOrderReceipt> result =
                fixture.handler().handle(fixture.command("DOCTOR"));

        assertFailure(result, "AUTH_ACCESS_DENIED",
                "Không được phép tạo đơn đặt hàng trong phiên hiện tại.");
        assertEquals(0, fixture.transactions.calls);
        assertEquals(0, fixture.saveCalls);
        assertEquals(1, fixture.audit.independentWrites);
        assertEquals(0, fixture.audit.inTransactionWrites);
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, fixture.audit.entry.actionCode());
        assertEquals(AuditOutcome.DENIED, fixture.audit.entry.outcome());
        assertEquals(AuditReasonCode.AUTH_FORBIDDEN, fixture.audit.entry.reasonCode());
    }

    @Test
    void convenienceConstructorCreatesAuditActorFromAuthorizationContext() {
        CreatePurchaseOrderCommand command = new CreatePurchaseOrderCommand(
                "SYN-PO-001", 5, List.of(validItem()), context("ADMIN"));

        assertNotNull(command.auditActorContext());
        assertEquals(17, command.auditActorContext().employeeId());
        assertEquals("ADMIN", command.auditActorContext().roleCode());
    }

    private static void assertPurchaseOrderCreated(String role) {
        Fixture fixture = new Fixture();

        Result<CreatePurchaseOrderReceipt> result = fixture.handler().handle(fixture.command(role));

        assertTrue(result.isSuccess());
        assertEquals(new CreatePurchaseOrderReceipt(91, "SYN-PO-001", new BigDecimal("11.00")),
                result.valueOrThrow());
        assertEquals(1, fixture.transactions.calls);
        assertEquals(1, fixture.saveCalls);
        assertEquals(5, fixture.saved.getMaNhaCungCap());
        assertEquals("SYN-PO-001", fixture.saved.getSoDonDatHang());
        assertEquals(TrangThaiDonDatHang.CHO_DUYET, fixture.saved.getTrangThai());
        assertEquals(new BigDecimal("11.00"), fixture.saved.getTongTien());
        assertEquals(2, fixture.saved.getChiTiets().size());

        ChiTietDonDatHang first = fixture.saved.getChiTiets().get(0);
        assertEquals(12, first.getMaItem());
        assertEquals(new BigDecimal("2"), first.getSoLuongDat());
        assertEquals(new BigDecimal("2.50"), first.getDonGia());
        ChiTietDonDatHang second = fixture.saved.getChiTiets().get(1);
        assertEquals(13, second.getMaItem());
        assertEquals(new BigDecimal("3"), second.getSoLuongDat());
        assertEquals(new BigDecimal("2"), second.getDonGia());

        assertEquals(1, fixture.audit.independentWrites);
        assertEquals(0, fixture.audit.inTransactionWrites);
        assertEquals(AuditActionCode.INV_RECEIPT_CREATED, fixture.audit.entry.actionCode());
        assertEquals(AuditResourceType.INVENTORY_RECEIPT, fixture.audit.entry.resourceType());
        assertEquals(91, fixture.audit.entry.resourceId());
        assertEquals(AuditOutcome.SUCCESS, fixture.audit.entry.outcome());
    }

    private static PurchaseOrderItemInput validItem() {
        return new PurchaseOrderItemInput(12, new BigDecimal("2"), new BigDecimal("2.50"));
    }

    private static void assertFailure(Result<CreatePurchaseOrderReceipt> result, String code, String message) {
        assertTrue(result.isFailure());
        assertEquals(code, result.getError().orElseThrow().code());
        assertEquals(message, result.getError().orElseThrow().message());
    }

    private static AuthorizationContext context(String role) {
        return new AuthorizationContext(17, 4, role, 3, 8, SESSION_ID);
    }

    private static AuditActorContext actor(String role) {
        return new AuditActorContext(17, 4, role, 3, 8, "station-1", CORRELATION_ID);
    }

    private static final class Fixture {
        final TransactionFixture transactions = new TransactionFixture();
        final AuditFixture audit = new AuditFixture();
        int saveCalls;
        DonDatHang saved;
        final IKhoRepository inventory = (IKhoRepository) Proxy.newProxyInstance(
                IKhoRepository.class.getClassLoader(),
                new Class<?>[]{IKhoRepository.class},
                (proxy, method, args) -> {
                    if ("savePurchaseOrder".equals(method.getName())) {
                        saveCalls++;
                        saved = (DonDatHang) args[0];
                        saved.setId(91);
                        return saved;
                    }
                    return null;
                });

        CreatePurchaseOrderHandler handler() {
            return new CreatePurchaseOrderHandler(inventory, transactions, audit);
        }

        CreatePurchaseOrderCommand command(String role) {
            return command("SYN-PO-001", 5, List.of(
                    validItem(),
                    new PurchaseOrderItemInput(13, new BigDecimal("3"), new BigDecimal("2"))), role);
        }

        CreatePurchaseOrderCommand command(
                String orderNumber, int supplierId, List<PurchaseOrderItemInput> items, String role) {
            return new CreatePurchaseOrderCommand(
                    orderNumber, supplierId, items, context(role), actor(role));
        }
    }

    private static final class TransactionFixture implements TransactionBoundary {
        int calls;

        @Override
        public <T> Result<T> inTransaction(Supplier<Result<T>> operation) {
            calls++;
            return operation.get();
        }
    }

    private static final class AuditFixture implements AuditWritePort {
        int independentWrites;
        int inTransactionWrites;
        SystemAuditEntry entry;

        @Override
        public void appendInCurrentTransaction(SystemAuditEntry auditEntry) {
            inTransactionWrites++;
            entry = auditEntry;
        }

        @Override
        public void appendIndependently(SystemAuditEntry auditEntry) {
            independentWrites++;
            entry = auditEntry;
        }

        @Override
        public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry auditEntry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void appendMedicalRecordIndependently(MedicalRecordAuditEntry auditEntry) {
            throw new UnsupportedOperationException();
        }
    }
}
