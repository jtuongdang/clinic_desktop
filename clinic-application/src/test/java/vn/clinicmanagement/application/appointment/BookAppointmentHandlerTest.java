package vn.clinicmanagement.application.appointment;

import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.appointment.LichHen;
import vn.clinicmanagement.domain.common.enums.TrangThaiLichHen;
import vn.clinicmanagement.domain.repositories.ILichHenRepository;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class BookAppointmentHandlerTest {
    private static final int PATIENT_ID = 24;
    private static final int DOCTOR_ID = 31;
    private static final int FACILITY_ID = 3;
    private static final int DEPARTMENT_ID = 8;
    private static final int EMPLOYEE_ID = 17;
    private static final int ROLE_ID = 4;
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 8, 9, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 11, 8, 9, 30);
    private static final UUID SESSION_ID = UUID.fromString("a43eeb1d-7e98-4ceb-8b84-903a1fdab203");
    private static final UUID CORRELATION_ID = UUID.fromString("0b8a0d60-0cc7-4fc8-9534-754f5f0e0017");

    @Test
    void booksAppointmentInPendingConfirmationStateAndAuditsInTransaction() {
        Fixture fixture = new Fixture(0);

        Result<AppointmentReceipt> result = fixture.handler().handle(fixture.command());

        assertTrue(result.isSuccess());
        assertEquals(91, result.valueOrThrow().appointmentId());
        assertEquals(TrangThaiLichHen.CHO_XAC_NHAN, result.valueOrThrow().status());
        assertEquals(TrangThaiLichHen.CHO_XAC_NHAN, fixture.saved.getTrangThai());
        assertEquals(PATIENT_ID, fixture.saved.getMaBenhNhan());
        assertEquals(DOCTOR_ID, fixture.saved.getMaBacSi());
        assertEquals(FACILITY_ID, fixture.saved.getMaCoSo());
        assertEquals(DEPARTMENT_ID, fixture.saved.getMaKhoa());
        assertEquals(EMPLOYEE_ID, fixture.saved.getCreatedBy());
        assertEquals(1, fixture.transactions.calls);
        assertEquals(1, fixture.slotChecks);
        assertEquals(0, fixture.audit.independentWrites);
        assertTrue(fixture.audit.inTransaction);
        assertEquals(AuditActionCode.VISIT_REGISTERED, fixture.audit.entry.actionCode());
        assertEquals(AuditResourceType.PATIENT, fixture.audit.entry.resourceType());
        assertEquals(PATIENT_ID, fixture.audit.entry.resourceId());
        assertEquals(AuditOutcome.SUCCESS, fixture.audit.entry.outcome());
    }

    @Test
    void rejectsStartTimeAfterOrEqualToEndTime() {
        for (LocalDateTime start : new LocalDateTime[]{END, END.plusMinutes(1)}) {
            Fixture fixture = new Fixture(0);

            Result<AppointmentReceipt> result = fixture.handler().handle(fixture.command(
                    PATIENT_ID, DOCTOR_ID, FACILITY_ID, start, END, "RECEPTIONIST"));

            assertFailure(result, "VALIDATION_ERROR", "Khung giờ hẹn không hợp lệ.");
            assertEquals(0, fixture.transactions.calls);
            assertEquals(0, fixture.slotChecks);
            assertNull(fixture.saved);
        }
    }

    @Test
    void rejectsAlreadyBookedDoctorSlotWithoutPersisting() {
        Fixture fixture = new Fixture(1);

        Result<AppointmentReceipt> result = fixture.handler().handle(fixture.command());

        assertFailure(result, "SLOT_UNAVAILABLE",
                "Bác sĩ đã có lịch hẹn trong khung giờ này. Không được phép đặt trùng.");
        assertEquals(1, fixture.slotChecks);
        assertNull(fixture.saved);
        assertNull(fixture.audit.entry);
    }

    @Test
    void rejectsInvalidIdsAndMissingTimestamps() {
        BookAppointmentCommand[] invalidCommands = {
                fixtureCommand(0, DOCTOR_ID, FACILITY_ID, START, END),
                fixtureCommand(PATIENT_ID, 0, FACILITY_ID, START, END),
                fixtureCommand(PATIENT_ID, DOCTOR_ID, 0, START, END),
                fixtureCommand(PATIENT_ID, DOCTOR_ID, FACILITY_ID, null, END),
                fixtureCommand(PATIENT_ID, DOCTOR_ID, FACILITY_ID, START, null)
        };
        for (BookAppointmentCommand command : invalidCommands) {
            Fixture fixture = new Fixture(0);

            Result<AppointmentReceipt> result = fixture.handler().handle(command);

            assertFailure(result, "VALIDATION_ERROR", "Thông tin đặt lịch không hợp lệ.");
            assertEquals(0, fixture.transactions.calls);
            assertEquals(0, fixture.slotChecks);
            assertNull(fixture.saved);
        }
    }

    @Test
    void deniesUnauthorizedRoleAndMismatchedFacilityWithForbiddenAudit() {
        for (BookAppointmentCommand command : new BookAppointmentCommand[]{
                fixtureCommand(PATIENT_ID, DOCTOR_ID, FACILITY_ID, START, END, "DOCTOR"),
                fixtureCommand(PATIENT_ID, DOCTOR_ID, FACILITY_ID + 1, START, END, "RECEPTIONIST")
        }) {
            Fixture fixture = new Fixture(0);

            Result<AppointmentReceipt> result = fixture.handler().handle(command);

            assertFailure(result, "AUTHORIZATION_DENIED",
                    "Không được phép đặt lịch trong phiên hiện tại.");
            assertEquals(0, fixture.transactions.calls);
            assertEquals(0, fixture.slotChecks);
            assertNull(fixture.saved);
            assertEquals(1, fixture.audit.independentWrites);
            assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, fixture.audit.entry.actionCode());
            assertEquals(AuditOutcome.DENIED, fixture.audit.entry.outcome());
            assertEquals(AuditReasonCode.AUTH_FORBIDDEN, fixture.audit.entry.reasonCode());
        }
    }

    @Test
    void permitsAdminRole() {
        Fixture fixture = new Fixture(0);

        Result<AppointmentReceipt> result = fixture.handler().handle(fixture.command(
                PATIENT_ID, DOCTOR_ID, FACILITY_ID, START, END, "ADMIN"));

        assertTrue(result.isSuccess());
        assertEquals(1, fixture.slotChecks);
        assertNotNull(fixture.saved);
    }

    private static void assertFailure(Result<AppointmentReceipt> result, String code, String message) {
        assertTrue(result.isFailure());
        assertEquals(code, result.getError().orElseThrow().code());
        assertEquals(message, result.getError().orElseThrow().message());
    }

    private static AuthorizationContext context(String roleCode) {
        return new AuthorizationContext(EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
    }

    private static AuditActorContext actor(String roleCode) {
        return new AuditActorContext(EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID, "station-1",
                CORRELATION_ID);
    }

    private static BookAppointmentCommand fixtureCommand(int patientId, int doctorId, int facilityId,
                                                          LocalDateTime start, LocalDateTime end) {
        return fixtureCommand(patientId, doctorId, facilityId, start, end, "RECEPTIONIST");
    }

    private static BookAppointmentCommand fixtureCommand(int patientId, int doctorId, int facilityId,
                                                          LocalDateTime start, LocalDateTime end, String roleCode) {
        return new BookAppointmentCommand(patientId, doctorId, facilityId, DEPARTMENT_ID, start, end,
                "Synthetic appointment reason", "RECEPTION", context(roleCode), actor(roleCode));
    }

    private static final class Fixture {
        final TransactionFixture transactions = new TransactionFixture();
        final AuditFixture audit = new AuditFixture(transactions);
        final ILichHenRepository appointments;
        final int occupiedSlots;
        int slotChecks;
        LichHen saved;

        Fixture(int occupiedSlots) {
            this.occupiedSlots = occupiedSlots;
            appointments = (ILichHenRepository) Proxy.newProxyInstance(
                    ILichHenRepository.class.getClassLoader(), new Class<?>[]{ILichHenRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "countAppointmentsInSlot" -> {
                            slotChecks++;
                            yield this.occupiedSlots;
                        }
                        case "add" -> {
                            saved = (LichHen) args[0];
                            saved.setId(91);
                            yield null;
                        }
                        default -> null;
                    });
        }

        BookAppointmentHandler handler() {
            return new BookAppointmentHandler(appointments, transactions, audit);
        }

        BookAppointmentCommand command() {
            return fixtureCommand(PATIENT_ID, DOCTOR_ID, FACILITY_ID, START, END);
        }

        BookAppointmentCommand command(int patientId, int doctorId, int facilityId,
                                       LocalDateTime start, LocalDateTime end, String roleCode) {
            return fixtureCommand(patientId, doctorId, facilityId, start, end, roleCode);
        }
    }

    private static final class TransactionFixture implements TransactionBoundary {
        boolean current;
        int calls;

        @Override
        public <T> Result<T> inTransaction(Supplier<Result<T>> operation) {
            calls++;
            current = true;
            try {
                return operation.get();
            } finally {
                current = false;
            }
        }
    }

    private static final class AuditFixture implements AuditWritePort {
        private final TransactionFixture transactions;
        boolean inTransaction;
        int independentWrites;
        SystemAuditEntry entry;

        AuditFixture(TransactionFixture transactions) {
            this.transactions = transactions;
        }

        @Override
        public void appendInCurrentTransaction(SystemAuditEntry auditEntry) {
            inTransaction = transactions.current;
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
