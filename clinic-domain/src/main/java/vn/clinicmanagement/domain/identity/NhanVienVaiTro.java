package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;

public class NhanVienVaiTro extends CoSoThucThe<Integer> {
    private int maNhanVien;
    private int maVaiTro;
    public NhanVienVaiTro() {}
    /** Hàm `getMaNhanVien`: lấy mã nhãn vien. */
    public int getMaNhanVien() { return maNhanVien; }
    /** Hàm `setMaNhanVien`: gán mã nhãn vien. */
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    /** Hàm `getMaVaiTro`: lấy mã vai tro. */
    public int getMaVaiTro() { return maVaiTro; }
    /** Hàm `setMaVaiTro`: gán mã vai tro. */
    public void setMaVaiTro(int maVaiTro) { this.maVaiTro = maVaiTro; }
}
