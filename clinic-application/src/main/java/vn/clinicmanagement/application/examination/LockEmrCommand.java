package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record LockEmrCommand(
        int visitId,
        int doctorId,
        String signature,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<LockEmrReceipt> {

    public LockEmrCommand(int visitId, int doctorId, String signature,
                          AuthorizationContext authorizationContext) {
        this(visitId, doctorId, signature, authorizationContext,
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
