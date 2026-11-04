package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.enums.LoaiGiaoDichKho;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InventoryTransaction extends BaseEntity<Integer> {
    private int maKho;
    private int maItem;
    private int maTonKho;
    private LoaiGiaoDichKho loaiGiaoDich = LoaiGiaoDichKho.NHAP;
    private BigDecimal soLuongThayDoi = BigDecimal.ZERO;
    private BigDecimal tonTruocKhiThayDoi = BigDecimal.ZERO;
    private BigDecimal tonSauKhiThayDoi = BigDecimal.ZERO;
    private LocalDateTime thoiGian = LocalDateTime.now();
    private String maChungTu;

    public InventoryTransaction() {}
    public int getMaKho() { return maKho; }
    public void setMaKho(int maKho) { this.maKho = maKho; }
    public int getMaItem() { return maItem; }
    public void setMaItem(int maItem) { this.maItem = maItem; }
    public int getMaTonKho() { return maTonKho; }
    public void setMaTonKho(int maTonKho) { this.maTonKho = maTonKho; }
    public LoaiGiaoDichKho getLoaiGiaoDich() { return loaiGiaoDich; }
    public void setLoaiGiaoDich(LoaiGiaoDichKho loaiGiaoDich) { this.loaiGiaoDich = loaiGiaoDich; }
    public BigDecimal getSoLuongThayDoi() { return soLuongThayDoi; }
    public void setSoLuongThayDoi(BigDecimal soLuongThayDoi) { this.soLuongThayDoi = soLuongThayDoi; }
    public BigDecimal getTonTruocKhiThayDoi() { return tonTruocKhiThayDoi; }
    public void setTonTruocKhiThayDoi(BigDecimal tonTruocKhiThayDoi) { this.tonTruocKhiThayDoi = tonTruocKhiThayDoi; }
    public BigDecimal getTonSauKhiThayDoi() { return tonSauKhiThayDoi; }
    public void setTonSauKhiThayDoi(BigDecimal tonSauKhiThayDoi) { this.tonSauKhiThayDoi = tonSauKhiThayDoi; }
    public LocalDateTime getThoiGian() { return thoiGian; }
    public void setThoiGian(LocalDateTime thoiGian) { this.thoiGian = thoiGian; }
    public String getMaChungTu() { return maChungTu; }
    public void setMaChungTu(String maChungTu) { this.maChungTu = maChungTu; }
}
