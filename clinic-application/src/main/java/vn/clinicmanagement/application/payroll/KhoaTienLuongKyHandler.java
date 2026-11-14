package vn.clinicmanagement.application.payroll;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.CommandHandler;
import vn.clinicmanagement.application.common.KetQua;
import vn.clinicmanagement.domain.payroll.KyLuong;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class KhoaTienLuongKyHandler
        implements CommandHandler<KhoaTienLuongKyCommand, KhoaTienLuongKyBienNhan> {

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "HR_ADMIN");
    private static final String AUDIT_ACTION = "PAYROLL_PERIOD_LOCKED";

    /** Trường `payrollRepository`: tienluong kho dữ liệu. */
    private final TienLuongRepository payrollRepository;
    /** Trường `auditWritePort`: kiểm toán ghi congketnoi. */
    private final TienLuongKiemToanWriteCongKetNoi auditWritePort;

    public KhoaTienLuongKyHandler(
            TienLuongRepository payrollRepository,
            TienLuongKiemToanWriteCongKetNoi auditWritePort) {
        this.payrollRepository = Objects.requireNonNull(payrollRepository, "payrollRepository");
        this.auditWritePort = Objects.requireNonNull(auditWritePort, "auditWritePort");
    }

    @Override
    public KetQua<KhoaTienLuongKyBienNhan> handle(KhoaTienLuongKyCommand command) {
        if (command == null) {
            return KetQua.failure("VALIDATION_ERROR", "Lệnh chốt kỳ lương không được để trống.");
        }
        PhanQuyenNguCanh authorization = command.authorizationContext();
        if (authorization == null || !authorization.isValid()
                || !isRoleAllowed(authorization.roleCode())) {
            return KetQua.failure("AUTHORIZATION_DENIED", "Chỉ ADMIN hoặc HR_ADMIN được chốt kỳ lương.");
        }
        KiemToanTacNhanNguCanh actor = command.auditActorContext();
        if (actor == null || actor.employeeId() != authorization.employeeId()
                || !actor.roleCode().equals(authorization.roleCode())) {
            return KetQua.failure("AUDIT_ACTOR_REQUIRED", "Thiếu ngữ cảnh kiểm toán phù hợp với người thao tác.");
        }
        if (command.periodId() <= 0) {
            return KetQua.failure("INVALID_PAYROLL_PERIOD", "Mã kỳ lương không hợp lệ.");
        }

        KyLuong period = payrollRepository.findPeriodById(command.periodId()).orElse(null);
        if (period == null) {
            return KetQua.failure("PAYROLL_PERIOD_NOT_FOUND", "Không tìm thấy kỳ lương.");
        }
        if (!period.isDaChot()) {
            period.setDaChot(true);
            KyLuong saved = payrollRepository.savePeriod(period);
            if (saved == null) {
                return KetQua.failure("PAYROLL_PERIOD_LOCK_FAILED", "Không thể chốt kỳ lương.");
            }
        }

        auditWritePort.appendIndependently(new TienLuongKyDaKhoaKiemToan(
                command.periodId(),
                actor.employeeId(),
                actor.roleId(),
                actor.roleCode().toUpperCase(Locale.ROOT),
                Instant.now(),
                AUDIT_ACTION));
        return KetQua.success(new KhoaTienLuongKyBienNhan(command.periodId(), true));
    }

    /** Hàm `isRoleAllowed`: là vai trò `Allowed`. */
    private boolean isRoleAllowed(String roleCode) {
        return roleCode != null && ALLOWED_ROLES.contains(roleCode.trim().toUpperCase(Locale.ROOT));
    }
}
