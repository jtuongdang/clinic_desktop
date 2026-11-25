package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.domain.examination.ICD;
import vn.clinicmanagement.domain.examination.LuotKham;
import vn.clinicmanagement.domain.facility.PhongKham;
import vn.clinicmanagement.domain.facility.Khoa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Cổng truy vấn hàng đợi khám bệnh hôm nay và lịch sử lượt khám cho màn hình Khám Bệnh.
 */
public interface KhamBenhHangDoiCongKetNoi {

    /** Lấy danh sách lượt khám trong ngày theo cơ sở và bác sĩ (doctorId = 0 nếu xem tất cả). */
    List<KhamBenhHangDoiMatHang> getTodayQueue(int facilityId, int doctorId);

    /** Lấy hàng đợi theo phạm vi người dùng và bộ lọc khoa/phòng (ID bằng 0 nghĩa là tất cả). */
    default List<KhamBenhHangDoiMatHang> getTodayQueue(int facilityId, int employeeId, String roleCode,
                                                       int departmentId, int roomId) {
        return getTodayQueue(facilityId, "DOCTOR".equalsIgnoreCase(roleCode) ? employeeId : 0);
    }

    /** Lấy hàng đợi theo khoảng ngày khám động (fromDate..toDate) cùng bộ lọc khoa/phòng. */
    default List<KhamBenhHangDoiMatHang> getQueue(int facilityId, int employeeId, String roleCode,
                                                  int departmentId, int roomId,
                                                  LocalDate fromDate, LocalDate toDate) {
        return getTodayQueue(facilityId, employeeId, roleCode, departmentId, roomId);
    }

    /** Danh sách khoa mà nhân viên được phân công trong cơ sở. */
    default List<Khoa> getAuthorizedDepartments(int facilityId, int employeeId) {
        return List.of();
    }

    /** Lấy toàn bộ lịch sử lượt khám của một bệnh nhân theo thời gian mới nhất trước. */
    List<KhamBenhLichSuMatHang> getPatientVisitHistory(int patientId);

    /** Tra cứu danh mục bệnh ICD-10 đang hoạt động trong CSDL theo mã hoặc tên bệnh. */
    default List<ICD> searchIcdCatalog(String keyword) {
        return List.of();
    }

    /** Tải đầy đủ thông tin lượt khám + các dòng `ChanDoan` (chính & kèm theo) phục vụ tab Xử Trí. */
    default Optional<LuotKham> getVisitDetail(int visitId) {
        return Optional.empty();
    }

    /** Lấy danh sách phòng khám đang hoạt động để hỗ trợ chuyển phòng khám từ tab Xử Trí. */
    default List<PhongKham> getActiveClinicRooms() {
        return List.of();
    }

    /** Phòng đích có thể thuộc khoa khác, nhưng phải trong cơ sở đang làm việc. */
    default List<PhongKham> getActiveClinicRooms(int facilityId) {
        if (facilityId <= 0) return List.of();
        return getActiveClinicRooms().stream()
                .filter(room -> room != null && room.getId() != null && room.getId() > 0 && room.isHoatDong()
                        && room.getKhoa() != null && room.getKhoa().getCoSo() != null
                        && java.util.Objects.equals(room.getKhoa().getCoSo().getId(), facilityId))
                .toList();
    }

    /** Danh sách phòng hoạt động thuộc khoa trong cơ sở. */
    default List<PhongKham> getActiveClinicRooms(int facilityId, int departmentId) {
        return getActiveClinicRooms().stream()
                .filter(room -> room.getMaKhoa() == departmentId)
                .toList();
    }
}
