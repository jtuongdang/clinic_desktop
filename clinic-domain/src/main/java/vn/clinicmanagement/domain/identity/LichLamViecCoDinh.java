package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.BaseEntity;

public class LichLamViecCoDinh extends BaseEntity<Integer> {
    private int maNhanVien;
    private int thuTrongTuan;
    private int maCaTruc;
    private int maCoSo;
    private Integer maKhoa;
    private Integer maPhongKham;
    private boolean hoatDong = true;
    public LichLamViecCoDinh() {}
    public int getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    public int getThuTrongTuan() { return thuTrongTuan; }
    public void setThuTrongTuan(int thuTrongTuan) { this.thuTrongTuan = thuTrongTuan; }
    public int getMaCaTruc() { return maCaTruc; }
    public void setMaCaTruc(int maCaTruc) { this.maCaTruc = maCaTruc; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public Integer getMaKhoa() { return maKhoa; }
    public void setMaKhoa(Integer maKhoa) { this.maKhoa = maKhoa; }
    public Integer getMaPhongKham() { return maPhongKham; }
    public void setMaPhongKham(Integer maPhongKham) { this.maPhongKham = maPhongKham; }
    public boolean isHoatDong() { return hoatDong; }
    public void setHoatDong(boolean hoatDong) { this.hoatDong = hoatDong; }
}
