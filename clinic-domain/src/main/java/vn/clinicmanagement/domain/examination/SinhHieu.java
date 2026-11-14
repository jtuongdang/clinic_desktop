package vn.clinicmanagement.domain.examination;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.math.BigDecimal;

public class SinhHieu extends CoSoThucThe<Integer> {
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
    /** Hàm `getMaLuotKham`: lấy mã luot kham. */
    public int getMaLuotKham() { return maLuotKham; }
    /** Hàm `setMaLuotKham`: gán mã luot kham. */
    public void setMaLuotKham(int maLuotKham) { this.maLuotKham = maLuotKham; }
    /** Hàm `getMach`: lấy `Mach`. */
    public Integer getMach() { return mach; }
    /** Hàm `setMach`: gán `Mach`. */
    public void setMach(Integer mach) { this.mach = mach; }
    /** Hàm `getNhietDo`: lấy `Nhiet` `Do`. */
    public BigDecimal getNhietDo() { return nhietDo; }
    /** Hàm `setNhietDo`: gán `Nhiet` `Do`. */
    public void setNhietDo(BigDecimal nhietDo) { this.nhietDo = nhietDo; }
    /** Hàm `getHuyetApTamThu`: lấy `Huyet` `Ap` `Tam` thu. */
    public Integer getHuyetApTamThu() { return huyetApTamThu; }
    /** Hàm `setHuyetApTamThu`: gán `Huyet` `Ap` `Tam` thu. */
    public void setHuyetApTamThu(Integer huyetApTamThu) { this.huyetApTamThu = huyetApTamThu; }
    /** Hàm `getHuyetApTamTruong`: lấy `Huyet` `Ap` `Tam` `Truong`. */
    public Integer getHuyetApTamTruong() { return huyetApTamTruong; }
    /** Hàm `setHuyetApTamTruong`: gán `Huyet` `Ap` `Tam` `Truong`. */
    public void setHuyetApTamTruong(Integer huyetApTamTruong) { this.huyetApTamTruong = huyetApTamTruong; }
    /** Hàm `getNhipTho`: lấy `Nhip` `Tho`. */
    public Integer getNhipTho() { return nhipTho; }
    /** Hàm `setNhipTho`: gán `Nhip` `Tho`. */
    public void setNhipTho(Integer nhipTho) { this.nhipTho = nhipTho; }
    /** Hàm `getCanNang`: lấy có thể `Nang`. */
    public BigDecimal getCanNang() { return canNang; }
    /** Hàm `setCanNang`: gán có thể `Nang`. */
    public void setCanNang(BigDecimal canNang) { this.canNang = canNang; }
    /** Hàm `getChieuCao`: lấy `Chieu` `Cao`. */
    public BigDecimal getChieuCao() { return chieuCao; }
    /** Hàm `setChieuCao`: gán `Chieu` `Cao`. */
    public void setChieuCao(BigDecimal chieuCao) { this.chieuCao = chieuCao; }
    /** Hàm `getBmi`: lấy `Bmi`. */
    public BigDecimal getBmi() { return bmi; }
    /** Hàm `setBmi`: gán `Bmi`. */
    public void setBmi(BigDecimal bmi) { this.bmi = bmi; }
    /** Hàm `getSpo2`: lấy `Spo` `2`. */
    public BigDecimal getSpo2() { return spo2; }
    /** Hàm `setSpo2`: gán `Spo` `2`. */
    public void setSpo2(BigDecimal spo2) { this.spo2 = spo2; }
    /** Hàm `getLuotKham`: lấy luot kham. */
    public LuotKham getLuotKham() { return luotKham; }
    /** Hàm `setLuotKham`: gán luot kham. */
    public void setLuotKham(LuotKham luotKham) { this.luotKham = luotKham; }
}
