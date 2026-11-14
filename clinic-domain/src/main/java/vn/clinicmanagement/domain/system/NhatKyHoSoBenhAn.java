package vn.clinicmanagement.domain.system;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.time.LocalDateTime;

public class NhatKyHoSoBenhAn extends CoSoThucThe<Integer> {
    private int maLuotKham;
    private int maNhanVien;
    private String hanhDong = "";
    private String lyDo;
    private LocalDateTime thoiGian = LocalDateTime.now();
    public NhatKyHoSoBenhAn() {}
    @Override
    public void markAsUpdated(int updatedBy) {
        throw new UnsupportedOperationException("Nhật ký hồ sơ bệnh án không thể cập nhật vòng đời.");
    }
    /** Hàm `softDelete`: `soft` xóa. Gốc khai báo tại `vn.clinicmanagement.domain.common.CoSoThucThe#softDelete`. */
    @Override
    public void softDelete(int deletedBy) {
        throw new UnsupportedOperationException("Nhật ký hồ sơ bệnh án không thể xóa mềm.");
    }
    @Override
    public void restore() {
        throw new UnsupportedOperationException("Nhật ký hồ sơ bệnh án không thể khôi phục.");
    }
    /** Hàm `setDeleted`: gán `Deleted`. Gốc khai báo tại `vn.clinicmanagement.domain.common.CoSoThucThe#setDeleted`. */
    @Override
    public void setDeleted(boolean deleted) {
        throw new UnsupportedOperationException("Không thể thay đổi trạng thái xóa của nhật ký hồ sơ bệnh án.");
    }
    /** Hàm `getMaLuotKham`: lấy mã luot kham. */
    public int getMaLuotKham() { return maLuotKham; }
    /** Hàm `setMaLuotKham`: gán mã luot kham. */
    public void setMaLuotKham(int maLuotKham) { this.maLuotKham = maLuotKham; }
    /** Hàm `getMaNhanVien`: lấy mã nhãn vien. */
    public int getMaNhanVien() { return maNhanVien; }
    /** Hàm `setMaNhanVien`: gán mã nhãn vien. */
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    /** Hàm `getHanhDong`: lấy hanh đóng. */
    public String getHanhDong() { return hanhDong; }
    /** Hàm `setHanhDong`: gán hanh đóng. */
    public void setHanhDong(String hanhDong) { this.hanhDong = hanhDong; }
    /** Hàm `getLyDo`: lấy `Ly` `Do`. */
    public String getLyDo() { return lyDo; }
    /** Hàm `setLyDo`: gán `Ly` `Do`. */
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }
    /** Hàm `getThoiGian`: lấy `Thoi` `Gian`. */
    public LocalDateTime getThoiGian() { return thoiGian; }
    /** Hàm `setThoiGian`: gán `Thoi` `Gian`. */
    public void setThoiGian(LocalDateTime thoiGian) { this.thoiGian = thoiGian; }
}
