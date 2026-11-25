package vn.clinicmanagement.domain.repositories;
import vn.clinicmanagement.domain.prescription.DonThuoc;
import java.util.List;

public interface IDonThuocRepository extends IRepository<DonThuoc> {
    List<DonThuoc> getByLuotKhamId(int maLuotKham);
}
