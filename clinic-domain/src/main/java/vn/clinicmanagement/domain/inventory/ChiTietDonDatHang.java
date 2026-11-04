package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.BaseEntity;
import java.math.BigDecimal;

public class ChiTietDonDatHang extends BaseEntity<Integer> {
    private int maDonDatHang;
    private int maItem;
    private BigDecimal soLuongDat = BigDecimal.ZERO;
    private BigDecimal soLuongDaNhan = BigDecimal.ZERO;
    private BigDecimal donGia = BigDecimal.ZERO;

    public ChiTietDonDatHang() {}
    public int getMaDonDatHang() { return maDonDatHang; }
    public void setMaDonDatHang(int maDonDatHang) { this.maDonDatHang = maDonDatHang; }
    public int getMaItem() { return maItem; }
    public void setMaItem(int maItem) { this.maItem = maItem; }
    public BigDecimal getSoLuongDat() { return soLuongDat; }
    public void setSoLuongDat(BigDecimal soLuongDat) { this.soLuongDat = soLuongDat; }
    public BigDecimal getSoLuongDaNhan() { return soLuongDaNhan; }
    public void setSoLuongDaNhan(BigDecimal soLuongDaNhan) { this.soLuongDaNhan = soLuongDaNhan; }
    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
}
