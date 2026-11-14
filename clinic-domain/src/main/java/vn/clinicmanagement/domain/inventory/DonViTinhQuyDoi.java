package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.math.BigDecimal;

public class DonViTinhQuyDoi extends CoSoThucThe<Integer> {
    /** Trường `maItem`: `ma` mathang. */
    private int maItem;
    private String tenDonVi = "";
    /** Trường `tyLeQuyDoi`: `ty` `Le` quy doi. */
    private BigDecimal tyLeQuyDoi = BigDecimal.ONE;
    private BigDecimal giaBan = BigDecimal.ZERO;

    public DonViTinhQuyDoi() {}
    /** Hàm `getMaItem`: lấy mã mathang. */
    public int getMaItem() { return maItem; }
    /** Hàm `setMaItem`: gán mã mathang. */
    public void setMaItem(int maItem) { this.maItem = maItem; }
    /** Hàm `getTenDonVi`: lấy `Ten` don vi. */
    public String getTenDonVi() { return tenDonVi; }
    /** Hàm `setTenDonVi`: gán `Ten` don vi. */
    public void setTenDonVi(String tenDonVi) { this.tenDonVi = tenDonVi; }
    /** Hàm `getTyLeQuyDoi`: lấy `Ty` `Le` quy doi. */
    public BigDecimal getTyLeQuyDoi() { return tyLeQuyDoi; }
    /** Hàm `setTyLeQuyDoi`: gán `Ty` `Le` quy doi. */
    public void setTyLeQuyDoi(BigDecimal tyLeQuyDoi) { this.tyLeQuyDoi = tyLeQuyDoi; }
    /** Hàm `getGiaBan`: lấy `Gia` `Ban`. */
    public BigDecimal getGiaBan() { return giaBan; }
    /** Hàm `setGiaBan`: gán `Gia` `Ban`. */
    public void setGiaBan(BigDecimal giaBan) { this.giaBan = giaBan; }
}
