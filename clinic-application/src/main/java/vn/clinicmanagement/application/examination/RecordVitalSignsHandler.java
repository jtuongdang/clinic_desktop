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
import vn.clinicmanagement.domain.examination.SinhHieu;
import vn.clinicmanagement.domain.repositories.ILuotKhamRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class RecordVitalSignsHandler
        implements CommandHandler<RecordVitalSignsCommand, RecordVitalSignsReceipt> {
    private static final BigDecimal MIN_TEMPERATURE = new BigDecimal("30.0");
    private static final BigDecimal MAX_TEMPERATURE = new BigDecimal("45.0");
    private static final BigDecimal MIN_WEIGHT = new BigDecimal("0.5");
    private static final BigDecimal MAX_WEIGHT = new BigDecimal("500.0");
    private static final BigDecimal MIN_HEIGHT = new BigDecimal("20.0");
    private static final BigDecimal MAX_HEIGHT = new BigDecimal("300.0");
    private static final BigDecimal MIN_SPO2 = new BigDecimal("50.0");
    private static final BigDecimal MAX_SPO2 = new BigDecimal("100.0");
    private static final String VITALS_VALIDATION_ERROR =
            "Chỉ số sinh hiệu không nằm trong giới hạn sinh lý hợp lệ.";

    private final ILuotKhamRepository visitRepository;
    private final TransactionBoundary transactions;
    private final AuditWritePort audit;

    public RecordVitalSignsHandler(ILuotKhamRepository visitRepository,
                                   TransactionBoundary transactions,
                                   AuditWritePort audit) {
        this.visitRepository = Objects.requireNonNull(visitRepository, "visitRepository");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public Result<RecordVitalSignsReceipt> handle(RecordVitalSignsCommand command) {
        if (command == null || command.visitId() <= 0) {
            return Result.failure("VALIDATION_ERROR", "Mã lượt khám không hợp lệ.");
        }
        if (!hasValidPhysiologicalValues(command)) {
            return Result.failure("VALIDATION_ERROR", VITALS_VALIDATION_ERROR);
        }

        AuthorizationContext context = command.authorizationContext();
        if (context == null || !context.isValid()
                || !("DOCTOR".equals(context.roleCode()) || "RECEPTIONIST".equals(context.roleCode()))) {
            return deny(context, command.auditActorContext());
        }

        return transactions.inTransaction(() -> {
            Optional<LuotKham> visitResult = visitRepository.getById(command.visitId());
            if (visitResult.isEmpty()) {
                return Result.failure("NOT_FOUND", "Không tìm thấy lượt khám.");
            }

            LuotKham visit = visitResult.get();
            if (visit.isEmrLocked()) {
                return Result.failure("EMR_LOCKED", "Hồ sơ bệnh án đã bị khóa không thể ghi thêm sinh hiệu.");
            }
            if (visit.getTrangThai() == TrangThaiLuotKham.DA_HUY) {
                return Result.failure("INVALID_STATE", "Lượt khám đã bị hủy.");
            }

            BigDecimal bmi = calculateBmi(command.canNang(), command.chieuCao());
            SinhHieu vitalSigns = new SinhHieu();
            vitalSigns.setMaLuotKham(visit.getId());
            vitalSigns.setMach(command.mach());
            vitalSigns.setNhietDo(command.nhietDo());
            vitalSigns.setHuyetApTamThu(command.huyetApTamThu());
            vitalSigns.setHuyetApTamTruong(command.huyetApTamTruong());
            vitalSigns.setNhipTho(command.nhipTho());
            vitalSigns.setCanNang(command.canNang());
            vitalSigns.setChieuCao(command.chieuCao());
            vitalSigns.setBmi(bmi);
            vitalSigns.setSpo2(command.spo2());
            vitalSigns.setLuotKham(visit);

            if (visit.getSinhHieus() == null) {
                visit.setSinhHieus(new ArrayList<>());
            }
            visit.getSinhHieus().add(vitalSigns);

            visitRepository.update(visit);
            appendSuccessAudit(command.auditActorContext(), context, visit.getId());
            return Result.success(new RecordVitalSignsReceipt(visit.getId(), bmi, LocalDateTime.now()));
        });
    }

    private static boolean hasValidPhysiologicalValues(RecordVitalSignsCommand command) {
        return within(command.mach(), 20, 300)
                && within(command.huyetApTamThu(), 40, 300)
                && within(command.huyetApTamTruong(), 20, 200)
                && within(command.nhipTho(), 5, 100)
                && within(command.nhietDo(), MIN_TEMPERATURE, MAX_TEMPERATURE)
                && within(command.canNang(), MIN_WEIGHT, MAX_WEIGHT)
                && within(command.chieuCao(), MIN_HEIGHT, MAX_HEIGHT)
                && within(command.spo2(), MIN_SPO2, MAX_SPO2);
    }

    private static boolean within(Integer value, int minimum, int maximum) {
        return value == null || (value >= minimum && value <= maximum);
    }

    private static boolean within(BigDecimal value, BigDecimal minimum, BigDecimal maximum) {
        return value == null || (value.compareTo(minimum) >= 0 && value.compareTo(maximum) <= 0);
    }

    private static BigDecimal calculateBmi(BigDecimal weight, BigDecimal height) {
        if (weight == null || height == null || height.signum() <= 0) {
            return null;
        }
        BigDecimal heightInMeters = height.movePointLeft(2);
        return weight.divide(heightInMeters.multiply(heightInMeters), 2, RoundingMode.HALF_UP);
    }

    private Result<RecordVitalSignsReceipt> deny(AuthorizationContext context, AuditActorContext actor) {
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
        return Result.failure("AUTHORIZATION_DENIED", "Không được phép ghi sinh hiệu trong phiên hiện tại.");
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
