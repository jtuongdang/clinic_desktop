package vn.clinicmanagement.application.hr;

@FunctionalInterface
public interface HrAuditLog {
    void append(HrAuditAction action, int actorId, int employeeId);
}
