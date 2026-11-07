package vn.clinicmanagement.application.inventory;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

public record ApproveStocktakeCommand(
        int stocktakeId,
        int approverId,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext) implements Command<ApproveStocktakeReceipt> {

    public ApproveStocktakeCommand(
            int stocktakeId, int approverId, AuthorizationContext authorizationContext) {
        this(stocktakeId, approverId, authorizationContext, createAuditActor(authorizationContext));
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
