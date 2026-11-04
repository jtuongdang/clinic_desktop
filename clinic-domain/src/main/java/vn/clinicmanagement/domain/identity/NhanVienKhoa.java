package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.BaseEntity;

public class NhanVienKhoa extends BaseEntity<Integer> {
    private int maNhanVien;
    private int maKhoa;
    public NhanVienKhoa() {}
    public int getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    public int getMaKhoa() { return maKhoa; }
    public void setMaKhoa(int maKhoa) { this.maKhoa = maKhoa; }
}
