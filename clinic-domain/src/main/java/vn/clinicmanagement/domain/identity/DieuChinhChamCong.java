package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.enums.TrangThaiDieuChinhChamCong;
import java.time.LocalDateTime;

public class DieuChinhChamCong extends CoSoThucThe<Integer> {
    /** Trường `maChamCong`: `ma` cham cong. */
    private int maChamCong;
    /** Trường `gioCheckInDeXuat`: `gio` kiểm tra trong `De` xuất. */
    private LocalDateTime gioCheckInDeXuat;
    /** Trường `gioCheckOutDeXuat`: `gio` kiểm tra `Out` `De` xuất. */
    private LocalDateTime gioCheckOutDeXuat;
    private String lyDo = "";
    private TrangThaiDieuChinhChamCong trangThai = TrangThaiDieuChinhChamCong.CHO_DUYET;
    public DieuChinhChamCong() {}
    /** Hàm `getMaChamCong`: lấy mã cham cong. */
    public int getMaChamCong() { return maChamCong; }
    /** Hàm `setMaChamCong`: gán mã cham cong. */
    public void setMaChamCong(int maChamCong) { this.maChamCong = maChamCong; }
    /** Hàm `getGioCheckInDeXuat`: lấy `Gio` kiểm tra trong `De` xuất. */
    public LocalDateTime getGioCheckInDeXuat() { return gioCheckInDeXuat; }
    /** Hàm `setGioCheckInDeXuat`: gán `Gio` kiểm tra trong `De` xuất. */
    public void setGioCheckInDeXuat(LocalDateTime gioCheckInDeXuat) { this.gioCheckInDeXuat = gioCheckInDeXuat; }
    /** Hàm `getGioCheckOutDeXuat`: lấy `Gio` kiểm tra `Out` `De` xuất. */
    public LocalDateTime getGioCheckOutDeXuat() { return gioCheckOutDeXuat; }
    /** Hàm `setGioCheckOutDeXuat`: gán `Gio` kiểm tra `Out` `De` xuất. */
    public void setGioCheckOutDeXuat(LocalDateTime gioCheckOutDeXuat) { this.gioCheckOutDeXuat = gioCheckOutDeXuat; }
    /** Hàm `getLyDo`: lấy `Ly` `Do`. */
    public String getLyDo() { return lyDo; }
    /** Hàm `setLyDo`: gán `Ly` `Do`. */
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }
    /** Hàm `getTrangThai`: lấy trang thai. */
    public TrangThaiDieuChinhChamCong getTrangThai() { return trangThai; }
    /** Hàm `setTrangThai`: gán trang thai. */
    public void setTrangThai(TrangThaiDieuChinhChamCong trangThai) { this.trangThai = trangThai; }
}
