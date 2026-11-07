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
import vn.clinicmanagement.domain.examination.ChanDoan;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class AddDiagnosisHandler implements CommandHandler<AddDiagnosisCommand, AddDiagnosisReceipt> {
    private static final Set<String> DIAGNOSIS_TYPES = Set.of("SO_BO", "XAC_DINH", "KEM_THEO", "BIEN_CHUNG");

    private final ILuotKhamRepository visitRepository;
    private final TransactionBoundary transactions;
    private final AuditWritePort audit;

    public AddDiagnosisHandler(ILuotKhamRepository visitRepository,
                               TransactionBoundary transactions,
                               AuditWritePort audit) {
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public Result<AddDiagnosisReceipt> handle(AddDiagnosisCommand command) {
        if (command == null || command.visitId() <= 0 || command.doctorId() <= 0
                || command.chanDoanText() == null || command.chanDoanText().isBlank()) {
            return Result.failure("VALIDATION_ERROR", "Thông tin chẩn đoán không hợp lệ.");
        }

        String diagnosisType = command.loaiChanDoan();
        if (diagnosisType == null || diagnosisType.isBlank()) {
            diagnosisType = "XAC_DINH";
        }
        if (!DIAGNOSIS_TYPES.contains(diagnosisType)) {
            return Result.failure("VALIDATION_ERROR", "Loại chẩn đoán không hợp lệ.");
        }

        AuthorizationContext context = command.authorizationContext();
        if (context == null || !context.isValid() || !"DOCTOR".equals(context.roleCode())) {
            return deny(context, command.auditActorContext());
        }

        String finalDiagnosisType = diagnosisType;
        return transactions.inTransaction(() -> {
            Optional<LuotKham> visitResult = visitRepository.getById(command.visitId());
            if (visitResult.isEmpty()) {
                return Result.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
            }

            LuotKham visit = visitResult.get();
            if (visit.getMaBacSi() != null && visit.getMaBacSi() != command.doctorId()) {
                return Result.failure("DOCTOR_MISMATCH", "Chỉ bác sĩ phụ trách mới có quyền thêm chẩn đoán.");
            }
            if (visit.getTrangThai() != TrangThaiLuotKham.DANG_KHAM) {
                return Result.failure("INVALID_STATE", "Chỉ có thể thêm chẩn đoán khi lượt khám đang diễn ra.");
            }
            if (visit.isEmrLocked()) {
                return Result.failure("EMR_LOCKED", "Hồ sơ bệnh án đã bị khóa không thể thêm chẩn đoán.");
            }

            if (command.laChinh() && visit.getChanDoans() != null) {
                for (ChanDoan diagnosis : visit.getChanDoans()) {
                    if (diagnosis.isLaChinh()) {
                        diagnosis.setLaChinh(false);
                    }
                }
            }

            ChanDoan diagnosis = new ChanDoan();
            diagnosis.setMaLuotKham(visit.getId());
            diagnosis.setMaICD(command.icdId());
            diagnosis.setChanDoanText(command.chanDoanText().trim());
            diagnosis.setLaChinh(command.laChinh());
            diagnosis.setLoaiChanDoan(finalDiagnosisType);
            diagnosis.setLuotKham(visit);
            if (visit.getChanDoans() == null) {
                visit.setChanDoans(new ArrayList<>());
            }
            visit.getChanDoans().add(diagnosis);

            visitRepository.update(visit);
            appendSuccessAudit(command.auditActorContext(), context, visit.getId());
            return Result.success(new AddDiagnosisReceipt(
                    visit.getId(), diagnosis.getChanDoanText(), diagnosis.isLaChinh(),
                    diagnosis.getLoaiChanDoan(), LocalDateTime.now()));
        });
    }

    private Result<AddDiagnosisReceipt> deny(AuthorizationContext context, AuditActorContext actor) {
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
        return Result.failure("AUTHORIZATION_DENIED", "Không được phép thêm chẩn đoán trong phiên hiện tại.");
    }

    private void appendSuccessAudit(AuditActorContext actor, AuthorizationContext context, int visitId) {
        audit.appendInCurrentTransaction(new SystemAuditEntry(
                context.employeeId(),
                context.roleId(),
                context.roleCode(),
                context.facilityId(),
                context.departmentId(),
                AuditActionCode.EMR_UPDATED,
                AuditResourceType.VISIT,
                visitId,
                AuditOutcome.SUCCESS,
                null,
                Instant.now(),
                actor != null ? actor.workstationId() : null,
                actor != null ? actor.correlationId() : UUID.randomUUID()));
    }
}
