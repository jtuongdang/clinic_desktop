package vn.clinicmanagement.application.inventory;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.KiemToanHanhDongMa;
import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.audit.KiemToanKetQua;
import vn.clinicmanagement.application.audit.KiemToanLyDoMa;
import vn.clinicmanagement.application.audit.KiemToanTaiNguyenLoai;
import vn.clinicmanagement.application.audit.KiemToanWriteCongKetNoi;
import vn.clinicmanagement.application.audit.YTeRecordKiemToanNhapLieu;
import vn.clinicmanagement.application.audit.HeThongKiemToanNhapLieu;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetTonKhoKhaDungHandlerTest {
    private static final int WAREHOUSE_ID = 44;
    private static final int ITEM_ID = 101;
    private static final int EMPLOYEE_ID = 17;
    private static final int ROLE_ID = 4;
    private static final int FACILITY_ID = 3;
    private static final int DEPARTMENT_ID = 8;
    private static final UUID SESSION_ID = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");
    private static final UUID CORRELATION_ID = UUID.fromString("0b8a0d60-0cc7-4fc8-9534-754f5f0e0017");

    @Test
    void allowsPharmacistDoctorAndAdminAndWritesCatalogAuditIndependently() {
        for (String roleCode : new String[]{"PHARMACIST", "DOCTOR", "ADMIN"}) {
            Fixture fixture = new Fixture(new Kho(), item(), List.of());

            KetQua<TonKhoKhaDungDuLieu> result = fixture.handler.handle(query(roleCode));

            assertTrue(result.isSuccess(), roleCode);
            assertEquals(0, result.valueOrThrow().batches().size());
            assertEquals(1, fixture.audit.independentEntries.size());
            assertSuccessAudit(fixture.audit.independentEntries.get(0));
        }
    }

    @Test
    void returnsSortedBatchesAndCountsOnlyPositiveUnexpiredStock() {
        LocalDate today = LocalDate.now();
        List<TonKho> batches = List.of(
                batch(1, "LOT-FUTURE", today.plusDays(5), "4"),
                batch(2, "LOT-EXPIRED", today.minusDays(1), "8"),
                batch(3, "LOT-TODAY", today, "2"),
                batch(4, "LOT-ZERO", today.plusDays(1), "0"),
                batch(5, "LOT-NO-EXPIRY", null, "3"));
        Fixture fixture = new Fixture(new Kho(), item(), batches);

        KetQua<TonKhoKhaDungDuLieu> result = fixture.handler.handle(query("PHARMACIST"));

        assertTrue(result.isSuccess());
        TonKhoKhaDungDuLieu dto = result.valueOrThrow();
        assertEquals(WAREHOUSE_ID, dto.warehouseId());
        assertEquals(ITEM_ID, dto.itemId());
        assertEquals("SYN-MED-101", dto.itemCode());
        assertEquals("Synthetic medicine", dto.itemName());
        assertEquals(new BigDecimal("9"), dto.tongTonKhaDung());
        assertEquals(List.of("LOT-EXPIRED", "LOT-TODAY", "LOT-ZERO", "LOT-FUTURE", "LOT-NO-EXPIRY"),
                dto.batches().stream().map(TonKhoLoDuLieu::soLo).toList());
        assertTrue(dto.batches().get(0).expired());
        assertFalse(dto.batches().get(1).expired());
        assertFalse(dto.batches().get(4).expired());
        assertEquals(1, fixture.audit.independentEntries.size());
        assertSuccessAudit(fixture.audit.independentEntries.get(0));
    }

    @Test
    void returnsEmptyBatchesAndZeroAvailabilityWhenRepositoryHasNoStock() {
        Fixture fixture = new Fixture(new Kho(), item(), List.of());

        KetQua<TonKhoKhaDungDuLieu> result = fixture.handler.handle(query("DOCTOR"));

        assertTrue(result.isSuccess());
        assertTrue(result.valueOrThrow().batches().isEmpty());
        assertEquals(BigDecimal.ZERO, result.valueOrThrow().tongTonKhaDung());
    }

    @Test
    void rejectsInvalidIdsBeforeAuthorizationOrRepositoryAccess() {
        for (int[] ids : new int[][]{{0, ITEM_ID}, {-1, ITEM_ID}, {WAREHOUSE_ID, 0}, {WAREHOUSE_ID, -1}}) {
            Fixture fixture = new Fixture(new Kho(), item(), List.of());

            KetQua<TonKhoKhaDungDuLieu> result = fixture.handler.handle(
                    new GetTonKhoKhaDungQuery(ids[0], ids[1], context("DOCTOR"), actor("DOCTOR")));

            assertFailure(result, "VALIDATION_ERROR", "Thông tin tra cứu kho hoặc mặt hàng không hợp lệ.");
            assertEquals(0, fixture.warehouseReads);
            assertEquals(0, fixture.itemReads);
            assertTrue(fixture.audit.independentEntries.isEmpty());
        }
    }

    @Test
    void returnsNotFoundWhenWarehouseDoesNotExist() {
        Fixture fixture = new Fixture(null, item(), List.of());

        KetQua<TonKhoKhaDungDuLieu> result = fixture.handler.handle(query("PHARMACIST"));

        assertFailure(result, "NOT_FOUND", "Không tìm thấy kho dược.");
        assertEquals(1, fixture.warehouseReads);
        assertEquals(0, fixture.itemReads);
        assertTrue(fixture.audit.independentEntries.isEmpty());
    }

    @Test
    void returnsNotFoundWhenItemDoesNotExist() {
        Fixture fixture = new Fixture(new Kho(), null, List.of());

        KetQua<TonKhoKhaDungDuLieu> result = fixture.handler.handle(query("PHARMACIST"));

        assertFailure(result, "NOT_FOUND", "Không tìm thấy mặt hàng.");
        assertEquals(1, fixture.warehouseReads);
        assertEquals(1, fixture.itemReads);
        assertEquals(0, fixture.batchReads);
        assertTrue(fixture.audit.independentEntries.isEmpty());
    }

    @Test
    void deniesUnauthorizedRoleAndUnauthenticatedCallerWithForbiddenAudit() {
        Fixture receptionist = new Fixture(new Kho(), item(), List.of());
        KetQua<TonKhoKhaDungDuLieu> receptionistResult = receptionist.handler.handle(query("RECEPTIONIST"));

        assertFailure(receptionistResult, "AUTH_ACCESS_DENIED",
                "Không có quyền tra cứu tồn kho trong phiên hiện tại.");
        assertEquals(0, receptionist.warehouseReads);
        assertDeniedAudit(receptionist.audit.independentEntries.get(0));

        Fixture unauthenticated = new Fixture(new Kho(), item(), List.of());
        KetQua<TonKhoKhaDungDuLieu> unauthenticatedResult = unauthenticated.handler.handle(
                new GetTonKhoKhaDungQuery(WAREHOUSE_ID, ITEM_ID, null, null));

        assertFailure(unauthenticatedResult, "AUTH_ACCESS_DENIED",
                "Không có quyền tra cứu tồn kho trong phiên hiện tại.");
        assertEquals(0, unauthenticated.warehouseReads);
        assertDeniedAudit(unauthenticated.audit.independentEntries.get(0));
    }

    @Test
    void convenienceConstructorCreatesAuditActorFromValidAuthorizationContext() {
        GetTonKhoKhaDungQuery query =
                new GetTonKhoKhaDungQuery(WAREHOUSE_ID, ITEM_ID, context("PHARMACIST"));

        assertNotNull(query.auditActorContext());
        assertEquals(EMPLOYEE_ID, query.auditActorContext().employeeId());
        assertEquals("PHARMACIST", query.auditActorContext().roleCode());
        assertNotNull(query.auditActorContext().correlationId());
    }

    private static GetTonKhoKhaDungQuery query(String roleCode) {
        return new GetTonKhoKhaDungQuery(WAREHOUSE_ID, ITEM_ID, context(roleCode), actor(roleCode));
    }

    private static PhanQuyenNguCanh context(String roleCode) {
        return new PhanQuyenNguCanh(
                EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    }

    private static KiemToanTacNhanNguCanh actor(String roleCode) {
        return new KiemToanTacNhanNguCanh(
                EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID, "station-1", CORRELATION_ID);
    }

    private static MatHang item() {
        MatHang item = new MatHang();
        item.setMaItem("SYN-MED-101");
        item.setTenItem("Synthetic medicine");
        return item;
    }

    private static TonKho batch(int id, String lot, LocalDate expiryDate, String quantity) {
        TonKho batch = new TonKho();
        batch.setId(id);
        batch.setSoLo(lot);
        batch.setHanSuDung(expiryDate);
        batch.setSoLuongTon(new BigDecimal(quantity));
        batch.setGiaNhap(new BigDecimal("10.00"));
        batch.setGiaBan(new BigDecimal("12.00"));
        return batch;
    }

    private static void assertSuccessAudit(HeThongKiemToanNhapLieu entry) {
        assertNotNull(entry);
        assertEquals(KiemToanHanhDongMa.CATALOG_VIEW, entry.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.ITEM, entry.resourceType());
        assertEquals(ITEM_ID, entry.resourceId());
        assertEquals(KiemToanKetQua.SUCCESS, entry.outcome());
        assertNull(entry.reasonCode());
        assertEquals(CORRELATION_ID, entry.correlationId());
    }

    private static void assertDeniedAudit(HeThongKiemToanNhapLieu entry) {
        assertNotNull(entry);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, entry.actionCode());
        assertNull(entry.resourceType());
        assertNull(entry.resourceId());
        assertEquals(KiemToanKetQua.DENIED, entry.outcome());
        assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, entry.reasonCode());
    }

    private static void assertFailure(KetQua<?> result, String code, String message) {
        assertFalse(result.isSuccess());
        assertEquals(code, result.getError().orElseThrow().code());
        assertEquals(message, result.getError().orElseThrow().message());
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String method, Object[] args);
    }

    private static <T> T proxy(Class<T> type, Invocation invocation) {
        Object instance = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> type.getSimpleName() + " synthetic test proxy";
                    default -> null;
                };
            }
            return invocation.invoke(method.getName(), args);
        });
        return type.cast(instance);
    }

    private static final class Fixture {
        final GetTonKhoKhaDungHandler handler;
        final AuditFixture audit = new AuditFixture();
        final Kho warehouse;
        final MatHang item;
        final List<TonKho> batches;
        int warehouseReads;
        int itemReads;
        int batchReads;

        Fixture(Kho warehouse, MatHang item, List<TonKho> batches) {
            this.warehouse = warehouse;
            this.item = item;
            this.batches = batches;
            IKhoRepository repository = proxy(IKhoRepository.class, (method, args) -> switch (method) {
                case "getKhoById" -> {
                    warehouseReads++;
                    yield Optional.ofNullable(this.warehouse);
                }
                case "getItemById" -> {
                    itemReads++;
                    yield Optional.ofNullable(this.item);
                }
                case "getAvailableBatches" -> {
                    batchReads++;
                    yield this.batches;
                }
                default -> null;
            });
            handler = new GetTonKhoKhaDungHandler(repository, audit);
        }
    }

    private static final class AuditFixture implements KiemToanWriteCongKetNoi {
        final List<HeThongKiemToanNhapLieu> independentEntries = new ArrayList<>();

        @Override
        public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void appendIndependently(HeThongKiemToanNhapLieu entry) {
            independentEntries.add(entry);
        }

        @Override
        public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {
            throw new UnsupportedOperationException();
        }
    }
}
