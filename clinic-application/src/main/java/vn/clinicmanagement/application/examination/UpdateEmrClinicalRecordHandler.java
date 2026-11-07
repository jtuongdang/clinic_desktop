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

public final class UpdateEmrClinicalRecordHandler implements CommandHandler<UpdateEmrClinicalRecordCommand, UpdateEmrClinicalRecordReceipt> {
    private final ILuotKhamRepository visitRepository;
    private final TransactionBoundary transactions;
    private final AuditWritePort audit;

    public UpdateEmrClinicalRecordHandler(ILuotKhamRepository visitRepository,
                                         TransactionBoundary transactions,
                                         AuditWritePort audit) {
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public Result<UpdateEmrClinicalRecordReceipt> handle(UpdateEmrClinicalRecordCommand command) {
        if (command == null || command.visitId() <= 0 || command.doctorId() <= 0) {
            return Result.failure("VALIDATION_ERROR", "Thông tin cập nhật bệnh án không hợp lệ.");
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
            if (visit.getMaBacSi() != null && visit.getMaBacSi() != command.doctorId()) {
                return Result.failure("DOCTOR_MISMATCH", "Chỉ bác sĩ phụ trách mới có quyền cập nhật bệnh án.");
            }

            if (visit.isEmrLocked()) {
                return Result.failure("EMR_ALREADY_LOCKED", "Hồ sơ bệnh án đã bị khóa không thể chỉnh sửa.");
            }

            if (visit.getTrangThai() != TrangThaiLuotKham.DANG_KHAM) {
                return Result.failure("INVALID_STATE", "Chỉ có thể cập nhật bệnh án khi đang khám.");
            }

            if (command.quaTrinhBenhLy() != null) {
                visit.setQuaTrinhBenhLy(command.quaTrinhBenhLy());
            }
            if (command.tienSuBanThan() != null) {
                visit.setTienSuBanThan(command.tienSuBanThan());
            }
            if (command.tienSuGiaDinh() != null) {
                visit.setTienSuGiaDinh(command.tienSuGiaDinh());
            }
            if (command.khamToanThan() != null) {
                visit.setKhamToanThan(command.khamToanThan());
            }
            if (command.khamBoPhan() != null) {
                visit.setKhamBoPhan(command.khamBoPhan());
            }
            if (command.tomTatBenhAn() != null) {
                visit.setTomTatBenhAn(command.tomTatBenhAn());
            }
            if (command.huongDieuTri() != null) {
                visit.setHuongDieuTri(command.huongDieuTri());
            }
            if (command.loiDanBacSi() != null) {
                visit.setLoiDanBacSi(command.loiDanBacSi());
            }

            visitRepository.update(visit);
            appendSuccessAudit(command.auditActorContext(), context, command.visitId());
            return Result.success(new UpdateEmrClinicalRecordReceipt(
                    visit.getId(), visit.isEmrLocked(), LocalDateTime.now()));
        });
    }

    private Result<UpdateEmrClinicalRecordReceipt> deny(AuthorizationContext context, AuditActorContext actor) {
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
        return Result.failure("AUTHORIZATION_DENIED", "Không được phép cập nhật bệnh án trong phiên hiện tại.");
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
