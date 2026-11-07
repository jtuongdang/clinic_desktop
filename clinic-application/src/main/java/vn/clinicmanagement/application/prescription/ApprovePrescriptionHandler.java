package vn.clinicmanagement.application.prescription;

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
import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import vn.clinicmanagement.domain.repositories.IDonThuocRepository;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ApprovePrescriptionHandler
        implements CommandHandler<ApprovePrescriptionCommand, ApprovePrescriptionReceipt> {
    private final IDonThuocRepository prescriptionRepository;
    private final ILuotKhamRepository visitRepository;
    private final TransactionBoundary transactions;
    private final AuditWritePort audit;

    public ApprovePrescriptionHandler(
            IDonThuocRepository prescriptionRepository,
            ILuotKhamRepository visitRepository,
            TransactionBoundary transactions,
            AuditWritePort audit) {
        this.prescriptionRepository = Objects.requireNonNull(prescriptionRepository, "prescriptionRepository");
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public Result<ApprovePrescriptionReceipt> handle(ApprovePrescriptionCommand command) {
        if (command == null || command.prescriptionId() <= 0 || command.doctorId() <= 0) {
            return Result.failure("VALIDATION_ERROR", "Thông tin duyệt đơn thuốc không hợp lệ.");
        }

        AuthorizationContext context = command.authorizationContext();
        if (context == null || !context.isValid()
                || !("DOCTOR".equals(context.roleCode()) || "ADMIN".equals(context.roleCode()))) {
            return deny(context, command.auditActorContext());
        }

        Optional<DonThuoc> prescriptionResult = prescriptionRepository.getById(command.prescriptionId());
        if (prescriptionResult.isEmpty()) {
            return Result.failure("NOT_FOUND", "Không tìm thấy đơn thuốc.");
        }

        DonThuoc prescription = prescriptionResult.get();
        if ("DOCTOR".equals(context.roleCode())
                && prescription.getMaBacSi() > 0
                && prescription.getMaBacSi() != command.doctorId()) {
            return Result.failure("DOCTOR_MISMATCH", "Chỉ bác sĩ kê đơn hoặc quản trị viên mới có quyền duyệt đơn.");
        }
        if (prescription.getTrangThai() != TrangThaiDonThuoc.TAM) {
            return Result.failure("INVALID_STATE", "Chỉ có thể duyệt đơn thuốc đang ở trạng thái lưu tạm.");
        }

        Optional<LuotKham> visitResult = visitRepository.getById(prescription.getMaLuotKham());
        if (visitResult.isPresent() && visitResult.get().isEmrLocked()) {
            return Result.failure("EMR_LOCKED", "Hồ sơ bệnh án đã bị khóa không thể duyệt đơn thuốc.");
        }

        return transactions.inTransaction(() -> {
            prescription.approve();
            prescriptionRepository.update(prescription);
            audit.appendInCurrentTransaction(new SystemAuditEntry(
                    context.employeeId(),
                    context.roleId(),
                    context.roleCode(),
                    context.facilityId(),
                    context.departmentId(),
                    AuditActionCode.RX_APPROVED,
                    AuditResourceType.PRESCRIPTION,
                    prescription.getId(),
                    AuditOutcome.SUCCESS,
                    null,
                    Instant.now(),
                    command.auditActorContext() == null ? null : command.auditActorContext().workstationId(),
                    command.auditActorContext() == null
                            ? context.sessionId()
                            : command.auditActorContext().correlationId()));
            return Result.success(new ApprovePrescriptionReceipt(
                    prescription.getId(),
                    prescription.getMaLuotKham(),
                    prescription.getTrangThai(),
                    LocalDateTime.now()));
        });
    }

    private Result<ApprovePrescriptionReceipt> deny(AuthorizationContext context, AuditActorContext actor) {
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
                actor == null ? null : actor.workstationId(),
                actor == null ? UUID.randomUUID() : actor.correlationId()));
        return Result.failure("AUTH_ACCESS_DENIED", "Không được phép duyệt đơn thuốc trong phiên hiện tại.");
    }
}
