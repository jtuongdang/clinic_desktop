package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import java.time.LocalDate;

public class LichLamViecDieuChinh extends CoSoThucThe<Integer> {
    private int maLichLamViec;
    private LocalDate ngayThayDoi;
    private int maCaTrucMoi;
    private String lyDo;
    public LichLamViecDieuChinh() {}
    /** Hàm `getMaLichLamViec`: lấy mã lich lam viec. */
    public int getMaLichLamViec() { return maLichLamViec; }
    /** Hàm `setMaLichLamViec`: gán mã lich lam viec. */
    public void setMaLichLamViec(int maLichLamViec) { this.maLichLamViec = maLichLamViec; }
    /** Hàm `getNgayThayDoi`: lấy `Ngay` `Thay` doi. */
    public LocalDate getNgayThayDoi() { return ngayThayDoi; }
    /** Hàm `setNgayThayDoi`: gán `Ngay` `Thay` doi. */
    public void setNgayThayDoi(LocalDate ngayThayDoi) { this.ngayThayDoi = ngayThayDoi; }
    /** Hàm `getMaCaTrucMoi`: lấy mã ca truc moi. */
    public int getMaCaTrucMoi() { return maCaTrucMoi; }
    /** Hàm `setMaCaTrucMoi`: gán mã ca truc moi. */
    public void setMaCaTrucMoi(int maCaTrucMoi) { this.maCaTrucMoi = maCaTrucMoi; }
    /** Hàm `getLyDo`: lấy `Ly` `Do`. */
    public String getLyDo() { return lyDo; }
    /** Hàm `setLyDo`: gán `Ly` `Do`. */
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }
}
