package vn.clinicmanagement.domain.payroll;
import vn.clinicmanagement.domain.common.BaseEntity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BangLuong extends BaseEntity<Integer> {
    private int maKyLuong;
    private int maNhanVien;
    private BigDecimal luongCoBan = BigDecimal.ZERO;
    private BigDecimal heSoLuong = BigDecimal.ONE;
    private BigDecimal tongPhuCap = BigDecimal.ZERO;
    private BigDecimal tongThuLaoKham = BigDecimal.ZERO;
    private BigDecimal tongKhauTru = BigDecimal.ZERO;
    private BigDecimal thucLinh = BigDecimal.ZERO;
    private List<ChiTietBangLuong> chiTiets = new ArrayList<>();

    public BangLuong() {}
    public int getMaKyLuong() { return maKyLuong; }
    public void setMaKyLuong(int maKyLuong) { this.maKyLuong = maKyLuong; }
    public int getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    public BigDecimal getLuongCoBan() { return luongCoBan; }
    public void setLuongCoBan(BigDecimal luongCoBan) { this.luongCoBan = luongCoBan; }
    public BigDecimal getHeSoLuong() { return heSoLuong; }
    public void setHeSoLuong(BigDecimal heSoLuong) { this.heSoLuong = heSoLuong; }
    public BigDecimal getTongPhuCap() { return tongPhuCap; }
    public void setTongPhuCap(BigDecimal tongPhuCap) { this.tongPhuCap = tongPhuCap; }
    public BigDecimal getTongThuLaoKham() { return tongThuLaoKham; }
    public void setTongThuLaoKham(BigDecimal tongThuLaoKham) { this.tongThuLaoKham = tongThuLaoKham; }
    public BigDecimal getTongKhauTru() { return tongKhauTru; }
    public void setTongKhauTru(BigDecimal tongKhauTru) { this.tongKhauTru = tongKhauTru; }
    public BigDecimal getThucLinh() { return thucLinh; }
    public void setThucLinh(BigDecimal thucLinh) { this.thucLinh = thucLinh; }
    public List<ChiTietBangLuong> getChiTiets() { return chiTiets; }
    public void setChiTiets(List<ChiTietBangLuong> chiTiets) { this.chiTiets = chiTiets; }
}
