package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;

public class NhanVienVaiTro extends CoSoThucThe<Integer> {
    private int maNhanVien;
    private int maVaiTro;
    public NhanVienVaiTro() {}
    public int getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    public int getMaVaiTro() { return maVaiTro; }
    public void setMaVaiTro(int maVaiTro) { this.maVaiTro = maVaiTro; }
}
