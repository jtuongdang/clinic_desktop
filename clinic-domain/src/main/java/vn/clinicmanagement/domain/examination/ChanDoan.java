package vn.clinicmanagement.domain.examination;

import vn.clinicmanagement.domain.common.CoSoThucThe;

public class ChanDoan extends CoSoThucThe<Integer> {
    private int maLuotKham;
    private Integer maICD;
    /** Trường `chanDoanText`: `chan` doan văn bản. */
    private String chanDoanText = "";
    private boolean laChinh = false;
    private String loaiChanDoan = "XAC_DINH"; // SO_BO, XAC_DINH, KEM_THEO, BIEN_CHUNG

    private LuotKham luotKham;
    /** Trường `icd`: icd. */
    private ICD icd;

    public ChanDoan() {}

    /** Hàm `getMaLuotKham`: lấy mã luot kham. */
    public int getMaLuotKham() { return maLuotKham; }
    /** Hàm `setMaLuotKham`: gán mã luot kham. */
    public void setMaLuotKham(int maLuotKham) { this.maLuotKham = maLuotKham; }
    /** Hàm `getMaICD`: lấy mã icd. */
    public Integer getMaICD() { return maICD; }
    /** Hàm `setMaICD`: gán mã icd. */
    public void setMaICD(Integer maICD) { this.maICD = maICD; }
    /** Hàm `getChanDoanText`: lấy chan doan văn bản. */
    public String getChanDoanText() { return chanDoanText; }
    /** Hàm `setChanDoanText`: gán chan doan văn bản. */
    public void setChanDoanText(String chanDoanText) { this.chanDoanText = chanDoanText; }
    /** Hàm `isLaChinh`: là `La` chinh. */
    public boolean isLaChinh() { return laChinh; }
    /** Hàm `setLaChinh`: gán `La` chinh. */
    public void setLaChinh(boolean laChinh) { this.laChinh = laChinh; }
    /** Hàm `getLoaiChanDoan`: lấy loai chan doan. */
    public String getLoaiChanDoan() { return loaiChanDoan; }
    /** Hàm `setLoaiChanDoan`: gán loai chan doan. */
    public void setLoaiChanDoan(String loaiChanDoan) { this.loaiChanDoan = loaiChanDoan; }
    /** Hàm `getLuotKham`: lấy luot kham. */
    public LuotKham getLuotKham() { return luotKham; }
    /** Hàm `setLuotKham`: gán luot kham. */
    public void setLuotKham(LuotKham luotKham) { this.luotKham = luotKham; }
    /** Hàm `getIcd`: lấy icd. */
    public ICD getIcd() { return icd; }
    /** Hàm `setIcd`: gán icd. */
    public void setIcd(ICD icd) { this.icd = icd; }
}
