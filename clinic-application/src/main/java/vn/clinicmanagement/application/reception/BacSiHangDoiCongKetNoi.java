package vn.clinicmanagement.application.reception;

import java.time.LocalDate;
import java.util.List;

/**
 * Cổng đọc hàng đợi nhóm bác sĩ đang hoạt động.
 */
public interface BacSiHangDoiCongKetNoi {
    List<TiepDonHangDoiMatHang> getActiveDoctorQueue(LocalDate date, int facilityId, int departmentId);
}
