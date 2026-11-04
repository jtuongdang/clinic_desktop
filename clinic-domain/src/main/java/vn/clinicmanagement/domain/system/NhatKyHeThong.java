package vn.clinicmanagement.domain.system;
import vn.clinicmanagement.domain.common.BaseEntity;
import java.time.LocalDateTime;

public class NhatKyHeThong extends BaseEntity<Integer> {
    private Integer maNhanVien;
    private String hanhDong = "";
    private String chiTiet = "";
    private String ipAddress;
    private LocalDateTime thoiGian = LocalDateTime.now();
    public NhatKyHeThong() {}
    public Integer getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(Integer maNhanVien) { this.maNhanVien = maNhanVien; }
    public String getHanhDong() { return hanhDong; }
    public void setHanhDong(String hanhDong) { this.hanhDong = hanhDong; }
    public String getChiTiet() { return chiTiet; }
    public void setChiTiet(String chiTiet) { this.chiTiet = chiTiet; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getThoiGian() { return thoiGian; }
    public void setThoiGian(LocalDateTime thoiGian) { this.thoiGian = thoiGian; }
}
