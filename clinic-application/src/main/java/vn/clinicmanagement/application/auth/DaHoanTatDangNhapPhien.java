package vn.clinicmanagement.application.auth;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable finalized login context produced only after role, scope, last-login, and audit succeed. */
public record DaHoanTatDangNhapPhien(
        /** Trường `userId`: người dùng mã. */
        int userId,
        String username,
        /** Trường `fullName`: `full` tên. */
        String fullName,
        /** Trường `role`: vai trò. */
        DaXacThucNguoiDung.RoleChoice role,
        /** Trường `workScope`: làm việc phamvi. */
        DaXacThucNguoiDung.WorkScopeChoice workScope,
        /** Trường `correlationId`: `correlation` mã. */
        UUID correlationId,
        /** Trường `loginAtUtc`: dangnhap luc `Utc`. */
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
