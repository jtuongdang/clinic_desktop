package vn.clinicmanagement.domain.service;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.inventory.MatHang;
import java.math.BigDecimal;

public class DinhMucVatTuDichVu extends CoSoThucThe<Integer> {
    private int maDichVu;
    /** Trường `maItem`: `ma` mathang. */
    private int maItem;
    private BigDecimal soLuongDinhMuc = BigDecimal.ONE;
    private DanhMucDichVu danhMucDichVu;
    /** Trường `item`: mathang. */
    private MatHang item;

    public DinhMucVatTuDichVu() {}
    /** Hàm `getMaDichVu`: lấy mã dich vu. */
    public int getMaDichVu() { return maDichVu; }
    /** Hàm `setMaDichVu`: gán mã dich vu. */
    public void setMaDichVu(int maDichVu) { this.maDichVu = maDichVu; }
    /** Hàm `getMaItem`: lấy mã mathang. */
    public int getMaItem() { return maItem; }
    /** Hàm `setMaItem`: gán mã mathang. */
    public void setMaItem(int maItem) { this.maItem = maItem; }
    /** Hàm `getSoLuongDinhMuc`: lấy so luong dinh muc. */
    public BigDecimal getSoLuongDinhMuc() { return soLuongDinhMuc; }
    /** Hàm `setSoLuongDinhMuc`: gán so luong dinh muc. */
    public void setSoLuongDinhMuc(BigDecimal soLuongDinhMuc) { this.soLuongDinhMuc = soLuongDinhMuc; }
    /** Hàm `getDanhMucDichVu`: lấy danh muc dich vu. */
    public DanhMucDichVu getDanhMucDichVu() { return danhMucDichVu; }
    /** Hàm `setDanhMucDichVu`: gán danh muc dich vu. */
    public void setDanhMucDichVu(DanhMucDichVu danhMucDichVu) { this.danhMucDichVu = danhMucDichVu; }
    /** Hàm `getItem`: lấy mathang. */
    public MatHang getItem() { return item; }
    /** Hàm `setItem`: gán mathang. */
    public void setItem(MatHang item) { this.item = item; }
}
