package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.audit.*;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.authorization.AuthorizationDecision;
import vn.clinicmanagement.application.authorization.AuthorizationRequest;
import vn.clinicmanagement.application.authorization.AuthorizationService;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;

import java.time.Instant;
import java.util.Objects;

/** Shared default-deny, per-operation actor validation, and independent read auditing. */
final class ReceptionReadAudit {
    private ReceptionReadAudit() {}

    static AuthorizationDecision evaluate(AuthorizationContext context, AuditActorContext actor,
                                          AuthorizationService authorization, String module) {
        if (authorization == null || !sameActor(context, actor)) {
            return AuthorizationDecision.deny(AuthorizationDecision.ReasonCode.INVALID_CONTEXT);
        }
        return authorization.evaluate(new AuthorizationRequest(context, module, HanhDongQuyen.XEM,
                context.facilityId(), context.departmentId()));
    }

    static boolean sameActor(AuthorizationContext context, AuditActorContext actor) {
        return context != null && context.isValid() && actor != null
                && context.employeeId() == actor.employeeId() && context.roleId() == actor.roleId()
                && Objects.equals(context.roleCode(), actor.roleCode())
                && context.facilityId() == actor.facilityId()
                && context.departmentId() == actor.departmentId();
    }

    static void success(AuditWritePort audit, AuditActorContext actor, AuditActionCode action) {
        audit.appendIndependently(entry(actor, action, AuditOutcome.SUCCESS, null));
    }

    static void denied(AuditWritePort audit, AuditActorContext actor, AuthorizationDecision decision) {
        if (audit == null || actor == null) return;
        AuditReasonCode reason = decision != null && (decision.reasonCode() == AuthorizationDecision.ReasonCode.SCOPE_INVALID
                || decision.reasonCode() == AuthorizationDecision.ReasonCode.TARGET_MISMATCH)
                ? AuditReasonCode.SCOPE_MISMATCH : AuditReasonCode.AUTH_FORBIDDEN;
        try {
            audit.appendIndependently(entry(actor, AuditActionCode.AUTH_ACCESS_DENIED, AuditOutcome.DENIED, reason));
        } catch (RuntimeException ignored) {
            // Denial remains a safe failure if the independent audit store is unavailable.
        }
    }

    private static SystemAuditEntry entry(AuditActorContext actor, AuditActionCode action,
                                          AuditOutcome outcome, AuditReasonCode reason) {
        return new SystemAuditEntry(actor.employeeId(), actor.roleId(), actor.roleCode(), actor.facilityId(),
                actor.departmentId(), action, null, null, outcome, reason, Instant.now(),
                actor.workstationId(), actor.correlationId());
    }
}
