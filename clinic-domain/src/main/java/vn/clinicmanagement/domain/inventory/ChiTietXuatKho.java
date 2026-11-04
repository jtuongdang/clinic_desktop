package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.BaseEntity;
import java.math.BigDecimal;

public class ChiTietXuatKho extends BaseEntity<Integer> {
    private int maPhieuXuat;
    private int maItem;
    private int maTonKho;
    private BigDecimal soLuong = BigDecimal.ZERO;
    private BigDecimal donGia = BigDecimal.ZERO;

    public ChiTietXuatKho() {}
    public int getMaPhieuXuat() { return maPhieuXuat; }
    public void setMaPhieuXuat(int maPhieuXuat) { this.maPhieuXuat = maPhieuXuat; }
    public int getMaItem() { return maItem; }
    public void setMaItem(int maItem) { this.maItem = maItem; }
    public int getMaTonKho() { return maTonKho; }
    public void setMaTonKho(int maTonKho) { this.maTonKho = maTonKho; }
    public BigDecimal getSoLuong() { return soLuong; }
    public void setSoLuong(BigDecimal soLuong) { this.soLuong = soLuong; }
    public BigDecimal getDonGia() { return donGia; }
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
}
