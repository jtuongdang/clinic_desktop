package vn.clinicmanagement.domain.patient;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.enums.LoaiTienSu;

public class TienSuBenh extends BaseEntity<Integer> {
    private int maBenhNhan;
    private LoaiTienSu loaiTienSu = LoaiTienSu.BENH_NEN;
    private String moTa = "";
    private BenhNhan benhNhan;

    public TienSuBenh() {}
    public int getMaBenhNhan() { return maBenhNhan; }
    public void setMaBenhNhan(int maBenhNhan) { this.maBenhNhan = maBenhNhan; }
    public LoaiTienSu getLoaiTienSu() { return loaiTienSu; }
    public void setLoaiTienSu(LoaiTienSu loaiTienSu) { this.loaiTienSu = loaiTienSu; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public BenhNhan getBenhNhan() { return benhNhan; }
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }
}
