package vn.clinicmanagement.application.attendance.record;

import vn.clinicmanagement.application.attendance.ChamCongLoai;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.time.LocalDateTime;

/**
 * Lệnh ghi nhận check-in hoặc check-out theo lịch làm việc.
 */
public record RecordChamCongCommand(
        int maLichLamViec,
        ChamCongLoai type,
        LocalDateTime timestamp,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext
) implements Command<RecordChamCongBienNhan> {

    public RecordChamCongCommand(int maLichLamViec, ChamCongLoai type, LocalDateTime timestamp) {
        this(maLichLamViec, type, timestamp,
                type == ChamCongLoai.CHECK_IN ? timestamp : null,
                type == ChamCongLoai.CHECK_OUT ? timestamp : null,
                null, null);
    }

    public RecordChamCongCommand(int maLichLamViec, ChamCongLoai type, LocalDateTime timestamp,
                                  PhanQuyenNguCanh authorizationContext) {
        this(maLichLamViec, type, timestamp,
                type == ChamCongLoai.CHECK_IN ? timestamp : null,
                type == ChamCongLoai.CHECK_OUT ? timestamp : null,
                authorizationContext, null);
    }

    public static RecordChamCongCommand checkIn(int maLichLamViec, LocalDateTime checkInTime) {
        return new RecordChamCongCommand(maLichLamViec, ChamCongLoai.CHECK_IN, checkInTime, checkInTime, null, null, null);
    }

    public static RecordChamCongCommand checkIn(int maLichLamViec, LocalDateTime checkInTime,
                                                  PhanQuyenNguCanh authorizationContext,
                                                  KiemToanTacNhanNguCanh auditActorContext) {
        return new RecordChamCongCommand(maLichLamViec, ChamCongLoai.CHECK_IN, checkInTime, checkInTime, null,
                authorizationContext, auditActorContext);
    }

    public static RecordChamCongCommand checkOut(int maLichLamViec, LocalDateTime checkOutTime) {
        return new RecordChamCongCommand(maLichLamViec, ChamCongLoai.CHECK_OUT, checkOutTime, null, checkOutTime, null, null);
    }

    public static RecordChamCongCommand checkOut(int maLichLamViec, LocalDateTime checkOutTime,
                                                   PhanQuyenNguCanh authorizationContext,
                                                   KiemToanTacNhanNguCanh auditActorContext) {
        return new RecordChamCongCommand(maLichLamViec, ChamCongLoai.CHECK_OUT, checkOutTime, null, checkOutTime,
                authorizationContext, auditActorContext);
    }

    public static RecordChamCongCommand recordBoth(int maLichLamViec, LocalDateTime checkInTime, LocalDateTime checkOutTime) {
        return new RecordChamCongCommand(maLichLamViec, ChamCongLoai.BOTH, null, checkInTime, checkOutTime, null, null);
    }

    public static RecordChamCongCommand recordBoth(int maLichLamViec, LocalDateTime checkInTime, LocalDateTime checkOutTime,
                                                     PhanQuyenNguCanh authorizationContext,
                                                     KiemToanTacNhanNguCanh auditActorContext) {
        return new RecordChamCongCommand(maLichLamViec, ChamCongLoai.BOTH, null, checkInTime, checkOutTime,
                authorizationContext, auditActorContext);
    }
}
