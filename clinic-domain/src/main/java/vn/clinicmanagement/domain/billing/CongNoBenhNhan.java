package vn.clinicmanagement.domain.billing;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.enums.TrangThaiCongNo;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CongNoBenhNhan extends CoSoThucThe<Integer> {
    private int maHoaDon;
    private int maBenhNhan;
    private BigDecimal soTienNo = BigDecimal.ZERO;
    private BigDecimal soTienDaTra = BigDecimal.ZERO;
    private LocalDate hanThanhToan;
    private TrangThaiCongNo trangThai = TrangThaiCongNo.DANG_NO;
    private HoaDon hoaDon;

    public CongNoBenhNhan() {}
    /** Hàm `getSoTienConLai`: lấy so tien `Con` `Lai`. */
    public BigDecimal getSoTienConLai() { return soTienNo.subtract(soTienDaTra); }
    /** Hàm `getMaHoaDon`: lấy mã hoa don. */
    public int getMaHoaDon() { return maHoaDon; }
    /** Hàm `setMaHoaDon`: gán mã hoa don. */
    public void setMaHoaDon(int maHoaDon) { this.maHoaDon = maHoaDon; }
    /** Hàm `getMaBenhNhan`: lấy mã benh nhãn. */
    public int getMaBenhNhan() { return maBenhNhan; }
    /** Hàm `setMaBenhNhan`: gán mã benh nhãn. */
    public void setMaBenhNhan(int maBenhNhan) { this.maBenhNhan = maBenhNhan; }
    /** Hàm `getSoTienNo`: lấy so tien no. */
    public BigDecimal getSoTienNo() { return soTienNo; }
    /** Hàm `setSoTienNo`: gán so tien no. */
    public void setSoTienNo(BigDecimal soTienNo) { this.soTienNo = soTienNo; }
    /** Hàm `getSoTienDaTra`: lấy so tien `Da` `Tra`. */
    public BigDecimal getSoTienDaTra() { return soTienDaTra; }
    /** Hàm `setSoTienDaTra`: gán so tien `Da` `Tra`. */
    public void setSoTienDaTra(BigDecimal soTienDaTra) { this.soTienDaTra = soTienDaTra; }
    /** Hàm `getHanThanhToan`: lấy han thanh toan. */
    public LocalDate getHanThanhToan() { return hanThanhToan; }
    /** Hàm `setHanThanhToan`: gán han thanh toan. */
    public void setHanThanhToan(LocalDate hanThanhToan) { this.hanThanhToan = hanThanhToan; }
    /** Hàm `getTrangThai`: lấy trang thai. */
    public TrangThaiCongNo getTrangThai() { return trangThai; }
    /** Hàm `setTrangThai`: gán trang thai. */
    public void setTrangThai(TrangThaiCongNo trangThai) { this.trangThai = trangThai; }
    /** Hàm `getHoaDon`: lấy hoa don. */
    public HoaDon getHoaDon() { return hoaDon; }
    /** Hàm `setHoaDon`: gán hoa don. */
    public void setHoaDon(HoaDon hoaDon) { this.hoaDon = hoaDon; }
}
