package vn.clinicmanagement.domain.billing;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.enums.TrangThaiCongNo;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CongNoBenhNhan extends BaseEntity<Integer> {
    private int maHoaDon;
    private int maBenhNhan;
    private BigDecimal soTienNo = BigDecimal.ZERO;
    private BigDecimal soTienDaTra = BigDecimal.ZERO;
    private LocalDate hanThanhToan;
    private TrangThaiCongNo trangThai = TrangThaiCongNo.DANG_NO;
    private HoaDon hoaDon;

    public CongNoBenhNhan() {}
    public BigDecimal getSoTienConLai() { return soTienNo.subtract(soTienDaTra); }
    public int getMaHoaDon() { return maHoaDon; }
    public void setMaHoaDon(int maHoaDon) { this.maHoaDon = maHoaDon; }
    public int getMaBenhNhan() { return maBenhNhan; }
    public void setMaBenhNhan(int maBenhNhan) { this.maBenhNhan = maBenhNhan; }
    public BigDecimal getSoTienNo() { return soTienNo; }
    public void setSoTienNo(BigDecimal soTienNo) { this.soTienNo = soTienNo; }
    public BigDecimal getSoTienDaTra() { return soTienDaTra; }
    public void setSoTienDaTra(BigDecimal soTienDaTra) { this.soTienDaTra = soTienDaTra; }
    public LocalDate getHanThanhToan() { return hanThanhToan; }
    public void setHanThanhToan(LocalDate hanThanhToan) { this.hanThanhToan = hanThanhToan; }
    public TrangThaiCongNo getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiCongNo trangThai) { this.trangThai = trangThai; }
    public HoaDon getHoaDon() { return hoaDon; }
    public void setHoaDon(HoaDon hoaDon) { this.hoaDon = hoaDon; }
}
