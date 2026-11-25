package vn.clinicmanagement.application.reception;
import java.util.Optional;
/** Cổng đọc phiếu trong phạm vi cơ sở và khoa đã xác thực. */
public interface IPhieuDangKyKhamRepository {
    Optional<PhieuDangKyKhamDuLieu> getByVisit(int visitId, int facilityId, int departmentId);
}
