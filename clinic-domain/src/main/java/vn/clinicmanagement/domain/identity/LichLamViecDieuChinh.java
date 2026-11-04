package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.BaseEntity;
import java.time.LocalDate;

public class LichLamViecDieuChinh extends BaseEntity<Integer> {
    private int maLichLamViec;
    private LocalDate ngayThayDoi;
    private int maCaTrucMoi;
    private String lyDo;
    public LichLamViecDieuChinh() {}
    public int getMaLichLamViec() { return maLichLamViec; }
    public void setMaLichLamViec(int maLichLamViec) { this.maLichLamViec = maLichLamViec; }
    public LocalDate getNgayThayDoi() { return ngayThayDoi; }
    public void setNgayThayDoi(LocalDate ngayThayDoi) { this.ngayThayDoi = ngayThayDoi; }
    public int getMaCaTrucMoi() { return maCaTrucMoi; }
    public void setMaCaTrucMoi(int maCaTrucMoi) { this.maCaTrucMoi = maCaTrucMoi; }
    public String getLyDo() { return lyDo; }
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }
}
