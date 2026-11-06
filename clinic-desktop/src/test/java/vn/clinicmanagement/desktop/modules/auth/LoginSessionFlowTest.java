package vn.clinicmanagement.desktop.modules.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.auth.*;
import vn.clinicmanagement.application.auth.AuthenticatedUser.RoleChoice;
import vn.clinicmanagement.application.auth.AuthenticatedUser.WorkScopeChoice;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.session.SessionTimeoutPolicy;
import vn.clinicmanagement.desktop.core.session.*;

import javax.swing.SwingUtilities;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class LoginSessionFlowTest {
    private final UserSession session = UserSession.getInstance();
    private final Instant now = Instant.parse("2026-01-01T00:00:00Z");
    private final RoleChoice role = new RoleChoice(42, "DOCTOR", "Synthetic role");
    private final WorkScopeChoice scope = new WorkScopeChoice(11, "Synthetic facility", 21, "Synthetic department");
    private final AuthenticatedUser user = new AuthenticatedUser(701, "synthetic.701", "Synthetic employee", List.of(role), List.of(scope));
    private final CompleteLoginSessionCommand command = new CompleteLoginSessionCommand(user, role, scope);
    private final CompletedLoginSession completed = new CompletedLoginSession(701, user.username(), user.fullName(), role, scope, UUID.randomUUID(), now);

    @AfterEach void clear() { session.clearSession(); }

    @Test void onlySingletonChoicesAreAutomaticAndMultipleChoicesRequireAnAssignedObject() {
        assertSame(role, LoginForm.chooseAssigned(List.of(role), ignored -> { fail("No singleton prompt"); return null; }));
        RoleChoice second = new RoleChoice(43, "RECEPTION", "Synthetic second role");
        assertSame(second, LoginForm.chooseAssigned(List.of(role, second), choices -> choices[1]));
        assertNull(LoginForm.chooseAssigned(List.of(role, second), choices -> null));
        assertNull(LoginForm.chooseAssigned(List.of(role, second), choices -> new RoleChoice(role.id(), role.code(), role.name())));
        WorkScopeChoice secondScope = new WorkScopeChoice(12, "Other facility", 22, "Other department");
        assertSame(secondScope, LoginForm.chooseAssigned(List.of(scope, secondScope), choices -> choices[1]));
        assertNull(LoginForm.chooseAssigned(List.of(scope, secondScope), choices -> null));
        assertNull(LoginForm.chooseAssigned(List.of(scope, secondScope), choices -> new WorkScopeChoice(11, "Synthetic facility", 22, "Other department")));
        assertFalse(session.isLoggedOn());
    }

    @Test void failedCompletionNeverLoadsGrantsOrCreatesLifecycle() throws Exception {
        var attempt = LoginForm.prepareSession(command, ignored -> Result.failure("AUDIT_UNAVAILABLE", "Synthetic denial"),
                ignored -> { fail("No grants before completion"); return null; },
                () -> { fail("No lifecycle after failure"); return null; });
        SwingUtilities.invokeAndWait(() -> assertFalse(attempt.activateIfCurrent(() -> true)));
        assertFalse(session.isLoggedOn());
    }

    @Test void grantFailureAfterCompletionCannotActivate() throws Exception {
        var attempt = LoginForm.prepareSession(command, ignored -> Result.success(completed),
                ignored -> Result.failure("NAVIGATION_PERMISSIONS_UNAVAILABLE", "Synthetic denial"),
                () -> { fail("No lifecycle after grant failure"); return null; });
        SwingUtilities.invokeAndWait(() -> assertFalse(attempt.activateIfCurrent(() -> true)));
        assertFalse(session.isLoggedOn());
    }

    @Test void preparationIsOffEdtAndPublicationWaitsForCurrentEdtAttempt() throws Exception {
        List<String> order = new ArrayList<>();
        var attempt = LoginForm.prepareSession(command, selected -> {
            assertSame(command, selected);
            assertFalse(SwingUtilities.isEventDispatchThread());
            order.add("complete");
            return Result.success(completed);
        }, context -> {
            assertSame(completed, context);
            order.add("grants");
            return Result.success(Set.of("HOME"));
        }, () -> { order.add("lifecycle"); return lifecycle(); });
        assertEquals(List.of("complete", "grants", "lifecycle"), order);
        assertFalse(session.isLoggedOn());
        SwingUtilities.invokeAndWait(() -> assertFalse(attempt.activateIfCurrent(() -> false)));
        assertFalse(session.isLoggedOn(), "A disposed/replaced login cannot publish a worker result");
        SwingUtilities.invokeAndWait(() -> assertTrue(attempt.activateIfCurrent(() -> true)));
        assertEquals(completed.correlationId().toString(), session.getSessionId());
        assertEquals(role.id(), session.getRoleId());
        assertEquals(scope.facilityId(), session.getFacilityId());
        assertEquals(scope.departmentId(), session.getDepartmentId());
    }

    @Test void grantOrPolicyExceptionLeavesNoPublishedSession() {
        assertThrows(IllegalStateException.class, () -> LoginForm.prepareSession(command,
                ignored -> Result.success(completed), ignored -> { throw new IllegalStateException("Synthetic unavailable"); }, this::lifecycle));
        assertThrows(IllegalStateException.class, () -> LoginForm.prepareSession(command,
                ignored -> Result.success(completed), ignored -> Result.success(Set.of("HOME")),
                () -> { throw new IllegalStateException("Synthetic unavailable"); }));
        assertFalse(session.isLoggedOn());
    }

    @Test void ioStageRejectsEdtBeforeInvokingAnyPort() throws Exception {
        AtomicBoolean invoked = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> assertThrows(IllegalStateException.class, () -> LoginForm.prepareSession(command,
                ignored -> { invoked.set(true); return Result.success(completed); },
                ignored -> Result.success(Set.of("HOME")), this::lifecycle)));
        assertFalse(invoked.get());
    }

    private SessionLifecycleService lifecycle() {
        return new SessionLifecycleService(session, new SessionTimeoutPolicy(Duration.ofMinutes(15), Duration.ofHours(12)),
                Clock.fixed(now, ZoneOffset.UTC), new AuditWritePort() {
            @Override public void appendInCurrentTransaction(SystemAuditEntry entry) { fail("No write on preparation"); }
            @Override public void appendIndependently(SystemAuditEntry entry) { fail("No write on preparation"); }
            @Override public void appendMedicalRecordInCurrentTransaction(MedicalRecordAuditEntry entry) { fail("No medical audit"); }
            @Override public void appendMedicalRecordIndependently(MedicalRecordAuditEntry entry) { fail("No medical audit"); }
        });
    }
}
