package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.BaseEntity;
import java.time.LocalTime;

public class CaTruc extends BaseEntity<Integer> {
    private int maLoaiCa;
    private String tenCa = "";
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;
    private boolean hoatDong = true;
    public CaTruc() {}
    public int getMaLoaiCa() { return maLoaiCa; }
    public void setMaLoaiCa(int maLoaiCa) { this.maLoaiCa = maLoaiCa; }
    public String getTenCa() { return tenCa; }
    public void setTenCa(String tenCa) { this.tenCa = tenCa; }
    public LocalTime getGioBatDau() { return gioBatDau; }
    public void setGioBatDau(LocalTime gioBatDau) { this.gioBatDau = gioBatDau; }
    public LocalTime getGioKetThuc() { return gioKetThuc; }
    public void setGioKetThuc(LocalTime gioKetThuc) { this.gioKetThuc = gioKetThuc; }
    public boolean isHoatDong() { return hoatDong; }
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
}
