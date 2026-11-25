package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.common.enums.DoiTuongKham;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Cổng lưu lượt khám tiếp đón và đọc hàng đợi tiếp đón hiện tại.
 */
public interface TiepDonHangDoiCongKetNoi {
    LuotKham registerVisit(int patientId, int facilityId, int departmentId, int roomId,
                           String registrationType, String reason, DoiTuongKham payerType,
                           boolean priority, int createdBy);
    default LuotKham registerVisit(int patientId, int facilityId, int departmentId, int roomId,
                                   Integer serviceId, String registrationType, String reason,
                                   DoiTuongKham payerType, boolean priority, int createdBy) {
        return registerVisit(patientId, facilityId, departmentId, roomId,
                registrationType, reason, payerType, priority, createdBy);
    }
    /** Đọc giá đã chốt trong giao dịch đăng ký để lập hóa đơn theo giá lịch sử. */
    default Optional<TiepDonKhamGia> getRegistrationPrice(int visitId, int serviceId) {
        return Optional.empty();
    }
    List<TiepDonHangDoiMatHang> getQueue(LocalDate date, int facilityId, int departmentId);
    /** Lấy hàng đợi theo khoảng ngày khám (fromDate..toDate) cho một khoa. */
    default List<TiepDonHangDoiMatHang> getQueue(LocalDate fromDate, LocalDate toDate, int facilityId, int departmentId) {
        return getQueue(fromDate != null ? fromDate : LocalDate.now(), facilityId, departmentId);
    }
    /** Lấy danh sách lượt khám trong ngày toàn cơ sở (phục vụ quầy Thu Ngân thu tập trung). */
    default List<TiepDonHangDoiMatHang> getFacilityBillingQueue(LocalDate date, int facilityId) {
        return List.of();
    }
    /** Lấy danh sách lượt khám theo khoảng ngày toàn cơ sở (phục vụ quầy Thu Ngân lọc động). */
    default List<TiepDonHangDoiMatHang> getFacilityBillingQueue(LocalDate fromDate, LocalDate toDate, int facilityId) {
        return getFacilityBillingQueue(fromDate != null ? fromDate : LocalDate.now(), facilityId);
    }
    List<PhongKhamPhongHangDoiTongHop> getRoomSummaries(LocalDate date, int facilityId, int departmentId);
    default List<TiepDonLichSuKhamMatHang> getPatientVisitHistory(int patientId, int facilityId, int departmentId) {
        return List.of();
    }
}
