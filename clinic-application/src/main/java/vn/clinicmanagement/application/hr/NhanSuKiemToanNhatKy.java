package vn.clinicmanagement.application.hr;

@FunctionalInterface
public interface NhanSuKiemToanNhatKy {
    void append(NhanSuKiemToanHanhDong action, int actorId, int employeeId);
}
