package vn.clinicmanagement.domain.payroll;
import vn.clinicmanagement.domain.common.BaseEntity;
import java.math.BigDecimal;

public class ChiTietBangLuong extends BaseEntity<Integer> {
    private int maBangLuong;
    private String loaiKhoan = "";
    private String tenKhoan = "";
    private BigDecimal soTien = BigDecimal.ZERO;
    public ChiTietBangLuong() {}
    public int getMaBangLuong() { return maBangLuong; }
    public void setMaBangLuong(int maBangLuong) { this.maBangLuong = maBangLuong; }
    public String getLoaiKhoan() { return loaiKhoan; }
    public void setLoaiKhoan(String loaiKhoan) { this.loaiKhoan = loaiKhoan; }
    public String getTenKhoan() { return tenKhoan; }
    public void setTenKhoan(String tenKhoan) { this.tenKhoan = tenKhoan; }
    public BigDecimal getSoTien() { return soTien; }
    public void setSoTien(BigDecimal soTien) { this.soTien = soTien; }
}
