package vn.clinicmanagement.domain.system;
import vn.clinicmanagement.domain.common.BaseEntity;

public class ThamSoHeThong extends BaseEntity<Integer> {
    private String maThamSo = "";
    private String tenThamSo = "";
    private String giaTri = "";
    private String moTa;
    public ThamSoHeThong() {}
    public String getMaThamSo() { return maThamSo; }
    public void setMaThamSo(String maThamSo) { this.maThamSo = maThamSo; }
    public String getTenThamSo() { return tenThamSo; }
    public void setTenThamSo(String tenThamSo) { this.tenThamSo = tenThamSo; }
    public String getGiaTri() { return giaTri; }
    public void setGiaTri(String giaTri) { this.giaTri = giaTri; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
}
