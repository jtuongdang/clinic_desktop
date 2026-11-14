package vn.clinicmanagement.application.attendance;

import vn.clinicmanagement.domain.common.enums.TrangThaiChamCong;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Biên lai xác nhận kết quả điều chỉnh chấm công.
 */
public record AdjustChamCongBienNhan(
        /** Trường `attendanceId`: chamcong mã. */
        int attendanceId,
        int maLichLamViec,
        /** Trường `checkInTime`: kiểm tra trong thời gian. */
        LocalDateTime checkInTime,
        /** Trường `checkOutTime`: kiểm tra `Out` thời gian. */
        LocalDateTime checkOutTime,
        BigDecimal soGioLam,
        BigDecimal soGioDiMuon,
        BigDecimal soGioVeSom,
        TrangThaiChamCong trangThai,
        /** Trường `reason`: lý do. */
        String reason,
        /** Trường `adjustedAt`: `adjusted` luc. */
        LocalDateTime adjustedAt
) {
    /** Hàm `getAttendanceId`: lấy chamcong mã. */
    public int getAttendanceId() { return attendanceId; }
    /** Hàm `getMaLichLamViec`: lấy mã lich lam viec. */
    public int getMaLichLamViec() { return maLichLamViec; }
    /** Hàm `getCheckInTime`: lấy kiểm tra trong thời gian. */
    public LocalDateTime getCheckInTime() { return checkInTime; }
    /** Hàm `getCheckOutTime`: lấy kiểm tra `Out` thời gian. */
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    /** Hàm `getSoGioLam`: lấy so `Gio` lam. */
    public BigDecimal getSoGioLam() { return soGioLam; }
    /** Hàm `getSoGioDiMuon`: lấy so `Gio` `Di` `Muon`. */
    public BigDecimal getSoGioDiMuon() { return soGioDiMuon; }
    /** Hàm `getSoGioVeSom`: lấy so `Gio` `Ve` `Som`. */
    public BigDecimal getSoGioVeSom() { return soGioVeSom; }
    /** Hàm `getTrangThai`: lấy trang thai. */
    public TrangThaiChamCong getTrangThai() { return trangThai; }
    /** Hàm `getReason`: lấy lý do. */
    public String getReason() { return reason; }
    /** Hàm `getAdjustedAt`: lấy `Adjusted` luc. */
    public LocalDateTime getAdjustedAt() { return adjustedAt; }
}
