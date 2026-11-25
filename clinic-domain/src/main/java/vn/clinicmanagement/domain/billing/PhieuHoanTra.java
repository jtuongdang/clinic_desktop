package vn.clinicmanagement.domain.billing;

import vn.clinicmanagement.domain.common.CoSoThucThe;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class PhieuHoanTra extends CoSoThucThe<Integer> {
    private UUID requestId;
    private int maHoaDon;
    private BigDecimal soTien = BigDecimal.ZERO;
    private LocalDateTime ngayHoan = LocalDateTime.now();
    private PhuongThucThanhToan phuongThuc = PhuongThucThanhToan.TIEN_MAT;
    private int nguoiHoan;
    private String lyDo = "";
    private HoaDon hoaDon;

    public PhieuHoanTra() {}

    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }

    public int getMaHoaDon() { return maHoaDon; }
    public void setMaHoaDon(int maHoaDon) { this.maHoaDon = maHoaDon; }

    public BigDecimal getSoTien() { return soTien; }
    public void setSoTien(BigDecimal soTien) { this.soTien = soTien; }

    public LocalDateTime getNgayHoan() { return ngayHoan; }
    public void setNgayHoan(LocalDateTime ngayHoan) { this.ngayHoan = ngayHoan; }

    public PhuongThucThanhToan getPhuongThuc() { return phuongThuc; }
    public void setPhuongThuc(PhuongThucThanhToan phuongThuc) { this.phuongThuc = phuongThuc; }

    public int getNguoiHoan() { return nguoiHoan; }
    public void setNguoiHoan(int nguoiHoan) { this.nguoiHoan = nguoiHoan; }

    public String getLyDo() { return lyDo; }
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }

    public HoaDon getHoaDon() { return hoaDon; }
    public void setHoaDon(HoaDon hoaDon) { this.hoaDon = hoaDon; }
}
