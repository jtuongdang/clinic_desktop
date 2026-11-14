package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.math.BigDecimal;

public class ChiTietXuatKho extends CoSoThucThe<Integer> {
    private int maPhieuXuat;
    /** Trường `maItem`: `ma` mathang. */
    private int maItem;
    private int maTonKho;
    private BigDecimal soLuong = BigDecimal.ZERO;
    private BigDecimal donGia = BigDecimal.ZERO;

    public ChiTietXuatKho() {}
    /** Hàm `getMaPhieuXuat`: lấy mã phiếu xuất. */
    public int getMaPhieuXuat() { return maPhieuXuat; }
    /** Hàm `setMaPhieuXuat`: gán mã phiếu xuất. */
    public void setMaPhieuXuat(int maPhieuXuat) { this.maPhieuXuat = maPhieuXuat; }
    /** Hàm `getMaItem`: lấy mã mathang. */
    public int getMaItem() { return maItem; }
    /** Hàm `setMaItem`: gán mã mathang. */
    public void setMaItem(int maItem) { this.maItem = maItem; }
    /** Hàm `getMaTonKho`: lấy mã ton kho. */
    public int getMaTonKho() { return maTonKho; }
    /** Hàm `setMaTonKho`: gán mã ton kho. */
    public void setMaTonKho(int maTonKho) { this.maTonKho = maTonKho; }
    /** Hàm `getSoLuong`: lấy so luong. */
    public BigDecimal getSoLuong() { return soLuong; }
    /** Hàm `setSoLuong`: gán so luong. */
    public void setSoLuong(BigDecimal soLuong) { this.soLuong = soLuong; }
    /** Hàm `getDonGia`: lấy don `Gia`. */
    public BigDecimal getDonGia() { return donGia; }
    /** Hàm `setDonGia`: gán don `Gia`. */
    public void setDonGia(BigDecimal donGia) { this.donGia = donGia; }
}
