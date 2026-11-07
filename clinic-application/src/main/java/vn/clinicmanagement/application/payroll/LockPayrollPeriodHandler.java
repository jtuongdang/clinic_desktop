package vn.clinicmanagement.application.payroll;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.Result;
import vn.clinicmanagement.domain.payroll.KyLuong;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class LockPayrollPeriodHandler
        implements CommandHandler<LockPayrollPeriodCommand, LockPayrollPeriodReceipt> {

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "HR_ADMIN");
    private static final String AUDIT_ACTION = "PAYROLL_PERIOD_LOCKED";

    private final PayrollRepository payrollRepository;
    private final PayrollAuditWritePort auditWritePort;

    public LockPayrollPeriodHandler(
            PayrollRepository payrollRepository,
            PayrollAuditWritePort auditWritePort) {
        this.payrollRepository = Objects.requireNonNull(payrollRepository, "payrollRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public Result<LockPayrollPeriodReceipt> handle(LockPayrollPeriodCommand command) {
        if (command == null) {
            return Result.failure("VALIDATION_ERROR", "Lệnh chốt kỳ lương không được để trống.");
        }
        AuthorizationContext authorization = command.authorizationContext();
        if (authorization == null || !authorization.isValid()
                || !isRoleAllowed(authorization.roleCode())) {
            return Result.failure("AUTHORIZATION_DENIED", "Chỉ ADMIN hoặc HR_ADMIN được chốt kỳ lương.");
        }
        AuditActorContext actor = command.auditActorContext();
        if (actor == null || actor.employeeId() != authorization.employeeId()
                || !actor.roleCode().equals(authorization.roleCode())) {
            return Result.failure("AUDIT_ACTOR_REQUIRED", "Thiếu ngữ cảnh kiểm toán phù hợp với người thao tác.");
        }
        if (command.periodId() <= 0) {
            return Result.failure("INVALID_PAYROLL_PERIOD", "Mã kỳ lương không hợp lệ.");
        }

        KyLuong period = payrollRepository.findPeriodById(command.periodId()).orElse(null);
        if (period == null) {
            return Result.failure("PAYROLL_PERIOD_NOT_FOUND", "Không tìm thấy kỳ lương.");
        }
        if (!period.isDaChot()) {
            period.setDaChot(true);
            KyLuong saved = payrollRepository.savePeriod(period);
            if (saved == null) {
                return Result.failure("PAYROLL_PERIOD_LOCK_FAILED", "Không thể chốt kỳ lương.");
            }
        }

        auditWritePort.appendIndependently(new PayrollPeriodLockedAudit(
                command.periodId(),
                actor.employeeId(),
                actor.roleId(),
                actor.roleCode().toUpperCase(Locale.ROOT),
                Instant.now(),
                AUDIT_ACTION));
        return Result.success(new LockPayrollPeriodReceipt(command.periodId(), true));
    }

    private boolean isRoleAllowed(String roleCode) {
        return roleCode != null && ALLOWED_ROLES.contains(roleCode.trim().toUpperCase(Locale.ROOT));
    }
}
