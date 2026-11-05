package vn.clinicmanagement.desktop.core.session;

import vn.clinicmanagement.application.auth.AuthenticatedUser;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Clock-driven idle and absolute timeout state transitions for the desktop session. */
public final class SessionLifecycleService {
    public enum State { ACTIVE, LOCKED_IDLE, LOCKED_ABSOLUTE, LOGGED_OUT }

    private final UserSession session;
    private final SessionTimeoutPolicy policy;
    private final Clock clock;

    public SessionLifecycleService(UserSession session, SessionTimeoutPolicy policy, Clock clock) {
        this.session = session;
        this.policy = policy;
        this.clock = clock;
    }

    public static SessionLifecycleService forCurrentSession(SessionTimeoutPolicy policy) {
        return new SessionLifecycleService(UserSession.getInstance(), policy, Clock.systemUTC());
    }

    public void startSession(int userId, String username, String fullName, RoleChoice role, WorkScopeChoice scope) {
        Instant now = clock.instant();
        session.setSession(userId, username, fullName, role, scope);
        session.setLifecycleMetadata(UUID.randomUUID().toString(), now, now, now.plus(policy.absoluteTimeout()));
    }

    public State evaluate() {
        if (!session.isLoggedOn()) return State.LOGGED_OUT;
        if (session.getSessionId() == null || session.getIssuedAt() == null || session.getLastActivityAt() == null
                || session.getAbsoluteExpiresAt() == null) {
            session.clearSession();
            return State.LOGGED_OUT;
        }
        Instant now = clock.instant();
        if (session.isLocked()) {
            if (!now.isBefore(session.getAbsoluteExpiresAt())) {
                session.setLocked(true, true);
                return State.LOCKED_ABSOLUTE;
            }
            return session.isAbsoluteExpiryPending() ? State.LOCKED_ABSOLUTE : State.LOCKED_IDLE;
        }
        if (!now.isBefore(session.getAbsoluteExpiresAt())) {
            session.setLocked(true, true);
            return State.LOCKED_ABSOLUTE;
        }
        if (!now.isBefore(session.getLastActivityAt().plus(policy.idleTimeout()))) {
            session.setLocked(true, false);
            return State.LOCKED_IDLE;
        }
        return State.ACTIVE;
    }

    /** Activity while locked is intentionally ignored. */
    public State recordActivity() {
        State state = evaluate();
        if (state == State.ACTIVE) session.setLastActivityAt(clock.instant());
        return state;
    }

    /** Unlocks or restarts only when the authenticated identity and live assignments match exactly. */
    public boolean reauthenticate(AuthenticatedUser authenticated) {
        State lockState = evaluate();
        if ((lockState != State.LOCKED_IDLE && lockState != State.LOCKED_ABSOLUTE) || authenticated == null
                || authenticated.userId() != session.getUserId()) return false;
        RoleChoice role = authenticated.roles().stream().filter(r -> r.id() == session.getRoleId()).findFirst().orElse(null);
        WorkScopeChoice scope = authenticated.workScopes().stream()
                .filter(s -> s.facilityId() == session.getFacilityId() && s.departmentId() == session.getDepartmentId())
                .findFirst().orElse(null);
        if (role == null || scope == null) return false;

        Instant now = clock.instant();
        if (lockState == State.LOCKED_ABSOLUTE) {
            startSession(authenticated.userId(), authenticated.username(), authenticated.fullName(), role, scope);
        } else {
            session.setLocked(false, false);
            session.setLastActivityAt(now);
        }
        return true;
    }

    public void clear() { session.clearSession(); }
    public State state() { return evaluate(); }
}
