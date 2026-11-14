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

class CreateMuaHangYeuCauHandlerTest {
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
        List<CreateMuaHangYeuCauCommand> commands = List.of(
                fixture.command("SYN-PO-001", 0, List.of(validItem()), "ADMIN"),
                fixture.command("SYN-PO-001", 5, List.of(), "ADMIN"),
                fixture.command("SYN-PO-001", 5,
                        List.of(new MuaHangYeuCauMatHangDauVao(12, new BigDecimal("-1"), new BigDecimal("2.50"))),
                        "ADMIN"));

        for (CreateMuaHangYeuCauCommand command : commands) {
            KetQua<CreateMuaHangYeuCauBienNhan> result = fixture.handler().handle(command);
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
                        "SYN-PO-001", 5, List.of(new MuaHangYeuCauMatHangDauVao(0, BigDecimal.ONE, BigDecimal.ONE)),
                        "ADMIN")),
                "VALIDATION_ERROR", "Thông tin đơn đặt hàng không hợp lệ.");
        assertFailure(fixture.handler().handle(fixture.command(
                        "SYN-PO-001", 5, List.of(new MuaHangYeuCauMatHangDauVao(12, BigDecimal.ONE, null)), "ADMIN")),
                "VALIDATION_ERROR", "Thông tin đơn đặt hàng không hợp lệ.");

        assertEquals(0, fixture.transactions.calls);
        assertEquals(0, fixture.saveCalls);
        assertNull(fixture.audit.entry);
    }

    @Test
    void deniesUnauthorizedRoleAndWritesForbiddenAuditIndependently() {
        Fixture fixture = new Fixture();

        KetQua<CreateMuaHangYeuCauBienNhan> result =
                fixture.handler().handle(fixture.command("DOCTOR"));

        assertFailure(result, "AUTH_ACCESS_DENIED",
                "Không được phép tạo đơn đặt hàng trong phiên hiện tại.");
        assertEquals(0, fixture.transactions.calls);
        assertEquals(0, fixture.saveCalls);
        assertEquals(1, fixture.audit.independentWrites);
        assertEquals(0, fixture.audit.inTransactionWrites);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, fixture.audit.entry.actionCode());
        assertEquals(KiemToanKetQua.DENIED, fixture.audit.entry.outcome());
        assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, fixture.audit.entry.reasonCode());
    }

    @Test
    void convenienceConstructorCreatesAuditActorFromAuthorizationContext() {
        CreateMuaHangYeuCauCommand command = new CreateMuaHangYeuCauCommand(
                "SYN-PO-001", 5, List.of(validItem()), context("ADMIN"));

        assertNotNull(command.auditActorContext());
        assertEquals(17, command.auditActorContext().employeeId());
        assertEquals("ADMIN", command.auditActorContext().roleCode());
    }

    private static void assertPurchaseOrderCreated(String role) {
        Fixture fixture = new Fixture();

        KetQua<CreateMuaHangYeuCauBienNhan> result = fixture.handler().handle(fixture.command(role));

        assertTrue(result.isSuccess());
        assertEquals(new CreateMuaHangYeuCauBienNhan(91, "SYN-PO-001", new BigDecimal("11.00")),
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
        assertEquals(KiemToanHanhDongMa.INV_RECEIPT_CREATED, fixture.audit.entry.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.INVENTORY_RECEIPT, fixture.audit.entry.resourceType());
        assertEquals(91, fixture.audit.entry.resourceId());
        assertEquals(KiemToanKetQua.SUCCESS, fixture.audit.entry.outcome());
    }

    private static MuaHangYeuCauMatHangDauVao validItem() {
        return new MuaHangYeuCauMatHangDauVao(12, new BigDecimal("2"), new BigDecimal("2.50"));
    }

    private static void assertFailure(KetQua<CreateMuaHangYeuCauBienNhan> result, String code, String message) {
        assertTrue(result.isFailure());
        assertEquals(code, result.getError().orElseThrow().code());
        assertEquals(message, result.getError().orElseThrow().message());
    }

    private static PhanQuyenNguCanh context(String role) {
        return new PhanQuyenNguCanh(17, 4, role, 3, 8, SESSION_ID);
    }

    private static KiemToanTacNhanNguCanh actor(String role) {
        return new KiemToanTacNhanNguCanh(17, 4, role, 3, 8, "station-1", CORRELATION_ID);
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

        CreateMuaHangYeuCauHandler handler() {
            return new CreateMuaHangYeuCauHandler(inventory, transactions, audit);
        }

        CreateMuaHangYeuCauCommand command(String role) {
            return command("SYN-PO-001", 5, List.of(
                    validItem(),
                    new MuaHangYeuCauMatHangDauVao(13, new BigDecimal("3"), new BigDecimal("2"))), role);
        }

        CreateMuaHangYeuCauCommand command(
                String orderNumber, int supplierId, List<MuaHangYeuCauMatHangDauVao> items, String role) {
            return new CreateMuaHangYeuCauCommand(
                    orderNumber, supplierId, items, context(role), actor(role));
        }
    }

    private static final class TransactionFixture implements GiaoDichRanhGioi {
        int calls;

        @Override
        public <T> KetQua<T> inTransaction(Supplier<KetQua<T>> operation) {
            calls++;
            return operation.get();
        }
    }

    private static final class AuditFixture implements KiemToanWriteCongKetNoi {
        int independentWrites;
        int inTransactionWrites;
        HeThongKiemToanNhapLieu entry;

        @Override
        public void appendInCurrentTransaction(HeThongKiemToanNhapLieu auditEntry) {
            inTransactionWrites++;
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
