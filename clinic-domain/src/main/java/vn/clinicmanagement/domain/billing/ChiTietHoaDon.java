package vn.clinicmanagement.domain.billing;
import vn.clinicmanagement.domain.common.BaseEntity;
import java.math.BigDecimal;

public class ChiTietHoaDon extends BaseEntity<Integer> {
    private int maHoaDon;
    private String loaiDoiTuong = "";
    private int maDoiTuong;
    private String moTa = "";
    private BigDecimal soLuong = BigDecimal.ONE;
    private BigDecimal donGia = BigDecimal.ZERO;
    private HoaDon hoaDon;

    public ChiTietHoaDon() {}
    public int getMaHoaDon() { return maHoaDon; }
    public void setMaHoaDon(int maHoaDon) { this.maHoaDon = maHoaDon; }
    public String getLoaiDoiTuong() { return loaiDoiTuong; }
    public void setLoaiDoiTuong(String loaiDoiTuong) { this.loaiDoiTuong = loaiDoiTuong; }
    public int getMaDoiTuong() { return maDoiTuong; }
    public void setMaDoiTuong(int maDoiTuong) { this.maDoiTuong = maDoiTuong; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public BigDecimal getSoLuong() { return soLuong; }
    public void setSoLuong(BigDecimal soLuong) { this.soLuong = soLuong; }
    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
    public HoaDon getHoaDon() { return hoaDon; }
    public void setHoaDon(HoaDon hoaDon) { this.hoaDon = hoaDon; }
}
