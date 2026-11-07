package vn.clinicmanagement.application.attendance;

import vn.clinicmanagement.domain.common.enums.TrangThaiChamCong;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO chi tiết một lượt chấm công của nhân viên.
 */
public record AttendanceRecordDto(
        int attendanceId,
        int maLichLamViec,
        int maCaTruc,
        String tenCa,
        LocalDate ngayLamViec,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,
        BigDecimal soGioLam,
        BigDecimal soGioDiMuon,
        BigDecimal soGioVeSom,
        TrangThaiChamCong trangThai
) {
    public int getAttendanceId() { return attendanceId; }
    public int getMaLichLamViec() { return maLichLamViec; }
    public int getMaCaTruc() { return maCaTruc; }
    public String getTenCa() { return tenCa; }
    public LocalDate getNgayLamViec() { return ngayLamViec; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public BigDecimal getSoGioLam() { return soGioLam; }
    public BigDecimal getSoGioDiMuon() { return soGioDiMuon; }
    public BigDecimal getSoGioVeSom() { return soGioVeSom; }
    public TrangThaiChamCong getTrangThai() { return trangThai; }
}
