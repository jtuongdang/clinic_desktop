package vn.clinicmanagement.domain.repositories;
import vn.clinicmanagement.domain.examination.LuotKham;
import java.time.LocalDate;
import java.util.List;

public interface ILuotKhamRepository extends IRepository<LuotKham> {
    List<LuotKham> getVisitsByDoctor(int maBacSi, LocalDate date);
    int getNextQueueNumber(int maCoSo, int maKhoa, LocalDate date);
}
