package vn.clinicmanagement.application.hr;

/**
 * Xác định quyền quản trị từ ngữ cảnh ứng dụng đã xác thực.
 */
@FunctionalInterface
public interface NhanSuPhanQuyen {
    boolean canManageEmployees(int actorId);
}
