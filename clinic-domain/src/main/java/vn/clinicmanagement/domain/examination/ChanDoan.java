package vn.clinicmanagement.domain.examination;

import vn.clinicmanagement.domain.common.BaseEntity;

public class ChanDoan extends BaseEntity<Integer> {
    private int maLuotKham;
    private Integer maICD;
    private String chanDoanText = "";
    private boolean laChinh = false;
    private String loaiChanDoan = "XAC_DINH"; // SO_BO, XAC_DINH, KEM_THEO, BIEN_CHUNG

    private LuotKham luotKham;
    private ICD icd;

    public ChanDoan() {}

    public int getMaLuotKham() { return maLuotKham; }
    public void setMaLuotKham(int maLuotKham) { this.maLuotKham = maLuotKham; }
    public Integer getMaICD() { return maICD; }
    public void setMaICD(Integer maICD) { this.maICD = maICD; }
    public String getChanDoanText() { return chanDoanText; }
    public void setChanDoanText(String chanDoanText) { this.chanDoanText = chanDoanText; }
    public boolean isLaChinh() { return laChinh; }
    public void setLaChinh(boolean laChinh) { this.laChinh = laChinh; }
    public String getLoaiChanDoan() { return loaiChanDoan; }
    public void setLoaiChanDoan(String loaiChanDoan) { this.loaiChanDoan = loaiChanDoan; }
    public LuotKham getLuotKham() { return luotKham; }
    public void setLuotKham(LuotKham luotKham) { this.luotKham = luotKham; }
    public ICD getIcd() { return icd; }
    public void setIcd(ICD icd) { this.icd = icd; }
}
