package vn.clinicmanagement.application.appointment;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.application.common.GiaoDichRanhGioi;
import vn.clinicmanagement.domain.appointment.LichHen;
import vn.clinicmanagement.domain.common.enums.TrangThaiLichHen;
import vn.clinicmanagement.domain.repositories.ILichHenRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class CancelLichHenHandlerTest {
    private static final int APPOINTMENT_ID = 91;
    private static final int PATIENT_ID = 24;
    private static final int EMPLOYEE_ID = 17;
    private static final int ROLE_ID = 4;
    private static final int FACILITY_ID = 3;
    private static final int DEPARTMENT_ID = 8;
    private static final UUID SESSION_ID = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");
    private static final UUID CORRELATION_ID = UUID.fromString("0b8a0d60-0cc7-4fc8-9534-754f5f0e0017");

    @Test
    void cancelsPendingConfirmationAppointmentAndAuditsWithinTransaction() {
        Fixture fixture = new Fixture(TrangThaiLichHen.CHO_XAC_NHAN);

        KetQua<CancelLichHenBienNhan> result = fixture.handler().handle(fixture.command("Patient request"));

        assertTrue(result.isSuccess());
        assertEquals(APPOINTMENT_ID, result.valueOrThrow().appointmentId());
        assertEquals(TrangThaiLichHen.DA_HUY, fixture.appointment.getTrangThai());
        assertEquals("Patient request", fixture.appointment.getGhiChuHuy());
        assertSame(fixture.appointment, fixture.updated);
        assertEquals(1, fixture.transactions.calls);
        assertEquals(1, fixture.repositoryReads);
        assertTrue(fixture.audit.inTransaction);
        assertEquals(0, fixture.audit.independentWrites);
        assertEquals(KiemToanTaiNguyenLoai.PATIENT, fixture.audit.entry.resourceType());
        assertEquals(PATIENT_ID, fixture.audit.entry.resourceId());
        assertEquals(KiemToanKetQua.SUCCESS, fixture.audit.entry.outcome());
        assertEquals(CORRELATION_ID, fixture.audit.entry.correlationId());
    }

    @Test
    void rejectsNullEmptyAndBlankCancellationReasons() {
        for (String reason : new String[]{null, "", "  \t"}) {
            Fixture fixture = new Fixture(TrangThaiLichHen.CHO_XAC_NHAN);

            KetQua<CancelLichHenBienNhan> result = fixture.handler().handle(fixture.command(reason));

            assertFailure(result, "VALIDATION_ERROR", "Lý do hủy không được để trống.");
            assertEquals(0, fixture.transactions.calls);
            assertEquals(0, fixture.repositoryReads);
            assertNull(fixture.updated);
        }
    }

    @Test
    void rejectsInvalidAppointmentId() {
        Fixture fixture = new Fixture(TrangThaiLichHen.CHO_XAC_NHAN);

        KetQua<CancelLichHenBienNhan> result = fixture.handler().handle(fixture.command(0, "Synthetic reason"));

        assertFailure(result, "VALIDATION_ERROR", "Thông tin hủy lịch hẹn không hợp lệ.");
        assertEquals(0, fixture.transactions.calls);
        assertEquals(0, fixture.repositoryReads);
    }

    @Test
    void rejectsAppointmentThatDoesNotExist() {
        Fixture fixture = new Fixture(null);

        KetQua<CancelLichHenBienNhan> result = fixture.handler().handle(fixture.command("Synthetic reason"));

        assertFailure(result, "NOT_FOUND", "Không tìm thấy lịch hẹn.");
        assertEquals(1, fixture.repositoryReads);
        assertNull(fixture.updated);
        assertNull(fixture.audit.entry);
    }

    @Test
    void rejectsAppointmentsAlreadyReceivedOrCompleted() {
        for (TrangThaiLichHen state : new TrangThaiLichHen[]{
                TrangThaiLichHen.DA_DEN, TrangThaiLichHen.HOAN_THANH}) {
            Fixture fixture = new Fixture(state);

            KetQua<CancelLichHenBienNhan> result = fixture.handler().handle(fixture.command("Synthetic reason"));

            assertFailure(result, "INVALID_STATE", "Không thể hủy lịch hẹn đã tiếp nhận hoặc hoàn thành.");
            assertEquals(state, fixture.appointment.getTrangThai());
            assertNull(fixture.updated);
            assertNull(fixture.audit.entry);
        }
    }

    @Test
    void deniesUnauthorizedRoleWithIndependentForbiddenAudit() {
        Fixture fixture = new Fixture(TrangThaiLichHen.CHO_XAC_NHAN);

        KetQua<CancelLichHenBienNhan> result = fixture.handler().handle(
                fixture.command("Synthetic reason", "DOCTOR"));

        assertFailure(result, "AUTHORIZATION_DENIED", "Không được phép hủy lịch hẹn trong phiên hiện tại.");
        assertEquals(0, fixture.transactions.calls);
        assertEquals(0, fixture.repositoryReads);
        assertNull(fixture.updated);
        assertEquals(1, fixture.audit.independentWrites);
        assertEquals(KiemToanHanhDongMa.AUTH_ACCESS_DENIED, fixture.audit.entry.actionCode());
        assertEquals(KiemToanKetQua.DENIED, fixture.audit.entry.outcome());
        assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, fixture.audit.entry.reasonCode());
    }

    @Test
    void deniesInvalidSessionContextAndAuditsUsingActorContext() {
        Fixture fixture = new Fixture(TrangThaiLichHen.CHO_XAC_NHAN);
        PhanQuyenNguCanh invalidContext = new PhanQuyenNguCanh(
                EMPLOYEE_ID, ROLE_ID, "RECEPTIONIST", FACILITY_ID, DEPARTMENT_ID, null);
        CancelLichHenCommand command = new CancelLichHenCommand(
                APPOINTMENT_ID, "Synthetic reason", invalidContext, actor("RECEPTIONIST"));

        KetQua<CancelLichHenBienNhan> result = fixture.handler().handle(command);

        assertFailure(result, "AUTHORIZATION_DENIED", "Không được phép hủy lịch hẹn trong phiên hiện tại.");
        assertEquals(0, fixture.repositoryReads);
        assertEquals(1, fixture.audit.independentWrites);
        assertEquals(KiemToanLyDoMa.AUTH_FORBIDDEN, fixture.audit.entry.reasonCode());
    }

    @Test
    void permitsAdminRole() {
        Fixture fixture = new Fixture(TrangThaiLichHen.CHO_XAC_NHAN);

        KetQua<CancelLichHenBienNhan> result = fixture.handler().handle(
                fixture.command("Synthetic reason", "ADMIN"));

        assertTrue(result.isSuccess());
        assertNotNull(fixture.updated);
    }

    private static void assertFailure(KetQua<CancelLichHenBienNhan> result, String code, String message) {
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
        final LichHen appointment;
        int repositoryReads;
        LichHen updated;
        final TransactionFixture transactions = new TransactionFixture();
        final AuditFixture audit = new AuditFixture(transactions);
        final ILichHenRepository appointments;

        Fixture(TrangThaiLichHen state) {
            if (state == null) {
                appointment = null;
            } else {
                appointment = new LichHen();
                appointment.setId(APPOINTMENT_ID);
                appointment.setMaBenhNhan(PATIENT_ID);
                appointment.setTrangThai(state);
            }
            appointments = (ILichHenRepository) Proxy.newProxyInstance(
                    ILichHenRepository.class.getClassLoader(), new Class<?>[]{ILichHenRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getById" -> {
                            repositoryReads++;
                            yield Optional.ofNullable(appointment);
                        }
                        case "update" -> {
                            updated = (LichHen) args[0];
                            yield null;
                        }
                        default -> null;
                    });
        }

        CancelLichHenHandler handler() {
            return new CancelLichHenHandler(appointments, transactions, audit);
        }

        CancelLichHenCommand command(String reason) {
            return command(APPOINTMENT_ID, reason, "RECEPTIONIST");
        }

        CancelLichHenCommand command(String reason, String roleCode) {
            return command(APPOINTMENT_ID, reason, roleCode);
        }

        CancelLichHenCommand command(int appointmentId, String reason) {
            return command(appointmentId, reason, "RECEPTIONIST");
        }

        CancelLichHenCommand command(int appointmentId, String reason, String roleCode) {
            return new CancelLichHenCommand(appointmentId, reason, context(roleCode), actor(roleCode));
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
