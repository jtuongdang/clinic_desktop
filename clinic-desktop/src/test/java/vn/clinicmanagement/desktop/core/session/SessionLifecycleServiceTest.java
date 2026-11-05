package vn.clinicmanagement.desktop.core.session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.auth.AuthenticatedUser;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionLifecycleServiceTest {
    private final UserSession session = UserSession.getInstance();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    private final SessionLifecycleService lifecycle = new SessionLifecycleService(session,
            new SessionTimeoutPolicy(Duration.ofMinutes(15), Duration.ofHours(12)), clock);
    private final RoleChoice role = new RoleChoice(42, "DOCTOR", "Bác sĩ");
    private final WorkScopeChoice scope = new WorkScopeChoice(11, "Cơ sở mẫu", 21, "Khoa mẫu");

    @AfterEach void clear() { lifecycle.clear(); }

    @Test void idleAtFourteenFiftyNineRemainsActiveThenLocksAtFifteenMinutes() {
        start();
        clock.advance(Duration.ofSeconds(899));
        assertEquals(SessionLifecycleService.State.ACTIVE, lifecycle.evaluate());
        clock.advance(Duration.ofSeconds(1));
        assertEquals(SessionLifecycleService.State.LOCKED_IDLE, lifecycle.evaluate());
        assertTrue(session.isLocked());
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
        assertTrue(session.isLocked());
    }

    @Test void idleReauthenticationRequiresSameUserRoleAndCurrentlyAssignedScope() {
        start();
        clock.advance(Duration.ofMinutes(15));
        assertFalse(lifecycle.reauthenticate(authenticated(702, List.of(role), List.of(scope))));
        assertFalse(lifecycle.reauthenticate(authenticated(701, List.of(new RoleChoice(43, "OTHER", "Khác")), List.of(scope))));
        assertFalse(lifecycle.reauthenticate(authenticated(701, List.of(role), List.of(new WorkScopeChoice(12, "Khác", 22, "Khác")))));
        assertTrue(session.isLocked());
        assertTrue(lifecycle.reauthenticate(authenticated(701, List.of(role), List.of(scope))));
        assertFalse(session.isLocked());
        assertEquals(clock.instant(), session.getLastActivityAt());
    }

    @Test void absoluteExpiryRequiresFreshAuthenticationAndStartsNewBoundedSession() {
        start();
        String oldId = session.getSessionId();
        clock.advance(Duration.ofHours(11).plusMinutes(59).plusSeconds(59));
        assertEquals(SessionLifecycleService.State.LOCKED_IDLE, lifecycle.evaluate());
        clock.advance(Duration.ofSeconds(1));
        assertEquals(SessionLifecycleService.State.LOCKED_ABSOLUTE, lifecycle.evaluate());
        assertFalse(lifecycle.reauthenticate(authenticated(701, List.of(role),
                List.of(new WorkScopeChoice(12, "Cơ sở thu hồi", 22, "Khoa thu hồi")))));
        assertTrue(session.isLocked());
        assertTrue(session.isAbsoluteExpiryPending());
        assertTrue(lifecycle.reauthenticate(authenticated(701, List.of(role), List.of(scope))));
        assertNotEquals(oldId, session.getSessionId());
        assertEquals(clock.instant(), session.getIssuedAt());
        assertEquals(clock.instant().plus(Duration.ofHours(12)), session.getAbsoluteExpiresAt());
        assertFalse(session.isLocked());
    }

    @Test void logoutClearsIdentifiersAndAllLifecycleTimestamps() {
        start();
        lifecycle.clear();
        assertFalse(session.isLoggedOn());
        assertNull(session.getSessionId());
        assertNull(session.getIssuedAt());
        assertNull(session.getLastActivityAt());
        assertNull(session.getAbsoluteExpiresAt());
        assertFalse(session.isLocked());
    }

    @Test void sessionWithoutBoundedLifecycleMetadataFailsClosed() {
        session.setSession(701, "synthetic.701", "Nhân viên mẫu", role, scope);
        assertEquals(SessionLifecycleService.State.LOGGED_OUT, lifecycle.evaluate());
        assertFalse(session.isLoggedOn());
    }

    private void start() { lifecycle.startSession(701, "synthetic.701", "Nhân viên mẫu", role, scope); }
    private AuthenticatedUser authenticated(int id, List<RoleChoice> roles, List<WorkScopeChoice> scopes) {
        return new AuthenticatedUser(id, "synthetic." + id, "Nhân viên mẫu", roles, scopes);
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
