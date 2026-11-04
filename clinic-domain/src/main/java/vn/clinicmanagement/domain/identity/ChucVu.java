package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.BaseEntity;

public class ChucVu extends BaseEntity<Integer> {
    private String tenChucVu = "";
    private String moTa;
    public ChucVu() {}
    public String getTenChucVu() { return tenChucVu; }
    public void setTenChucVu(String tenChucVu) { this.tenChucVu = tenChucVu; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
}
