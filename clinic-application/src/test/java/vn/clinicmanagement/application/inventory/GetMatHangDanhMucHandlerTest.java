package vn.clinicmanagement.application.inventory;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.common.enums.LoaiMatHang;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class GetMatHangDanhMucHandlerTest {
    private static final UUID SESSION_ID = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");
    private static final UUID CORRELATION_ID = UUID.fromString("0b8a0d60-0cc7-4fc8-9534-754f5f0e0017");

    @Test
    void allowsCatalogSearchForEveryApprovedRoleAndAuditsIndependently() {
        for (String role : new String[]{"DOCTOR", "PHARMACIST", "ADMIN", "RECEPTIONIST"}) {
            Fixture fixture = new Fixture(List.of(item(12, "SYN-12", true)));

            KetQua<List<MatHangTongHopDuLieu>> result = fixture.handler().handle(fixture.query(role));

            assertTrue(result.isSuccess());
            assertEquals(List.of(summary(12, "SYN-12", true)), result.valueOrThrow());
            assertEquals("Synthetic query", fixture.keyword);
            assertEquals(Boolean.TRUE, fixture.activeOnly);
            assertEquals(1, fixture.searchCalls);
            assertEquals(KiemToanHanhDongMa.CATALOG_VIEW, fixture.audit.entry.actionCode());
            assertNull(fixture.audit.entry.resourceType());
            assertNull(fixture.audit.entry.resourceId());
            assertEquals(KiemToanKetQua.SUCCESS, fixture.audit.entry.outcome());
            assertEquals(1, fixture.audit.independentWrites);
        }
    }

    @Test
    void returnsEmptyListAndPassesThroughNullSearchFilters() {
        Fixture fixture = new Fixture(List.of());

        KetQua<List<MatHangTongHopDuLieu>> result = fixture.handler().handle(
                new GetMatHangDanhMucQuery(null, null, context("PHARMACIST"), actor("PHARMACIST")));

        assertTrue(result.isSuccess());
        assertTrue(result.valueOrThrow().isEmpty());
        assertNull(fixture.keyword);
        assertNull(fixture.activeOnly);
        assertEquals(1, fixture.searchCalls);
        assertEquals(KiemToanHanhDongMa.CATALOG_VIEW, fixture.audit.entry.actionCode());
    }

    @Test
    void deniesInvalidRoleWithoutSearchingAndWritesForbiddenAudit() {
        Fixture fixture = new Fixture(List.of(item(12, "SYN-12", true)));

        KetQua<List<MatHangTongHopDuLieu>> result = fixture.handler().handle(fixture.query("BILLING"));

        assertTrue(result.isFailure());
        assertEquals("AUTHORIZATION_DENIED", result.getError().orElseThrow().code());
        assertEquals(0, fixture.searchCalls);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, fixture.audit.entry.actionCode());
        assertEquals(KiemToanKetQua.DENIED, fixture.audit.entry.outcome());
        assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, fixture.audit.entry.reasonCode());
        assertEquals(1, fixture.audit.independentWrites);
    }

    private static MatHang item(int id, String code, boolean active) {
        MatHang item = new MatHang();
        item.setId(id);
        item.setMaItem(code);
        item.setTenItem("Synthetic item " + id);
        item.setLoaiItem(LoaiMatHang.VAT_TU);
        item.setHoatChat("Synthetic substance");
        item.setHamLuong("20 mg");
        item.setDonViCoBan("EA");
        item.setGiaBanMacDinh(new BigDecimal("5.25"));
        item.setHoatDong(active);
        return item;
    }

    private static MatHangTongHopDuLieu summary(int id, String code, boolean active) {
        return new MatHangTongHopDuLieu(id, code, "Synthetic item " + id, LoaiMatHang.VAT_TU, "Synthetic substance",
                "20 mg", "EA", new BigDecimal("5.25"), active);
    }

    private static PhanQuyenNguCanh context(String role) {
        return new PhanQuyenNguCanh(17, 4, role, 3, 8, SESSION_ID);
    }

    private static KiemToanTacNhanNguCanh actor(String role) {
        return new KiemToanTacNhanNguCanh(17, 4, role, 3, 8, "station-1", CORRELATION_ID);
    }

    private static final class Fixture {
        final AuditFixture audit = new AuditFixture();
        final List<MatHang> items;
        final IKhoRepository inventory;
        String keyword;
        Boolean activeOnly;
        int searchCalls;

        Fixture(List<MatHang> items) {
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

        GetMatHangDanhMucHandler handler() {
            return new GetMatHangDanhMucHandler(inventory, audit);
        }

        GetMatHangDanhMucQuery query(String role) {
            return new GetMatHangDanhMucQuery("Synthetic query", true, context(role), actor(role));
        }
    }

    private static final class AuditFixture implements KiemToanWriteCongKetNoi {
        int independentWrites;
        HeThongKiemToanNhapLieu entry;

        @Override
        public void appendInCurrentTransaction(HeThongKiemToanNhapLieu auditEntry) {
            entry = auditEntry;
        }

        @Override
        public void appendIndependently(HeThongKiemToanNhapLieu auditEntry) {
            independentWrites++;
            entry = auditEntry;
        }

        @Override
        public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu auditEntry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu auditEntry) {
            throw new UnsupportedOperationException();
        }
    }
}
