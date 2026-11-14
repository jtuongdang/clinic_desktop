package vn.clinicmanagement.domain.repositories;
import vn.clinicmanagement.domain.examination.LuotKham;
import java.time.LocalDate;
import java.util.List;

public interface ILuotKhamRepository extends IRepository<LuotKham> {
    /** Hàm `getVisitsByDoctor`: lấy `Visits` theo bacsi. */
    List<LuotKham> getVisitsByDoctor(int maBacSi, LocalDate date);
    /** Hàm `getNextQueueNumber`: lấy `Next` hangdoi so. */
    int getNextQueueNumber(int maCoSo, int maKhoa, LocalDate date);
}
