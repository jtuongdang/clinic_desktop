package vn.clinicmanagement.application.patienthistory;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.LoaiTienSu;

/** Create when historyId is null; otherwise update using the supplied optimistic version. */
public record SavePatientHistoryCommand(
        int patientId,
        Integer historyId,
        Long expectedVersion,
        LoaiTienSu category,
        String narrative,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<PatientHistoryReceipt> {
}
