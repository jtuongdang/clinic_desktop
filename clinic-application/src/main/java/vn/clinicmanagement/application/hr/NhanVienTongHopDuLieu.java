package vn.clinicmanagement.application.hr;

/** Trường `employeeId`: nhân viên mã. */
public record NhanVienTongHopDuLieu(int employeeId, String maNhanVien, String hoTen,
                                 /** Trường `maChucVu`: `ma` chuc vu. */
                                 String soDienThoai, String email, Integer maChucVu,
                                 /** Trường `active`: hoạt động. */
                                 String username, boolean active) {}
