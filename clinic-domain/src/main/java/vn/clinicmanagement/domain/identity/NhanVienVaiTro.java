package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.BaseEntity;

public class NhanVienVaiTro extends BaseEntity<Integer> {
    private int maNhanVien;
    private int maVaiTro;
    public NhanVienVaiTro() {}
    public int getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    public int getMaVaiTro() { return maVaiTro; }
    public void setMaVaiTro(int maVaiTro) { this.maVaiTro = maVaiTro; }
}
