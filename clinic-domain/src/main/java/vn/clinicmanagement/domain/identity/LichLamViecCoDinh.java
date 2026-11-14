package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;

public class LichLamViecCoDinh extends CoSoThucThe<Integer> {
    private int maNhanVien;
    private int thuTrongTuan;
    private int maCaTruc;
    private int maCoSo;
    private Integer maKhoa;
    private Integer maPhongKham;
    private boolean hoatDong = true;
    public LichLamViecCoDinh() {}
    /** Hàm `getMaNhanVien`: lấy mã nhãn vien. */
    public int getMaNhanVien() { return maNhanVien; }
    /** Hàm `setMaNhanVien`: gán mã nhãn vien. */
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    /** Hàm `getThuTrongTuan`: lấy thu `Trong` `Tuan`. */
    public int getThuTrongTuan() { return thuTrongTuan; }
    /** Hàm `setThuTrongTuan`: gán thu `Trong` `Tuan`. */
    public void setThuTrongTuan(int thuTrongTuan) { this.thuTrongTuan = thuTrongTuan; }
    /** Hàm `getMaCaTruc`: lấy mã ca truc. */
    public int getMaCaTruc() { return maCaTruc; }
    /** Hàm `setMaCaTruc`: gán mã ca truc. */
    public void setMaCaTruc(int maCaTruc) { this.maCaTruc = maCaTruc; }
    /** Hàm `getMaCoSo`: lấy mã có so. */
    public int getMaCoSo() { return maCoSo; }
    /** Hàm `setMaCoSo`: gán mã có so. */
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    /** Hàm `getMaKhoa`: lấy mã khoa. */
    public Integer getMaKhoa() { return maKhoa; }
    /** Hàm `setMaKhoa`: gán mã khoa. */
    public void setMaKhoa(Integer maKhoa) { this.maKhoa = maKhoa; }
    /** Hàm `getMaPhongKham`: lấy mã phòng kham. */
    public Integer getMaPhongKham() { return maPhongKham; }
    /** Hàm `setMaPhongKham`: gán mã phòng kham. */
    public void setMaPhongKham(Integer maPhongKham) { this.maPhongKham = maPhongKham; }
    /** Hàm `isHoatDong`: là `Hoat` đóng. */
    public boolean isHoatDong() { return hoatDong; }
    /** Hàm `setHoatDong`: gán `Hoat` đóng. */
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
}
