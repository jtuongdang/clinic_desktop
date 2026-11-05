package vn.clinicmanagement.domain.identity;

/** An employee's assigned, valid facility and department pair. */
public record NhanVienWorkScope(int maCoSo, int maKhoa) {
    public NhanVienWorkScope {
        if (maCoSo <= 0 || maKhoa <= 0) {
            throw new IllegalArgumentException("Cơ sở và khoa phải có mã hợp lệ.");
        }
    }
}
