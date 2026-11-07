package vn.clinicmanagement.application.appointment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.appointment.LichHen;
import vn.clinicmanagement.domain.common.IUnitOfWork;
import vn.clinicmanagement.domain.common.enums.TrangThaiLichHen;
import vn.clinicmanagement.domain.repositories.ILichHenRepository;
import vn.clinicmanagement.domain.repositories.ISpecification;
import vn.clinicmanagement.domain.repositories.PagedResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GetUpcomingAppointmentsHandlerTest {

    private static final int PATIENT_ID = 42;
    private static final int EMPLOYEE_ID = 10;
    private static final int ROLE_ID = 2;
    private static final String ROLE_CODE = "RECEPTIONIST";
    private static final int FACILITY_ID = 1;
    private static final int DEPARTMENT_ID = 5;
    private static final String WORKSTATION_ID = "WS-RECEPTION-01";
    private static final UUID SESSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CORRELATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private FakeLichHenRepository appointmentRepository;
    private FakeAuditWritePort auditPort;
    private GetUpcomingAppointmentsHandler handler;

    @BeforeEach
    void setUp() {
        appointmentRepository = new FakeLichHenRepository();
        auditPort = new FakeAuditWritePort();
        handler = new GetUpcomingAppointmentsHandler(appointmentRepository, auditPort);
    }

    @Test
    void handleSuccessfulRetrievalAndMappingOfUpcomingAppointments() {
        LocalDateTime start1 = LocalDateTime.of(2026, 11, 10, 9, 0);
        LocalDateTime end1 = LocalDateTime.of(2026, 11, 10, 9, 30);
        LichHen appt1 = new LichHen();
        appt1.setId(101);
        appt1.setMaBenhNhan(PATIENT_ID);
        appt1.setMaBacSi(201);
        appt1.setMaCoSo(FACILITY_ID);
        appt1.setMaKhoa(DEPARTMENT_ID);
        appt1.setThoiGianBatDau(start1);
        appt1.setThoiGianKetThuc(end1);
        appt1.setTrangThai(TrangThaiLichHen.DA_XAC_NHAN);
        appt1.setLyDo("Tái khám tim mạch");
        appt1.setNguonDat("ONLINE");

        LocalDateTime start2 = LocalDateTime.of(2026, 11, 15, 14, 0);
        LocalDateTime end2 = LocalDateTime.of(2026, 11, 15, 14, 30);
        LichHen appt2 = new LichHen();
        appt2.setId(102);
        appt2.setMaBenhNhan(PATIENT_ID);
        appt2.setMaBacSi(202);
        appt2.setMaCoSo(FACILITY_ID);
        appt2.setMaKhoa(null);
        appt2.setThoiGianBatDau(start2);
        appt2.setThoiGianKetThuc(end2);
        appt2.setTrangThai(TrangThaiLichHen.CHO_XAC_NHAN);
        appt2.setLyDo("Khám sức khỏe tổng quát");
        appt2.setNguonDat("LE_TAN");

        appointmentRepository.appointments = List.of(appt1, appt2);

        GetUpcomingAppointmentsQuery query = createValidQuery(PATIENT_ID, ROLE_CODE);
        Result<List<AppointmentSummaryDto>> result = handler.handle(query);

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
        assertTrue(result.getValue().isPresent());

        List<AppointmentSummaryDto> dtos = result.getValue().get();
        assertEquals(2, dtos.size());

        AppointmentSummaryDto dto1 = dtos.get(0);
        assertEquals(101, dto1.appointmentId());
        assertEquals(201, dto1.doctorId());
        assertEquals(FACILITY_ID, dto1.facilityId());
        assertEquals(DEPARTMENT_ID, dto1.departmentId());
        assertEquals(start1, dto1.startTime());
        assertEquals(end1, dto1.endTime());
        assertEquals("DA_XAC_NHAN", dto1.status());
        assertEquals("Tái khám tim mạch", dto1.reason());
        assertEquals("ONLINE", dto1.bookingSource());

        AppointmentSummaryDto dto2 = dtos.get(1);
        assertEquals(102, dto2.appointmentId());
        assertEquals(202, dto2.doctorId());
        assertEquals(FACILITY_ID, dto2.facilityId());
        assertNull(dto2.departmentId());
        assertEquals(start2, dto2.startTime());
        assertEquals(end2, dto2.endTime());
        assertEquals("CHO_XAC_NHAN", dto2.status());
        assertEquals("Khám sức khỏe tổng quát", dto2.reason());
        assertEquals("LE_TAN", dto2.bookingSource());

        assertEquals(1, appointmentRepository.callCount);
        assertEquals(PATIENT_ID, appointmentRepository.capturedPatientId);

        assertEquals(1, auditPort.independentEntries.size());
        SystemAuditEntry auditEntry = auditPort.independentEntries.get(0);
        assertEquals(AuditActionCode.PATIENT_VIEW, auditEntry.actionCode());
        assertEquals(AuditResourceType.PATIENT, auditEntry.resourceType());
        assertEquals(PATIENT_ID, auditEntry.resourceId());
        assertEquals(AuditOutcome.SUCCESS, auditEntry.outcome());
        assertNull(auditEntry.reasonCode());
        assertEquals(EMPLOYEE_ID, auditEntry.actorEmployeeId());
        assertEquals(ROLE_ID, auditEntry.roleId());
        assertEquals(ROLE_CODE, auditEntry.roleCode());
        assertEquals(FACILITY_ID, auditEntry.facilityId());
        assertEquals(DEPARTMENT_ID, auditEntry.departmentId());
        assertEquals(WORKSTATION_ID, auditEntry.workstationId());
        assertEquals(CORRELATION_ID, auditEntry.correlationId());
        assertNotNull(auditEntry.occurredAtUtc());
    }

    @Test
    void handleRejectionWhenPatientIdIsZero() {
        GetUpcomingAppointmentsQuery query = createValidQuery(0, ROLE_CODE);
        Result<List<AppointmentSummaryDto>> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertFalse(result.isSuccess());
        assertTrue(result.getError().isPresent());
        assertEquals("VALIDATION_ERROR", result.getError().get().code());
        assertEquals("Mã bệnh nhân không hợp lệ.", result.getError().get().message());

        assertEquals(0, appointmentRepository.callCount);
        assertTrue(auditPort.independentEntries.isEmpty());
    }

    @Test
    void handleRejectionWhenPatientIdIsNegative() {
        GetUpcomingAppointmentsQuery query = createValidQuery(-5, ROLE_CODE);
        Result<List<AppointmentSummaryDto>> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertEquals("VALIDATION_ERROR", result.getError().orElseThrow().code());
        assertEquals("Mã bệnh nhân không hợp lệ.", result.getError().get().message());

        assertEquals(0, appointmentRepository.callCount);
        assertTrue(auditPort.independentEntries.isEmpty());
    }

    @Test
    void handleRejectionWhenQueryIsNull() {
        Result<List<AppointmentSummaryDto>> result = handler.handle(null);

        assertTrue(result.isFailure());
        assertEquals("VALIDATION_ERROR", result.getError().orElseThrow().code());
        assertEquals("Mã bệnh nhân không hợp lệ.", result.getError().get().message());

        assertEquals(0, appointmentRepository.callCount);
        assertTrue(auditPort.independentEntries.isEmpty());
    }

    @Test
    void handleDenialWhenCallerSessionContextIsNull() {
        GetUpcomingAppointmentsQuery query = new GetUpcomingAppointmentsQuery(PATIENT_ID, null, null);
        Result<List<AppointmentSummaryDto>> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertEquals("AUTH_ACCESS_DENIED", result.getError().orElseThrow().code());
        assertEquals(0, appointmentRepository.callCount);

        assertEquals(1, auditPort.independentEntries.size());
        SystemAuditEntry auditEntry = auditPort.independentEntries.get(0);
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, auditEntry.actionCode());
        assertEquals(AuditOutcome.DENIED, auditEntry.outcome());
        assertEquals(AuditReasonCode.AUTH_FORBIDDEN, auditEntry.reasonCode());
        assertNull(auditEntry.actorEmployeeId());
        assertNull(auditEntry.roleId());
        assertNull(auditEntry.roleCode());
    }

    @Test
    void handleDenialWhenCallerHasUnauthorizedRole() {
        GetUpcomingAppointmentsQuery query = createValidQuery(PATIENT_ID, "PHARMACIST");
        Result<List<AppointmentSummaryDto>> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertEquals("AUTH_ACCESS_DENIED", result.getError().orElseThrow().code());
        assertEquals(0, appointmentRepository.callCount);

        assertEquals(1, auditPort.independentEntries.size());
        SystemAuditEntry auditEntry = auditPort.independentEntries.get(0);
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, auditEntry.actionCode());
        assertEquals(AuditOutcome.DENIED, auditEntry.outcome());
        assertEquals(AuditReasonCode.AUTH_FORBIDDEN, auditEntry.reasonCode());
        assertEquals(EMPLOYEE_ID, auditEntry.actorEmployeeId());
        assertEquals(ROLE_ID, auditEntry.roleId());
        assertEquals("PHARMACIST", auditEntry.roleCode());
        assertEquals(FACILITY_ID, auditEntry.facilityId());
        assertEquals(DEPARTMENT_ID, auditEntry.departmentId());
    }

    @Test
    void handleDenialWhenCallerSessionContextIsInvalid() {
        AuthorizationContext invalidContext = new AuthorizationContext(
                -1, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
        GetUpcomingAppointmentsQuery query = new GetUpcomingAppointmentsQuery(PATIENT_ID, invalidContext, null);

        Result<List<AppointmentSummaryDto>> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertEquals("AUTH_ACCESS_DENIED", result.getError().orElseThrow().code());
        assertEquals(0, appointmentRepository.callCount);

        assertEquals(1, auditPort.independentEntries.size());
        SystemAuditEntry auditEntry = auditPort.independentEntries.get(0);
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, auditEntry.actionCode());
        assertEquals(AuditOutcome.DENIED, auditEntry.outcome());
        assertEquals(AuditReasonCode.AUTH_FORBIDDEN, auditEntry.reasonCode());
    }

    @Test
    void handleDenialWhenAuditActorDoesNotMatchSessionContext() {
        AuthorizationContext authContext = new AuthorizationContext(
                EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
        AuditActorContext mismatchedActor = new AuditActorContext(
                999, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, WORKSTATION_ID, CORRELATION_ID);
        GetUpcomingAppointmentsQuery query = new GetUpcomingAppointmentsQuery(PATIENT_ID, authContext, mismatchedActor);

        Result<List<AppointmentSummaryDto>> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertEquals("AUTH_ACCESS_DENIED", result.getError().orElseThrow().code());
        assertEquals(0, appointmentRepository.callCount);

        assertEquals(1, auditPort.independentEntries.size());
        SystemAuditEntry auditEntry = auditPort.independentEntries.get(0);
        assertEquals(AuditActionCode.AUTH_ACCESS_DENIED, auditEntry.actionCode());
        assertEquals(AuditOutcome.DENIED, auditEntry.outcome());
        assertEquals(AuditReasonCode.AUTH_FORBIDDEN, auditEntry.reasonCode());
    }

    @Test
    void handleAuthorizedDoctorRoleSucceeds() {
        appointmentRepository.appointments = List.of();
        GetUpcomingAppointmentsQuery query = createValidQuery(PATIENT_ID, "DOCTOR");

        Result<List<AppointmentSummaryDto>> result = handler.handle(query);

        assertTrue(result.isSuccess());
        assertEquals(1, appointmentRepository.callCount);
        assertEquals(1, auditPort.independentEntries.size());
        assertEquals(AuditActionCode.PATIENT_VIEW, auditPort.independentEntries.get(0).actionCode());
        assertEquals("DOCTOR", auditPort.independentEntries.get(0).roleCode());
    }

    @Test
    void handleAuthorizedAdminRoleSucceeds() {
        appointmentRepository.appointments = List.of();
        GetUpcomingAppointmentsQuery query = createValidQuery(PATIENT_ID, "ADMIN");

        Result<List<AppointmentSummaryDto>> result = handler.handle(query);

        assertTrue(result.isSuccess());
        assertEquals(1, appointmentRepository.callCount);
        assertEquals(1, auditPort.independentEntries.size());
        assertEquals(AuditActionCode.PATIENT_VIEW, auditPort.independentEntries.get(0).actionCode());
        assertEquals("ADMIN", auditPort.independentEntries.get(0).roleCode());
    }

    @Test
    void handleRepositoryRuntimeExceptionReturnsFailureAndDoesNotEmitSuccessAudit() {
        appointmentRepository.exceptionToThrow = new RuntimeException("DB query timed out");
        GetUpcomingAppointmentsQuery query = createValidQuery(PATIENT_ID, ROLE_CODE);

        Result<List<AppointmentSummaryDto>> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertEquals("GET_UPCOMING_APPOINTMENTS_FAILED", result.getError().orElseThrow().code());
        assertEquals("Không thể tải danh sách lịch hẹn.", result.getError().get().message());

        assertEquals(1, appointmentRepository.callCount);
        assertTrue(auditPort.independentEntries.isEmpty(), "No success audit must be emitted when repository fails");
    }

    @Test
    void handleAuditFailureDuringDenialStillReturnsFailureSafely() {
        auditPort.appendException = new RuntimeException("Audit sink unreachable");
        GetUpcomingAppointmentsQuery query = new GetUpcomingAppointmentsQuery(PATIENT_ID, null, null);

        Result<List<AppointmentSummaryDto>> result = handler.handle(query);

        assertTrue(result.isFailure());
        assertEquals("AUTH_ACCESS_DENIED", result.getError().orElseThrow().code());
    }

    @Test
    void handleQueryWithSessionContextOnlyWithoutAuditActorSucceeds() {
        AuthorizationContext authContext = new AuthorizationContext(
                EMPLOYEE_ID, ROLE_ID, ROLE_CODE, FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
        GetUpcomingAppointmentsQuery query = new GetUpcomingAppointmentsQuery(PATIENT_ID, authContext);

        Result<List<AppointmentSummaryDto>> result = handler.handle(query);

        assertTrue(result.isSuccess());
        assertEquals(1, appointmentRepository.callCount);
        assertEquals(1, auditPort.independentEntries.size());
        SystemAuditEntry entry = auditPort.independentEntries.get(0);
        assertEquals(AuditActionCode.PATIENT_VIEW, entry.actionCode());
        assertEquals(SESSION_ID, entry.correlationId());
    }

    private static GetUpcomingAppointmentsQuery createValidQuery(int patientId, String roleCode) {
        AuthorizationContext authContext = new AuthorizationContext(
                EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID, SESSION_ID);
        AuditActorContext actorContext = new AuditActorContext(
                EMPLOYEE_ID, ROLE_ID, roleCode, FACILITY_ID, DEPARTMENT_ID, WORKSTATION_ID, CORRELATION_ID);
        return new GetUpcomingAppointmentsQuery(patientId, authContext, actorContext);
    }

    private static final class FakeLichHenRepository implements ILichHenRepository {
        List<LichHen> appointments = new ArrayList<>();
        RuntimeException exceptionToThrow;
        int callCount;
        int capturedPatientId = -1;

        @Override
        public List<LichHen> getUpcomingAppointments(int patientId) {
            callCount++;
            capturedPatientId = patientId;
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            return appointments;
        }

        @Override
        public int countAppointmentsInSlot(int doctorId, LocalDateTime start, LocalDateTime end) {
            return 0;
        }

        @Override public Optional<LichHen> getById(int id) { return Optional.empty(); }
        @Override public List<LichHen> getAll() { return List.of(); }
        @Override public PagedResult<LichHen> getPaged(int pageIndex, int pageSize, ISpecification<LichHen> specification) { return null; }
        @Override public List<LichHen> find(ISpecification<LichHen> specification) { return List.of(); }
        @Override public boolean exists(ISpecification<LichHen> specification) { return false; }
        @Override public int count(ISpecification<LichHen> specification) { return 0; }
        @Override public void add(LichHen entity) {}
        @Override public void update(LichHen entity) {}
        @Override public void delete(LichHen entity) {}
        @Override public IUnitOfWork getUnitOfWork() { return null; }
    }

    private static final class FakeAuditWritePort implements AuditWritePort {
        final List<SystemAuditEntry> currentTransactionEntries = new ArrayList<>();
        final List<SystemAuditEntry> independentEntries = new ArrayList<>();
        RuntimeException appendException;

        @Override
        public void appendInCurrentTransaction(SystemAuditEntry entry) {
            if (appendException != null) throw appendException;
            currentTransactionEntries.add(entry);
        }

        @Override
        public void appendIndependently(SystemAuditEntry entry) {
            if (appendException != null) throw appendException;
            independentEntries.add(entry);
        }

        @Override
        public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {
            if (appendException != null) throw appendException;
        }

        @Override
        public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {
            if (appendException != null) throw appendException;
        }
    }
}
