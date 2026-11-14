package vn.clinicmanagement.domain.payroll;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.math.BigDecimal;

public class ChiTietBangLuong extends CoSoThucThe<Integer> {
    private int maBangLuong;
    private String loaiKhoan = "";
    private String tenKhoan = "";
    private BigDecimal soTien = BigDecimal.ZERO;
    public ChiTietBangLuong() {}
    /** Hàm `getMaBangLuong`: lấy mã bảng luong. */
    public int getMaBangLuong() { return maBangLuong; }
    /** Hàm `setMaBangLuong`: gán mã bảng luong. */
    public void setMaBangLuong(int maBangLuong) { this.maBangLuong = maBangLuong; }
    /** Hàm `getLoaiKhoan`: lấy loai `Khoan`. */
    public String getLoaiKhoan() { return loaiKhoan; }
    /** Hàm `setLoaiKhoan`: gán loai `Khoan`. */
    public void setLoaiKhoan(String loaiKhoan) { this.loaiKhoan = loaiKhoan; }
    /** Hàm `getTenKhoan`: lấy `Ten` `Khoan`. */
    public String getTenKhoan() { return tenKhoan; }
    /** Hàm `setTenKhoan`: gán `Ten` `Khoan`. */
    public void setTenKhoan(String tenKhoan) { this.tenKhoan = tenKhoan; }
    /** Hàm `getSoTien`: lấy so tien. */
    public BigDecimal getSoTien() { return soTien; }
    /** Hàm `setSoTien`: gán so tien. */
    public void setSoTien(BigDecimal soTien) { this.soTien = soTien; }
}
