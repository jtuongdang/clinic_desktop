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
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.PhieuNhapKho;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiveKhoTonKhoHandlerTest {

    private static final int WAREHOUSE_ID = 10;
    private static final int SUPPLIER_ID = 20;
    private static final String SO_PHIEU = "PN-20261107-001";
    private static final UUID SESSION_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID CORRELATION_ID = UUID.fromString("66666666-7777-8888-9999-aaaaaaaaaaaa");

    private static final PhanQuyenNguCanh PHARMACIST_CONTEXT =
            new PhanQuyenNguCanh(5, 1, "PHARMACIST", 100, 200, SESSION_ID);

    private static final PhanQuyenNguCanh ADMIN_CONTEXT =
            new PhanQuyenNguCanh(1, 2, "ADMIN", 100, 200, SESSION_ID);

    private static final PhanQuyenNguCanh DOCTOR_CONTEXT =
            new PhanQuyenNguCanh(9, 3, "DOCTOR", 100, 200, SESSION_ID);

    @Test
    void testSuccessfulReceiptByPharmacist() {
        Fixture fixture = new Fixture();
        List<TonKhoBienNhanMatHangDauVao> items = List.of(
                new TonKhoBienNhanMatHangDauVao(1, "L001", LocalDate.now().plusMonths(6),
                        new BigDecimal("10"), new BigDecimal("5000"), new BigDecimal("6000")),
                new TonKhoBienNhanMatHangDauVao(2, "L002", LocalDate.now().plusYears(1),
                        new BigDecimal("20"), new BigDecimal("3000"), new BigDecimal("4000")));

        ReceiveKhoTonKhoCommand command = new ReceiveKhoTonKhoCommand(
                SO_PHIEU, WAREHOUSE_ID, SUPPLIER_ID, items, PHARMACIST_CONTEXT);

        KetQua<ReceiveKhoTonKhoBienNhan> result = fixture.handler.handle(command);

        assertTrue(result.isSuccess());
        ReceiveKhoTonKhoBienNhan receipt = result.valueOrThrow();
        assertEquals(101, receipt.receiptId());
        assertEquals(SO_PHIEU, receipt.soPhieuNhap());
        assertEquals(new BigDecimal("110000"), receipt.tongTien());
        assertEquals(2, receipt.totalBatchesReceived());

        assertEquals(1, fixture.transactions.calls);
        assertEquals(1, fixture.savedPhieuList.size());
        assertEquals(2, fixture.savedTonKhoList.size());

        PhieuNhapKho savedPhieu = fixture.savedPhieuList.get(0);
        assertEquals(SO_PHIEU, savedPhieu.getSoPhieuNhap());
        assertEquals(WAREHOUSE_ID, savedPhieu.getMaKho());
        assertEquals(SUPPLIER_ID, savedPhieu.getMaNhaCungCap());
        assertEquals(new BigDecimal("110000"), savedPhieu.getTongTien());
        assertEquals(2, savedPhieu.getChiTiets().size());

        TonKho batch1 = fixture.savedTonKhoList.get(0);
        assertEquals(WAREHOUSE_ID, batch1.getMaKho());
        assertEquals(1, batch1.getMaItem());
        assertEquals("L001", batch1.getSoLo());
        assertEquals(new BigDecimal("10"), batch1.getSoLuongTon());
        assertEquals(new BigDecimal("5000"), batch1.getGiaNhap());
        assertEquals(new BigDecimal("6000"), batch1.getGiaBan());

        assertEquals(1, fixture.audit.independentEntries.size());
        HeThongKiemToanNhapLieu auditEntry = fixture.audit.independentEntries.get(0);
        assertEquals(KiemToanHanhDongMa.INV_RECEIPT_CREATED, auditEntry.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.INVENTORY_RECEIPT, auditEntry.resourceType());
        assertEquals(101, auditEntry.resourceId());
        assertEquals(KiemToanKetQua.SUCCESS, auditEntry.outcome());
    }

    @Test
    void testSuccessfulReceiptByAdmin() {
        Fixture fixture = new Fixture();
        List<TonKhoBienNhanMatHangDauVao> items = List.of(
                new TonKhoBienNhanMatHangDauVao(1, "L001", LocalDate.now().plusMonths(3),
                        new BigDecimal("5"), new BigDecimal("10000"), new BigDecimal("12000")));

        ReceiveKhoTonKhoCommand command = new ReceiveKhoTonKhoCommand(
                SO_PHIEU, WAREHOUSE_ID, SUPPLIER_ID, items, ADMIN_CONTEXT);

        KetQua<ReceiveKhoTonKhoBienNhan> result = fixture.handler.handle(command);

        assertTrue(result.isSuccess());
        ReceiveKhoTonKhoBienNhan receipt = result.valueOrThrow();
        assertEquals(101, receipt.receiptId());
        assertEquals(new BigDecimal("50000"), receipt.tongTien());
        assertEquals(1, receipt.totalBatchesReceived());

        assertEquals(1, fixture.audit.independentEntries.size());
        assertEquals(KiemToanHanhDongMa.INV_RECEIPT_CREATED, fixture.audit.independentEntries.get(0).actionCode());
        assertEquals(KiemToanKetQua.SUCCESS, fixture.audit.independentEntries.get(0).outcome());
    }

    @Test
    void testExpiredBatchRejection() {
        Fixture fixture = new Fixture();
        List<TonKhoBienNhanMatHangDauVao> items = List.of(
                new TonKhoBienNhanMatHangDauVao(1, "L-EXP", LocalDate.now().minusDays(1),
                        new BigDecimal("10"), new BigDecimal("5000"), new BigDecimal("6000")));

        ReceiveKhoTonKhoCommand command = new ReceiveKhoTonKhoCommand(
                SO_PHIEU, WAREHOUSE_ID, SUPPLIER_ID, items, PHARMACIST_CONTEXT);

        KetQua<ReceiveKhoTonKhoBienNhan> result = fixture.handler.handle(command);

        assertTrue(result.isFailure());
        assertEquals("EXPIRED_BATCH", result.getErrorCode().orElse(null));
        assertEquals("Không thể nhập lô hàng đã hết hạn sử dụng.", result.getError().orElseThrow().message());

        assertEquals(0, fixture.transactions.calls);
        assertTrue(fixture.savedPhieuList.isEmpty());
        assertTrue(fixture.savedTonKhoList.isEmpty());
        assertTrue(fixture.audit.independentEntries.isEmpty());
    }

    @Test
    void testEmptyItemsValidation() {
        Fixture fixture = new Fixture();
        ReceiveKhoTonKhoCommand command = new ReceiveKhoTonKhoCommand(
                SO_PHIEU, WAREHOUSE_ID, SUPPLIER_ID, List.of(), PHARMACIST_CONTEXT);

        KetQua<ReceiveKhoTonKhoBienNhan> result = fixture.handler.handle(command);

        assertTrue(result.isFailure());
        assertEquals("INVALID_INPUT", result.getErrorCode().orElse(null));
        assertEquals(0, fixture.transactions.calls);
        assertTrue(fixture.savedPhieuList.isEmpty());
        assertTrue(fixture.savedTonKhoList.isEmpty());
    }

    @Test
    void testWarehouseNotFound() {
        Fixture fixture = new Fixture();
        fixture.warehouseExists = false;

        List<TonKhoBienNhanMatHangDauVao> items = List.of(
                new TonKhoBienNhanMatHangDauVao(1, "L001", LocalDate.now().plusMonths(6),
                        new BigDecimal("10"), new BigDecimal("5000"), new BigDecimal("6000")));

        ReceiveKhoTonKhoCommand command = new ReceiveKhoTonKhoCommand(
                SO_PHIEU, WAREHOUSE_ID, SUPPLIER_ID, items, PHARMACIST_CONTEXT);

        KetQua<ReceiveKhoTonKhoBienNhan> result = fixture.handler.handle(command);

        assertTrue(result.isFailure());
        assertEquals("NOT_FOUND", result.getErrorCode().orElse(null));
        assertEquals("Không tìm thấy kho hàng.", result.getError().orElseThrow().message());

        assertEquals(0, fixture.transactions.calls);
        assertTrue(fixture.savedPhieuList.isEmpty());
        assertTrue(fixture.savedTonKhoList.isEmpty());
    }

    @Test
    void testUnauthorizedRoleDenial() {
        Fixture fixture = new Fixture();
        List<TonKhoBienNhanMatHangDauVao> items = List.of(
                new TonKhoBienNhanMatHangDauVao(1, "L001", LocalDate.now().plusMonths(6),
                        new BigDecimal("10"), new BigDecimal("5000"), new BigDecimal("6000")));

        ReceiveKhoTonKhoCommand command = new ReceiveKhoTonKhoCommand(
                SO_PHIEU, WAREHOUSE_ID, SUPPLIER_ID, items, DOCTOR_CONTEXT);

        KetQua<ReceiveKhoTonKhoBienNhan> result = fixture.handler.handle(command);

        assertTrue(result.isFailure());
        assertEquals("AUTHORIZATION_DENIED", result.getErrorCode().orElse(null));
        assertEquals(0, fixture.transactions.calls);
        assertTrue(fixture.savedPhieuList.isEmpty());
        assertTrue(fixture.savedTonKhoList.isEmpty());

        assertEquals(1, fixture.audit.independentEntries.size());
        HeThongKiemToanNhapLieu entry = fixture.audit.independentEntries.get(0);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, entry.actionCode());
        assertEquals(KiemToanKetQua.DENIED, entry.outcome());
        assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, entry.reasonCode());
        assertNull(entry.resourceType());
        assertNull(entry.resourceId());
    }

    @Test
    void testAuditLoggingVerification() {
        Fixture fixture = new Fixture();
        List<TonKhoBienNhanMatHangDauVao> items = List.of(
                new TonKhoBienNhanMatHangDauVao(1, "L001", LocalDate.now().plusMonths(6),
                        new BigDecimal("10"), new BigDecimal("5000"), new BigDecimal("6000")));

        KiemToanTacNhanNguCanh actorContext = new KiemToanTacNhanNguCanh(
                PHARMACIST_CONTEXT.employeeId(),
                PHARMACIST_CONTEXT.roleId(),
                PHARMACIST_CONTEXT.roleCode(),
                PHARMACIST_CONTEXT.facilityId(),
                PHARMACIST_CONTEXT.departmentId(),
                "WS-PHARM-01",
                CORRELATION_ID);

        ReceiveKhoTonKhoCommand command = new ReceiveKhoTonKhoCommand(
                SO_PHIEU, WAREHOUSE_ID, SUPPLIER_ID, items, PHARMACIST_CONTEXT, actorContext);

        KetQua<ReceiveKhoTonKhoBienNhan> result = fixture.handler.handle(command);

        assertTrue(result.isSuccess());
        assertEquals(1, fixture.audit.independentEntries.size());
        HeThongKiemToanNhapLieu entry = fixture.audit.independentEntries.get(0);
        assertEquals(Integer.valueOf(5), entry.actorEmployeeId());
        assertEquals(Integer.valueOf(1), entry.roleId());
        assertEquals("PHARMACIST", entry.roleCode());
        assertEquals(Integer.valueOf(100), entry.facilityId());
        assertEquals(Integer.valueOf(200), entry.departmentId());
        assertEquals(KiemToanHanhDongMa.INV_RECEIPT_CREATED, entry.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.INVENTORY_RECEIPT, entry.resourceType());
        assertEquals(Integer.valueOf(101), entry.resourceId());
        assertEquals(KiemToanKetQua.SUCCESS, entry.outcome());
        assertNull(entry.reasonCode());
        assertNotNull(entry.occurredAtUtc());
        assertEquals("WS-PHARM-01", entry.workstationId());
        assertEquals(CORRELATION_ID, entry.correlationId());
    }

    @Test
    void testBatchExpiringTodayIsAccepted() {
        Fixture fixture = new Fixture();
        List<TonKhoBienNhanMatHangDauVao> items = List.of(
                new TonKhoBienNhanMatHangDauVao(1, "L-TODAY", LocalDate.now(),
                        new BigDecimal("5"), new BigDecimal("2000"), new BigDecimal("2500")));

        ReceiveKhoTonKhoCommand command = new ReceiveKhoTonKhoCommand(
                SO_PHIEU, WAREHOUSE_ID, SUPPLIER_ID, items, PHARMACIST_CONTEXT);

        KetQua<ReceiveKhoTonKhoBienNhan> result = fixture.handler.handle(command);

        assertTrue(result.isSuccess());
        assertEquals(1, fixture.transactions.calls);
    }

    @Test
    void testInvalidInputsValidation() {
        Fixture fixture = new Fixture();
        List<TonKhoBienNhanMatHangDauVao> validItems = List.of(
                new TonKhoBienNhanMatHangDauVao(1, "L001", LocalDate.now().plusMonths(6),
                        new BigDecimal("10"), new BigDecimal("5000"), new BigDecimal("6000")));

        // Null command
        KetQua<ReceiveKhoTonKhoBienNhan> r0 = fixture.handler.handle(null);
        assertTrue(r0.isFailure());
        assertEquals("INVALID_INPUT", r0.getErrorCode().orElse(null));

        // Warehouse ID <= 0
        ReceiveKhoTonKhoCommand cmdWarehouse = new ReceiveKhoTonKhoCommand(
                SO_PHIEU, 0, SUPPLIER_ID, validItems, PHARMACIST_CONTEXT);
        KetQua<ReceiveKhoTonKhoBienNhan> r1 = fixture.handler.handle(cmdWarehouse);
        assertTrue(r1.isFailure());
        assertEquals("INVALID_INPUT", r1.getErrorCode().orElse(null));

        // Supplier ID <= 0
        ReceiveKhoTonKhoCommand cmdSupplier = new ReceiveKhoTonKhoCommand(
                SO_PHIEU, WAREHOUSE_ID, -1, validItems, PHARMACIST_CONTEXT);
        KetQua<ReceiveKhoTonKhoBienNhan> r2 = fixture.handler.handle(cmdSupplier);
        assertTrue(r2.isFailure());
        assertEquals("INVALID_INPUT", r2.getErrorCode().orElse(null));

        // Blank soPhieuNhap
        ReceiveKhoTonKhoCommand cmdPhieu = new ReceiveKhoTonKhoCommand(
                "   ", WAREHOUSE_ID, SUPPLIER_ID, validItems, PHARMACIST_CONTEXT);
        KetQua<ReceiveKhoTonKhoBienNhan> r3 = fixture.handler.handle(cmdPhieu);
        assertTrue(r3.isFailure());
        assertEquals("INVALID_INPUT", r3.getErrorCode().orElse(null));

        // Item with itemId <= 0
        ReceiveKhoTonKhoCommand cmdItemId = new ReceiveKhoTonKhoCommand(
                SO_PHIEU, WAREHOUSE_ID, SUPPLIER_ID,
                List.of(new TonKhoBienNhanMatHangDauVao(0, "L001", LocalDate.now().plusMonths(6),
                        new BigDecimal("10"), new BigDecimal("5000"), new BigDecimal("6000"))),
                PHARMACIST_CONTEXT);
        KetQua<ReceiveKhoTonKhoBienNhan> r4 = fixture.handler.handle(cmdItemId);
        assertTrue(r4.isFailure());
        assertEquals("INVALID_INPUT", r4.getErrorCode().orElse(null));

        // Item with blank soLo
        ReceiveKhoTonKhoCommand cmdSoLo = new ReceiveKhoTonKhoCommand(
                SO_PHIEU, WAREHOUSE_ID, SUPPLIER_ID,
                List.of(new TonKhoBienNhanMatHangDauVao(1, "", LocalDate.now().plusMonths(6),
                        new BigDecimal("10"), new BigDecimal("5000"), new BigDecimal("6000"))),
                PHARMACIST_CONTEXT);
        KetQua<ReceiveKhoTonKhoBienNhan> r5 = fixture.handler.handle(cmdSoLo);
        assertTrue(r5.isFailure());
        assertEquals("INVALID_INPUT", r5.getErrorCode().orElse(null));

        // Item with null hanSuDung
        ReceiveKhoTonKhoCommand cmdHsd = new ReceiveKhoTonKhoCommand(
                SO_PHIEU, WAREHOUSE_ID, SUPPLIER_ID,
                List.of(new TonKhoBienNhanMatHangDauVao(1, "L001", null,
                        new BigDecimal("10"), new BigDecimal("5000"), new BigDecimal("6000"))),
                PHARMACIST_CONTEXT);
        KetQua<ReceiveKhoTonKhoBienNhan> r6 = fixture.handler.handle(cmdHsd);
        assertTrue(r6.isFailure());
        assertEquals("INVALID_INPUT", r6.getErrorCode().orElse(null));

        // Item with soLuong <= 0
        ReceiveKhoTonKhoCommand cmdQty = new ReceiveKhoTonKhoCommand(
                SO_PHIEU, WAREHOUSE_ID, SUPPLIER_ID,
                List.of(new TonKhoBienNhanMatHangDauVao(1, "L001", LocalDate.now().plusMonths(6),
                        BigDecimal.ZERO, new BigDecimal("5000"), new BigDecimal("6000"))),
                PHARMACIST_CONTEXT);
        KetQua<ReceiveKhoTonKhoBienNhan> r7 = fixture.handler.handle(cmdQty);
        assertTrue(r7.isFailure());
        assertEquals("INVALID_INPUT", r7.getErrorCode().orElse(null));
    }

    @Test
    void testConvenienceConstructorCreatesAuditActor() {
        List<TonKhoBienNhanMatHangDauVao> items = List.of(
                new TonKhoBienNhanMatHangDauVao(1, "L001", LocalDate.now().plusMonths(6),
                        new BigDecimal("10"), new BigDecimal("5000"), new BigDecimal("6000")));

        ReceiveKhoTonKhoCommand command = new ReceiveKhoTonKhoCommand(
                SO_PHIEU, WAREHOUSE_ID, SUPPLIER_ID, items, PHARMACIST_CONTEXT);

        assertNotNull(command.auditActorContext());
        assertEquals(PHARMACIST_CONTEXT.employeeId(), command.auditActorContext().employeeId());
        assertEquals(PHARMACIST_CONTEXT.roleId(), command.auditActorContext().roleId());
        assertEquals(PHARMACIST_CONTEXT.roleCode(), command.auditActorContext().roleCode());
        assertEquals(PHARMACIST_CONTEXT.facilityId(), command.auditActorContext().facilityId());
        assertEquals(PHARMACIST_CONTEXT.departmentId(), command.auditActorContext().departmentId());
        assertEquals(PHARMACIST_CONTEXT.sessionId(), command.auditActorContext().correlationId());
    }

    private static final class Fixture {
        private boolean warehouseExists = true;
        private final List<PhieuNhapKho> savedPhieuList = new ArrayList<>();
        private final List<TonKho> savedTonKhoList = new ArrayList<>();
        private final SyntheticTransactionBoundary transactions = new SyntheticTransactionBoundary();
        private final SyntheticAuditWritePort audit = new SyntheticAuditWritePort();
        private final ReceiveKhoTonKhoHandler handler;

        private Fixture() {
            IKhoRepository repository = proxy(IKhoRepository.class, (method, args) -> switch (method) {
                case "getKhoById" -> {
                    int id = (int) args[0];
                    if (warehouseExists && id == WAREHOUSE_ID) {
                        Kho kho = new Kho();
                        kho.setId(WAREHOUSE_ID);
                        kho.setTenKho("Kho Chinh");
                        yield Optional.of(kho);
                    }
                    yield Optional.empty();
                }
                case "savePhieuNhapKho" -> {
                    PhieuNhapKho phieu = (PhieuNhapKho) args[0];
                    phieu.setId(101);
                    savedPhieuList.add(phieu);
                    yield phieu;
                }
                case "saveTonKho" -> {
                    TonKho tonKho = (TonKho) args[0];
                    tonKho.setId(200 + savedTonKhoList.size() + 1);
                    savedTonKhoList.add(tonKho);
                    yield tonKho;
                }
                default -> defaultValue(method);
            });
            this.handler = new ReceiveKhoTonKhoHandler(repository, transactions, audit);
        }
    }

    private static Object defaultValue(String method) {
        return switch (method) {
            case "getItemById", "getItemByCode", "getKhoById", "getBatchById",
                 "getPhieuNhapKhoById", "getPhieuXuatKhoById", "getPurchaseOrderById",
                 "getStocktakeById" -> Optional.empty();
            case "searchItems", "getAvailableBatches", "getBatchesByKho" -> List.of();
            default -> null;
        };
    }

    @FunctionalInterface
    private interface Invocation {
        Object invoke(String method, Object[] args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Invocation handler) {
        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, method, args) -> {
            if (method.getName().equals("equals")) return p == args[0];
            if (method.getName().equals("hashCode")) return System.identityHashCode(p);
            if (method.getName().equals("toString")) return type.getSimpleName() + "Proxy";
            return handler.invoke(method.getName(), args == null ? new Object[0] : args);
        });
        return type.cast(proxy);
    }

    private static final class SyntheticTransactionBoundary implements GiaoDichRanhGioi {
        private int calls = 0;

        @Override
        public <T> KetQua<T> inTransaction(Supplier<KetQua<T>> operation) {
            calls++;
            return operation.get();
        }
    }

    private static final class SyntheticAuditWritePort implements KiemToanWriteCongKetNoi {
        private final List<HeThongKiemToanNhapLieu> independentEntries = new ArrayList<>();
        private final List<HeThongKiemToanNhapLieu> transactionalEntries = new ArrayList<>();

        @Override
        public void appendInCurrentTransaction(HeThongKiemToanNhapLieu entry) {
            transactionalEntries.add(entry);
        }

        @Override
        public void appendIndependently(HeThongKiemToanNhapLieu entry) {
            independentEntries.add(entry);
        }

        @Override
        public void appendMedicalRecordInCurrentTransaction(YTeRecordKiemToanNhapLieu entry) {}

        @Override
        public void appendMedicalRecordIndependently(YTeRecordKiemToanNhapLieu entry) {}
    }
}
