package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;

public class LoaiCa extends CoSoThucThe<Integer> {
    private String tenLoaiCa = "";
    private String moTa;
    public LoaiCa() {}
    public String getTenLoaiCa() { return tenLoaiCa; }
    public void setTenLoaiCa(String tenLoaiCa) { this.tenLoaiCa = tenLoaiCa; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
}
