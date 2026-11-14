package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.math.BigDecimal;

public class KiemKeDem extends CoSoThucThe<Integer> {
    /** Trường `maStocktake`: `ma` kiemke. */
    private int maStocktake;
    private int maTonKho;
    private BigDecimal soLuongSoSach = BigDecimal.ZERO;
    private BigDecimal soLuongThucTe = BigDecimal.ZERO;
    private BigDecimal chenhLech = BigDecimal.ZERO;

    public KiemKeDem() {}
    /** Hàm `getMaStocktake`: lấy mã kiemke. */
    public int getMaStocktake() { return maStocktake; }
    /** Hàm `setMaStocktake`: gán mã kiemke. */
    public void setMaStocktake(int maStocktake) { this.maStocktake = maStocktake; }
    /** Hàm `getMaTonKho`: lấy mã ton kho. */
    public int getMaTonKho() { return maTonKho; }
    /** Hàm `setMaTonKho`: gán mã ton kho. */
    public void setMaTonKho(int maTonKho) { this.maTonKho = maTonKho; }
    /** Hàm `getSoLuongSoSach`: lấy so luong so `Sach`. */
    public BigDecimal getSoLuongSoSach() { return soLuongSoSach; }
    /** Hàm `setSoLuongSoSach`: gán so luong so `Sach`. */
    public void setSoLuongSoSach(BigDecimal soLuongSoSach) { this.soLuongSoSach = soLuongSoSach; }
    /** Hàm `getSoLuongThucTe`: lấy so luong thuc `Te`. */
    public BigDecimal getSoLuongThucTe() { return soLuongThucTe; }
    /** Hàm `setSoLuongThucTe`: gán so luong thuc `Te`. */
    public void setSoLuongThucTe(BigDecimal soLuongThucTe) { this.soLuongThucTe = soLuongThucTe; }
    /** Hàm `getChenhLech`: lấy `Chenh` `Lech`. */
    public BigDecimal getChenhLech() { return chenhLech; }
    /** Hàm `setChenhLech`: gán `Chenh` `Lech`. */
    public void setChenhLech(BigDecimal chenhLech) { this.chenhLech = chenhLech; }
}
