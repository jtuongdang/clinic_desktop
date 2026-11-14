package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;

import java.time.LocalDate;
import java.util.List;

/** Persistence port for creating a reception visit and reading the live reception queue. */
public interface TiepDonHangDoiCongKetNoi {
    /** Hàm `registerVisit`: đăng ký luotkham. */
    LuotKham registerVisit(int patientId, int facilityId, int departmentId, int roomId,
                           String registrationType, String reason, DoiTuongKham payerType,
                           boolean priority, int createdBy);
    /** Hàm `getQueue`: lấy hangdoi. */
    List<TiepDonHangDoiMatHang> getQueue(LocalDate date, int facilityId, int departmentId);
    /** Hàm `getRoomSummaries`: lấy phòng `Summaries`. */
    List<PhongKhamPhongHangDoiTongHop> getRoomSummaries(LocalDate date, int facilityId, int departmentId);
}
