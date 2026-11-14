package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.math.BigDecimal;

public class ChiTietDonDatHang extends CoSoThucThe<Integer> {
    private int maDonDatHang;
    /** Trường `maItem`: `ma` mathang. */
    private int maItem;
    private BigDecimal soLuongDat = BigDecimal.ZERO;
    private BigDecimal soLuongDaNhan = BigDecimal.ZERO;
    private BigDecimal donGia = BigDecimal.ZERO;

    public ChiTietDonDatHang() {}
    /** Hàm `getMaDonDatHang`: lấy mã don đặt hang. */
    public int getMaDonDatHang() { return maDonDatHang; }
    /** Hàm `setMaDonDatHang`: gán mã don đặt hang. */
    public void setMaDonDatHang(int maDonDatHang) { this.maDonDatHang = maDonDatHang; }
    /** Hàm `getMaItem`: lấy mã mathang. */
    public int getMaItem() { return maItem; }
    /** Hàm `setMaItem`: gán mã mathang. */
    public void setMaItem(int maItem) { this.maItem = maItem; }
    /** Hàm `getSoLuongDat`: lấy so luong đặt. */
    public BigDecimal getSoLuongDat() { return soLuongDat; }
    /** Hàm `setSoLuongDat`: gán so luong đặt. */
    public void setSoLuongDat(BigDecimal soLuongDat) { this.soLuongDat = soLuongDat; }
    /** Hàm `getSoLuongDaNhan`: lấy so luong `Da` nhãn. */
    public BigDecimal getSoLuongDaNhan() { return soLuongDaNhan; }
    /** Hàm `setSoLuongDaNhan`: gán so luong `Da` nhãn. */
    public void setSoLuongDaNhan(BigDecimal soLuongDaNhan) { this.soLuongDaNhan = soLuongDaNhan; }
    /** Hàm `getDonGia`: lấy don `Gia`. */
    public BigDecimal getDonGia() { return donGia; }
    /** Hàm `setDonGia`: gán don `Gia`. */
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
}
