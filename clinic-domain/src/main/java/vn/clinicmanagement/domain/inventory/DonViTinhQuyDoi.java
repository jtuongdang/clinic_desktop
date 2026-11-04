package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.BaseEntity;
import java.math.BigDecimal;

public class DonViTinhQuyDoi extends BaseEntity<Integer> {
    private int maItem;
    private String tenDonVi = "";
    private BigDecimal tyLeQuyDoi = BigDecimal.ONE;
    private BigDecimal giaBan = BigDecimal.ZERO;

    public DonViTinhQuyDoi() {}
    public int getMaItem() { return maItem; }
    public void setMaItem(int maItem) { this.maItem = maItem; }
    public String getTenDonVi() { return tenDonVi; }
    public void setTenDonVi(String tenDonVi) { this.tenDonVi = tenDonVi; }
    public BigDecimal getTyLeQuyDoi() { return tyLeQuyDoi; }
    public void setTyLeQuyDoi(BigDecimal tyLeQuyDoi) { this.tyLeQuyDoi = tyLeQuyDoi; }
    public BigDecimal getGiaBan() { return giaBan; }
    public void setGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; }
}
