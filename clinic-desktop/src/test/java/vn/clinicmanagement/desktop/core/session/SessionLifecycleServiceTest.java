package vn.clinicmanagement.desktop.core.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.MedicalRecordAuditEntry;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.ActiveSessionPort;
import vn.clinicmanagement.application.auth.AuthenticatedUser;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;

import javax.swing.SwingUtilities;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SessionLifecycleServiceTest {
    static {
        System.setProperty("java.awt.headless", "true");
    }

    private final UserSession session = UserSession.getInstance();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    private final AuditRecorder audit = new AuditRecorder();
    private final List<String> diagnostics = new ArrayList<>();
    private final SessionLifecycleService lifecycle = new SessionLifecycleService(
            session,
            new SessionTimeoutPolicy(Duration.ofMinutes(15), Duration.ofHours(12)),
            clock,
            audit,
            Runnable::run,
            diagnostics::add);
    private final RoleChoice role = new RoleChoice(42, "DOCTOR", "Bác sĩ");
    private final WorkScopeChoice scope = new WorkScopeChoice(11, "Cơ sở mẫu", 21, "Khoa mẫu");

    @AfterEach void clear() { session.clearSession(); }

    @Test void idleAtFourteenFiftyNineRemainsActiveThenLocksAtFifteenMinutes() {
        start();
        clock.advance(Duration.ofSeconds(899));
        assertEquals(SessionLifecycleService.State.ACTIVE, lifecycle.evaluate());
        assertTrue(audit.independentEntries.isEmpty());
        clock.advance(Duration.ofSeconds(1));
        assertEquals(SessionLifecycleService.State.LOCKED_IDLE, lifecycle.evaluate());
        assertTrue(session.isLocked());
        assertEquals(1, audit.independentEntries.size());
        SystemAuditEntry entry = audit.independentEntries.get(0);
        assertEquals(AuditActionCode.AUTH_SESSION_LOCKED_IDLE, entry.actionCode());
        assertEquals(AuditResourceType.EMPLOYEE, entry.resourceType());
        assertEquals(AuditOutcome.SUCCESS, entry.outcome());
        assertNull(entry.reasonCode());
        assertEquals(701, entry.actorEmployeeId());
        assertEquals("DOCTOR", entry.roleCode());
        assertEquals(11, entry.facilityId());
        assertEquals(21, entry.departmentId());
    }

    @Test void activityBeforeIdleBoundaryResetsItAndActivityWhileLockedDoesNotUnlock() {
        start();
        clock.advance(Duration.ofMinutes(14));
        lifecycle.recordActivity();
        clock.advance(Duration.ofMinutes(14));
        assertEquals(SessionLifecycleService.State.ACTIVE, lifecycle.evaluate());
        clock.advance(Duration.ofMinutes(1));
        assertEquals(SessionLifecycleService.State.LOCKED_IDLE, lifecycle.evaluate());
        clock.advance(Duration.ofMinutes(1));
        assertEquals(SessionLifecycleService.State.LOCKED_IDLE, lifecycle.recordActivity());
        assertEquals(SessionLifecycleService.State.LOCKED_IDLE, lifecycle.evaluate());
        assertTrue(session.isLocked());
        assertEquals(1, audit.independentEntries.size());
        assertEquals(AuditActionCode.AUTH_SESSION_LOCKED_IDLE, audit.independentEntries.get(0).actionCode());
    }

    @Test void idleReauthenticationRequiresSameUserRoleAndCurrentlyAssignedScopeAndAuditsEachOutcome() {
        start();
        clock.advance(Duration.ofMinutes(15));
        assertFalse(lifecycle.reauthenticate(null));
        assertFalse(lifecycle.reauthenticate(authenticated(702, List.of(role), List.of(scope))));
        assertFalse(lifecycle.reauthenticate(authenticated(701, List.of(new RoleChoice(43, "OTHER", "Khác")), List.of(scope))));
        assertFalse(lifecycle.reauthenticate(authenticated(701, List.of(role), List.of(new WorkScopeChoice(12, "Khác", 22, "Khác")))));
        assertTrue(session.isLocked());
        assertTrue(lifecycle.reauthenticate(authenticated(701, List.of(role), List.of(scope))));
        assertFalse(session.isLocked());
        assertEquals(clock.instant(), session.getLastActivityAt());

        assertEquals(6, audit.independentEntries.size());
        assertEquals(AuditActionCode.AUTH_SESSION_LOCKED_IDLE, audit.independentEntries.get(0).actionCode());

        assertEquals(AuditActionCode.AUTH_REAUTH_FAILED, audit.independentEntries.get(1).actionCode());
        assertEquals(AuditOutcome.FAILURE, audit.independentEntries.get(1).outcome());
        assertEquals(AuditReasonCode.AUTH_INVALID, audit.independentEntries.get(1).reasonCode());

        assertEquals(AuditActionCode.AUTH_REAUTH_FAILED, audit.independentEntries.get(2).actionCode());
        assertEquals(AuditOutcome.DENIED, audit.independentEntries.get(2).outcome());
        assertEquals(AuditReasonCode.AUTH_FORBIDDEN, audit.independentEntries.get(2).reasonCode());

        assertEquals(AuditActionCode.AUTH_REAUTH_FAILED, audit.independentEntries.get(3).actionCode());
        assertEquals(AuditOutcome.DENIED, audit.independentEntries.get(3).outcome());
        assertEquals(AuditReasonCode.ACCOUNT_REVOKED, audit.independentEntries.get(3).reasonCode());

        assertEquals(AuditActionCode.AUTH_REAUTH_FAILED, audit.independentEntries.get(4).actionCode());
        assertEquals(AuditOutcome.DENIED, audit.independentEntries.get(4).outcome());
        assertEquals(AuditReasonCode.SCOPE_MISMATCH, audit.independentEntries.get(4).reasonCode());

        assertEquals(AuditActionCode.AUTH_REAUTH_SUCCESS, audit.independentEntries.get(5).actionCode());
        assertEquals(AuditOutcome.SUCCESS, audit.independentEntries.get(5).outcome());
        assertNull(audit.independentEntries.get(5).reasonCode());
    }

    @Test void absoluteExpiryRequiresFreshAuthenticationAndStartsNewBoundedSessionWithSingleTransitionAudit() {
        start();
        String oldId = session.getSessionId();
        clock.advance(Duration.ofHours(11).plusMinutes(59).plusSeconds(59));
        assertEquals(SessionLifecycleService.State.LOCKED_IDLE, lifecycle.evaluate());
        assertEquals(SessionLifecycleService.State.LOCKED_IDLE, lifecycle.evaluate());
        clock.advance(Duration.ofSeconds(1));
        assertEquals(SessionLifecycleService.State.LOCKED_ABSOLUTE, lifecycle.evaluate());
        assertEquals(SessionLifecycleService.State.LOCKED_ABSOLUTE, lifecycle.evaluate());
        assertEquals(SessionLifecycleService.State.LOCKED_ABSOLUTE, lifecycle.recordActivity());

        assertEquals(2, audit.independentEntries.size());
        assertEquals(AuditActionCode.AUTH_SESSION_LOCKED_IDLE, audit.independentEntries.get(0).actionCode());
        assertEquals(AuditActionCode.AUTH_SESSION_EXPIRED_ABSOLUTE, audit.independentEntries.get(1).actionCode());
        assertNull(audit.independentEntries.get(1).reasonCode());

        assertFalse(lifecycle.reauthenticate(authenticated(701, List.of(role),
                List.of(new WorkScopeChoice(12, "Cơ sở thu hồi", 22, "Khoa thu hồi")))));
        assertTrue(session.isLocked());
        assertTrue(session.isAbsoluteExpiryPending());
        assertTrue(lifecycle.reauthenticate(authenticated(701, List.of(role), List.of(scope))));
        assertNotEquals(oldId, session.getSessionId());
        assertEquals(clock.instant(), session.getIssuedAt());
        assertEquals(clock.instant().plus(Duration.ofHours(12)), session.getAbsoluteExpiresAt());
        assertFalse(session.isLocked());

        assertEquals(4, audit.independentEntries.size());
        assertEquals(AuditActionCode.AUTH_REAUTH_FAILED, audit.independentEntries.get(2).actionCode());
        assertEquals(AuditActionCode.AUTH_REAUTH_SUCCESS, audit.independentEntries.get(3).actionCode());
        assertEquals(java.util.UUID.fromString(oldId), audit.independentEntries.get(3).correlationId());
    }

    @Test void logoutClearsIdentifiersAndAllLifecycleTimestampsAndAuditsExactlyOnce() {
        start();
        String activeSessionId = session.getSessionId();
        lifecycle.clear();
        lifecycle.clear();
        assertFalse(session.isLoggedOn());
        assertNull(session.getSessionId());
        assertNull(session.getIssuedAt());
        assertNull(session.getLastActivityAt());
        assertNull(session.getAbsoluteExpiresAt());
        assertFalse(session.isLocked());

        assertEquals(1, audit.independentEntries.size());
        SystemAuditEntry logoutEntry = audit.independentEntries.get(0);
        assertEquals(AuditActionCode.AUTH_LOGOUT, logoutEntry.actionCode());
        assertEquals(AuditOutcome.SUCCESS, logoutEntry.outcome());
        assertEquals(701, logoutEntry.actorEmployeeId());
        assertEquals("DOCTOR", logoutEntry.roleCode());
        assertEquals(11, logoutEntry.facilityId());
        assertEquals(21, logoutEntry.departmentId());
        assertEquals(java.util.UUID.fromString(activeSessionId), logoutEntry.correlationId());
    }

    @Test void sessionWithoutBoundedLifecycleMetadataFailsClosed() {
        session.setSession(701, "synthetic.701", "Nhân viên mẫu", role, scope);
        assertEquals(SessionLifecycleService.State.LOGGED_OUT, lifecycle.evaluate());
        assertFalse(session.isLoggedOn());
    }

    @Test void activeSessionPortReturnsAnAtomicMatchingActorScopeSnapshot() {
        start();
        java.util.UUID activeId = java.util.UUID.fromString(session.getSessionId());

        var snapshot = lifecycle.findActiveSession(activeId).orElseThrow();

        assertEquals(activeId, snapshot.sessionId());
        assertEquals(701, snapshot.employeeId());
        assertEquals(42, snapshot.roleId());
        assertEquals("DOCTOR", snapshot.roleCode());
        assertEquals(11, snapshot.facilityId());
        assertEquals(21, snapshot.departmentId());
        assertTrue(snapshot.active());
        assertTrue(snapshot.unlocked());
        assertTrue(snapshot.notExpired());
        assertTrue(lifecycle.findActiveSession(java.util.UUID.randomUUID()).isEmpty());
    }

    @Test void activeSessionPortReportsLockedAndExpiredSessionsAsInactive() {
        start();
        java.util.UUID activeId = java.util.UUID.fromString(session.getSessionId());
        clock.advance(Duration.ofMinutes(15));

        ActiveSessionPort.ActiveSession locked = lifecycle.findActiveSession(activeId).orElseThrow();
        assertFalse(locked.active());
        assertFalse(locked.unlocked());
        assertTrue(locked.notExpired());

        clock.advance(Duration.ofHours(12).minusMinutes(15));
        ActiveSessionPort.ActiveSession expired = lifecycle.findActiveSession(activeId).orElseThrow();
        assertFalse(expired.active());
        assertFalse(expired.unlocked());
        assertFalse(expired.notExpired());
    }

    @Test void lockingAndLogoutStillApplyWhenIndependentAuditWriteFailsAndDiagnosticsRemainSafe() {
        audit.failure = new IllegalStateException("Simulated SQL exception with secret-value-999 for synthetic.701");
        start();
        clock.advance(Duration.ofMinutes(15));

        assertEquals(SessionLifecycleService.State.LOCKED_IDLE, lifecycle.evaluate());
        assertTrue(session.isLocked());

        lifecycle.clear();
        assertFalse(session.isLoggedOn());
        assertEquals(2, diagnostics.size());
        for (String diagnostic : diagnostics) {
            assertFalse(diagnostic.contains("secret-value-999"));
            assertFalse(diagnostic.contains("synthetic.701"));
            assertFalse(diagnostic.contains("Simulated SQL exception"));
        }
        assertEquals(diagnostics.get(1), lifecycle.lastAuditDiagnostic());
    }

    @Test void reauthenticationRemainsLockedWhenSuccessAuditCannotBeWritten() {
        start();
        clock.advance(Duration.ofMinutes(15));
        assertEquals(SessionLifecycleService.State.LOCKED_IDLE, lifecycle.evaluate());
        audit.failure = new IllegalStateException("synthetic audit store unavailable");

        assertFalse(lifecycle.reauthenticate(authenticated(701, List.of(role), List.of(scope))));
        assertTrue(session.isLocked());
        assertTrue(lifecycle.lastAuditDiagnostic().contains("AUTH_REAUTH_SUCCESS"));
        assertEquals(1, audit.independentEntries.size());
    }

    @Test void sessionTransitionAuditRunsAsynchronouslyOffSwingEdtWithoutBlockingUiThread() throws Exception {
        CountDownLatch allowAuditToProceed = new CountDownLatch(1);
        CountDownLatch auditCompleted = new CountDownLatch(1);
        AtomicBoolean auditRanOnEdt = new AtomicBoolean(true);
        AtomicReference<SystemAuditEntry> capturedEntry = new AtomicReference<>();

        AuditWritePort blockingAuditPort = new AuditWritePort() {
            @Override
            public void appendInCurrentTransaction(SystemAuditEntry entry) {
                throw new AssertionError("Session transitions must use appendIndependently");
            }

            @Override
            public void appendIndependently(SystemAuditEntry entry) {
                auditRanOnEdt.set(SwingUtilities.isEventDispatchThread());
                try {
                    allowAuditToProceed.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                capturedEntry.set(entry);
                auditCompleted.countDown();
            }

            @Override public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {
                throw new AssertionError("Session transitions must not write medical-record audit events");
            }

            @Override public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {
                throw new AssertionError("Session transitions must not write medical-record audit events");
            }
        };

        SessionLifecycleService asyncLifecycle = new SessionLifecycleService(
                session,
                new SessionTimeoutPolicy(Duration.ofMinutes(15), Duration.ofHours(12)),
                clock,
                blockingAuditPort);

        asyncLifecycle.startSession(701, "synthetic.701", "Nhân viên mẫu", role, scope);
        clock.advance(Duration.ofMinutes(15));

        SwingUtilities.invokeAndWait(() -> {
            assertEquals(SessionLifecycleService.State.LOCKED_IDLE, asyncLifecycle.evaluate());
            assertTrue(session.isLocked());
        });

        allowAuditToProceed.countDown();
        assertTrue(auditCompleted.await(2, TimeUnit.SECONDS));
        assertFalse(auditRanOnEdt.get(), "Audit I/O must not execute on the Swing EDT");
        assertNotNull(capturedEntry.get());
        assertEquals(AuditActionCode.AUTH_SESSION_LOCKED_IDLE, capturedEntry.get().actionCode());
    }

    @Test void listenerNotifiedImmediatelyOnSessionReplacementAndUnsubscribeSafely() {
        java.util.concurrent.atomic.AtomicInteger notifications = new java.util.concurrent.atomic.AtomicInteger();
        SessionLifecycleService.SessionListener listener = notifications::incrementAndGet;
        lifecycle.addSessionListener(listener);
        assertEquals(0, notifications.get());

        start();
        assertEquals(1, notifications.get(), "Immediate notification on initial session start");

        // Identity/session replacement
        lifecycle.startSession(702, "synthetic.702", "Bác sĩ khác", role, scope);
        assertEquals(2, notifications.get(), "Immediate notification on session replacement");

        lifecycle.removeSessionListener(listener);
        lifecycle.startSession(703, "synthetic.703", "Bác sĩ thứ ba", role, scope);
        assertEquals(2, notifications.get(), "Unsubscribed listener receives no further notifications");
    }

    @Test void listenerNotifiedImmediatelyOnIdleLockAndAbsoluteExpiryTransitions() {
        start();
        java.util.concurrent.atomic.AtomicInteger notifications = new java.util.concurrent.atomic.AtomicInteger();
        lifecycle.addSessionListener(notifications::incrementAndGet);

        clock.advance(Duration.ofMinutes(15));
        assertEquals(0, notifications.get(), "No notification before evaluation");
        assertEquals(SessionLifecycleService.State.LOCKED_IDLE, lifecycle.evaluate());
        assertEquals(1, notifications.get(), "Immediate notification on idle lock transition");

        // Idempotent evaluate calls do not re-notify
        assertEquals(SessionLifecycleService.State.LOCKED_IDLE, lifecycle.evaluate());
        assertEquals(1, notifications.get());

        clock.advance(Duration.ofHours(12));
        assertEquals(SessionLifecycleService.State.LOCKED_ABSOLUTE, lifecycle.evaluate());
        assertEquals(2, notifications.get(), "Immediate notification on absolute expiry transition");

        assertEquals(SessionLifecycleService.State.LOCKED_ABSOLUTE, lifecycle.evaluate());
        assertEquals(2, notifications.get());
    }

    @Test void listenerNotifiedImmediatelyOnLogoutAndClearTransitions() {
        start();
        java.util.concurrent.atomic.AtomicInteger notifications = new java.util.concurrent.atomic.AtomicInteger();
        lifecycle.addSessionListener(notifications::incrementAndGet);

        lifecycle.clear();
        assertEquals(1, notifications.get(), "Immediate notification on session clear/logout");

        // Subsequent clear when already logged out does not re-notify
        lifecycle.clear();
        assertEquals(1, notifications.get());
    }

    @Test void listenerNotifiedImmediatelyOnSuccessfulReauthentication() {
        start();
        clock.advance(Duration.ofMinutes(15));
        assertEquals(SessionLifecycleService.State.LOCKED_IDLE, lifecycle.evaluate());

        java.util.concurrent.atomic.AtomicInteger notifications = new java.util.concurrent.atomic.AtomicInteger();
        lifecycle.addSessionListener(notifications::incrementAndGet);

        // Denied reauthentication should not trigger transition
        assertFalse(lifecycle.reauthenticate(authenticated(702, List.of(role), List.of(scope))));
        assertEquals(0, notifications.get());

        // Successful reauthentication unlocks session and notifies
        assertTrue(lifecycle.reauthenticate(authenticated(701, List.of(role), List.of(scope))));
        assertEquals(1, notifications.get(), "Immediate notification on unlocking reauthentication");
        assertEquals(SessionLifecycleService.State.ACTIVE, lifecycle.evaluate());
    }

    private void start() { lifecycle.startSession(701, "synthetic.701", "Nhân viên mẫu", role, scope); }
    private AuthenticatedUser authenticated(int id, List<RoleChoice> roles, List<WorkScopeChoice> scopes) {
        return new AuthenticatedUser(id, "synthetic." + id, "Nhân viên mẫu", roles, scopes);
    }

    private static final class AuditRecorder implements AuditWritePort {
        private final List<SystemAuditEntry> independentEntries = new ArrayList<>();
        private RuntimeException failure;

        @Override
        public void appendInCurrentTransaction(SystemAuditEntry entry) {
            throw new AssertionError("Session transitions must use appendIndependently");
        }

        @Override
        public void appendIndependently(SystemAuditEntry entry) {
            if (failure != null) throw failure;
            independentEntries.add(entry);
        }

        @Override public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) {
            throw new AssertionError("Session transitions must not write medical-record audit events");
        }

        @Override public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) {
            throw new AssertionError("Session transitions must not write medical-record audit events");
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        void advance(Duration duration) { instant = instant.plus(duration); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
