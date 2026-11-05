package vn.clinicmanagement.domain.repositories;
import vn.clinicmanagement.domain.patient.BenhNhan;
import java.util.List;
import java.util.Optional;

public interface IBenhNhanRepository extends IRepository<BenhNhan> {
    Optional<BenhNhan> getByCCCD(String cccd);
    default Optional<BenhNhan> getByCCCDInScope(String cccd, int facilityId, int departmentId) {
        return Optional.empty();
    }
    Optional<BenhNhan> getByPhone(String phone);
    List<BenhNhan> search(String keyword);
}
