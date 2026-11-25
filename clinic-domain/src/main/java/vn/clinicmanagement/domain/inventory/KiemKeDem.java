package vn.clinicmanagement.domain.inventory;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.math.BigDecimal;

public class KiemKeDem extends CoSoThucThe<Integer> {
    private int maStocktake;
    private int maTonKho;
    private BigDecimal soLuongSoSach = BigDecimal.ZERO;
    private BigDecimal soLuongThucTe = BigDecimal.ZERO;
    private BigDecimal chenhLech = BigDecimal.ZERO;

    public KiemKeDem() {}
    public int getMaStocktake() { return maStocktake; }
    public void setMaStocktake(int maStocktake) { this.maStocktake = maStocktake; }
    public int getMaTonKho() { return maTonKho; }
    public void setMaTonKho(int maTonKho) { this.maTonKho = maTonKho; }
    public BigDecimal getSoLuongSoSach() { return soLuongSoSach; }
    public void setSoLuongSoSach(BigDecimal soLuongSoSach) { this.soLuongSoSach = soLuongSoSach; }
    public BigDecimal getSoLuongThucTe() { return soLuongThucTe; }
    public void setSoLuongThucTe(BigDecimal soLuongThucTe) { this.soLuongThucTe = soLuongThucTe; }
    public BigDecimal getChenhLech() { return chenhLech; }
    public void setChenhLech(BigDecimal chenhLech) { this.chenhLech = chenhLech; }
}
