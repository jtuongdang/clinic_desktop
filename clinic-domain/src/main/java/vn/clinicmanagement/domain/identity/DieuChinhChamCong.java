package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.enums.TrangThaiDieuChinhChamCong;
import java.time.LocalDateTime;

public class DieuChinhChamCong extends BaseEntity<Integer> {
    private int maChamCong;
    private LocalDateTime gioCheckInDeXuat;
    private LocalDateTime gioCheckOutDeXuat;
    private String lyDo = "";
    private TrangThaiDieuChinhChamCong trangThai = TrangThaiDieuChinhChamCong.CHO_DUYET;
    public DieuChinhChamCong() {}
    public int getMaChamCong() { return maChamCong; }
    public void setMaChamCong(int maChamCong) { this.maChamCong = maChamCong; }
    public LocalDateTime getGioCheckInDeXuat() { return gioCheckInDeXuat; }
    public void setGioCheckInDeXuat(LocalDateTime gioCheckInDeXuat) { this.gioCheckInDeXuat = gioCheckInDeXuat; }
    public LocalDateTime getGioCheckOutDeXuat() { return gioCheckOutDeXuat; }
    public void setGioCheckOutDeXuat(LocalDateTime gioCheckOutDeXuat) { this.gioCheckOutDeXuat = gioCheckOutDeXuat; }
    public String getLyDo() { return lyDo; }
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }
    public TrangThaiDieuChinhChamCong getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiDieuChinhChamCong trangThai) { this.trangThai = trangThai; }
}
