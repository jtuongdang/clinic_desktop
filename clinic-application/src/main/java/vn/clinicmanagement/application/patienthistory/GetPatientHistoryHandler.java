package vn.clinicmanagement.application.patienthistory;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.authorization.AuthorizationDecision;
import vn.clinicmanagement.application.authorization.AuthorizationRequest;
import vn.clinicmanagement.application.authorization.AuthorizationService;
import vn.clinicmanagement.application.common.ApplicationFailureDiagnostics;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.patient.TienSuBenh;
import vn.clinicmanagement.domain.repositories.ITienSuBenhRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Exact-scope doctor history read with independent commit-before-response audit. */
public final class GetPatientHistoryHandler
        implements QueryHandler<GetPatientHistoryQuery, List<TienSuBenh>> {
    private static final String FAILURE_MESSAGE = "Không thể tải tiền sử bệnh lúc này.";
    private final ITienSuBenhRepository histories;
    private final AuthorizationService authorization;
    private final AuditWritePort audit;

    public GetPatientHistoryHandler(
            ITienSuBenhRepository histories,
            AuthorizationService authorization,
            AuditWritePort audit) {
        this.histories = histories;
        this.authorization = authorization;
        this.audit = audit;
    }

    @Override
    public Result<List<TienSuBenh>> handle(GetPatientHistoryQuery query) {
        if (query == null || query.patientId() <= 0) {
            return Result.failure("PATIENT_ID_REQUIRED", "Mã người bệnh không hợp lệ.");
        }

        AuthorizationContext context = query.authorizationContext();
        AuditActorContext actor = query.auditActorContext();
        if (histories == null || authorization == null || audit == null
                || !isDoctor(context) || !sameActor(context, actor)) {
            return denied();
        }

        AuthorizationDecision decision = authorization.evaluate(new AuthorizationRequest(
                context, "PATIENT", HanhDongQuyen.XEM, context.facilityId(), context.departmentId()));
        if (decision == null || !decision.allowed()) return denied();

        List<TienSuBenh> result;
        try {
            List<TienSuBenh> found = histories.getHistoryByPatientAndScope(
                    query.patientId(), context.facilityId(), context.departmentId());
            if (found == null) throw new IllegalStateException("History repository returned no result list.");
            result = List.copyOf(found);
        } catch (RuntimeException exception) {
            logFailure("PATIENT_HISTORY_QUERY_FAILED", query, exception);
            return Result.failure("PATIENT_HISTORY_FAILED", FAILURE_MESSAGE);
        }

        try {
            audit.appendIndependently(new SystemAuditEntry(
                    actor.employeeId(), actor.roleId(), actor.roleCode(), actor.facilityId(), actor.departmentId(),
                    AuditActionCode.PATIENT_VIEW, AuditResourceType.PATIENT, query.patientId(),
                    AuditOutcome.SUCCESS, null, Instant.now(), actor.workstationId(), actor.correlationId()));
        } catch (RuntimeException exception) {
            logFailure("PATIENT_HISTORY_AUDIT_FAILED", query, exception);
            return Result.failure("PATIENT_HISTORY_AUDIT_FAILED", FAILURE_MESSAGE);
        }
        return Result.success(result);
    }

    private static boolean isDoctor(AuthorizationContext context) {
        return context != null && context.isValid() && "DOCTOR".equals(context.roleCode());
    }

    private static boolean sameActor(AuthorizationContext context, AuditActorContext actor) {
        return actor != null
                && context.employeeId() == actor.employeeId()
                && context.roleId() == actor.roleId()
                && Objects.equals(context.roleCode(), actor.roleCode())
                && context.facilityId() == actor.facilityId()
                && context.departmentId() == actor.departmentId();
    }

    private static Result<List<TienSuBenh>> denied() {
        return Result.failure("AUTHORIZATION_DENIED", "Không được phép xem tiền sử bệnh trong phiên hiện tại.");
    }

    private static void logFailure(String code, GetPatientHistoryQuery query, RuntimeException exception) {
        AuditActorContext actor = query.auditActorContext();
        String correlationId = actor == null || actor.correlationId() == null
                ? null : actor.correlationId().toString();
        ApplicationFailureDiagnostics.logUnexpectedFailure("PATIENT_HISTORY_READ", code, correlationId, exception);
    }
}
