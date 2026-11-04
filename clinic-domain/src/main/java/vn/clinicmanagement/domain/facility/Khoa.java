package vn.clinicmanagement.domain.facility;
import vn.clinicmanagement.domain.common.BaseEntity;
import java.util.ArrayList;
import java.util.List;

public class Khoa extends BaseEntity<Integer> {
    private int maCoSo;
    private String tenKhoa = "";
    private String moTa;
    private boolean hoatDong = true;
    private CoSo coSo;
    private List<PhongKham> phongKhams = new ArrayList<>();

    public Khoa() {}
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public String getTenKhoa() { return tenKhoa; }
    public void setTenKhoa(String tenKhoa) { this.tenKhoa = tenKhoa; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public boolean isHoatDong() { return hoatDong; }
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
    public CoSo getCoSo() { return coSo; }
    public void setCoSo(CoSo coSo) { this.coSo = coSo; }
    public List<PhongKham> getPhongKhams() { return phongKhams; }
    public void setPhongKhams(List<PhongKham> phongKhams) { this.phongKhams = phongKhams; }
}
