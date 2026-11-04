package vn.clinicmanagement.domain.repositories;
import vn.clinicmanagement.domain.billing.HoaDon;
import java.util.Optional;

public interface IHoaDonRepository extends IRepository<HoaDon> {
    Optional<HoaDon> getBySoHoaDon(String soHoaDon);
    Optional<HoaDon> getByLuotKham(int maLuotKham);
}
