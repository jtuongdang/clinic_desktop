package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.application.audit.AuditActionCode;
import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.audit.AuditOutcome;
import vn.clinicmanagement.application.audit.AuditReasonCode;
import vn.clinicmanagement.application.audit.AuditResourceType;
import vn.clinicmanagement.application.audit.AuditWritePort;
import vn.clinicmanagement.application.audit.SystemAuditEntry;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.time.Instant;

public final class CancelVisitHandler implements CommandHandler<CancelVisitCommand, CancelVisitReceipt> {
    private final ILuotKhamRepository visits;
    private final TransactionBoundary transactions;
    private final AuditWritePort audit;

    public CancelVisitHandler(ILuotKhamRepository visits, TransactionBoundary transactions, AuditWritePort audit) {
        this.visits = visits;
        this.transactions = transactions;
        this.audit = audit;
    }

    @Override
    public Result<CancelVisitReceipt> handle(CancelVisitCommand command) {
        if (command == null) {
            return Result.failure("VALIDATION_ERROR", "Thông tin hủy lượt khám không hợp lệ.");
        }

        AuthorizationContext context = command.authorizationContext();
        if (context == null || !"RECEPTIONIST".equals(context.roleCode())) {
            return deny(context, command.auditActorContext());
        }
        if (command.cancellationReason() == null || command.cancellationReason().isBlank()) {
            return Result.failure("VALIDATION_ERROR", "Lý do hủy không được để trống.");
        }
        if (command.visitId() <= 0) {
            return Result.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
        }

        return transactions.inTransaction(() -> {
            var visitResult = visits.getById(command.visitId());
            if (visitResult.isEmpty()) {
                return Result.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
            }

            LuotKham visit = visitResult.get();
            if (visit.getTrangThai() != TrangThaiLuotKham.CHO_KHAM) {
                return Result.failure("INVALID_STATE", "Chỉ có thể hủy lượt khám ở trạng thái chờ khám.");
            }

            visit.setTrangThai(TrangThaiLuotKham.DA_HUY);
            visits.update(visit);
            appendSuccessAudit(command.auditActorContext(), context, command.visitId());
            return Result.success(new CancelVisitReceipt(command.visitId()));
        });
    }

    private Result<CancelVisitReceipt> deny(AuthorizationContext context, AuditActorContext actor) {
        if (context != null && actor != null) {
            audit.appendIndependently(new SystemAuditEntry(
                    context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                    context.departmentId(), AuditActionCode.AUTH_ACCESS_DENIED, null, null,
                    AuditOutcome.DENIED, AuditReasonCode.AUTH_FORBIDDEN, Instant.now(),
                    actor.workstationId(), actor.correlationId()));
        }
        return Result.failure("AUTHORIZATION_DENIED", "Không được phép hủy lượt khám trong phiên hiện tại.");
    }

    private void appendSuccessAudit(AuditActorContext actor, AuthorizationContext context, int visitId) {
        audit.appendInCurrentTransaction(new SystemAuditEntry(
                context.employeeId(), context.roleId(), context.roleCode(), context.facilityId(),
                context.departmentId(), AuditActionCode.VISIT_TRANSFERRED, AuditResourceType.VISIT, visitId,
                AuditOutcome.SUCCESS, null, Instant.now(), actor.workstationId(), actor.correlationId()));
    }
}
