package vn.clinicmanagement.domain.identity;
import vn.clinicmanagement.domain.common.BaseEntity;
import vn.clinicmanagement.domain.common.enums.TrangThaiChamCong;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ChamCong extends BaseEntity<Integer> {
    private int maLichLamViec;
    private LocalDateTime thoiGianCheckIn;
    private LocalDateTime thoiGianCheckOut;
    private TrangThaiChamCong trangThai = TrangThaiChamCong.CHUA_CHAM_CONG;
    private BigDecimal soGioLam = BigDecimal.ZERO;
    private BigDecimal soGioDiMuon = BigDecimal.ZERO;
    private BigDecimal soGioVeSom = BigDecimal.ZERO;

    public ChamCong() {}
    public int getMaLichLamViec() { return maLichLamViec; }
    public void setMaLichLamViec(int maLichLamViec) { this.maLichLamViec = maLichLamViec; }
    public LocalDateTime getThoiGianCheckIn() { return thoiGianCheckIn; }
    public void setThoiGianCheckIn(LocalDateTime thoiGianCheckIn) { this.thoiGianCheckIn = thoiGianCheckIn; }
    public LocalDateTime getThoiGianCheckOut() { return thoiGianCheckOut; }
    public void setThoiGianCheckOut(LocalDateTime thoiGianCheckOut) { this.thoiGianCheckOut = thoiGianCheckOut; }
    public TrangThaiChamCong getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiChamCong trangThai) { this.trangThai = trangThai; }
    public BigDecimal getSoGioLam() { return soGioLam; }
    public void setSoGioLam(BigDecimal soGioLam) { this.soGioLam = soGioLam; }
    public BigDecimal getSoGioDiMuon() { return soGioDiMuon; }
    public void setSoGioDiMuon(BigDecimal soGioDiMuon) { this.soGioDiMuon = soGioDiMuon; }
    public BigDecimal getSoGioVeSom() { return soGioVeSom; }
    public void setSoGioVeSom(BigDecimal soGioVeSom) { this.soGioVeSom = soGioVeSom; }
}
