package vn.clinicmanagement.domain.patient;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.facility.CoSo;
import java.time.LocalDateTime;

public class BenhNhanCoSo extends CoSoThucThe<Integer> {
    private int maBenhNhan;
    private int maCoSo;
    private LocalDateTime ngayDangKy = LocalDateTime.now();
    private BenhNhan benhNhan;
    private CoSo coSo;

    public BenhNhanCoSo() {}
    /** Hàm `getMaBenhNhan`: lấy mã benh nhãn. */
    public int getMaBenhNhan() { return maBenhNhan; }
    /** Hàm `setMaBenhNhan`: gán mã benh nhãn. */
    public void setMaBenhNhan(int maBenhNhan) { this.maBenhNhan = maBenhNhan; }
    /** Hàm `getMaCoSo`: lấy mã có so. */
    public int getMaCoSo() { return maCoSo; }
    /** Hàm `setMaCoSo`: gán mã có so. */
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    /** Hàm `getNgayDangKy`: lấy `Ngay` `Dang` ky. */
    public LocalDateTime getNgayDangKy() { return ngayDangKy; }
    /** Hàm `setNgayDangKy`: gán `Ngay` `Dang` ky. */
    public void setNgayDangKy(LocalDateTime ngayDangKy) { this.ngayDangKy = ngayDangKy; }
    /** Hàm `getBenhNhan`: lấy benh nhãn. */
    public BenhNhan getBenhNhan() { return benhNhan; }
    /** Hàm `setBenhNhan`: gán benh nhãn. */
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }
    /** Hàm `getCoSo`: lấy có so. */
    public CoSo getCoSo() { return coSo; }
    /** Hàm `setCoSo`: gán có so. */
    public void setCoSo(CoSo coSo) { this.coSo = coSo; }
}
