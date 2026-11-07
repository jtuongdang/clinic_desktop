package vn.clinicmanagement.application.attendance;

import vn.clinicmanagement.domain.common.enums.TrangThaiChamCong;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Biên lai xác nhận kết quả ghi nhận chấm công.
 */
public record RecordAttendanceReceipt(
        int attendanceId,
        int maLichLamViec,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,
        BigDecimal soGioLam,
        BigDecimal soGioDiMuon,
        BigDecimal soGioVeSom,
        TrangThaiChamCong trangThai
) {
    public int getAttendanceId() { return attendanceId; }
    public int getMaLichLamViec() { return maLichLamViec; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public BigDecimal getSoGioLam() { return soGioLam; }
    public BigDecimal getSoGioDiMuon() { return soGioDiMuon; }
    public BigDecimal getSoGioVeSom() { return soGioVeSom; }
    public TrangThaiChamCong getTrangThai() { return trangThai; }
}
