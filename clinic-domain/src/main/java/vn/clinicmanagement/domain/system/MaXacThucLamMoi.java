package vn.clinicmanagement.domain.system;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.time.LocalDateTime;

public class MaXacThucLamMoi extends CoSoThucThe<Integer> {
    private int maNhanVien;
    /** Trường `token`: maxacthuc. */
    private String token = "";
    private LocalDateTime hetHan;
    private boolean daHuy = false;
    private String lyDoHuy;
    private String ipTao;
    private String thietBi;

    public MaXacThucLamMoi() {}
    public boolean daHetHan() { return LocalDateTime.now().isAfter(hetHan); }
    /** Hàm `getMaNhanVien`: lấy mã nhãn vien. */
    public int getMaNhanVien() { return maNhanVien; }
    /** Hàm `setMaNhanVien`: gán mã nhãn vien. */
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    /** Hàm `getToken`: lấy maxacthuc. */
    public String getToken() { return token; }
    /** Hàm `setToken`: gán maxacthuc. */
    public void setToken(String token) { this.token = token; }
    /** Hàm `getHetHan`: lấy `Het` han. */
    public LocalDateTime getHetHan() { return hetHan; }
    /** Hàm `setHetHan`: gán `Het` han. */
    public void setHetHan(LocalDateTime hetHan) { this.hetHan = hetHan; }
    /** Hàm `isDaHuy`: là `Da` `Huy`. */
    public boolean isDaHuy() { return daHuy; }
    /** Hàm `setDaHuy`: gán `Da` `Huy`. */
    public void setDaHuy(boolean daHuy) { this.daHuy = daHuy; }
    /** Hàm `getLyDoHuy`: lấy `Ly` `Do` `Huy`. */
    public String getLyDoHuy() { return lyDoHuy; }
    /** Hàm `setLyDoHuy`: gán `Ly` `Do` `Huy`. */
    public void setLyDoHuy(String lyDoHuy) { this.lyDoHuy = lyDoHuy; }
    /** Hàm `getIpTao`: lấy `Ip` `Tao`. */
    public String getIpTao() { return ipTao; }
    /** Hàm `setIpTao`: gán `Ip` `Tao`. */
    public void setIpTao(String ipTao) { this.ipTao = ipTao; }
    /** Hàm `getThietBi`: lấy `Thiet` `Bi`. */
    public String getThietBi() { return thietBi; }
    /** Hàm `setThietBi`: gán `Thiet` `Bi`. */
    public void setThietBi(String thietBi) { this.thietBi = thietBi; }
}
