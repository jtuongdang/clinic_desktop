package vn.clinicmanagement.domain.system;
import vn.clinicmanagement.domain.common.BaseEntity;
import java.time.LocalDateTime;

public class NhatKyHoSoBenhAn extends BaseEntity<Integer> {
    private int maLuotKham;
    private int maNhanVien;
    private String hanhDong = "";
    private String lyDo;
    private LocalDateTime thoiGian = LocalDateTime.now();
    public NhatKyHoSoBenhAn() {}
    public int getMaLuotKham() { return maLuotKham; }
    public void setMaLuotKham(int maLuotKham) { this.maLuotKham = maLuotKham; }
    public int getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    public String getHanhDong() { return hanhDong; }
    public void setHanhDong(String hanhDong) { this.hanhDong = hanhDong; }
    public String getLyDo() { return lyDo; }
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }
    public LocalDateTime getThoiGian() { return thoiGian; }
    public void setThoiGian(LocalDateTime thoiGian) { this.thoiGian = thoiGian; }
}
