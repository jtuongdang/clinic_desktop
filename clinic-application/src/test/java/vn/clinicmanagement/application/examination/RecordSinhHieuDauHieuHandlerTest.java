package vn.clinicmanagement.application.examination;

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
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.examination.SinhHieu;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class RecordSinhHieuDauHieuHandlerTest {
    private static final int VISIT_ID = 73;
    private static final int EMPLOYEE_ID = 17;
    private static final int ROLE_ID = 4;
    private static final int FACILITY_ID = 3;
    private static final int DEPARTMENT_ID = 8;
    private static final UUID SESSION_ID = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");
    private static final UUID CORRELATION_ID = UUID.fromString("0b8a0d60-0cc7-4fc8-9534-754f5f0e0017");

    @Test
    void recordsVitalSignsCalculatesBmiAndAuditsWithinTransaction() {
        Fixture fixture = new Fixture(TrangThaiLuotKham.DANG_KHAM);
        fixture.visit.setSinhHieus(null);

        KetQua<RecordSinhHieuDauHieuBienNhan> result = fixture.handler().handle(fixture.command("DOCTOR"));

        assertTrue(result.isSuccess());
        RecordSinhHieuDauHieuBienNhan receipt = result.valueOrThrow();
        assertEquals(VISIT_ID, receipt.visitId());
        assertEquals(new BigDecimal("22.86"), receipt.bmi());
        assertNotNull(receipt.recordedAt());
        assertSame(fixture.visit, fixture.updated);
        assertEquals(1, fixture.visit.getSinhHieus().size());
        SinhHieu recorded = fixture.visit.getSinhHieus().get(0);
        assertEquals(VISIT_ID, recorded.getMaLuotKham());
        assertEquals(72, recorded.getMach());
        assertEquals(new BigDecimal("37.0"), recorded.getNhietDo());
        assertEquals(120, recorded.getHuyetApTamThu());
        assertEquals(80, recorded.getHuyetApTamTruong());
        assertEquals(16, recorded.getNhipTho());
        assertEquals(new BigDecimal("70.00"), recorded.getCanNang());
        assertEquals(new BigDecimal("175.0"), recorded.getChieuCao());
        assertEquals(new BigDecimal("22.86"), recorded.getBmi());
        assertEquals(new BigDecimal("98.0"), recorded.getSpo2());
        assertSame(fixture.visit, recorded.getLuotKham());
        assertEquals(1, fixture.transactions.calls);
        assertEquals(1, fixture.repositoryReads);
        assertTrue(fixture.audit.inTransaction);
        assertSuccessAudit(fixture.audit.entry);
    }

    @Test
    void recordsWithoutWeightOrHeightWithNullBmi() {
        Fixture fixture = new Fixture(TrangThaiLuotKham.CHO_KHAM);
        RecordSinhHieuDauHieuCommand command = new RecordSinhHieuDauHieuCommand(
                VISIT_ID, 72, null, null, null, null, null, null, null, context("RECEPTIONIST"));

        KetQua<RecordSinhHieuDauHieuBienNhan> result = fixture.handler().handle(command);

        assertTrue(result.isSuccess());
        assertNull(result.valueOrThrow().bmi());
        assertNull(fixture.visit.getSinhHieus().get(0).getBmi());
        assertEquals(KiemToanHanhDongMa.EMR_UPDATED, fixture.audit.entry.actionCode());
    }

    @Test
    void rejectsVisitThatDoesNotExist() {
        Fixture fixture = new Fixture(null);

        KetQua<RecordSinhHieuDauHieuBienNhan> result = fixture.handler().handle(fixture.command("DOCTOR"));

        assertFailure(result, "NOT_FOUND", "Không tìm thấy lượt khám.");
        assertNull(fixture.updated);
        assertNull(fixture.audit.entry);
    }

    @Test
    void rejectsEachPhysiologicalValueOutsideItsInclusiveRange() {
        Fixture fixture = new Fixture(TrangThaiLuotKham.DANG_KHAM);
        RecordSinhHieuDauHieuCommand[] invalidCommands = {
                command(19, decimal("37.0"), 120, 80, 16, decimal("70"), decimal("175"), decimal("98")),
                command(72, decimal("29.9"), 120, 80, 16, decimal("70"), decimal("175"), decimal("98")),
                command(72, decimal("37.0"), 39, 80, 16, decimal("70"), decimal("175"), decimal("98")),
                command(72, decimal("37.0"), 120, 19, 16, decimal("70"), decimal("175"), decimal("98")),
                command(72, decimal("37.0"), 120, 80, 4, decimal("70"), decimal("175"), decimal("98")),
                command(72, decimal("37.0"), 120, 80, 16, decimal("0.4"), decimal("175"), decimal("98")),
                command(72, decimal("37.0"), 120, 80, 16, decimal("70"), decimal("19.9"), decimal("98")),
                command(72, decimal("37.0"), 120, 80, 16, decimal("70"), decimal("175"), decimal("49.9")),
                command(301, decimal("37.0"), 120, 80, 16, decimal("70"), decimal("175"), decimal("98")),
                command(72, decimal("45.1"), 120, 80, 16, decimal("70"), decimal("175"), decimal("98")),
                command(72, decimal("37.0"), 301, 80, 16, decimal("70"), decimal("175"), decimal("98")),
                command(72, decimal("37.0"), 120, 201, 16, decimal("70"), decimal("175"), decimal("98")),
                command(72, decimal("37.0"), 120, 80, 101, decimal("70"), decimal("175"), decimal("98")),
                command(72, decimal("37.0"), 120, 80, 16, decimal("500.1"), decimal("175"), decimal("98")),
                command(72, decimal("37.0"), 120, 80, 16, decimal("70"), decimal("300.1"), decimal("98")),
                command(72, decimal("37.0"), 120, 80, 16, decimal("70"), decimal("175"), decimal("100.1"))
        };

        for (RecordSinhHieuDauHieuCommand command : invalidCommands) {
            KetQua<RecordSinhHieuDauHieuBienNhan> result = fixture.handler().handle(command);

            assertFailure(result, "VALIDATION_ERROR",
                    "Chỉ số sinh hiệu không nằm trong giới hạn sinh lý hợp lệ.");
        }

        assertEquals(0, fixture.repositoryReads);
        assertEquals(0, fixture.transactions.calls);
    }

    @Test
    void rejectsInvalidVisitId() {
        Fixture fixture = new Fixture(TrangThaiLuotKham.DANG_KHAM);
        RecordSinhHieuDauHieuCommand command = new RecordSinhHieuDauHieuCommand(0, context("DOCTOR"));

        KetQua<RecordSinhHieuDauHieuBienNhan> result = fixture.handler().handle(command);

        assertFailure(result, "VALIDATION_ERROR", "Mã lượt khám không hợp lệ.");
        assertEquals(0, fixture.repositoryReads);
        assertEquals(0, fixture.transactions.calls);
    }

    @Test
    void rejectsWhenEmrIsLocked() {
        Fixture fixture = new Fixture(TrangThaiLuotKham.DANG_KHAM);
        fixture.visit.lockEmr("synthetic-signature", EMPLOYEE_ID);

        KetQua<RecordSinhHieuDauHieuBienNhan> result = fixture.handler().handle(fixture.command("DOCTOR"));

        assertFailure(result, "EMR_LOCKED", "Hồ sơ bệnh án đã bị khóa không thể ghi thêm sinh hiệu.");
        assertNull(fixture.updated);
        assertNull(fixture.audit.entry);
    }

    @Test
    void rejectsWhenVisitIsCancelled() {
        Fixture fixture = new Fixture(TrangThaiLuotKham.DA_HUY);

        KetQua<RecordSinhHieuDauHieuBienNhan> result = fixture.handler().handle(fixture.command("DOCTOR"));

        assertFailure(result, "INVALID_STATE", "Lượt khám đã bị hủy.");
        assertNull(fixture.updated);
        assertNull(fixture.audit.entry);
    }

    @Test
    void deniesUnauthorizedRoleAndInvalidSessionWithForbiddenAudit() {
        for (PhanQuyenNguCanh context : new PhanQuyenNguCanh[]{
                context("NURSE"),
                new PhanQuyenNguCanh(EMPLOYEE_ID, ROLE_ID, "DOCTOR", FACILITY_ID, DEPARTMENT_ID, null)
        }) {
            Fixture fixture = new Fixture(TrangThaiLuotKham.DANG_KHAM);
            RecordSinhHieuDauHieuCommand command = new RecordSinhHieuDauHieuCommand(
                    VISIT_ID, 72, decimal("37.0"), 120, 80, 16, decimal("70"), decimal("175"),
                    decimal("98"), context, actor("DOCTOR"));

            KetQua<RecordSinhHieuDauHieuBienNhan> result = fixture.handler().handle(command);

            assertTrue(result.isFailure());
            assertEquals("AUTHORIZATION_DENIED", result.getError().orElseThrow().code());
            assertEquals(0, fixture.repositoryReads);
            assertEquals(0, fixture.transactions.calls);
            assertNull(fixture.updated);
            assertEquals(1, fixture.audit.independentWrites);
            assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, fixture.audit.entry.actionCode());
            assertEquals(KiemToanKetQua.DENIED, fixture.audit.entry.outcome());
            assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, fixture.audit.entry.reasonCode());
        }
    }

    private static RecordSinhHieuDauHieuCommand command(
            Integer pulse, BigDecimal temperature, Integer systolic, Integer diastolic, Integer respiration,
            BigDecimal weight, BigDecimal height, BigDecimal spo2) {
        return new RecordSinhHieuDauHieuCommand(VISIT_ID, pulse, temperature, systolic, diastolic, respiration,
                weight, height, spo2, context("DOCTOR"));
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private static void assertFailure(KetQua<RecordSinhHieuDauHieuBienNhan> result, String code, String message) {
        assertTrue(result.isFailure());
        assertEquals(code, result.getError().orElseThrow().code());
        assertEquals(message, result.getError().orElseThrow().message());
    }

    private static void assertSuccessAudit(HeThongKiemToanNhapLieu entry) {
        assertEquals(KiemToanHanhDongMa.EMR_UPDATED, entry.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.VISIT, entry.resourceType());
        assertEquals(VISIT_ID, entry.resourceId());
        assertEquals(KiemToanKetQua.SUCCESS, entry.outcome());
        assertEquals(CORRELATION_ID, entry.correlationId());
    }

    private static PhanQuyenNguCanh context(String roleCode) {
        return new PhanQuyenNguCanh(EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    }

    private static KiemToanTacNhanNguCanh actor(String roleCode) {
        return new KiemToanTacNhanNguCanh(EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID, "station-1",
                CORRELATION_ID);
    }

    private static final class Fixture {
        final LuotKham visit;
        int repositoryReads;
        LuotKham updated;
        final TransactionFixture transactions = new TransactionFixture();
        final AuditFixture audit = new AuditFixture(transactions);
        final ILuotKhamRepository visits;

        Fixture(TrangThaiLuotKham state) {
            if (state == null) {
                visit = null;
            } else {
                visit = new LuotKham();
                visit.setId(VISIT_ID);
                visit.setTrangThai(state);
            }
            visits = (ILuotKhamRepository) Proxy.newProxyInstance(
                    ILuotKhamRepository.class.getClassLoader(), new Class<?>[]{ILuotKhamRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getById" -> {
                            repositoryReads++;
                            yield Optional.ofNullable(visit);
                        }
                        case "update" -> {
                            updated = (LuotKham) args[0];
                            yield null;
                        }
                        default -> null;
                    });
        }

        RecordSinhHieuDauHieuHandler handler() {
            return new RecordSinhHieuDauHieuHandler(visits, transactions, audit);
        }

        RecordSinhHieuDauHieuCommand command(String roleCode) {
            return new RecordSinhHieuDauHieuCommand(
                    VISIT_ID, 72, decimal("37.0"), 120, 80, 16, decimal("70.00"),
                    decimal("175.0"), decimal("98.0"), context(roleCode), actor(roleCode));
        }
    }

    private static final class TransactionFixture implements GiaoDichRanhGioi {
        boolean current;
        int calls;

        @Override
        public <T> KetQua<T> inTransaction(Supplier<KetQua<T>> operation) {
            calls++;
            current = true;
            try {
                return operation.get();
            } finally {
                current = false;
            }
        }
    }

    private static final class AuditFixture implements KiemToanWriteCongKetNoi {
        private final TransactionFixture transactions;
        boolean inTransaction;
        int independentWrites;
        HeThongKiemToanNhapLieu entry;

        AuditFixture(TransactionFixture transactions) {
            this.transactions = transactions;
        }

        @Override
        public void appendInCurrentTransaction(HeThongKiemToanNhapLieu auditEntry) {
            inTransaction = transactions.current;
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
