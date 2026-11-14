package vn.clinicmanagement.application.auth;

import vn.clinicmanagement.application.common.Command;

import java.util.UUID;

/** Command to finalize login after the user selects one assigned role and one assigned work scope. */
public record CompleteDangNhapPhienCommand(
        /** Trường `authenticatedUser`: daxacthuc người dùng. */
        DaXacThucNguoiDung authenticatedUser,
        /** Trường `selectedRole`: duocchon vai trò. */
        DaXacThucNguoiDung.RoleChoice selectedRole,
        /** Trường `selectedWorkScope`: duocchon làm việc phamvi. */
        DaXacThucNguoiDung.WorkScopeChoice selectedWorkScope,
        /** Trường `workstationId`: `workstation` mã. */
        String workstationId,
        /** Trường `correlationId`: `correlation` mã. */
        UUID correlationId) implements Command<DaHoanTatDangNhapPhien> {

    public CompleteDangNhapPhienCommand(
            DaXacThucNguoiDung authenticatedUser,
            DaXacThucNguoiDung.RoleChoice selectedRole,
            DaXacThucNguoiDung.WorkScopeChoice selectedWorkScope) {
        this(authenticatedUser, selectedRole, selectedWorkScope, null, null);
    }
}
