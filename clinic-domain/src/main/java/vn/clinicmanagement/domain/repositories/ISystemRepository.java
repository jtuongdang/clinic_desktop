package vn.clinicmanagement.domain.repositories;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;
import vn.clinicmanagement.domain.system.ThamSoHeThong;
import java.util.List;
import java.util.Optional;

public interface ISystemRepository {
    List<MaTranPhanQuyen> getMatrixByRoleId(int roleId);
    Optional<String> getSystemParameterValue(String key);

    default List<ThamSoHeThong> getAllSystemParameters() {
        return List.of();
    }

    default void updateSystemParameter(String key, String value, Integer updatedBy) {
    }
}
