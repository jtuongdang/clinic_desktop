package vn.clinicmanagement.domain.service;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.inventory.MatHang;
import java.math.BigDecimal;

public class DinhMucVatTuDichVu extends CoSoThucThe<Integer> {
    private int maDichVu;
    private int maItem;
    private BigDecimal soLuongDinhMuc = BigDecimal.ONE;
    private DanhMucDichVu danhMucDichVu;
    private MatHang item;

    public DinhMucVatTuDichVu() {}
    public int getMaDichVu() { return maDichVu; }
    public void setMaDichVu(int maDichVu) { this.maDichVu = maDichVu; }
    public int getMaItem() { return maItem; }
    public void setMaItem(int maItem) { this.maItem = maItem; }
    public BigDecimal getSoLuongDinhMuc() { return soLuongDinhMuc; }
    public void setSoLuongDinhMuc(BigDecimal soLuongDinhMuc) { this.soLuongDinhMuc = soLuongDinhMuc; }
    public DanhMucDichVu getDanhMucDichVu() { return danhMucDichVu; }
    public void setDanhMucDichVu(DanhMucDichVu danhMucDichVu) { this.danhMucDichVu = danhMucDichVu; }
    public MatHang getItem() { return item; }
    public void setItem(MatHang item) { this.item = item; }
}
