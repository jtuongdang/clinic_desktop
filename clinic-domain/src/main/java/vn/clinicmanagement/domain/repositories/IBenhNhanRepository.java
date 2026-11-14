package vn.clinicmanagement.domain.repositories;
import vn.clinicmanagement.domain.patient.BenhNhan;
import java.util.List;
import java.util.Optional;

public interface IBenhNhanRepository extends IRepository<BenhNhan> {
    /** Hàm `getByCCCD`: lấy theo `CCCD`. */
    Optional<BenhNhan> getByCCCD(String cccd);
    /** Hàm `getByIdInScope`: lấy theo mã trong phamvi. */
    default Optional<BenhNhan> getByIdInScope(int patientId, int facilityId, int departmentId) {
        return Optional.empty();
    }
    /** Hàm `getByCCCDInScope`: lấy theo `CCCD` trong phamvi. */
    default Optional<BenhNhan> getByCCCDInScope(String cccd, int facilityId, int departmentId) {
        return Optional.empty();
    }
    /** Hàm `getByPhone`: lấy theo dienthoai. */
    Optional<BenhNhan> getByPhone(String phone);
    /** Hàm `search`: tìm kiếm. */
    List<BenhNhan> search(String keyword);
    /** Hàm `searchInScope`: tìm kiếm trong phamvi. */
    List<BenhNhan> searchInScope(String keyword, int facilityId, int departmentId, int limit);
}
