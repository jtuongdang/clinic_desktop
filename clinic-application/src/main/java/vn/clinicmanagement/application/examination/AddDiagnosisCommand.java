package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record AddDiagnosisCommand(
        int visitId,
        int doctorId,
        Integer icdId,
        String chanDoanText,
        boolean laChinh,
        String loaiChanDoan,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<AddDiagnosisReceipt> {

    public AddDiagnosisCommand(int visitId, int doctorId, Integer icdId, String chanDoanText,
                               boolean laChinh, String loaiChanDoan, AuthorizationContext authorizationContext) {
        this(visitId, doctorId, icdId, chanDoanText, laChinh, loaiChanDoan, authorizationContext,
                createAuditActor(authorizationContext));
    }

    public AddDiagnosisCommand(int visitId, int doctorId, String chanDoanText, boolean laChinh,
                               AuthorizationContext authorizationContext) {
        this(visitId, doctorId, null, chanDoanText, laChinh, null, authorizationContext);
    }

    private static AuditActorContext createAuditActor(AuthorizationContext context) {
        return context != null && context.isValid()
                ? new AuditActorContext(
                        context.employeeId(),
                        context.roleId(),
                        context.roleCode(),
                        context.facilityId(),
                        context.departmentId(),
                        null,
                        UUID.randomUUID())
                : null;
    }
}
