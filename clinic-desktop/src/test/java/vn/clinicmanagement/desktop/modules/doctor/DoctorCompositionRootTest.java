package vn.clinicmanagement.desktop.modules.doctor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;
import vn.clinicmanagement.application.auth.CompletedLoginSession;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.UserSession;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DoctorCompositionRootTest {
    private static final AuditWritePort NOOP_AUDIT = new AuditWritePort() {
        @Override public void appendInCurrentTransaction(SystemAuditEntry entry) {}
        @Override public void appendIndependently(SystemAuditEntry entry) {}
        @Override public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {}
        @Override public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {}
    };

    private SessionLifecycleService lifecycle;

    @AfterEach
    void cleanUp() {
        UserSession.getInstance().clearSession();
    }

    @Test
    void composesGuardedQueueAndHistoryHandlersWithoutOpeningDatabaseConnections() {
        lifecycle = new SessionLifecycleService(
                UserSession.getInstance(),
                new SessionTimeoutPolicy(Duration.ofMinutes(15), Duration.ofHours(12)),
                Clock.systemUTC(),
                NOOP_AUDIT);
        lifecycle.startSession(new CompletedLoginSession(
                701, "synthetic-doctor", "Synthetic Doctor",
                new RoleChoice(42, "DOCTOR", "Synthetic Doctor"),
                new WorkScopeChoice(11, "Synthetic Facility", 21, "Synthetic Department"),
                UUID.randomUUID(),
                Instant.now()));

        DoctorCompositionRoot.Services services = DoctorCompositionRoot.create(lifecycle);

        assertNotNull(services.doctorQueue());
        assertNotNull(services.patientHistory());
    }

    @Test
    void rejectsNullDependencies() {
        lifecycle = new SessionLifecycleService(
                UserSession.getInstance(),
                new SessionTimeoutPolicy(Duration.ofMinutes(15), Duration.ofHours(12)),
                Clock.systemUTC(),
                NOOP_AUDIT);
        DoctorCompositionRoot.Services services = DoctorCompositionRoot.create(lifecycle);

        assertThrows(NullPointerException.class, () -> DoctorCompositionRoot.create(null));
        assertThrows(NullPointerException.class,
                () -> new DoctorCompositionRoot.Services(null, services.patientHistory()));
        assertThrows(NullPointerException.class,
                () -> new DoctorCompositionRoot.Services(services.doctorQueue(), null));
    }
}
