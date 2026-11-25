package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;

public class VaiTro extends CoSoThucThe<Integer> {
    private String tenVaiTro = "";
    private String moTa;
    public VaiTro() {}
    public String getTenVaiTro() { return tenVaiTro; }
    public void setTenVaiTro(String tenVaiTro) { this.tenVaiTro = tenVaiTro; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
}
