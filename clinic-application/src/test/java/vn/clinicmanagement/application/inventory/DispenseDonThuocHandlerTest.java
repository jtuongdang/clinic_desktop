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
import vn.clinicmanagement.domain.common.enums.LoaiXuatKho;
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.inventory.FEFOPhanBoDichVu;
import vn.clinicmanagement.domain.inventory.Kho;
import vn.clinicmanagement.domain.inventory.PhieuXuatKho;
import vn.clinicmanagement.domain.inventory.TonKho;
import vn.clinicmanagement.domain.prescription.ChiTietDonThuoc;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.repositories.IDonThuocRepository;
import vn.clinicmanagement.domain.repositories.IKhoRepository;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DispenseDonThuocHandlerTest {

    private static final int PRESCRIPTION_ID = 10;
    private static final int PHARMACIST_ID = 20;
    private static final int WAREHOUSE_ID = 30;
    private static final int EMPLOYEE_ID = 20;
    private static final int ROLE_ID = 5;
    private static final int FACILITY_ID = 1;
    private static final int DEPARTMENT_ID = 2;
    private static final UUID SESSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void successfulDispensingByPharmacist() {
        assertSuccessfulDispensing("PHARMACIST");
    }

    @Test
    void successfulDispensingByAdmin() {
        assertSuccessfulDispensing("ADMIN");
    }

    private void assertSuccessfulDispensing(String roleCode) {
        Fixture fixture = new Fixture();
        DonThuoc prescription = createApprovedPrescription(PRESCRIPTION_ID);

        ChiTietDonThuoc item1 = createPrescriptionItem(101, new BigDecimal("5"), BigDecimal.ZERO, new BigDecimal("10000"));
        ChiTietDonThuoc item2 = createPrescriptionItem(102, new BigDecimal("2"), BigDecimal.ZERO, new BigDecimal("25000"));
        prescription.getChiTietDonThuocs().add(item1);
        prescription.getChiTietDonThuocs().add(item2);
        fixture.prescription = prescription;

        LocalDate today = LocalDate.now();
        TonKho batch1 = createTonKho(1, 101, "LOT-01", today.plusDays(10), new BigDecimal("3"), new BigDecimal("10000"));
        TonKho batch2 = createTonKho(2, 101, "LOT-02", today.plusDays(20), new BigDecimal("10"), new BigDecimal("10000"));
        fixture.batchesByItem.put(101, List.of(batch1, batch2));

        TonKho batch3 = createTonKho(3, 102, "LOT-03", today.plusDays(15), new BigDecimal("5"), new BigDecimal("25000"));
        fixture.batchesByItem.put(102, List.of(batch3));

        DispenseDonThuocCommand command = new DispenseDonThuocCommand(
                PRESCRIPTION_ID, PHARMACIST_ID, WAREHOUSE_ID, context(roleCode), actor(roleCode));

        KetQua<DispenseDonThuocBienNhan> result = fixture.handler().handle(command);

        assertTrue(result.isSuccess(), "Dispensing should succeed for " + roleCode);
        DispenseDonThuocBienNhan receipt = result.valueOrThrow();

        assertEquals(PRESCRIPTION_ID, receipt.prescriptionId());
        assertEquals(501, receipt.issueReceiptId());
        assertEquals("PXK-" + PRESCRIPTION_ID, receipt.soPhieuXuat());
        // 3 * 10000 + 2 * 10000 + 2 * 25000 = 100000
        assertEquals(0, new BigDecimal("100000").compareTo(receipt.tongTien()));
        assertEquals(3, receipt.allocations().size());

        DaCapPhatMatHangPhanBoDuLieu alloc1 = receipt.allocations().get(0);
        assertEquals(101, alloc1.itemId());
        assertEquals(1, alloc1.tonKhoId());
        assertEquals("LOT-01", alloc1.soLo());
        assertEquals(0, new BigDecimal("3").compareTo(alloc1.quantityDispensed()));

        DaCapPhatMatHangPhanBoDuLieu alloc2 = receipt.allocations().get(1);
        assertEquals(101, alloc2.itemId());
        assertEquals(2, alloc2.tonKhoId());
        assertEquals("LOT-02", alloc2.soLo());
        assertEquals(0, new BigDecimal("2").compareTo(alloc2.quantityDispensed()));

        DaCapPhatMatHangPhanBoDuLieu alloc3 = receipt.allocations().get(2);
        assertEquals(102, alloc3.itemId());
        assertEquals(3, alloc3.tonKhoId());
        assertEquals("LOT-03", alloc3.soLo());
        assertEquals(0, new BigDecimal("2").compareTo(alloc3.quantityDispensed()));

        // Stock deductions verified
        assertEquals(0, BigDecimal.ZERO.compareTo(batch1.getSoLuongTon()));
        assertEquals(0, new BigDecimal("8").compareTo(batch2.getSoLuongTon()));
        assertEquals(0, new BigDecimal("3").compareTo(batch3.getSoLuongTon()));
        assertEquals(3, fixture.savedTonKhos.size());

        // ChiTietDonThuoc.soLuongDaXuat updated
        assertEquals(0, item1.getSoLuongKe().compareTo(item1.getSoLuongDaXuat()));
        assertEquals(0, item2.getSoLuongKe().compareTo(item2.getSoLuongDaXuat()));

        // PhieuXuatKho created and saved
        assertNotNull(fixture.savedPhieuXuatKho);
        assertEquals(WAREHOUSE_ID, fixture.savedPhieuXuatKho.getMaKhoXuat());
        assertEquals(LoaiXuatKho.BAN_THUOC, fixture.savedPhieuXuatKho.getLoaiXuat());
        assertEquals(Integer.valueOf(PRESCRIPTION_ID), fixture.savedPhieuXuatKho.getMaDonThuoc());
        assertEquals(3, fixture.savedPhieuXuatKho.getChiTiets().size());

        // Prescription completed and updated
        assertEquals(TrangThaiDonThuoc.DA_XUAT_HET, prescription.getTrangThai());
        assertEquals(Integer.valueOf(PHARMACIST_ID), prescription.getMaNguoiCapThuoc());
        assertNotNull(prescription.getNgayCapThuoc());
        assertEquals(prescription, fixture.updatedPrescription);

        // Audit entry recorded
        assertEquals(1, fixture.audit.independentEntries.size());
        HeThongKiemToanNhapLieu auditEntry = fixture.audit.independentEntries.get(0);
        assertEquals(KiemToanHanhDongMa.RX_DISPENSED, auditEntry.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.PRESCRIPTION, auditEntry.resourceType());
        assertEquals(PRESCRIPTION_ID, auditEntry.resourceId());
        assertEquals(KiemToanKetQua.SUCCESS, auditEntry.outcome());
        assertNull(auditEntry.reasonCode());
        assertEquals(CORRELATION_ID, auditEntry.correlationId());
        assertEquals(1, fixture.transactions.calls);
    }

    @Test
    void insufficientStockFailsWithInsufficientStock() {
        Fixture fixture = new Fixture();
        DonThuoc prescription = createApprovedPrescription(PRESCRIPTION_ID);
        ChiTietDonThuoc item = createPrescriptionItem(101, new BigDecimal("10"), BigDecimal.ZERO, new BigDecimal("10000"));
        prescription.getChiTietDonThuocs().add(item);
        fixture.prescription = prescription;

        TonKho batch = createTonKho(1, 101, "LOT-01", LocalDate.now().plusDays(10), new BigDecimal("4"), new BigDecimal("10000"));
        fixture.batchesByItem.put(101, List.of(batch));

        DispenseDonThuocCommand command = new DispenseDonThuocCommand(
                PRESCRIPTION_ID, PHARMACIST_ID, WAREHOUSE_ID, context("PHARMACIST"), actor("PHARMACIST"));

        KetQua<DispenseDonThuocBienNhan> result = fixture.handler().handle(command);

        assertFalse(result.isSuccess());
        assertEquals("INSUFFICIENT_STOCK", result.getError().orElseThrow().code());
        assertEquals("Không đủ tồn kho.", result.getError().orElseThrow().message());

        // Invariant: prescription not updated to DA_XUAT_HET
        assertEquals(TrangThaiDonThuoc.DA_DUYET, prescription.getTrangThai());
        assertNull(fixture.updatedPrescription);
        assertNull(fixture.savedPhieuXuatKho);
        assertTrue(fixture.audit.independentEntries.isEmpty());
    }

    @Test
    void prescriptionNotFoundReturnsNotFound() {
        Fixture fixture = new Fixture();
        fixture.prescription = null;

        DispenseDonThuocCommand command = new DispenseDonThuocCommand(
                PRESCRIPTION_ID, PHARMACIST_ID, WAREHOUSE_ID, context("PHARMACIST"), actor("PHARMACIST"));

        KetQua<DispenseDonThuocBienNhan> result = fixture.handler().handle(command);

        assertFalse(result.isSuccess());
        assertEquals("NOT_FOUND", result.getError().orElseThrow().code());
        assertEquals("Không tìm thấy đơn thuốc.", result.getError().orElseThrow().message());
        assertEquals(0, fixture.transactions.calls);
        assertTrue(fixture.audit.independentEntries.isEmpty());
    }

    @Test
    void prescriptionNotInDaDuyetStateReturnsInvalidState() {
        TrangThaiDonThuoc[] nonApprovedStates = {
                TrangThaiDonThuoc.TAM,
                TrangThaiDonThuoc.HUY,
                TrangThaiDonThuoc.DA_XUAT_HET
        };

        for (TrangThaiDonThuoc state : nonApprovedStates) {
            Fixture fixture = new Fixture();
            DonThuoc prescription = new DonThuoc();
            prescription.setId(PRESCRIPTION_ID);
            prescription.setTrangThai(state);
            fixture.prescription = prescription;

            DispenseDonThuocCommand command = new DispenseDonThuocCommand(
                    PRESCRIPTION_ID, PHARMACIST_ID, WAREHOUSE_ID, context("PHARMACIST"), actor("PHARMACIST"));

            KetQua<DispenseDonThuocBienNhan> result = fixture.handler().handle(command);

            assertFalse(result.isSuccess(), "State " + state + " must fail");
            assertEquals("INVALID_STATE", result.getError().orElseThrow().code());
            assertEquals("Chỉ có thể cấp phát đơn thuốc đã được duyệt.", result.getError().orElseThrow().message());
            assertEquals(0, fixture.transactions.calls);
            assertTrue(fixture.audit.independentEntries.isEmpty());
        }
    }

    @Test
    void warehouseNotFoundReturnsNotFound() {
        Fixture fixture = new Fixture();
        fixture.warehouse = null;
        fixture.prescription = createApprovedPrescription(PRESCRIPTION_ID);

        DispenseDonThuocCommand command = new DispenseDonThuocCommand(
                PRESCRIPTION_ID, PHARMACIST_ID, WAREHOUSE_ID, context("PHARMACIST"), actor("PHARMACIST"));

        KetQua<DispenseDonThuocBienNhan> result = fixture.handler().handle(command);

        assertFalse(result.isSuccess());
        assertEquals("NOT_FOUND", result.getError().orElseThrow().code());
        assertEquals("Không tìm thấy kho dược.", result.getError().orElseThrow().message());
        assertEquals(0, fixture.transactions.calls);
        assertTrue(fixture.audit.independentEntries.isEmpty());
    }

    @Test
    void rejectsInvalidCommandInputsWithValidationError() {
        Fixture fixture = new Fixture();

        DispenseDonThuocCommand[] invalidCommands = {
                null,
                new DispenseDonThuocCommand(0, PHARMACIST_ID, WAREHOUSE_ID, context("PHARMACIST"), actor("PHARMACIST")),
                new DispenseDonThuocCommand(-1, PHARMACIST_ID, WAREHOUSE_ID, context("PHARMACIST"), actor("PHARMACIST")),
                new DispenseDonThuocCommand(PRESCRIPTION_ID, 0, WAREHOUSE_ID, context("PHARMACIST"), actor("PHARMACIST")),
                new DispenseDonThuocCommand(PRESCRIPTION_ID, -1, WAREHOUSE_ID, context("PHARMACIST"), actor("PHARMACIST")),
                new DispenseDonThuocCommand(PRESCRIPTION_ID, PHARMACIST_ID, 0, context("PHARMACIST"), actor("PHARMACIST")),
                new DispenseDonThuocCommand(PRESCRIPTION_ID, PHARMACIST_ID, -1, context("PHARMACIST"), actor("PHARMACIST"))
        };

        for (DispenseDonThuocCommand cmd : invalidCommands) {
            KetQua<DispenseDonThuocBienNhan> result = fixture.handler().handle(cmd);
            assertFalse(result.isSuccess());
            assertEquals("VALIDATION_ERROR", result.getError().orElseThrow().code());
            assertEquals("Thông tin cấp phát thuốc không hợp lệ.", result.getError().orElseThrow().message());
        }
        assertEquals(0, fixture.transactions.calls);
        assertTrue(fixture.audit.independentEntries.isEmpty());
    }

    @Test
    void unauthorizedDenialForDoctorAndReceptionistLogsAudit() {
        String[] unauthorizedRoles = {"DOCTOR", "RECEPTIONIST"};

        for (String role : unauthorizedRoles) {
            Fixture fixture = new Fixture();
            fixture.prescription = createApprovedPrescription(PRESCRIPTION_ID);

            DispenseDonThuocCommand command = new DispenseDonThuocCommand(
                    PRESCRIPTION_ID, PHARMACIST_ID, WAREHOUSE_ID, context(role), actor(role));

            KetQua<DispenseDonThuocBienNhan> result = fixture.handler().handle(command);

            assertFalse(result.isSuccess());
            assertEquals("AUTH_ACCESS_DENIED", result.getError().orElseThrow().code());
            assertEquals("Không có quyền cấp phát đơn thuốc trong phiên hiện tại.", result.getError().orElseThrow().message());
            assertEquals(0, fixture.transactions.calls);

            assertEquals(1, fixture.audit.independentEntries.size());
            HeThongKiemToanNhapLieu auditEntry = fixture.audit.independentEntries.get(0);
            assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, auditEntry.actionCode());
            assertNull(auditEntry.resourceType());
            assertNull(auditEntry.resourceId());
            assertEquals(KiemToanKetQua.DENIED, auditEntry.outcome());
            assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, auditEntry.reasonCode());
        }
    }

    @Test
    void unauthenticatedOrInvalidContextDeniesAccess() {
        Fixture fixture = new Fixture();
        fixture.prescription = createApprovedPrescription(PRESCRIPTION_ID);

        DispenseDonThuocCommand command = new DispenseDonThuocCommand(
                PRESCRIPTION_ID, PHARMACIST_ID, WAREHOUSE_ID, null, null);

        KetQua<DispenseDonThuocBienNhan> result = fixture.handler().handle(command);

        assertFalse(result.isSuccess());
        assertEquals("AUTH_ACCESS_DENIED", result.getError().orElseThrow().code());
        assertEquals(0, fixture.transactions.calls);
        assertEquals(1, fixture.audit.independentEntries.size());
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, fixture.audit.independentEntries.get(0).actionCode());
        assertEquals(KiemToanKetQua.DENIED, fixture.audit.independentEntries.get(0).outcome());
    }

    @Test
    void skipsItemsWithZeroOrNegativeNeededQuantity() {
        Fixture fixture = new Fixture();
        DonThuoc prescription = createApprovedPrescription(PRESCRIPTION_ID);

        // Item with neededQty = 0 (already fully dispensed)
        ChiTietDonThuoc fullyDispensed = createPrescriptionItem(101, new BigDecimal("5"), new BigDecimal("5"), new BigDecimal("10000"));
        // Item with neededQty = 2
        ChiTietDonThuoc activeItem = createPrescriptionItem(102, new BigDecimal("2"), BigDecimal.ZERO, new BigDecimal("20000"));
        prescription.getChiTietDonThuocs().add(fullyDispensed);
        prescription.getChiTietDonThuocs().add(activeItem);
        fixture.prescription = prescription;

        TonKho batch = createTonKho(1, 102, "LOT-02", LocalDate.now().plusDays(10), new BigDecimal("10"), new BigDecimal("20000"));
        fixture.batchesByItem.put(102, List.of(batch));

        DispenseDonThuocCommand command = new DispenseDonThuocCommand(
                PRESCRIPTION_ID, PHARMACIST_ID, WAREHOUSE_ID, context("PHARMACIST"), actor("PHARMACIST"));

        KetQua<DispenseDonThuocBienNhan> result = fixture.handler().handle(command);

        assertTrue(result.isSuccess());
        DispenseDonThuocBienNhan receipt = result.valueOrThrow();
        assertEquals(1, receipt.allocations().size());
        assertEquals(102, receipt.allocations().get(0).itemId());
        assertEquals(0, new BigDecimal("40000").compareTo(receipt.tongTien()));
    }

    @Test
    void convenienceConstructorCreatesAuditActorContext() {
        PhanQuyenNguCanh ctx = context("PHARMACIST");
        DispenseDonThuocCommand command = new DispenseDonThuocCommand(
                PRESCRIPTION_ID, PHARMACIST_ID, WAREHOUSE_ID, ctx);

        assertNotNull(command.auditActorContext());
        assertEquals(EMPLOYEE_ID, command.auditActorContext().employeeId());
        assertEquals(ROLE_ID, command.auditActorContext().roleId());
        assertEquals("PHARMACIST", command.auditActorContext().roleCode());
        assertEquals(FACILITY_ID, command.auditActorContext().facilityId());
        assertEquals(DEPARTMENT_ID, command.auditActorContext().departmentId());
        assertNotNull(command.auditActorContext().correlationId());
    }

    private static PhanQuyenNguCanh context(String roleCode) {
        return new PhanQuyenNguCanh(EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    }

    private static KiemToanTacNhanNguCanh actor(String roleCode) {
        return new KiemToanTacNhanNguCanh(EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID, "station-dispense", CORRELATION_ID);
    }

    private static DonThuoc createApprovedPrescription(int id) {
        DonThuoc prescription = new DonThuoc();
        prescription.setId(id);
        prescription.setTrangThai(TrangThaiDonThuoc.DA_DUYET);
        return prescription;
    }

    private static ChiTietDonThuoc createPrescriptionItem(int itemId, BigDecimal soLuongKe, BigDecimal soLuongDaXuat, BigDecimal donGia) {
        ChiTietDonThuoc item = new ChiTietDonThuoc();
        item.setMaItem(itemId);
        item.setSoLuongKe(soLuongKe);
        item.setSoLuongDaXuat(soLuongDaXuat);
        item.setDonGiaKe(donGia);
        return item;
    }

    private static TonKho createTonKho(int id, int itemId, String soLo, LocalDate hanSuDung, BigDecimal soLuongTon, BigDecimal giaBan) {
        TonKho tk = new TonKho();
        tk.setId(id);
        tk.setMaKho(WAREHOUSE_ID);
        tk.setMaItem(itemId);
        tk.setSoLo(soLo);
        tk.setHanSuDung(hanSuDung);
        tk.setSoLuongTon(soLuongTon);
        tk.setGiaBan(giaBan);
        return tk;
    }

    private static final class Fixture {
        final TransactionFixture transactions = new TransactionFixture();
        final AuditFixture audit = new AuditFixture();
        final FEFOPhanBoDichVu fefoService = new FEFOPhanBoDichVu();

        DonThuoc prescription;
        DonThuoc updatedPrescription;
        Kho warehouse = new Kho();
        final Map<Integer, List<TonKho>> batchesByItem = new HashMap<>();
        final List<TonKho> savedTonKhos = new ArrayList<>();
        PhieuXuatKho savedPhieuXuatKho;

        final IDonThuocRepository prescriptionRepo = (IDonThuocRepository) Proxy.newProxyInstance(
                IDonThuocRepository.class.getClassLoader(),
                new Class<?>[]{IDonThuocRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getById" -> Optional.ofNullable(prescription);
                    case "update" -> {
                        updatedPrescription = (DonThuoc) args[0];
                        yield null;
                    }
                    default -> null;
                });

        final IKhoRepository inventoryRepo = (IKhoRepository) Proxy.newProxyInstance(
                IKhoRepository.class.getClassLoader(),
                new Class<?>[]{IKhoRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getKhoById" -> Optional.ofNullable(warehouse);
                    case "getAvailableBatches" -> batchesByItem.getOrDefault((Integer) args[1], List.of());
                    case "saveTonKho" -> {
                        TonKho tk = (TonKho) args[0];
                        savedTonKhos.add(tk);
                        yield tk;
                    }
                    case "savePhieuXuatKho" -> {
                        PhieuXuatKho pxk = (PhieuXuatKho) args[0];
                        if (pxk.getId() == null) {
                            pxk.setId(501);
                        }
                        savedPhieuXuatKho = pxk;
                        yield pxk;
                    }
                    default -> null;
                });

        DispenseDonThuocHandler handler() {
            return new DispenseDonThuocHandler(prescriptionRepo, inventoryRepo, fefoService, transactions, audit);
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
