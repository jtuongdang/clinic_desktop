package vn.clinicmanagement.application.auth;

import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

/** Command to finalize login after the user selects one assigned role and one assigned work scope. */
public record CompleteLoginSessionCommand(
        AuthenticatedUser authenticatedUser,
        AuthenticatedUser.RoleChoice selectedRole,
        AuthenticatedUser.WorkScopeChoice selectedWorkScope,
        String workstationId,
        UUID correlationId) implements Command<CompletedLoginSession> {

    public CompleteLoginSessionCommand(
            AuthenticatedUser authenticatedUser,
            AuthenticatedUser.RoleChoice selectedRole,
            AuthenticatedUser.WorkScopeChoice selectedWorkScope) {
        this(authenticatedUser, selectedRole, selectedWorkScope, null, null);
    }
}
