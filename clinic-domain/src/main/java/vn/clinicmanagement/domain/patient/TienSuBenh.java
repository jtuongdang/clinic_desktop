package vn.clinicmanagement.domain.patient;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.enums.LoaiTienSu;

public class TienSuBenh extends BaseEntity<Integer> {
    private int maBenhNhan;
    private LoaiTienSu loaiTienSu = LoaiTienSu.BENH_NEN;
    private String moTa = "";
    private BenhNhan benhNhan;
    private Integer maCoSo;
    private Integer maKhoa;

    public TienSuBenh() {}
    public int getMaBenhNhan() { return maBenhNhan; }
    public void setMaBenhNhan(int maBenhNhan) { this.maBenhNhan = maBenhNhan; }
    public LoaiTienSu getLoaiTienSu() { return loaiTienSu; }
    public void setLoaiTienSu(LoaiTienSu loaiTienSu) { this.loaiTienSu = loaiTienSu; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String moTa) { this.moTa = moTa; }
    public BenhNhan getBenhNhan() { return benhNhan; }
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }

    public Integer getMaCoSo() { return maCoSo; }
    public Integer getMaKhoa() { return maKhoa; }

    /** Assigns an ownership pair atomically; null/null represents an unscoped legacy row. */
    public void setOwnershipScope(Integer maCoSo, Integer maKhoa) {
        if ((maCoSo == null) != (maKhoa == null)) {
            throw new IllegalArgumentException("Facility and department scope must be provided together.");
        }
        if (maCoSo != null && (maCoSo <= 0 || maKhoa <= 0)) {
            throw new IllegalArgumentException("Facility and department IDs must be positive.");
        }
        this.maCoSo = maCoSo;
        this.maKhoa = maKhoa;
    }

    public boolean hasExactOwnershipScope(int maCoSo, int maKhoa) {
        return maCoSo > 0 && maKhoa > 0
                && this.maCoSo != null && this.maKhoa != null
                && this.maCoSo == maCoSo && this.maKhoa == maKhoa;
    }
}
