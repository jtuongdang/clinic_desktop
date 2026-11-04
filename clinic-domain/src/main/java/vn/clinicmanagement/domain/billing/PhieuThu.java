package vn.clinicmanagement.domain.billing;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PhieuThu extends BaseEntity<Integer> {
    private int maHoaDon;
    private BigDecimal soTien = BigDecimal.ZERO;
    private LocalDateTime ngayThu = LocalDateTime.now();
    private PhuongThucThanhToan phuongThuc = PhuongThucThanhToan.TIEN_MAT;
    private int nguoiThu;
    private String ghiChu;
    private HoaDon hoaDon;

    public PhieuThu() {}
    public int getMaHoaDon() { return maHoaDon; }
    public void setMaHoaDon(int maHoaDon) { this.maHoaDon = maHoaDon; }
    public BigDecimal getSoTien() { return soTien; }
    public void setSoTien(BigDecimal soTien) { this.soTien = soTien; }
    public LocalDateTime getNgayThu() { return ngayThu; }
    public void setNgayThu(LocalDateTime ngayThu) { this.ngayThu = ngayThu; }
    public PhuongThucThanhToan getPhuongThuc() { return phuongThuc; }
    public void setPhuongThuc(PhuongThucThanhToan phuongThuc) { this.phuongThuc = phuongThuc; }
    public int getNguoiThu() { return nguoiThu; }
    public void setNguoiThu(int nguoiThu) { this.nguoiThu = nguoiThu; }
    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }
    public HoaDon getHoaDon() { return hoaDon; }
    public void setHoaDon(HoaDon hoaDon) { this.hoaDon = hoaDon; }
}
