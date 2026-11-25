package vn.clinicmanagement.application.hr;

public record NhanVienTongHopDuLieu(int employeeId, String maNhanVien, String hoTen,
                                 String soDienThoai, String email, Integer maChucVu,
                                 String username, boolean active) {}
