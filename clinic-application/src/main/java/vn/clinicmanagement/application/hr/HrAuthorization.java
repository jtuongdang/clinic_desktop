package vn.clinicmanagement.application.hr;

/** Resolves administrative permission from the authenticated application context. */
@FunctionalInterface
public interface HrAuthorization {
    boolean canManageEmployees(int actorId);
}
