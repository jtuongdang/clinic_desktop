package vn.clinicmanagement.domain.repositories;
import vn.clinicmanagement.domain.identity.NhanVien;
import vn.clinicmanagement.domain.identity.NhanVienWorkScope;
import vn.clinicmanagement.domain.identity.VaiTro;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface INhanVienRepository extends IRepository<NhanVien> {
    Optional<NhanVien> getByUsername(String username);
    List<NhanVien> getActiveDoctors(int maCoSo);
    List<String> getRoleNames(int employeeId);
    List<VaiTro> getAssignedRoles(int employeeId);
    List<NhanVienWorkScope> getAssignedWorkScopes(int employeeId);
    void recordSuccessfulLogin(int employeeId, LocalDateTime loginAt);
}
