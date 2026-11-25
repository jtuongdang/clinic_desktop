package vn.clinicmanagement.domain.identity;

/**
 * Cặp cơ sở và khoa hợp lệ được phân công cho nhân viên để chọn phạm vi phiên.
 */
public record NhanVienWorkPhamVi(int maCoSo, String tenCoSo, int maKhoa, String tenKhoa) {
    public NhanVienWorkPhamVi {
        if (maCoSo <= 0 || maKhoa <= 0) {
            throw new IllegalArgumentException("Cơ sở và khoa phải có mã hợp lệ.");
        }
        if (tenCoSo == null || tenCoSo.isBlank() || tenKhoa == null || tenKhoa.isBlank()) {
            throw new IllegalArgumentException("Tên cơ sở và khoa không được để trống.");
        }
    }
}
