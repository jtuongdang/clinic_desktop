package vn.clinicmanagement.desktop.modules.doctor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;
import vn.clinicmanagement.desktop.core.session.SessionLifecycleService;
import vn.clinicmanagement.desktop.core.session.UserSession;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DoctorCompositionRootTest {
    private SessionLifecycleService lifecycle;

    @AfterEach
    void cleanUp() {
        if (lifecycle != null) lifecycle.clear();
    }

    @Test
    void composesGuardedQueueAndHistoryHandlersWithoutOpeningDatabaseConnections() {
        lifecycle = new SessionLifecycleService(
                UserSession.getInstance(),
                new SessionTimeoutPolicy(Duration.ofMinutes(15), Duration.ofHours(12)),
                Clock.systemUTC());
        lifecycle.startSession(
                701, "synthetic-doctor", "Synthetic Doctor",
                new RoleChoice(42, "DOCTOR", "Synthetic Doctor"),
                new WorkScopeChoice(11, "Synthetic Facility", 21, "Synthetic Department"),
                UUID.randomUUID().toString());

        DoctorCompositionRoot.Services services = DoctorCompositionRoot.create(lifecycle);

        assertNotNull(services.doctorQueue());
        assertNotNull(services.patientHistory());
    }
}
