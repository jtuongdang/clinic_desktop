package vn.clinicmanagement.application.auth.complete;

import vn.clinicmanagement.application.auth.DaHoanTatDangNhapPhien;
import vn.clinicmanagement.application.auth.DaXacThucNguoiDung;

import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

/**
 * Lệnh hoàn tất đăng nhập sau khi người dùng chọn một vai trò và một phạm vi làm việc đã được phân công.
 */
public record CompleteDangNhapPhienCommand(
        DaXacThucNguoiDung authenticatedUser,
        DaXacThucNguoiDung.RoleChoice selectedRole,
        DaXacThucNguoiDung.WorkScopeChoice selectedWorkScope,
        String workstationId,
        UUID correlationId) implements Command<DaHoanTatDangNhapPhien> {

    public CompleteDangNhapPhienCommand(
            DaXacThucNguoiDung authenticatedUser,
            DaXacThucNguoiDung.RoleChoice selectedRole,
            DaXacThucNguoiDung.WorkScopeChoice selectedWorkScope) {
        this(authenticatedUser, selectedRole, selectedWorkScope, null, null);
    }
}
