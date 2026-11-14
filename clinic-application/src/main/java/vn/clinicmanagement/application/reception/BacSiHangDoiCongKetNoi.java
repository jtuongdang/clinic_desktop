package vn.clinicmanagement.application.reception;

import java.time.LocalDate;
import java.util.List;

/** Read port for the active doctor team queue. */
public interface BacSiHangDoiCongKetNoi {
    /** Hàm `getActiveDoctorQueue`: lấy hoạt động bacsi hangdoi. */
    List<TiepDonHangDoiMatHang> getActiveDoctorQueue(LocalDate date, int facilityId, int departmentId);
}
