package vn.clinicmanagement.domain.examination;
import vn.clinicmanagement.domain.common.BaseEntity;

public class ICD extends BaseEntity<Integer> {
    private String maICD = "";
    private String tenBenh = "";
    private String moTa;
    private boolean hoatDong = true;

    public ICD() {}
    public String getMaICD() { return maICD; }
    public void setMaICD(String maICD) { this.maICD = maICD; }
    public String getTenBenh() { return tenBenh; }
    public void setTenBenh(String tenBenh) { this.tenBenh = tenBenh; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public boolean isHoatDong() { return hoatDong; }
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
}
