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
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ReopenEmrHandler implements CommandHandler<ReopenEmrCommand, ReopenEmrReceipt> {
    private final ILuotKhamRepository visitRepository;
    private final TransactionBoundary transactions;
    private final AuditWritePort audit;

    public ReopenEmrHandler(ILuotKhamRepository visitRepository,
                            TransactionBoundary transactions,
                            AuditWritePort audit) {
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public Result<ReopenEmrReceipt> handle(ReopenEmrCommand command) {
        if (command == null || command.visitId() <= 0 || command.approverId() <= 0
                || command.lyDoMoKhoa() == null || command.lyDoMoKhoa().trim().length() < 5) {
            return Result.failure("VALIDATION_ERROR", "Thông tin mở khóa bệnh án không hợp lệ.");
        }

        AuthorizationContext context = command.authorizationContext();
        if (context == null || !context.isValid() || !isAuthorizedApprover(context.roleCode())) {
            return deny(context, command.auditActorContext());
        }

        return transactions.inTransaction(() -> {
            Optional<LuotKham> visitResult = visitRepository.getById(command.visitId());
            if (visitResult.isEmpty()) {
                return Result.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
            }

            LuotKham visit = visitResult.get();
            if (!visit.isEmrLocked()) {
                return Result.failure("EMR_NOT_LOCKED", "Hồ sơ bệnh án chưa bị khóa.");
            }

            if (visit.getTrangThai() != TrangThaiLuotKham.HOAN_THANH) {
                return Result.failure("INVALID_STATE", "Chỉ có thể mở khóa bệnh án đã hoàn thành khám.");
            }

            try {
                visit.reopenEmr(command.approverId(), command.lyDoMoKhoa().trim());
            } catch (IllegalStateException ex) {
                return Result.failure("INVALID_STATE", ex.getMessage());
            } catch (IllegalArgumentException ex) {
                return Result.failure("VALIDATION_ERROR", ex.getMessage());
            }

            visitRepository.update(visit);
            appendSuccessAudit(command.auditActorContext(), context, command.visitId());
            return Result.success(new ReopenEmrReceipt(
                    visit.getId(), visit.isEmrLocked(), visit.getTrangThai(), command.approverId(), LocalDateTime.now()));
        });
    }

    private static boolean isAuthorizedApprover(String roleCode) {
        return "ADMIN".equals(roleCode)
                || "CHIEF_DOCTOR".equals(roleCode)
                || "TRUONG_KHOA".equals(roleCode);
    }

    private Result<ReopenEmrReceipt> deny(AuthorizationContext context, AuditActorContext actor) {
        boolean validContext = context != null && context.isValid();
        Integer actorId = null;
        Integer roleId = null;
        String roleCode = null;
        Integer facilityId = null;
        Integer departmentId = null;
        if (validContext) {
            actorId = context.employeeId();
            roleId = context.roleId();
            roleCode = context.roleCode();
            facilityId = context.facilityId();
            departmentId = context.departmentId();
        }

        audit.appendIndependently(new SystemAuditEntry(
                actorId,
                roleId,
                roleCode,
                facilityId,
                departmentId,
                AuditActionCode.AUTH_ACCESS_DENIED,
                null,
                null,
                AuditOutcome.DENIED,
                AuditReasonCode.AUTH_FORBIDDEN,
                Instant.now(),
                actor != null ? actor.workstationId() : null,
                actor != null ? actor.correlationId() : UUID.randomUUID()));
        return Result.failure("AUTHORIZATION_DENIED", "Không được phép mở khóa bệnh án trong phiên hiện tại.");
    }

    private void appendSuccessAudit(AuditActorContext actor, AuthorizationContext context, int visitId) {
        audit.appendInCurrentTransaction(new SystemAuditEntry(
                context.employeeId(),
                context.roleId(),
                context.roleCode(),
                context.facilityId(),
                context.departmentId(),
                AuditActionCode.EMR_REOPENED,
                AuditResourceType.VISIT,
                visitId,
                AuditOutcome.SUCCESS,
                null,
                Instant.now(),
                actor != null ? actor.workstationId() : null,
                actor != null ? actor.correlationId() : UUID.randomUUID()));
    }
}
