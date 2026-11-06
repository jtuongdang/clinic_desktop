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
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.application.common.TransactionBoundary;
import vn.clinicmanagement.domain.common.enums.HanhDongQuyen;
import vn.clinicmanagement.domain.common.enums.LoaiTienSu;
import vn.clinicmanagement.domain.patient.BenhNhan;
import vn.clinicmanagement.domain.patient.TienSuBenh;
import vn.clinicmanagement.domain.repositories.IBenhNhanRepository;
import vn.clinicmanagement.domain.repositories.ITienSuBenhRepository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Audited, exact-session-scope create/update workflow for approved patient history categories. */
public final class SavePatientHistoryHandler implements CommandHandler<SavePatientHistoryCommand, PatientHistoryReceipt> {
    private static final String FAILURE_MESSAGE = "Không thể lưu tiền sử bệnh lúc này.";
    private final ITienSuBenhRepository histories;
    private final IBenhNhanRepository patients;
    private final AuthorizationService authorization;
    private final TransactionBoundary transactions;
    private final AuditWritePort audit;

    public SavePatientHistoryHandler(ITienSuBenhRepository histories, IBenhNhanRepository patients,
                                     AuthorizationService authorization, TransactionBoundary transactions,
                                     AuditWritePort audit) {
        this.histories = histories;
        this.patients = patients;
        this.authorization = authorization;
        this.transactions = transactions;
        this.audit = audit;
    }

    @Override
    public Result<PatientHistoryReceipt> handle(SavePatientHistoryCommand command) {
        Result<PatientHistoryReceipt> invalid = validatePayload(command);
        if (invalid != null) return invalid;
        AuthorizationContext context = command.authorizationContext();
        AuditActorContext actor = command.auditActorContext();
        if (histories == null || patients == null || authorization == null || transactions == null || audit == null
                || !isDoctor(context) || !sameActor(context, actor)) return denied();
        try {
            AuthorizationDecision decision = authorization.evaluate(new AuthorizationRequest(
                    context, "PATIENT", HanhDongQuyen.XEM, context.facilityId(), context.departmentId()));
            if (decision == null || !decision.allowed()) return denied();
        } catch (RuntimeException ignored) {
            return denied();
        }

        return transactions.inTransaction(() -> {
            try {
                Optional<BenhNhan> patient = patients.getByIdInScope(
                        command.patientId(), context.facilityId(), context.departmentId());
                if (patient == null || patient.isEmpty() || patient.get().isDeleted()
                        || patient.get().getId() == null || patient.get().getId() != command.patientId()) return denied();
                TienSuBenh history;
                boolean create = command.historyId() == null;
                if (create) {
                    history = new TienSuBenh();
                    history.setMaBenhNhan(command.patientId());
                    history.setCreatedBy(context.employeeId());
                    history.setOwnershipScope(context.facilityId(), context.departmentId());
                } else {
                    Optional<TienSuBenh> existing = histories.getActiveByIdPatientAndScope(
                            command.historyId(), command.patientId(), context.facilityId(), context.departmentId());
                    if (existing == null || existing.isEmpty()) return denied();
                    history = existing.get();
                    if (history.getId() == null || history.getId() != command.historyId()
                            || history.getMaBenhNhan() != command.patientId() || history.isDeleted()
                            || !history.hasExactOwnershipScope(context.facilityId(), context.departmentId())
                            || history.getVersion() == null || !history.getVersion().equals(command.expectedVersion())) {
                        return denied();
                    }
                    history.setUpdatedBy(context.employeeId());
                }
                history.setLoaiTienSu(command.category());
                history.setMoTa(command.narrative().trim());
                if (create) histories.add(history);
                else if (!histories.update(history, command.expectedVersion())) {
                    return Result.failure("PATIENT_HISTORY_CONFLICT", "Tiền sử đã thay đổi hoặc không còn khả dụng.");
                }
                audit.appendInCurrentTransaction(new SystemAuditEntry(
                        actor.employeeId(), actor.roleId(), actor.roleCode(), actor.facilityId(), actor.departmentId(),
                        AuditActionCode.PATIENT_UPDATED, AuditResourceType.PATIENT, command.patientId(),
                        AuditOutcome.SUCCESS, null, Instant.now(), actor.workstationId(), actor.correlationId()));
                return Result.success(new PatientHistoryReceipt(command.patientId(), history.getId(),
                        history.getVersion() == null ? 0L : history.getVersion()));
            } catch (RuntimeException exception) {
                ApplicationFailureDiagnostics.logUnexpectedFailure("PATIENT_HISTORY_WRITE",
                        "PATIENT_HISTORY_FAILED", actor.correlationId().toString(), exception);
                return Result.failure("PATIENT_HISTORY_FAILED", FAILURE_MESSAGE);
            }
        });
    }

    private static Result<PatientHistoryReceipt> validatePayload(SavePatientHistoryCommand command) {
        if (command == null || command.patientId() <= 0
                || (command.historyId() != null && command.historyId() <= 0)
                || (command.historyId() == null) != (command.expectedVersion() == null)
                || (command.expectedVersion() != null && command.expectedVersion() < 0)) {
            return Result.failure("PATIENT_HISTORY_INVALID", "Thông tin tiền sử bệnh không hợp lệ.");
        }
        if (!isApproved(command.category())) {
            return Result.failure("PATIENT_HISTORY_CATEGORY_INVALID", "Loại tiền sử bệnh không được hỗ trợ.");
        }
        if (command.narrative() == null || command.narrative().isBlank() || command.narrative().length() > 1000) {
            return Result.failure("PATIENT_HISTORY_NARRATIVE_INVALID", "Mô tả phải có nội dung và không quá 1000 ký tự.");
        }
        return null;
    }

    private static boolean isApproved(LoaiTienSu category) {
        return category == LoaiTienSu.BENH_NEN || category == LoaiTienSu.DI_UNG || category == LoaiTienSu.PHAU_THUAT;
    }

    private static boolean isDoctor(AuthorizationContext context) {
        return context != null && context.isValid() && "DOCTOR".equals(context.roleCode());
    }

    private static boolean sameActor(AuthorizationContext context, AuditActorContext actor) {
        return actor != null && context.employeeId() == actor.employeeId() && context.roleId() == actor.roleId()
                && Objects.equals(context.roleCode(), actor.roleCode())
                && context.facilityId() == actor.facilityId() && context.departmentId() == actor.departmentId();
    }

    private static Result<PatientHistoryReceipt> denied() {
        return Result.failure("AUTHORIZATION_DENIED", "Không được phép lưu tiền sử bệnh trong phiên hiện tại.");
    }
}
