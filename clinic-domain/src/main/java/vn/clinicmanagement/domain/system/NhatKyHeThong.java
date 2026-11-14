package vn.clinicmanagement.domain.system;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.time.LocalDateTime;

public class NhatKyHeThong extends CoSoThucThe<Integer> {
    private Integer maNhanVien;
    private String hanhDong = "";
    private String chiTiet = "";
    private String ipAddress;
    private LocalDateTime thoiGian = LocalDateTime.now();
    public NhatKyHeThong() {}
    @Override
    public void markAsUpdated(int updatedBy) {
        throw new UnsupportedOperationException("Nhật ký hệ thống không thể cập nhật vòng đời.");
    }
    /** Hàm `softDelete`: `soft` xóa. Gốc khai báo tại `vn.clinicmanagement.domain.common.CoSoThucThe#softDelete`. */
    @Override
    public void softDelete(int deletedBy) {
        throw new UnsupportedOperationException("Nhật ký hệ thống không thể xóa mềm.");
    }
    @Override
    public void restore() {
        throw new UnsupportedOperationException("Nhật ký hệ thống không thể khôi phục.");
    }
    /** Hàm `setDeleted`: gán `Deleted`. Gốc khai báo tại `vn.clinicmanagement.domain.common.CoSoThucThe#setDeleted`. */
    @Override
    public void setDeleted(boolean deleted) {
        throw new UnsupportedOperationException("Không thể thay đổi trạng thái xóa của nhật ký hệ thống.");
    }
    /** Hàm `getMaNhanVien`: lấy mã nhãn vien. */
    public Integer getMaNhanVien() { return maNhanVien; }
    /** Hàm `setMaNhanVien`: gán mã nhãn vien. */
    public void setMaNhanVien(Integer maNhanVien) { this.maNhanVien = maNhanVien; }
    /** Hàm `getHanhDong`: lấy hanh đóng. */
    public String getHanhDong() { return hanhDong; }
    /** Hàm `setHanhDong`: gán hanh đóng. */
    public void setHanhDong(String hanhDong) { this.hanhDong = hanhDong; }
    /** Hàm `getChiTiet`: lấy chi tiet. */
    public String getChiTiet() { return chiTiet; }
    /** Hàm `setChiTiet`: gán chi tiet. */
    public void setChiTiet(String chiTiet) { this.chiTiet = chiTiet; }
    /** Hàm `getIpAddress`: lấy `Ip` `Address`. */
    public String getIpAddress() { return ipAddress; }
    /** Hàm `setIpAddress`: gán `Ip` `Address`. */
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    /** Hàm `getThoiGian`: lấy `Thoi` `Gian`. */
    public LocalDateTime getThoiGian() { return thoiGian; }
    /** Hàm `setThoiGian`: gán `Thoi` `Gian`. */
    public void setThoiGian(LocalDateTime thoiGian) { this.thoiGian = thoiGian; }
}
