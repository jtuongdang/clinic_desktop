package vn.clinicmanagement.domain.system;
import vn.clinicmanagement.domain.common.BaseEntity;
import java.time.LocalDateTime;

public class TokenLamMoi extends BaseEntity<Integer> {
    private int maNhanVien;
    private String token = "";
    private LocalDateTime hetHan;
    private boolean daHuy = false;
    private String lyDoHuy;
    private String ipTao;
    private String thietBi;

    public TokenLamMoi() {}
    public boolean daHetHan() { return LocalDateTime.now().isAfter(hetHan); }
    public int getMaNhanVien() { return maNhanVien; }
    public void setMaNhanVien(int maNhanVien) { this.maNhanVien = maNhanVien; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public LocalDateTime getHetHan() { return hetHan; }
    public void setHetHan(LocalDateTime hetHan) { this.hetHan = hetHan; }
    public boolean isDaHuy() { return daHuy; }
    public void setDaHuy(boolean daHuy) { this.daHuy = daHuy; }
    public String getLyDoHuy() { return lyDoHuy; }
    public void setLyDoHuy(String lyDoHuy) { this.lyDoHuy = lyDoHuy; }
    public String getIpTao() { return ipTao; }
    public void setIpTao(String ipTao) { this.ipTao = ipTao; }
    public String getThietBi() { return thietBi; }
    public void setThietBi(String thietBi) { this.thietBi = thietBi; }
}
