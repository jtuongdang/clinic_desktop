package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.authorization.AuthorizationDecision;
import vn.clinicmanagement.application.authorization.AuthorizationService;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.repositories.IBaoHiemRepository;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;

import java.util.List;

/** Authorized, scoped patient search with commit-before-response audit semantics. */
public final class SearchPatientsHandler implements QueryHandler<SearchPatientsQuery, List<PatientSearchItem>> {
    private static final String FAILURE_MESSAGE = "Không thể tìm hồ sơ lúc này. Hãy kiểm tra kết nối cơ sở dữ liệu.";
    private final IBenhNhanRepository patients;
    private final IBaoHiemRepository insuranceCards;
    private final AuthorizationService authorization;
    private final AuditWritePort audit;

    public SearchPatientsHandler(IBenhNhanRepository patients, IBaoHiemRepository insuranceCards,
                                 AuthorizationService authorization, AuditWritePort audit) {
        this.patients = patients;
        this.insuranceCards = insuranceCards;
        this.authorization = authorization;
        this.audit = audit;
    }

    /** Legacy wiring fails closed; protected results require an authorization service and audit port. */
    @Deprecated
    public SearchPatientsHandler(IBenhNhanRepository patients, IBaoHiemRepository insuranceCards) {
        this(patients, insuranceCards, null, null);
    }

    @Override
    public Result<List<PatientSearchItem>> handle(SearchPatientsQuery query) {
        if (query == null || query.keyword() == null || query.keyword().isBlank()) {
            return Result.failure("SEARCH_KEYWORD_REQUIRED",
                    "Nhập mã bệnh nhân, CCCD, số điện thoại hoặc họ tên để tìm.");
        }
        AuthorizationDecision decision = ReceptionReadAudit.evaluate(query.authorizationContext(),
                query.auditActorContext(), authorization, "PATIENT");
        if (!decision.allowed()) {
            ReceptionReadAudit.denied(audit, query.auditActorContext(), decision);
            return Result.failure("AUTHORIZATION_DENIED", "Không được phép xem hồ sơ trong phiên hiện tại.");
        }
        try {
            String keyword = query.keyword() == null ? "" : query.keyword().trim();
            List<PatientSearchItem> matches = patients.searchInScope(keyword,
                            query.authorizationContext().facilityId(), query.authorizationContext().departmentId(), 50)
                    .stream().limit(50)
                    .map(patient -> new PatientSearchItem(patient,
                            insuranceCards.getLatestByPatientId(patient.getId()).orElse(null)))
                    .toList();
            ReceptionReadAudit.success(audit, query.auditActorContext(), AuditActionCode.PATIENT_SEARCH);
            return Result.success(matches);
        } catch (RuntimeException ex) {
            return Result.failure("PATIENT_SEARCH_FAILED", FAILURE_MESSAGE);
        }
    }
}
