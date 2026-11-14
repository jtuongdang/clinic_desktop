package vn.clinicmanagement.application.attendance;

import vn.clinicmanagement.domain.common.enums.TrangThaiChamCong;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO chi tiết một lượt chấm công của nhân viên.
 */
public record ChamCongRecordDuLieu(
        /** Trường `attendanceId`: chamcong mã. */
        int attendanceId,
        int maLichLamViec,
        int maCaTruc,
        String tenCa,
        LocalDate ngayLamViec,
        /** Trường `checkInTime`: kiểm tra trong thời gian. */
        LocalDateTime checkInTime,
        /** Trường `checkOutTime`: kiểm tra `Out` thời gian. */
        LocalDateTime checkOutTime,
        BigDecimal soGioLam,
        BigDecimal soGioDiMuon,
        BigDecimal soGioVeSom,
        TrangThaiChamCong trangThai
) {
    /** Hàm `getAttendanceId`: lấy chamcong mã. */
    public int getAttendanceId() { return attendanceId; }
    /** Hàm `getMaLichLamViec`: lấy mã lich lam viec. */
    public int getMaLichLamViec() { return maLichLamViec; }
    /** Hàm `getMaCaTruc`: lấy mã ca truc. */
    public int getMaCaTruc() { return maCaTruc; }
    /** Hàm `getTenCa`: lấy `Ten` ca. */
    public String getTenCa() { return tenCa; }
    /** Hàm `getNgayLamViec`: lấy `Ngay` lam viec. */
    public LocalDate getNgayLamViec() { return ngayLamViec; }
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
}
