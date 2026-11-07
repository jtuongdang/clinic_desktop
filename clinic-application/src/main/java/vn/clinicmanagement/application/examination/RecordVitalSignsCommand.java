package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.math.BigDecimal;
import java.util.UUID;

public record RecordVitalSignsCommand(
        int visitId,
        Integer mach,
        BigDecimal nhietDo,
        Integer huyetApTamThu,
        Integer huyetApTamTruong,
        Integer nhipTho,
        BigDecimal canNang,
        BigDecimal chieuCao,
        BigDecimal spo2,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<RecordVitalSignsReceipt> {

    public RecordVitalSignsCommand(
            int visitId,
            Integer mach,
            BigDecimal nhietDo,
            Integer huyetApTamThu,
            Integer huyetApTamTruong,
            Integer nhipTho,
            BigDecimal canNang,
            BigDecimal chieuCao,
            BigDecimal spo2,
            AuthorizationContext authorizationContext) {
        this(visitId, mach, nhietDo, huyetApTamThu, huyetApTamTruong, nhipTho, canNang, chieuCao, spo2,
                authorizationContext, auditActor(authorizationContext));
    }

    public RecordVitalSignsCommand(int visitId, AuthorizationContext authorizationContext) {
        this(visitId, null, null, null, null, null, null, null, null, authorizationContext);
    }

    private static AuditActorContext auditActor(AuthorizationContext context) {
        return context != null && context.isValid()
                ? new AuditActorContext(context.employeeId(), context.roleId(), context.roleCode(),
                        context.facilityId(), context.departmentId(), null, UUID.randomUUID())
                : null;
    }
}
