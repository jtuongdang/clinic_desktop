package vn.clinicmanagement.application.inventory;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.common.enums.LoaiItem;
import vn.clinicmanagement.domain.inventory.Item;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class CreateItemHandlerTest {
    private static final UUID SESSION_ID = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");
    private static final UUID CORRELATION_ID = UUID.fromString("0b8a0d60-0cc7-4fc8-9534-754f5f0e0017");

    @Test
    void createsItemForAdminAndAuditsIndependently() {
        assertItemCreated("ADMIN");
    }

    @Test
    void createsItemForPharmacistAndMapsAllFields() {
        assertItemCreated("PHARMACIST");
    }

    @Test
    void rejectsNullOrBlankRequiredFieldsBeforeRepositoryOrTransaction() {
        Fixture fixture = new Fixture();
        CreateItemCommand[] commands = {
                null,
                fixture.command(null, "Synthetic item", "EA", "ADMIN"),
                fixture.command("   ", "Synthetic item", "EA", "ADMIN"),
                fixture.command("SYN-ITEM", null, "EA", "ADMIN"),
                fixture.command("SYN-ITEM", "  ", "EA", "ADMIN"),
                fixture.command("SYN-ITEM", "Synthetic item", null, "ADMIN"),
                fixture.command("SYN-ITEM", "Synthetic item", " ", "ADMIN")
        };

        for (CreateItemCommand command : commands) {
            Result<CreateItemReceipt> result = fixture.handler().handle(command);
            assertFailure(result, "VALIDATION_ERROR", "Thông tin danh mục không hợp lệ.");
        }

        assertEquals(0, fixture.transactions.calls);
        assertEquals(0, fixture.codeLookups);
        assertNull(fixture.saved);
        assertNull(fixture.audit.entry);
    }

    @Test
    void rejectsDuplicateItemCodeWithoutSaving() {
        Fixture fixture = new Fixture();
        fixture.existingItem = new Item();

        Result<CreateItemReceipt> result = fixture.handler().handle(fixture.command());

        assertFailure(result, "DUPLICATE_CODE", "Mã hàng đã tồn tại trên hệ thống.");
        assertEquals("SYN-ITEM", fixture.lookedUpCode);
        assertEquals(1, fixture.codeLookups);
        assertEquals(0, fixture.transactions.calls);
        assertNull(fixture.saved);
        assertNull(fixture.audit.entry);
    }

    @Test
    void deniesDoctorAndReceptionistAndWritesForbiddenAudit() {
        for (String role : new String[]{"DOCTOR", "RECEPTIONIST"}) {
            Fixture fixture = new Fixture();

            Result<CreateItemReceipt> result = fixture.handler().handle(fixture.command(role));

            assertFailure(result, "AUTHORIZATION_DENIED",
                    "Không được phép tạo danh mục hàng hóa trong phiên hiện tại.");
            assertEquals(0, fixture.codeLookups);
            assertEquals(0, fixture.transactions.calls);
            assertNull(fixture.saved);
            assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, fixture.audit.entry.actionCode());
            assertEquals(AuditOutcome.DENIED, fixture.audit.entry.outcome());
            assertEquals(AuditReasonCode.AUTH_FORBIDDEN, fixture.audit.entry.reasonCode());
        }
    }

    private static void assertItemCreated(String role) {
        Fixture fixture = new Fixture();

        Result<CreateItemReceipt> result = fixture.handler().handle(fixture.command(role));

        assertTrue(result.isSuccess());
        assertEquals(new CreateItemReceipt(91, "SYN-ITEM", "Synthetic item"), result.valueOrThrow());
        assertEquals("SYN-ITEM", fixture.saved.getMaItem());
        assertEquals("Synthetic item", fixture.saved.getTenItem());
        assertEquals(LoaiItem.THUOC, fixture.saved.getLoaiItem());
        assertEquals("Synthetic ingredient", fixture.saved.getHoatChat());
        assertEquals("10 mg", fixture.saved.getHamLuong());
        assertEquals("EA", fixture.saved.getDonViCoBan());
        assertEquals("Box of 10", fixture.saved.getQuyCachDongGoi());
        assertEquals(new BigDecimal("2.50"), fixture.saved.getGiaNhapMacDinh());
        assertEquals(new BigDecimal("3.75"), fixture.saved.getGiaBanMacDinh());
        assertEquals(new BigDecimal("2"), fixture.saved.getTonToiThieu());
        assertEquals(new BigDecimal("200"), fixture.saved.getTonToiDa());
        assertEquals("Tablet", fixture.saved.getDangBaoChe());
        assertEquals("Synthetic country", fixture.saved.getNuocSanXuat());
        assertEquals("Synthetic manufacturer", fixture.saved.getHangSanXuat());
        assertEquals("REG-SYN-01", fixture.saved.getSoDangKy());
        assertEquals(1, fixture.transactions.calls);
        assertEquals(1, fixture.codeLookups);
        assertEquals(AuditActionCode.INV_ITEM_CREATED, fixture.audit.entry.actionCode());
        assertEquals(AuditResourceType.ITEM, fixture.audit.entry.resourceType());
        assertEquals(91, fixture.audit.entry.resourceId());
        assertEquals(AuditOutcome.SUCCESS, fixture.audit.entry.outcome());
        assertEquals(1, fixture.audit.independentWrites);
        assertEquals(0, fixture.audit.inTransactionWrites);
    }

    private static void assertFailure(Result<CreateItemReceipt> result, String code, String message) {
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
        final AuditFixture audit = new AuditFixture(transactions);
        int codeLookups;
        String lookedUpCode;
        Item existingItem;
        Item saved;
        final IKhoRepository inventory = (IKhoRepository) Proxy.newProxyInstance(
                IKhoRepository.class.getClassLoader(), new Class<?>[]{IKhoRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getItemByCode" -> {
                        codeLookups++;
                        lookedUpCode = (String) args[0];
                        yield Optional.ofNullable(existingItem);
                    }
                    case "saveItem" -> {
                        saved = (Item) args[0];
                        saved.setId(91);
                        yield saved;
                    }
                    default -> null;
                });

        CreateItemHandler handler() {
            return new CreateItemHandler(inventory, transactions, audit);
        }

        CreateItemCommand command() {
            return command("ADMIN");
        }

        CreateItemCommand command(String role) {
            return command("SYN-ITEM", "Synthetic item", "EA", role);
        }

        CreateItemCommand command(String code, String name, String unit, String role) {
            return new CreateItemCommand(code, name, LoaiItem.THUOC, "Synthetic ingredient", "10 mg", unit,
                    "Box of 10", new BigDecimal("2.50"), new BigDecimal("3.75"), new BigDecimal("2"),
                    new BigDecimal("200"), "Tablet", "Synthetic country", "Synthetic manufacturer",
                    "REG-SYN-01", context(role), actor(role));
        }
    }

    private static final class TransactionFixture implements TransactionBoundary {
        boolean current;
        int calls;

        @Override
        public <T> Result<T> inTransaction(Supplier<Result<T>> operation) {
            calls++;
            current = true;
            try {
                return operation.get();
            } finally {
                current = false;
            }
        }
    }

    private static final class AuditFixture implements AuditWritePort {
        private final TransactionFixture transactions;
        int independentWrites;
        int inTransactionWrites;
        SystemAuditEntry entry;

        AuditFixture(TransactionFixture transactions) {
            this.transactions = transactions;
        }

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
