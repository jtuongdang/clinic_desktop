package vn.clinicmanagement.application.hr;

@FunctionalInterface
public interface NhanSuKiemToanNhatKy {
    /** Hàm `append`: thêm vào. */
    void append(NhanSuKiemToanHanhDong action, int actorId, int employeeId);
}
