package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.authorization.AuthorizationDecision;
import vn.clinicmanagement.application.authorization.AuthorizationService;
import vn.clinicmanagement.application.common.QueryHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.repositories.IPhongKhamRepository;

import java.util.List;

/** Authorized active-room picker read. */
public final class GetActiveReceptionRoomsHandler
        implements QueryHandler<GetActiveReceptionRoomsQuery, List<PhongKham>> {
    private final IPhongKhamRepository rooms;
    private final AuthorizationService authorization;
    private final AuditWritePort audit;

    public GetActiveReceptionRoomsHandler(IPhongKhamRepository rooms, AuthorizationService authorization,
                                          AuditWritePort audit) {
        this.rooms = rooms;
        this.authorization = authorization;
        this.audit = audit;
    }

    @Override
    public Result<List<PhongKham>> handle(GetActiveReceptionRoomsQuery query) {
        if (query == null) return Result.failure("ROOM_CATALOG_FAILED", "Không thể tải danh sách phòng khám.");
        AuthorizationDecision decision = ReceptionReadAudit.evaluate(query.authorizationContext(),
                query.auditActorContext(), authorization, "CATALOG");
        if (!decision.allowed()) {
            ReceptionReadAudit.denied(audit, query.auditActorContext(), decision);
            return Result.failure("AUTHORIZATION_DENIED", "Không được phép xem danh mục phòng trong phiên hiện tại.");
        }
        try {
            List<PhongKham> result = rooms.getActiveRoomsInScope(query.authorizationContext().facilityId(),
                    query.authorizationContext().departmentId());
            ReceptionReadAudit.success(audit, query.auditActorContext(), AuditActionCode.CATALOG_VIEW);
            return Result.success(result);
        } catch (RuntimeException ex) {
            return Result.failure("ROOM_CATALOG_FAILED", "Không thể tải danh sách phòng khám.");
        }
    }
}
