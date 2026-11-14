package vn.clinicmanagement.domain.payroll;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BangLuong extends CoSoThucThe<Integer> {
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
    /** Hàm `getMaKyLuong`: lấy mã ky luong. */
    public int getMaKyLuong() { return maKyLuong; }
    /** Hàm `setMaKyLuong`: gán mã ky luong. */
    public void setMaKyLuong(int maKyLuong) { this.maKyLuong = maKyLuong; }
    /** Hàm `getMaNhanVien`: lấy mã nhãn vien. */
    public int getMaNhanVien() { return maNhanVien; }
    /** Hàm `setMaNhanVien`: gán mã nhãn vien. */
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    /** Hàm `getLuongCoBan`: lấy luong có `Ban`. */
    public BigDecimal getLuongCoBan() { return luongCoBan; }
    /** Hàm `setLuongCoBan`: gán luong có `Ban`. */
    public void setLuongCoBan(BigDecimal luongCoBan) { this.luongCoBan = luongCoBan; }
    /** Hàm `getHeSoLuong`: lấy he so luong. */
    public BigDecimal getHeSoLuong() { return heSoLuong; }
    /** Hàm `setHeSoLuong`: gán he so luong. */
    public void setHeSoLuong(BigDecimal heSoLuong) { this.heSoLuong = heSoLuong; }
    /** Hàm `getTongPhuCap`: lấy `Tong` `Phu` cap. */
    public BigDecimal getTongPhuCap() { return tongPhuCap; }
    /** Hàm `setTongPhuCap`: gán `Tong` `Phu` cap. */
    public void setTongPhuCap(BigDecimal tongPhuCap) { this.tongPhuCap = tongPhuCap; }
    /** Hàm `getTongThuLaoKham`: lấy `Tong` thu `Lao` kham. */
    public BigDecimal getTongThuLaoKham() { return tongThuLaoKham; }
    /** Hàm `setTongThuLaoKham`: gán `Tong` thu `Lao` kham. */
    public void setTongThuLaoKham(BigDecimal tongThuLaoKham) { this.tongThuLaoKham = tongThuLaoKham; }
    /** Hàm `getTongKhauTru`: lấy `Tong` `Khau` `Tru`. */
    public BigDecimal getTongKhauTru() { return tongKhauTru; }
    /** Hàm `setTongKhauTru`: gán `Tong` `Khau` `Tru`. */
    public void setTongKhauTru(BigDecimal tongKhauTru) { this.tongKhauTru = tongKhauTru; }
    /** Hàm `getThucLinh`: lấy thuc `Linh`. */
    public BigDecimal getThucLinh() { return thucLinh; }
    /** Hàm `setThucLinh`: gán thuc `Linh`. */
    public void setThucLinh(BigDecimal thucLinh) { this.thucLinh = thucLinh; }
    /** Hàm `getChiTiets`: lấy chi `Tiets`. */
    public List<ChiTietBangLuong> getChiTiets() { return chiTiets; }
    /** Hàm `setChiTiets`: gán chi `Tiets`. */
    public void setChiTiets(List<ChiTietBangLuong> chiTiets) { this.chiTiets = chiTiets; }
}
