package vn.clinicmanagement.domain.repositories;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkPhamVi;
import vn.clinicmanagement.domain.identity.VaiTro;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.Objects;

public interface INhanVienRepository extends IRepository<NhanVien> {
    /** Hàm `getByUsername`: lấy theo `Username`. */
    Optional<NhanVien> getByUsername(String username);
    /** Hàm `getActiveDoctors`: lấy hoạt động `Doctors`. */
    List<NhanVien> getActiveDoctors(int maCoSo);
    /** Hàm `getRoleNames`: lấy vai trò `Names`. */
    List<String> getRoleNames(int employeeId);
    /** Hàm `getAssignedRoles`: lấy daphancong vai trò. */
    List<VaiTro> getAssignedRoles(int employeeId);
    /** Hàm `getAssignedWorkScopes`: lấy daphancong làm việc `Scopes`. */
    List<NhanVienWorkPhamVi> getAssignedWorkScopes(int employeeId);
    /** Hàm `recordSuccessfulLogin`: ghi nhận thành công dangnhap. */
    void recordSuccessfulLogin(int employeeId, LocalDateTime loginAt);

    /** Hàm `getByMaNhanVien`: lấy theo mã nhãn vien. */
    default Optional<NhanVien> getByMaNhanVien(String maNhanVien) {
        if (maNhanVien == null) return Optional.empty();
        return getAll().stream()
                .filter(employee -> !employee.isDeleted())
                .filter(employee -> maNhanVien.equals(employee.getMaNhanVien()))
                .findFirst();
    }

    /** Hàm `getByStatus`: lấy theo trạng thái. */
    default List<NhanVien> getByStatus(Boolean active) {
        return getAll().stream()
                .filter(employee -> !employee.isDeleted())
                .filter(employee -> active == null || employee.isHoatDong() == active)
                .toList();
    }

    default boolean usernameExists(String username) {
        if (username == null || username.isBlank()) return false;
        return getAll().stream()
                .filter(employee -> !employee.isDeleted())
                .map(NhanVien::getUsername)
                .filter(Objects::nonNull)
                .anyMatch(existing -> existing.toLowerCase(java.util.Locale.ROOT)
                        .equals(username.toLowerCase(java.util.Locale.ROOT)));
    }
}
