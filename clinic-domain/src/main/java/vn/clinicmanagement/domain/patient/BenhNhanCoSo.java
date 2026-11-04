package vn.clinicmanagement.domain.patient;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.facility.CoSo;
import java.time.LocalDateTime;

public class BenhNhanCoSo extends BaseEntity<Integer> {
    private int maBenhNhan;
    private int maCoSo;
    private LocalDateTime ngayDangKy = LocalDateTime.now();
    private BenhNhan benhNhan;
    private CoSo coSo;

    public BenhNhanCoSo() {}
    public int getMaBenhNhan() { return maBenhNhan; }
    public void setMaBenhNhan(int maBenhNhan) { this.maBenhNhan = maBenhNhan; }
    public int getMaCoSo() { return maCoSo; }
    public void setMaCoSo(int maCoSo) { this.maCoSo = maCoSo; }
    public LocalDateTime getNgayDangKy() { return ngayDangKy; }
    public void setNgayDangKy(LocalDateTime ngayDangKy) { this.ngayDangKy = ngayDangKy; }
    public BenhNhan getBenhNhan() { return benhNhan; }
    public void setBenhNhan(BenhNhan benhNhan) { this.benhNhan = benhNhan; }
    public CoSo getCoSo() { return coSo; }
    public void setCoSo(CoSo coSo) { this.coSo = coSo; }
}
