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
import vn.clinicmanagement.domain.common.exceptions.EmrLockedException;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class LockEmrHandler implements CommandHandler<LockEmrCommand, LockEmrReceipt> {
    private final ILuotKhamRepository visitRepository;
    private final TransactionBoundary transactions;
    private final AuditWritePort audit;

    public LockEmrHandler(ILuotKhamRepository visitRepository,
                          TransactionBoundary transactions,
                          AuditWritePort audit) {
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public Result<LockEmrReceipt> handle(LockEmrCommand command) {
        if (command == null || command.visitId() <= 0 || command.doctorId() <= 0
                || command.signature() == null || command.signature().trim().isEmpty()) {
            return Result.failure("VALIDATION_ERROR", "Thông tin khóa bệnh án không hợp lệ.");
        }

        AuthorizationContext context = command.authorizationContext();
        if (context == null || !context.isValid()
                || !("DOCTOR".equals(context.roleCode()) || "ADMIN".equals(context.roleCode()))) {
            return deny(context, command.auditActorContext());
        }

        return transactions.inTransaction(() -> {
            Optional<LuotKham> visitResult = visitRepository.getById(command.visitId());
            if (visitResult.isEmpty()) {
                return Result.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
            }

            LuotKham visit = visitResult.get();
            if (visit.getTrangThai() != TrangThaiLuotKham.HOAN_THANH) {
                return Result.failure("INVALID_STATE", "Chỉ có thể khóa bệnh án khi lượt khám đã hoàn thành.");
            }

            try {
                visit.lockEmr(command.signature(), command.doctorId());
            } catch (EmrLockedException ex) {
                return Result.failure("EMR_ALREADY_LOCKED", "Hồ sơ bệnh án đã bị khóa trước đó.");
            } catch (IllegalStateException ex) {
                return Result.failure("INVALID_STATE", ex.getMessage());
            } catch (IllegalArgumentException ex) {
                return Result.failure("VALIDATION_ERROR", ex.getMessage());
            }

            visitRepository.update(visit);
            appendSuccessAudit(command.auditActorContext(), context, command.visitId());
            return Result.success(new LockEmrReceipt(
                    visit.getId(), visit.isEmrLocked(), visit.getEmrLockedAt(),
                    visit.getEmrLockedBy(), visit.getEmrSignature()));
        });
    }

    private Result<LockEmrReceipt> deny(AuthorizationContext context, AuditActorContext actor) {
        boolean validContext = context != null && context.isValid();
        audit.appendIndependently(new SystemAuditEntry(
                validContext ? context.employeeId() : null,
                validContext ? context.roleId() : null,
                validContext ? context.roleCode() : null,
                validContext ? context.facilityId() : null,
                validContext ? context.departmentId() : null,
                AuditActionCode.AUTH_ACCESS_DENIED,
                null,
                null,
                AuditOutcome.DENIED,
                AuditReasonCode.AUTH_FORBIDDEN,
                Instant.now(),
                actor != null ? actor.workstationId() : null,
                actor != null ? actor.correlationId() : UUID.randomUUID()));
        return Result.failure("AUTHORIZATION_DENIED", "Không được phép khóa bệnh án trong phiên hiện tại.");
    }

    private void appendSuccessAudit(AuditActorContext actor, AuthorizationContext context, int visitId) {
        audit.appendInCurrentTransaction(new SystemAuditEntry(
                context.employeeId(),
                context.roleId(),
                context.roleCode(),
                context.facilityId(),
                context.departmentId(),
                AuditActionCode.EMR_LOCKED,
                AuditResourceType.VISIT,
                visitId,
                AuditOutcome.SUCCESS,
                null,
                Instant.now(),
                actor != null ? actor.workstationId() : null,
                actor != null ? actor.correlationId() : UUID.randomUUID()));
    }
}
