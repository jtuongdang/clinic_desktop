package vn.clinicmanagement.domain.repositories;
import vn.clinicmanagement.domain.identity.NhanVien;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.Objects;

public interface INhanVienRepository extends IRepository<NhanVien> {
    Optional<NhanVien> getByUsername(String username);
    List<NhanVien> getActiveDoctors(int maCoSo);
    List<String> getRoleNames(int employeeId);
    void recordSuccessfulLogin(int employeeId, LocalDateTime loginAt);

    default Optional<NhanVien> getByMaNhanVien(String maNhanVien) {
        if (maNhanVien == null) return Optional.empty();
        return getAll().stream()
                .filter(employee -> !employee.isDeleted())
                .filter(employee -> maNhanVien.equals(employee.getMaNhanVien()))
                .findFirst();
    }

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
