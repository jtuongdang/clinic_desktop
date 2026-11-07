package vn.clinicmanagement.domain.repositories;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;
import java.util.List;
import java.util.Optional;

public interface ISystemRepository {
    List<MaTranPhanQuyen> getMatrixByRoleId(int roleId);
    Optional<String> getSystemParameterValue(String key);
}
