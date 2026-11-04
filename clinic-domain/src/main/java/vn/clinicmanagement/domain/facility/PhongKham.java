package vn.clinicmanagement.domain.facility;
import vn.clinicmanagement.domain.common.BaseEntity;

public class PhongKham extends BaseEntity<Integer> {
    private int maKhoa;
    private String tenPhong = "";
    private String viTri;
    private boolean hoatDong = true;
    private Khoa khoa;

    public PhongKham() {}
    public int getMaKhoa() { return maKhoa; }
    public void setMaKhoa(int maKhoa) { this.maKhoa = maKhoa; }
    public String getTenPhong() { return tenPhong; }
    public void setTenPhong(String tenPhong) { this.tenPhong = tenPhong; }
    public String getViTri() { return viTri; }
    public void setViTri(String viTri) { this.viTri = viTri; }
    public boolean isHoatDong() { return hoatDong; }
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
    public Khoa getKhoa() { return khoa; }
    public void setKhoa(Khoa khoa) { this.khoa = khoa; }
}
