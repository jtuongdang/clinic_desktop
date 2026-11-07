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
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class StartExamHandler implements CommandHandler<StartExamCommand, StartExamReceipt> {
    private final ILuotKhamRepository visitRepository;
    private final TransactionBoundary transactions;
    private final AuditWritePort audit;

    public StartExamHandler(ILuotKhamRepository visitRepository,
                            TransactionBoundary transactions,
                            AuditWritePort audit) {
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public Result<StartExamReceipt> handle(StartExamCommand command) {
        if (command == null || command.visitId() <= 0 || command.doctorId() <= 0) {
            return Result.failure("VALIDATION_ERROR", "Thông tin bắt đầu khám không hợp lệ.");
        }

        AuthorizationContext context = command.authorizationContext();
        if (context == null || !context.isValid() || !"DOCTOR".equals(context.roleCode())) {
            return deny(context, command.auditActorContext());
        }

        return transactions.inTransaction(() -> {
            Optional<LuotKham> visitResult = visitRepository.getById(command.visitId());
            if (visitResult.isEmpty()) {
                return Result.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
            }

            LuotKham visit = visitResult.get();
            Integer previousDoctorId = visit.getMaBacSi();
            visit.setMaBacSi(command.doctorId());
            try {
                visit.startExam();
            } catch (IllegalStateException ex) {
                visit.setMaBacSi(previousDoctorId);
                return Result.failure("INVALID_STATE", ex.getMessage());
            }

            visitRepository.update(visit);
            appendSuccessAudit(command.auditActorContext(), context, command.visitId());
            return Result.success(new StartExamReceipt(
                    visit.getId(), visit.getTrangThai(), visit.getThoiGianBatDauKham()));
        });
    }

    private Result<StartExamReceipt> deny(AuthorizationContext context, AuditActorContext actor) {
        if (context != null && context.isValid()) {
            audit.appendIndependently(new SystemAuditEntry(
                    context.employeeId(),
                    context.roleId(),
                    context.roleCode(),
                    context.facilityId(),
                    context.departmentId(),
                    AuditActionCode.AUTH_ACCESS_DENIED,
                    null,
                    null,
                    AuditOutcome.DENIED,
                    AuditReasonCode.AUTH_FORBIDDEN,
                    Instant.now(),
                    actor != null ? actor.workstationId() : null,
                    actor != null ? actor.correlationId() : UUID.randomUUID()));
        }
        return Result.failure("AUTHORIZATION_DENIED", "Không được phép bắt đầu khám trong phiên hiện tại.");
    }

    private void appendSuccessAudit(AuditActorContext actor, AuthorizationContext context, int visitId) {
        audit.appendInCurrentTransaction(new SystemAuditEntry(
                context.employeeId(),
                context.roleId(),
                context.roleCode(),
                context.facilityId(),
                context.departmentId(),
                AuditActionCode.EMR_STARTED,
                AuditResourceType.VISIT,
                visitId,
                AuditOutcome.SUCCESS,
                null,
                Instant.now(),
                actor != null ? actor.workstationId() : null,
                actor != null ? actor.correlationId() : UUID.randomUUID()));
    }
}
