package vn.clinicmanagement.application.auth;

import java.time.LocalDateTime;
import java.util.Optional;

/** Transaction-scoped persistence for account login throttle state. */
public interface TaiKhoanDangNhapGioiHanTanSuatLuuTru {
    /** Hàm `lockForUpdate`: khoa cho cập nhật. */
    Optional<Snapshot> lockForUpdate(int employeeId);

    /** Hàm `recordFailure`: ghi nhận thất bại. */
    void recordFailure(int employeeId, LocalDateTime windowStartedAt, int failureCount, int delaySeconds);

    /** Hàm `reset`: đặt lại. */
    void reset(int employeeId);

    record Snapshot(
            /** Trường `databaseNow`: cosodulieu `Now`. */
            LocalDateTime databaseNow,
            /** Trường `windowStartedAt`: `window` dabatdau luc. */
            LocalDateTime windowStartedAt,
            /** Trường `failureCount`: thất bại dem. */
            int failureCount,
            /** Trường `lastFailureAt`: `last` thất bại luc. */
            LocalDateTime lastFailureAt,
            int delaySeconds,
            /** Trường `nextAllowedAt`: `next` `Allowed` luc. */
            LocalDateTime nextAllowedAt) {}
}
