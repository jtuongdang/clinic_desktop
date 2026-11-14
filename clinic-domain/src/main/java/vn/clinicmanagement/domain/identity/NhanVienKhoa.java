package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;

public class NhanVienKhoa extends CoSoThucThe<Integer> {
    private int maNhanVien;
    private int maKhoa;
    public NhanVienKhoa() {}
    /** Hàm `getMaNhanVien`: lấy mã nhãn vien. */
    public int getMaNhanVien() { return maNhanVien; }
    /** Hàm `setMaNhanVien`: gán mã nhãn vien. */
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    /** Hàm `getMaKhoa`: lấy mã khoa. */
    public int getMaKhoa() { return maKhoa; }
    /** Hàm `setMaKhoa`: gán mã khoa. */
    public void setMaKhoa(int maKhoa) { this.maKhoa = maKhoa; }
}
