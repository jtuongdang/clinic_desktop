package vn.clinicmanagement.domain.repositories;
import vn.clinicmanagement.domain.system.MaTranPhanQuyen;
import vn.clinicmanagement.domain.system.ThamSoHeThong;
import java.util.List;
import java.util.Optional;

public interface IHeThongRepository {
    /** Hàm `getMatrixByRoleId`: lấy matran theo vai trò mã. */
    List<MaTranPhanQuyen> getMatrixByRoleId(int roleId);
    /** Hàm `getSystemParameterValue`: lấy hệ thống `Parameter` giá trị. */
    Optional<String> getSystemParameterValue(String key);

    /** Hàm `getAllSystemParameters`: lấy `All` hệ thống tham số. */
    default List<ThamSoHeThong> getAllSystemParameters() {
        return List.of();
    }

    /** Hàm `updateSystemParameter`: cập nhật hệ thống `Parameter`. */
    default void updateSystemParameter(String key, String value, Integer updatedBy) {
    }
}
