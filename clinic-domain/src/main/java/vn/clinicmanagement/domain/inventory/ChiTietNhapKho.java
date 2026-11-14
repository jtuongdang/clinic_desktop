package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ChiTietNhapKho extends CoSoThucThe<Integer> {
    private int maPhieuNhap;
    /** Trường `maItem`: `ma` mathang. */
    private int maItem;
    private String soLo = "";
    private LocalDate hanSuDung;
    private BigDecimal soLuong = BigDecimal.ZERO;
    private BigDecimal giaNhap = BigDecimal.ZERO;
    private BigDecimal giaBan = BigDecimal.ZERO;

    public ChiTietNhapKho() {}
    /** Hàm `getMaPhieuNhap`: lấy mã phiếu nhap. */
    public int getMaPhieuNhap() { return maPhieuNhap; }
    /** Hàm `setMaPhieuNhap`: gán mã phiếu nhap. */
    public void setMaPhieuNhap(int maPhieuNhap) { this.maPhieuNhap = maPhieuNhap; }
    /** Hàm `getMaItem`: lấy mã mathang. */
    public int getMaItem() { return maItem; }
    /** Hàm `setMaItem`: gán mã mathang. */
    public void setMaItem(int maItem) { this.maItem = maItem; }
    /** Hàm `getSoLo`: lấy so lo. */
    public String getSoLo() { return soLo; }
    /** Hàm `setSoLo`: gán so lo. */
    public void setSoLo(String soLo) { this.soLo = soLo; }
    /** Hàm `getHanSuDung`: lấy han su `Dung`. */
    public LocalDate getHanSuDung() { return hanSuDung; }
    /** Hàm `setHanSuDung`: gán han su `Dung`. */
    public void setHanSuDung(LocalDate hanSuDung) { this.hanSuDung = hanSuDung; }
    /** Hàm `getSoLuong`: lấy so luong. */
    public BigDecimal getSoLuong() { return soLuong; }
    /** Hàm `setSoLuong`: gán so luong. */
    public void setSoLuong(BigDecimal soLuong) { this.soLuong = soLuong; }
    /** Hàm `getGiaNhap`: lấy `Gia` nhap. */
    public BigDecimal getGiaNhap() { return giaNhap; }
    /** Hàm `setGiaNhap`: gán `Gia` nhap. */
    public void setGiaNhap(BigDecimal giaNhap) { this.giaNhap = giaNhap; }
    /** Hàm `getGiaBan`: lấy `Gia` `Ban`. */
    public BigDecimal getGiaBan() { return giaBan; }
    /** Hàm `setGiaBan`: gán `Gia` `Ban`. */
    public void setGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; }
}
