package vn.clinicmanagement.domain.examination;
import vn.clinicmanagement.domain.common.BaseEntity;
import java.math.BigDecimal;

public class SinhHieu extends BaseEntity<Integer> {
    private int maLuotKham;
    private Integer mach;
    private BigDecimal nhietDo;
    private Integer huyetApTamThu;
    private Integer huyetApTamTruong;
    private Integer nhipTho;
    private BigDecimal canNang;
    private BigDecimal chieuCao;
    private BigDecimal bmi;
    private BigDecimal spo2;
    private LuotKham luotKham;

    public SinhHieu() {}
    public int getMaLuotKham() { return maLuotKham; }
    public void setMaLuotKham(int maLuotKham) { this.maLuotKham = maLuotKham; }
    public Integer getMach() { return mach; }
    public void setMach(Integer mach) { this.mach = mach; }
    public BigDecimal getNhietDo() { return nhietDo; }
    public void setNhietDo(BigDecimal nhietDo) { this.nhietDo = nhietDo; }
    public Integer getHuyetApTamThu() { return huyetApTamThu; }
    public void setHuyetApTamThu(Integer huyetApTamThu) { this.huyetApTamThu = huyetApTamThu; }
    public Integer getHuyetApTamTruong() { return huyetApTamTruong; }
    public void setHuyetApTamTruong(Integer huyetApTamTruong) { this.huyetApTamTruong = huyetApTamTruong; }
    public Integer getNhipTho() { return nhipTho; }
    public void setNhipTho(Integer nhipTho) { this.nhipTho = nhipTho; }
    public BigDecimal getCanNang() { return canNang; }
    public void setCanNang(BigDecimal canNang) { this.canNang = canNang; }
    public BigDecimal getChieuCao() { return chieuCao; }
    public void setChieuCao(BigDecimal chieuCao) { this.chieuCao = chieuCao; }
    public BigDecimal getBmi() { return bmi; }
    public void setBmi(BigDecimal bmi) { this.bmi = bmi; }
    public BigDecimal getSpo2() { return spo2; }
    public void setSpo2(BigDecimal spo2) { this.spo2 = spo2; }
    public LuotKham getLuotKham() { return luotKham; }
    public void setLuotKham(LuotKham luotKham) { this.luotKham = luotKham; }
}
