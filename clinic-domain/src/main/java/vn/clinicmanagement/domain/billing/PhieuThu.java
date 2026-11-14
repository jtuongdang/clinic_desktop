package vn.clinicmanagement.domain.billing;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PhieuThu extends CoSoThucThe<Integer> {
    private int maHoaDon;
    private BigDecimal soTien = BigDecimal.ZERO;
    private LocalDateTime ngayThu = LocalDateTime.now();
    private PhuongThucThanhToan phuongThuc = PhuongThucThanhToan.TIEN_MAT;
    private int nguoiThu;
    private String ghiChu;
    private HoaDon hoaDon;

    public PhieuThu() {}
    /** Hàm `getMaHoaDon`: lấy mã hoa don. */
    public int getMaHoaDon() { return maHoaDon; }
    /** Hàm `setMaHoaDon`: gán mã hoa don. */
    public void setMaHoaDon(int maHoaDon) { this.maHoaDon = maHoaDon; }
    /** Hàm `getSoTien`: lấy so tien. */
    public BigDecimal getSoTien() { return soTien; }
    /** Hàm `setSoTien`: gán so tien. */
    public void setSoTien(BigDecimal soTien) { this.soTien = soTien; }
    /** Hàm `getNgayThu`: lấy `Ngay` thu. */
    public LocalDateTime getNgayThu() { return ngayThu; }
    /** Hàm `setNgayThu`: gán `Ngay` thu. */
    public void setNgayThu(LocalDateTime ngayThu) { this.ngayThu = ngayThu; }
    /** Hàm `getPhuongThuc`: lấy phuong thuc. */
    public PhuongThucThanhToan getPhuongThuc() { return phuongThuc; }
    /** Hàm `setPhuongThuc`: gán phuong thuc. */
    public void setPhuongThuc(PhuongThucThanhToan phuongThuc) { this.phuongThuc = phuongThuc; }
    /** Hàm `getNguoiThu`: lấy `Nguoi` thu. */
    public int getNguoiThu() { return nguoiThu; }
    /** Hàm `setNguoiThu`: gán `Nguoi` thu. */
    public void setNguoiThu(int nguoiThu) { this.nguoiThu = nguoiThu; }
    /** Hàm `getGhiChu`: lấy `Ghi` chu. */
    public String getGhiChu() { return ghiChu; }
    /** Hàm `setGhiChu`: gán `Ghi` chu. */
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    /** Hàm `getHoaDon`: lấy hoa don. */
    public HoaDon getHoaDon() { return hoaDon; }
    /** Hàm `setHoaDon`: gán hoa don. */
    public void setHoaDon(HoaDon hoaDon) { this.hoaDon = hoaDon; }
}
