package vn.clinicmanagement.domain.service;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.inventory.Item;
import java.math.BigDecimal;

public class DinhMucVatTuDichVu extends BaseEntity<Integer> {
    private int maDichVu;
    private int maItem;
    private BigDecimal soLuongDinhMuc = BigDecimal.ONE;
    private DanhMucDichVu danhMucDichVu;
    private Item item;

    public DinhMucVatTuDichVu() {}
    public int getMaDichVu() { return maDichVu; }
    public void setMaDichVu(int maDichVu) { this.maDichVu = maDichVu; }
    public int getMaItem() { return maItem; }
    public void setMaItem(int maItem) { this.maItem = maItem; }
    public BigDecimal getSoLuongDinhMuc() { return soLuongDinhMuc; }
    public void setSoLuongDinhMuc(BigDecimal soLuongDinhMuc) { this.soLuongDinhMuc = soLuongDinhMuc; }
    public DanhMucDichVu getDanhMucDichVu() { return danhMucDichVu; }
    public void setDanhMucDichVu(DanhMucDichVu danhMucDichVu) { this.danhMucDichVu = danhMucDichVu; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
}
