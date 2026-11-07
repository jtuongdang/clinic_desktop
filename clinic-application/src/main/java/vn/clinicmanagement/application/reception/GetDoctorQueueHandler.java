package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.authorization.AuthorizationDecision;
import vn.clinicmanagement.application.authorization.AuthorizationService;
import vn.clinicmanagement.application.common.ApplicationFailureDiagnostics;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.Result;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** Authorized, audited read of the active doctor team queue. */
public final class GetDoctorQueueHandler implements QueryHandler<GetDoctorQueueQuery, List<ReceptionQueueItem>> {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DoctorQueuePort doctorQueue;
    private final AuthorizationService authorization;
    private final AuditWritePort audit;

    public GetDoctorQueueHandler(DoctorQueuePort doctorQueue, AuthorizationService authorization,
                                 AuditWritePort audit) {
        this.doctorQueue = doctorQueue;
        this.authorization = authorization;
        this.audit = audit;
    }

    @Override
    public Result<List<ReceptionQueueItem>> handle(GetDoctorQueueQuery query) {
        if (query == null) return Result.failure("RECEPTION_QUEUE_FAILED", "Không thể tải hàng đợi bác sĩ.");
        AuthorizationDecision decision = ReceptionReadAudit.evaluate(query.authorizationContext(),
                query.auditActorContext(), authorization, "RECEPTION");
        if (!decision.allowed()) {
            ReceptionReadAudit.denied(audit, query.auditActorContext(), decision);
            return Result.failure("AUTHORIZATION_DENIED", "Không được phép xem hàng đợi trong phiên hiện tại.");
        }
        try {
            LocalDate date = query.date() == null ? LocalDate.now(BUSINESS_ZONE) : query.date();
            int facilityId = query.authorizationContext().facilityId();
            int departmentId = query.authorizationContext().departmentId();
            List<ReceptionQueueItem> items = doctorQueue.getActiveDoctorQueue(date, facilityId, departmentId);
            ReceptionReadAudit.success(audit, query.auditActorContext(), AuditActionCode.PATIENT_VIEW);
            return Result.success(items);
        } catch (RuntimeException ex) {
            ApplicationFailureDiagnostics.logUnexpectedFailure("DOCTOR_GET_QUEUE", "RECEPTION_QUEUE_FAILED",
                    correlationId(query), ex);
            return Result.failure("RECEPTION_QUEUE_FAILED", "Không thể tải hàng đợi bác sĩ.");
        }
    }

    private static String correlationId(GetDoctorQueueQuery query) {
        return query.auditActorContext() == null || query.auditActorContext().correlationId() == null
                ? null : query.auditActorContext().correlationId().toString();
    }
}
