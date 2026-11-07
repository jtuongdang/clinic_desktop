package vn.clinicmanagement.application.inventory;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.common.enums.LoaiItem;
import vn.clinicmanagement.domain.inventory.Item;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class GetItemCatalogHandlerTest {
    private static final UUID SESSION_ID = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");
    private static final UUID CORRELATION_ID = UUID.fromString("0b8a0d60-0cc7-4fc8-9534-754f5f0e0017");

    @Test
    void allowsCatalogSearchForEveryApprovedRoleAndAuditsIndependently() {
        for (String role : new String[]{"DOCTOR", "PHARMACIST", "ADMIN", "RECEPTIONIST"}) {
            Fixture fixture = new Fixture(List.of(item(12, "SYN-12", true)));

            Result<List<ItemSummaryDto>> result = fixture.handler().handle(fixture.query(role));

            assertTrue(result.isSuccess());
            assertEquals(List.of(summary(12, "SYN-12", true)), result.valueOrThrow());
            assertEquals("Synthetic query", fixture.keyword);
            assertEquals(Boolean.TRUE, fixture.activeOnly);
            assertEquals(1, fixture.searchCalls);
            assertEquals(AuditActionCode.CATALOG_VIEW, fixture.audit.entry.actionCode());
            assertNull(fixture.audit.entry.resourceType());
            assertNull(fixture.audit.entry.resourceId());
            assertEquals(AuditOutcome.SUCCESS, fixture.audit.entry.outcome());
            assertEquals(1, fixture.audit.independentWrites);
        }
    }

    @Test
    void returnsEmptyListAndPassesThroughNullSearchFilters() {
        Fixture fixture = new Fixture(List.of());

        Result<List<ItemSummaryDto>> result = fixture.handler().handle(
                new GetItemCatalogQuery(null, null, context("PHARMACIST"), actor("PHARMACIST")));

        assertTrue(result.isSuccess());
        assertTrue(result.valueOrThrow().isEmpty());
        assertNull(fixture.keyword);
        assertNull(fixture.activeOnly);
        assertEquals(1, fixture.searchCalls);
        assertEquals(AuditActionCode.CATALOG_VIEW, fixture.audit.entry.actionCode());
    }

    @Test
    void deniesInvalidRoleWithoutSearchingAndWritesForbiddenAudit() {
        Fixture fixture = new Fixture(List.of(item(12, "SYN-12", true)));

        Result<List<ItemSummaryDto>> result = fixture.handler().handle(fixture.query("BILLING"));

        assertTrue(result.isFailure());
        assertEquals("AUTHORIZATION_DENIED", result.getError().orElseThrow().code());
        assertEquals(0, fixture.searchCalls);
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, fixture.audit.entry.actionCode());
        assertEquals(AuditOutcome.DENIED, fixture.audit.entry.outcome());
        assertEquals(AuditReasonCode.AUTH_FORBIDDEN, fixture.audit.entry.reasonCode());
        assertEquals(1, fixture.audit.independentWrites);
    }

    private static Item item(int id, String code, boolean active) {
        Item item = new Item();
        item.setId(id);
        item.setMaItem(code);
        item.setTenItem("Synthetic item " + id);
        item.setLoaiItem(LoaiItem.VAT_TU);
        item.setHoatChat("Synthetic substance");
        item.setHamLuong("20 mg");
        item.setDonViCoBan("EA");
        item.setGiaBanMacDinh(new BigDecimal("5.25"));
        item.setHoatDong(active);
        return item;
    }

    private static ItemSummaryDto summary(int id, String code, boolean active) {
        return new ItemSummaryDto(id, code, "Synthetic item " + id, LoaiItem.VAT_TU, "Synthetic substance",
                "20 mg", "EA", new BigDecimal("5.25"), active);
    }

    private static AuthorizationContext context(String role) {
        return new AuthorizationContext(17, 4, role, 3, 8, SESSION_ID);
    }

    private static AuditActorContext actor(String role) {
        return new AuditActorContext(17, 4, role, 3, 8, "station-1", CORRELATION_ID);
    }

    private static final class Fixture {
        final AuditFixture audit = new AuditFixture();
        final List<Item> items;
        final IKhoRepository inventory;
        String keyword;
        Boolean activeOnly;
        int searchCalls;

        Fixture(List<Item> items) {
            this.items = new ArrayList<>(items);
            inventory = (IKhoRepository) Proxy.newProxyInstance(
                    IKhoRepository.class.getClassLoader(), new Class<?>[]{IKhoRepository.class},
                    (proxy, method, args) -> {
                        if ("searchItems".equals(method.getName())) {
                            searchCalls++;
                            keyword = (String) args[0];
                            activeOnly = (Boolean) args[1];
                            return this.items;
                        }
                        return null;
                    });
        }

        GetItemCatalogHandler handler() {
            return new GetItemCatalogHandler(inventory, audit);
        }

        GetItemCatalogQuery query(String role) {
            return new GetItemCatalogQuery("Synthetic query", true, context(role), actor(role));
        }
    }

    private static final class AuditFixture implements AuditWritePort {
        int independentWrites;
        SystemAuditEntry entry;

        @Override
        public void appendInCurrentTransaction(SystemAuditEntry auditEntry) {
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
