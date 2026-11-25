package vn.clinicmanagement.application.auth;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Cổng lưu trạng thái giới hạn tần suất đăng nhập trong phạm vi giao dịch.
 */
public interface TaiKhoanDangNhapGioiHanTanSuatLuuTru {
    Optional<Snapshot> lockForUpdate(int employeeId);

    void recordFailure(int employeeId, LocalDateTime windowStartedAt, int failureCount, int delaySeconds);

    void reset(int employeeId);

    record Snapshot(
            LocalDateTime databaseNow,
            LocalDateTime windowStartedAt,
            int failureCount,
            LocalDateTime lastFailureAt,
            int delaySeconds,
            LocalDateTime nextAllowedAt) {}
}
