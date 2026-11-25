package vn.clinicmanagement.application.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Ngữ cảnh đăng nhập bất biến, chỉ được tạo sau khi vai trò, phạm vi, thời điểm đăng nhập gần nhất và ghi kiểm toán đều thành công.
 */
public record DaHoanTatDangNhapPhien(
        int userId,
        String username,
        String fullName,
        DaXacThucNguoiDung.RoleChoice role,
        DaXacThucNguoiDung.WorkScopeChoice workScope,
        UUID correlationId,
        Instant loginAtUtc) {

    public DaHoanTatDangNhapPhien {
        if (userId <= 0) throw new IllegalArgumentException("userId must be positive.");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(role, "role");
        Objects.requireNonNull(workScope, "workScope");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(loginAtUtc, "loginAtUtc");
    }
}
