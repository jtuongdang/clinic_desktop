package vn.clinicmanagement.domain.patient;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.enums.LoaiTienSu;

public class TienSuBenh extends CoSoThucThe<Integer> {
    private int maBenhNhan;
    private LoaiTienSu loaiTienSu = LoaiTienSu.BENH_NEN;
    private String moTa = "";
    private BenhNhan benhNhan;
    private Integer maCoSo;
    private Integer maKhoa;

    public TienSuBenh() {}
    /** Hàm `getMaBenhNhan`: lấy mã benh nhãn. */
    public int getMaBenhNhan() { return maBenhNhan; }
    /** Hàm `setMaBenhNhan`: gán mã benh nhãn. */
    public void setMaBenhNhan(int maBenhNhan) { this.maBenhNhan = maBenhNhan; }
    /** Hàm `getLoaiTienSu`: lấy loai tien su. */
    public LoaiTienSu getLoaiTienSu() { return loaiTienSu; }
    /** Hàm `setLoaiTienSu`: gán loai tien su. */
    public void setLoaiTienSu(LoaiTienSu loaiTienSu) { this.loaiTienSu = loaiTienSu; }
    /** Hàm `getMoTa`: lấy `Mo` `Ta`. */
    public String getMoTa() { return moTa; }
    /** Hàm `setMoTa`: gán `Mo` `Ta`. */
    public void setMoTa(String moTa) { this.moTa = moTa; }
    /** Hàm `getBenhNhan`: lấy benh nhãn. */
    public BenhNhan getBenhNhan() { return benhNhan; }
    /** Hàm `setBenhNhan`: gán benh nhãn. */
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }

    /** Hàm `getMaCoSo`: lấy mã có so. */
    public Integer getMaCoSo() { return maCoSo; }
    /** Hàm `getMaKhoa`: lấy mã khoa. */
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

    /** Hàm `hasExactOwnershipScope`: có chinhxac `Ownership` phamvi. */
    public boolean hasExactOwnershipScope(int maCoSo, int maKhoa) {
        return maCoSo > 0 && maKhoa > 0
                && this.maCoSo != null && this.maKhoa != null
                && this.maCoSo == maCoSo && this.maKhoa == maKhoa;
    }
}
