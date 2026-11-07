package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record StartExamCommand(
        int visitId,
        int doctorId,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<StartExamReceipt> {

    public StartExamCommand(int visitId, int doctorId, AuthorizationContext authorizationContext) {
        this(visitId, doctorId, authorizationContext,
                authorizationContext != null && authorizationContext.isValid()
                        ? new AuditActorContext(
                                authorizationContext.employeeId(),
                                authorizationContext.roleId(),
                                authorizationContext.roleCode(),
                                authorizationContext.facilityId(),
                                authorizationContext.departmentId(),
                                null,
                                UUID.randomUUID())
                        : null);
    }
}
