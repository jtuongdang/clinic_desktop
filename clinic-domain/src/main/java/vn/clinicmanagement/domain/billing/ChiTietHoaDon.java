package vn.clinicmanagement.domain.billing;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.math.BigDecimal;

public class ChiTietHoaDon extends CoSoThucThe<Integer> {
    private int maHoaDon;
    private String loaiDoiTuong = "";
    private int maDoiTuong;
    private String moTa = "";
    private BigDecimal soLuong = BigDecimal.ONE;
    private BigDecimal donGia = BigDecimal.ZERO;
    private HoaDon hoaDon;

    public ChiTietHoaDon() {}
    /** Hàm `getMaHoaDon`: lấy mã hoa don. */
    public int getMaHoaDon() { return maHoaDon; }
    /** Hàm `setMaHoaDon`: gán mã hoa don. */
    public void setMaHoaDon(int maHoaDon) { this.maHoaDon = maHoaDon; }
    /** Hàm `getLoaiDoiTuong`: lấy loai doi tuong. */
    public String getLoaiDoiTuong() { return loaiDoiTuong; }
    /** Hàm `setLoaiDoiTuong`: gán loai doi tuong. */
    public void setLoaiDoiTuong(String loaiDoiTuong) { this.loaiDoiTuong = loaiDoiTuong; }
    /** Hàm `getMaDoiTuong`: lấy mã doi tuong. */
    public int getMaDoiTuong() { return maDoiTuong; }
    /** Hàm `setMaDoiTuong`: gán mã doi tuong. */
    public void setMaDoiTuong(int maDoiTuong) { this.maDoiTuong = maDoiTuong; }
    /** Hàm `getMoTa`: lấy `Mo` `Ta`. */
    public String getMoTa() { return moTa; }
    /** Hàm `setMoTa`: gán `Mo` `Ta`. */
    public void setMoTa(String moTa) { this.moTa = moTa; }
    /** Hàm `getSoLuong`: lấy so luong. */
    public BigDecimal getSoLuong() { return soLuong; }
    /** Hàm `setSoLuong`: gán so luong. */
    public void setSoLuong(BigDecimal soLuong) { this.soLuong = soLuong; }
    /** Hàm `getDonGia`: lấy don `Gia`. */
    public BigDecimal getDonGia() { return donGia; }
    /** Hàm `setDonGia`: gán don `Gia`. */
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
    /** Hàm `getHoaDon`: lấy hoa don. */
    public HoaDon getHoaDon() { return hoaDon; }
    /** Hàm `setHoaDon`: gán hoa don. */
    public void setHoaDon(HoaDon hoaDon) { this.hoaDon = hoaDon; }
}
