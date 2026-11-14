package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.enums.TrangThaiChamCong;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ChamCong extends CoSoThucThe<Integer> {
    private int maLichLamViec;
    /** Trường `thoiGianCheckIn`: `thoi` `Gian` kiểm tra trong. */
    private LocalDateTime thoiGianCheckIn;
    /** Trường `thoiGianCheckOut`: `thoi` `Gian` kiểm tra `Out`. */
    private LocalDateTime thoiGianCheckOut;
    private TrangThaiChamCong trangThai = TrangThaiChamCong.CHUA_CHAM_CONG;
    private BigDecimal soGioLam = BigDecimal.ZERO;
    private BigDecimal soGioDiMuon = BigDecimal.ZERO;
    private BigDecimal soGioVeSom = BigDecimal.ZERO;

    public ChamCong() {}
    /** Hàm `getMaLichLamViec`: lấy mã lich lam viec. */
    public int getMaLichLamViec() { return maLichLamViec; }
    /** Hàm `setMaLichLamViec`: gán mã lich lam viec. */
    public void setMaLichLamViec(int maLichLamViec) { this.maLichLamViec = maLichLamViec; }
    /** Hàm `getThoiGianCheckIn`: lấy `Thoi` `Gian` kiểm tra trong. */
    public LocalDateTime getThoiGianCheckIn() { return thoiGianCheckIn; }
    /** Hàm `setThoiGianCheckIn`: gán `Thoi` `Gian` kiểm tra trong. */
    public void setThoiGianCheckIn(LocalDateTime thoiGianCheckIn) { this.thoiGianCheckIn = thoiGianCheckIn; }
    /** Hàm `getThoiGianCheckOut`: lấy `Thoi` `Gian` kiểm tra `Out`. */
    public LocalDateTime getThoiGianCheckOut() { return thoiGianCheckOut; }
    /** Hàm `setThoiGianCheckOut`: gán `Thoi` `Gian` kiểm tra `Out`. */
    public void setThoiGianCheckOut(LocalDateTime thoiGianCheckOut) { this.thoiGianCheckOut = thoiGianCheckOut; }
    /** Hàm `getTrangThai`: lấy trang thai. */
    public TrangThaiChamCong getTrangThai() { return trangThai; }
    /** Hàm `setTrangThai`: gán trang thai. */
    public void setTrangThai(TrangThaiChamCong trangThai) { this.trangThai = trangThai; }
    /** Hàm `getSoGioLam`: lấy so `Gio` lam. */
    public BigDecimal getSoGioLam() { return soGioLam; }
    /** Hàm `setSoGioLam`: gán so `Gio` lam. */
    public void setSoGioLam(BigDecimal soGioLam) { this.soGioLam = soGioLam; }
    /** Hàm `getSoGioDiMuon`: lấy so `Gio` `Di` `Muon`. */
    public BigDecimal getSoGioDiMuon() { return soGioDiMuon; }
    /** Hàm `setSoGioDiMuon`: gán so `Gio` `Di` `Muon`. */
    public void setSoGioDiMuon(BigDecimal soGioDiMuon) { this.soGioDiMuon = soGioDiMuon; }
    /** Hàm `getSoGioVeSom`: lấy so `Gio` `Ve` `Som`. */
    public BigDecimal getSoGioVeSom() { return soGioVeSom; }
    /** Hàm `setSoGioVeSom`: gán so `Gio` `Ve` `Som`. */
    public void setSoGioVeSom(BigDecimal soGioVeSom) { this.soGioVeSom = soGioVeSom; }
}
