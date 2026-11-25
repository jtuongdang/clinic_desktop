package vn.clinicmanagement.application.hr;

import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.repositories.INhanVienRepository;

import java.util.Locale;

/** Đánh giá quyền nhân sự dựa trên các vai trò đang hoạt động hiện được phân công cho nhân viên. */
public final class KhoDuLieuNhanSuPhanQuyen implements NhanSuPhanQuyen {
    private final INhanVienRepository employees;

    public KhoDuLieuNhanSuPhanQuyen(INhanVienRepository employees) {
        this.employees = employees;
    }

    @Override
    public boolean canManageEmployees(int actorId) {
        if (actorId <= 0) return false;
        NhanVien actor = employees.getById(actorId).orElse(null);
        if (actor == null || actor.isDeleted() || !actor.isHoatDong()) return false;
        return employees.getRoleNames(actorId).stream()
                .filter(role -> role != null)
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .anyMatch(role -> role.equals("ADMIN") || role.equals("HR_ADMIN"));
    }
}
