package vn.clinicmanagement.application.payroll;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

public record LockPayrollPeriodCommand(
        int periodId,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext
) implements Command<LockPayrollPeriodReceipt> {}
