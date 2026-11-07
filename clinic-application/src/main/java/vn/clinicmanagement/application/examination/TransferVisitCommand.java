package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.examination.TransferReasonCode;

import java.util.UUID;

public record TransferVisitCommand(
        int visitId,
        int destinationRoomId,
        TransferReasonCode reasonCode,
        String transferNote,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<TransferVisitReceipt> {

    public TransferVisitCommand(int visitId, int destinationRoomId, TransferReasonCode reasonCode,
                                String transferNote, AuthorizationContext authorizationContext) {
        this(visitId, destinationRoomId, reasonCode, transferNote, authorizationContext,
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

    public TransferVisitCommand(int visitId, int destinationRoomId, TransferReasonCode reasonCode,
                                AuthorizationContext authorizationContext) {
        this(visitId, destinationRoomId, reasonCode, null, authorizationContext);
    }

    public AuthorizationContext context() {
        return authorizationContext;
    }

    public String note() {
        return transferNote;
    }
}
