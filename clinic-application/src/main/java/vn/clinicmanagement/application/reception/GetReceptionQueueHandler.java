package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.authorization.AuthorizationDecision;
import vn.clinicmanagement.application.authorization.AuthorizationService;
import vn.clinicmanagement.application.common.ApplicationFailureDiagnostics;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.Result;

import java.time.LocalDate;

/** Authorized queue and room-summary read. */
public final class GetReceptionQueueHandler implements QueryHandler<GetReceptionQueueQuery, ReceptionQueueSnapshot> {
    private final ReceptionQueuePort receptionQueue;
    private final AuthorizationService authorization;
    private final AuditWritePort audit;

    public GetReceptionQueueHandler(ReceptionQueuePort receptionQueue, AuthorizationService authorization,
                                    AuditWritePort audit) {
        this.receptionQueue = receptionQueue;
        this.authorization = authorization;
        this.audit = audit;
    }

    /** Legacy wiring fails closed; protected results require an authorization service and audit port. */
    @Deprecated
    public GetReceptionQueueHandler(ReceptionQueuePort receptionQueue) {
        this(receptionQueue, null, null);
    }

    @Override
    public Result<ReceptionQueueSnapshot> handle(GetReceptionQueueQuery query) {
        if (query == null) return Result.failure("RECEPTION_QUEUE_FAILED", "Không thể tải hàng đợi tiếp đón.");
        AuthorizationDecision decision = ReceptionReadAudit.evaluate(query.authorizationContext(),
                query.auditActorContext(), authorization, "RECEPTION");
        if (!decision.allowed()) {
            ReceptionReadAudit.denied(audit, query.auditActorContext(), decision);
            return Result.failure("AUTHORIZATION_DENIED", "Không được phép xem hàng đợi trong phiên hiện tại.");
        }
        try {
            LocalDate date = query.date() == null ? LocalDate.now() : query.date();
            int facilityId = query.authorizationContext().facilityId();
            int departmentId = query.authorizationContext().departmentId();
            ReceptionQueueSnapshot snapshot = new ReceptionQueueSnapshot(
                    receptionQueue.getQueue(date, facilityId, departmentId),
                    receptionQueue.getRoomSummaries(date, facilityId, departmentId));
            ReceptionReadAudit.success(audit, query.auditActorContext(), AuditActionCode.PATIENT_VIEW);
            return Result.success(snapshot);
        } catch (RuntimeException ex) {
            ApplicationFailureDiagnostics.logUnexpectedFailure("RECEPTION_GET_QUEUE", "RECEPTION_QUEUE_FAILED",
                    correlationId(query), ex);
            return Result.failure("RECEPTION_QUEUE_FAILED", "Không thể tải hàng đợi tiếp đón.");
        }
    }

    private static String correlationId(GetReceptionQueueQuery query) {
        return query.auditActorContext() == null || query.auditActorContext().correlationId() == null
                ? null : query.auditActorContext().correlationId().toString();
    }
}
