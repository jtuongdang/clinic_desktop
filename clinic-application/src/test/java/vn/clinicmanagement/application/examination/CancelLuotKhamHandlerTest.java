package vn.clinicmanagement.application.examination;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class CancelLuotKhamHandlerTest {
    private static final int VISIT_ID = 73;
    private static final int EMPLOYEE_ID = 17;
    private static final int ROLE_ID = 4;
    private static final int FACILITY_ID = 3;
    private static final int DEPARTMENT_ID = 8;
    private static final UUID SESSION_ID = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");
    private static final UUID CORRELATION_ID = UUID.fromString("0b8a0d60-0cc7-4fc8-9534-754f5f0e0017");

    @Test
    void cancelsWaitingVisitAndAuditsWithinTransaction() {
        Fixture fixture = new Fixture(TrangThaiLuotKham.CHO_KHAM);

        KetQua<CancelLuotKhamBienNhan> result = fixture.handler().handle(fixture.command("Patient request"));

        assertTrue(result.isSuccess());
        assertEquals(VISIT_ID, result.valueOrThrow().visitId());
        assertEquals(TrangThaiLuotKham.DA_HUY, fixture.visit.getTrangThai());
        assertSame(fixture.visit, fixture.updated);
        assertEquals(1, fixture.transactions.calls);
        assertEquals(1, fixture.repositoryReads);
        assertTrue(fixture.audit.inTransaction);
        assertEquals(KiemToanHanhDongMa.VISIT_TRANSFERRED, fixture.audit.entry.actionCode());
        assertEquals(KiemToanTaiNguyenLoai.VISIT, fixture.audit.entry.resourceType());
        assertEquals(VISIT_ID, fixture.audit.entry.resourceId());
        assertEquals(KiemToanKetQua.SUCCESS, fixture.audit.entry.outcome());
        assertEquals(CORRELATION_ID, fixture.audit.entry.correlationId());
    }

    @Test
    void deniesDoctorAndOtherRolesWithIndependentForbiddenAudit() {
        for (String roleCode : new String[]{"DOCTOR", "ADMIN"}) {
            Fixture fixture = new Fixture(TrangThaiLuotKham.CHO_KHAM);

            KetQua<CancelLuotKhamBienNhan> result = fixture.handler().handle(fixture.command("Synthetic reason", roleCode));

            assertTrue(result.isFailure());
            assertEquals("AUTHORIZATION_DENIED", result.getError().orElseThrow().code());
            assertEquals(0, fixture.repositoryReads);
            assertNull(fixture.updated);
            assertEquals(1, fixture.audit.independentWrites);
            assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, fixture.audit.entry.actionCode());
            assertEquals(KiemToanKetQua.DENIED, fixture.audit.entry.outcome());
            assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, fixture.audit.entry.reasonCode());
        }
    }

    @Test
    void rejectsVisitThatDoesNotExist() {
        Fixture fixture = new Fixture(null);

        KetQua<CancelLuotKhamBienNhan> result = fixture.handler().handle(fixture.command("Synthetic reason"));

        assertFailure(result, "NOT_FOUND", "Không tìm thấy lượt khám.");
        assertNull(fixture.updated);
        assertNull(fixture.audit.entry);
    }

    @Test
    void rejectsVisitsNotWaitingForExamination() {
        for (TrangThaiLuotKham state : new TrangThaiLuotKham[]{
                TrangThaiLuotKham.DANG_KHAM, TrangThaiLuotKham.HOAN_THANH, TrangThaiLuotKham.DA_HUY}) {
            Fixture fixture = new Fixture(state);

            KetQua<CancelLuotKhamBienNhan> result = fixture.handler().handle(fixture.command("Synthetic reason"));

            assertFailure(result, "INVALID_STATE", "Chỉ có thể hủy lượt khám ở trạng thái chờ khám.");
            assertEquals(state, fixture.visit.getTrangThai());
            assertNull(fixture.updated);
            assertNull(fixture.audit.entry);
        }
    }

    @Test
    void rejectsNullEmptyAndBlankCancellationReasons() {
        for (String reason : new String[]{null, "", "  \t"}) {
            Fixture fixture = new Fixture(TrangThaiLuotKham.CHO_KHAM);

            KetQua<CancelLuotKhamBienNhan> result = fixture.handler().handle(fixture.command(reason));

            assertFailure(result, "VALIDATION_ERROR", "Lý do hủy không được để trống.");
            assertEquals(0, fixture.repositoryReads);
            assertNull(fixture.updated);
        }
    }

    private static void assertFailure(KetQua<CancelLuotKhamBienNhan> result, String code, String message) {
        assertTrue(result.isFailure());
        assertEquals(code, result.getError().orElseThrow().code());
        assertEquals(message, result.getError().orElseThrow().message());
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
                            transactions.mutationWasStaged = true;
                            yield null;
                        }
                        default -> null;
                    });
        }

        CancelLuotKhamHandler handler() {
            return new CancelLuotKhamHandler(visits, transactions, audit);
        }

        CancelLuotKhamCommand command(String reason) {
            return command(reason, "RECEPTIONIST");
        }

        CancelLuotKhamCommand command(String reason, String roleCode) {
            return new CancelLuotKhamCommand(VISIT_ID, reason, context(roleCode), actor(roleCode));
        }
    }

    private static final class TransactionFixture implements GiaoDichRanhGioi {
        boolean current;
        boolean mutationWasStaged;
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
