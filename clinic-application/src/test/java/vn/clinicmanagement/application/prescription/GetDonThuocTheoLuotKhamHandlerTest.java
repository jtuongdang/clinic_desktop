package vn.clinicmanagement.application.prescription;

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
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.inventory.MatHang;
import vn.clinicmanagement.domain.prescription.ChiTietDonThuoc;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.repositories.IDonThuocRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetDonThuocTheoLuotKhamHandlerTest {
    private static final int VISIT_ID = 73;
    private static final int EMPLOYEE_ID = 17;
    private static final int ROLE_ID = 4;
    private static final int FACILITY_ID = 3;
    private static final int DEPARTMENT_ID = 8;
    private static final UUID SESSION_ID = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");
    private static final UUID CORRELATION_ID = UUID.fromString("0b8a0d60-0cc7-4fc8-9534-754f5f0e0017");

    @Test
    void retrievesAndMapsPrescriptionDetailsWithCalculatedTotalsAndSuccessAudit() {
        DonThuoc prescription = prescription();
        Fixture fixture = new Fixture(new LuotKham(), List.of(prescription));

        KetQua<List<DonThuocDuLieu>> result = fixture.handler.handle(query("DOCTOR"));

        assertTrue(result.isSuccess());
        assertEquals(1, result.valueOrThrow().size());
        DonThuocDuLieu dto = result.valueOrThrow().get(0);
        assertEquals(901, dto.id());
        assertEquals(VISIT_ID, dto.visitId());
        assertEquals(21, dto.doctorId());
        assertEquals("Synthetic Doctor", dto.doctorName());
        assertEquals(FACILITY_ID, dto.facilityId());
        assertEquals(44, dto.warehouseId());
        assertEquals(TrangThaiDonThuoc.DA_DUYET, dto.trangThai());
        assertEquals(LocalDateTime.of(2026, 11, 7, 9, 15), dto.ngayKe());
        assertEquals("Synthetic note", dto.ghiChu());
        assertEquals("Synthetic instructions", dto.loiDanDonThuoc());
        assertEquals(new BigDecimal("31.00"), dto.tongTien());
        assertEquals(2, dto.items().size());
        DonThuocMatHangDuLieu first = dto.items().get(0);
        assertEquals(1, first.id());
        assertEquals(101, first.itemId());
        assertEquals("MED-101", first.itemCode());
        assertEquals("Synthetic medicine A", first.itemName());
        assertEquals("viên", first.donViTinh());
        assertEquals(new BigDecimal("2"), first.soLuongKe());
        assertEquals(new BigDecimal("1"), first.soLuongDaXuat());
        assertEquals(new BigDecimal("10.50"), first.donGiaKe());
        assertEquals(new BigDecimal("21.00"), first.thanhTien());
        assertEquals("Uống", first.duongDung());
        assertEquals("1 viên", first.lieuDung());
        assertEquals("2 lần/ngày", first.tanSuat());
        assertEquals(3, first.soNgay());
        assertEquals(new BigDecimal("1"), first.sang());
        assertEquals(new BigDecimal("0"), first.trua());
        assertEquals(new BigDecimal("1"), first.chieu());
        assertEquals(new BigDecimal("0"), first.toi());
        assertEquals("Sau ăn", first.huongDan());
        assertEquals(1, fixture.visitReads);
        assertEquals(1, fixture.prescriptionReads);
        assertEquals(1, fixture.audit.independentWrites);
        assertSuccessAudit(fixture.audit.entry);
    }

    @Test
    void allowsDoctorReceptionistAndAdminRoles() {
        for (String roleCode : new String[]{"DOCTOR", "RECEPTIONIST", "ADMIN"}) {
            Fixture fixture = new Fixture(new LuotKham(), List.of());

            KetQua<List<DonThuocDuLieu>> result = fixture.handler.handle(query(roleCode));

            assertTrue(result.isSuccess(), roleCode);
            assertEquals(KiemToanHanhDongMa.EMR_VIEW, fixture.audit.entry.actionCode());
            assertEquals(KiemToanTaiNguyenLoai.PRESCRIPTION, fixture.audit.entry.resourceType());
        }
    }

    @Test
    void returnsEmptyListForVisitWithoutPrescriptions() {
        Fixture fixture = new Fixture(new LuotKham(), List.of());

        KetQua<List<DonThuocDuLieu>> result = fixture.handler.handle(query("RECEPTIONIST"));

        assertTrue(result.isSuccess());
        assertTrue(result.valueOrThrow().isEmpty());
        assertSuccessAudit(fixture.audit.entry);
    }

    @Test
    void rejectsNonPositiveVisitIdsBeforeRepositoryOrAuditAccess() {
        for (int visitId : new int[]{0, -1}) {
            Fixture fixture = new Fixture(new LuotKham(), List.of());

            KetQua<List<DonThuocDuLieu>> result = fixture.handler.handle(
                    new GetDonThuocTheoLuotKhamQuery(visitId, context("DOCTOR"), actor("DOCTOR")));

            assertFailure(result, "VALIDATION_ERROR", "Mã lượt khám không hợp lệ.");
            assertEquals(0, fixture.visitReads);
            assertEquals(0, fixture.audit.independentWrites);
        }
    }

    @Test
    void returnsNotFoundWhenVisitDoesNotExist() {
        Fixture fixture = new Fixture(null, List.of());

        KetQua<List<DonThuocDuLieu>> result = fixture.handler.handle(query("DOCTOR"));

        assertFailure(result, "NOT_FOUND", "Không tìm thấy lượt khám.");
        assertEquals(1, fixture.visitReads);
        assertEquals(0, fixture.prescriptionReads);
        assertEquals(0, fixture.audit.independentWrites);
    }

    @Test
    void deniesUnauthorizedAndInvalidContextsWithForbiddenAudit() {
        Fixture unauthorized = new Fixture(new LuotKham(), List.of());
        KetQua<List<DonThuocDuLieu>> unauthorizedResult = unauthorized.handler.handle(query("PHARMACIST"));

        assertFailure(unauthorizedResult, "AUTH_ACCESS_DENIED",
                "Không có quyền xem đơn thuốc trong phiên hiện tại.");
        assertEquals(0, unauthorized.visitReads);
        assertDeniedAudit(unauthorized.audit.entry);

        Fixture invalidContextFixture = new Fixture(new LuotKham(), List.of());
        PhanQuyenNguCanh invalidContext =
                new PhanQuyenNguCanh(EMPLOYEE_ID, ROLE_ID, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, null);
        KetQua<List<DonThuocDuLieu>> invalidContextResult = invalidContextFixture.handler.handle(
                new GetDonThuocTheoLuotKhamQuery(VISIT_ID, invalidContext, actor("DOCTOR")));

        assertFailure(invalidContextResult, "AUTH_ACCESS_DENIED",
                "Không có quyền xem đơn thuốc trong phiên hiện tại.");
        assertEquals(0, invalidContextFixture.visitReads);
        assertDeniedAudit(invalidContextFixture.audit.entry);
    }

    private static GetDonThuocTheoLuotKhamQuery query(String roleCode) {
        return new GetDonThuocTheoLuotKhamQuery(VISIT_ID, context(roleCode), actor(roleCode));
    }

    private static PhanQuyenNguCanh context(String roleCode) {
        return new PhanQuyenNguCanh(
                EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    }

    private static KiemToanTacNhanNguCanh actor(String roleCode) {
        return new KiemToanTacNhanNguCanh(
                EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID, "station-1", CORRELATION_ID);
    }

    private static DonThuoc prescription() {
        DonThuoc prescription = new DonThuoc();
        prescription.setId(901);
        prescription.setMaLuotKham(VISIT_ID);
        prescription.setMaBacSi(21);
        prescription.setMaCoSo(FACILITY_ID);
        prescription.setMaKhoXuat(44);
        prescription.setTrangThai(TrangThaiDonThuoc.DA_DUYET);
        prescription.setNgayKe(LocalDateTime.of(2026, 11, 7, 9, 15));
        prescription.setGhiChu("Synthetic note");
        prescription.setLoiDanDonThuoc("Synthetic instructions");
        NhanVien doctor = new NhanVien();
        doctor.setHo("Synthetic");
        doctor.setTen("Doctor");
        prescription.setBacSi(doctor);
        prescription.setChiTietDonThuocs(List.of(
                detail(1, 101, "MED-101", "Synthetic medicine A", new BigDecimal("2"),
                        new BigDecimal("1"), new BigDecimal("10.50")),
                detail(2, 102, "MED-102", "Synthetic medicine B", new BigDecimal("2"),
                        BigDecimal.ZERO, new BigDecimal("5.00"))));
        return prescription;
    }

    private static ChiTietDonThuoc detail(
            int detailId,
            int itemId,
            String itemCode,
            String itemName,
            BigDecimal quantity,
            BigDecimal dispensed,
            BigDecimal unitPrice) {
        ChiTietDonThuoc detail = new ChiTietDonThuoc();
        detail.setId(detailId);
        detail.setMaItem(itemId);
        detail.setSoLuongKe(quantity);
        detail.setSoLuongDaXuat(dispensed);
        detail.setDonGiaKe(unitPrice);
        detail.setDuongDung("Uống");
        detail.setLieuDung("1 viên");
        detail.setTanSuat("2 lần/ngày");
        detail.setSoNgay(3);
        detail.setSang(BigDecimal.ONE);
        detail.setTrua(BigDecimal.ZERO);
        detail.setChieu(BigDecimal.ONE);
        detail.setToi(BigDecimal.ZERO);
        detail.setHuongDan("Sau ăn");
        MatHang item = new MatHang();
        item.setId(itemId);
        item.setMaItem(itemCode);
        item.setTenItem(itemName);
        item.setDonViCoBan("viên");
        detail.setItem(item);
        return detail;
    }

    private static void assertSuccessAudit(HeThongKiemToanNhapLieu entry) {
        assertNotNull(entry);
        assertEquals(KiemToanHanhDongMa.EMR_VIEW, entry.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.PRESCRIPTION, entry.resourceType());
        assertEquals(VISIT_ID, entry.resourceId());
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
        assertEquals(CORRELATION_ID, entry.correlationId());
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
        final GetDonThuocTheoLuotKhamHandler handler;
        final AuditFixture audit = new AuditFixture();
        int visitReads;
        int prescriptionReads;

        Fixture(LuotKham visit, List<DonThuoc> prescriptions) {
            IDonThuocRepository prescriptionRepository = proxy(IDonThuocRepository.class, (method, args) -> {
                if ("getByLuotKhamId".equals(method)) {
                    prescriptionReads++;
                    return prescriptions;
                }
                return null;
            });
            ILuotKhamRepository visitRepository = proxy(ILuotKhamRepository.class, (method, args) -> {
                if ("getById".equals(method)) {
                    visitReads++;
                    return Optional.ofNullable(visit);
                }
                return null;
            });
            handler = new GetDonThuocTheoLuotKhamHandler(prescriptionRepository, visitRepository, audit);
        }
    }

    private static final class AuditFixture implements KiemToanWriteCongKetNoi {
        int independentWrites;
        HeThongKiemToanNhapLieu entry;

        @Override
        public void appendInCurrentTransaction(HeThongKiemToanNhapLieu auditEntry) {
            throw new UnsupportedOperationException();
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
