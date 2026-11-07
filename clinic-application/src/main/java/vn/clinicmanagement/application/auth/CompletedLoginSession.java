package vn.clinicmanagement.application.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable finalized login context produced only after role, scope, last-login, and audit succeed. */
public record CompletedLoginSession(
        int userId,
        String username,
        String fullName,
        AuthenticatedUser.RoleChoice role,
        AuthenticatedUser.WorkScopeChoice workScope,
        UUID correlationId,
        Instant loginAtUtc) {

    public CompletedLoginSession {
        if (userId <= 0) throw new IllegalArgumentException("userId must be positive.");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(workScope, "workScope");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(loginAtUtc, "loginAtUtc");
    }
}
