package vn.clinicmanagement.domain.repositories;

/**
 * Cổng lưu liên kết cơ sở đang hoạt động của bệnh nhân.
 */
public interface IBenhNhanCoSoRepository {
    /** Đảm bảo có liên kết cơ sở đang hoạt động cho bệnh nhân. @return {@code true} chỉ khi đã thêm liên kết mới. */
    boolean ensureActiveLink(int patientId, int facilityId, int createdBy);
}
