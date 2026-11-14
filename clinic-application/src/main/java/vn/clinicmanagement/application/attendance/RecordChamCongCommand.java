package vn.clinicmanagement.application.attendance;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.time.LocalDateTime;

/**
 * Lệnh ghi nhận check-in hoặc check-out theo lịch làm việc.
 */
public record RecordChamCongCommand(
        int maLichLamViec,
        /** Trường `type`: loai. */
        ChamCongLoai type,
        LocalDateTime timestamp,
        /** Trường `checkInTime`: kiểm tra trong thời gian. */
        LocalDateTime checkInTime,
        /** Trường `checkOutTime`: kiểm tra `Out` thời gian. */
        LocalDateTime checkOutTime,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
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

    /** Hàm `checkIn`: kiểm tra trong. */
    public static RecordChamCongCommand checkIn(int maLichLamViec, LocalDateTime checkInTime) {
        return new RecordChamCongCommand(maLichLamViec, ChamCongLoai.CHECK_IN, checkInTime, checkInTime, null, null, null);
    }

    /** Hàm `checkIn`: kiểm tra trong. */
    public static RecordChamCongCommand checkIn(int maLichLamViec, LocalDateTime checkInTime,
                                                  PhanQuyenNguCanh authorizationContext,
                                                  KiemToanTacNhanNguCanh auditActorContext) {
        return new RecordChamCongCommand(maLichLamViec, ChamCongLoai.CHECK_IN, checkInTime, checkInTime, null,
                authorizationContext, auditActorContext);
    }

    /** Hàm `checkOut`: kiểm tra `Out`. */
    public static RecordChamCongCommand checkOut(int maLichLamViec, LocalDateTime checkOutTime) {
        return new RecordChamCongCommand(maLichLamViec, ChamCongLoai.CHECK_OUT, checkOutTime, null, checkOutTime, null, null);
    }

    /** Hàm `checkOut`: kiểm tra `Out`. */
    public static RecordChamCongCommand checkOut(int maLichLamViec, LocalDateTime checkOutTime,
                                                   PhanQuyenNguCanh authorizationContext,
                                                   KiemToanTacNhanNguCanh auditActorContext) {
        return new RecordChamCongCommand(maLichLamViec, ChamCongLoai.CHECK_OUT, checkOutTime, null, checkOutTime,
                authorizationContext, auditActorContext);
    }

    /** Hàm `recordBoth`: ghi nhận `Both`. */
    public static RecordChamCongCommand recordBoth(int maLichLamViec, LocalDateTime checkInTime, LocalDateTime checkOutTime) {
        return new RecordChamCongCommand(maLichLamViec, ChamCongLoai.BOTH, null, checkInTime, checkOutTime, null, null);
    }

    /** Hàm `recordBoth`: ghi nhận `Both`. */
    public static RecordChamCongCommand recordBoth(int maLichLamViec, LocalDateTime checkInTime, LocalDateTime checkOutTime,
                                                     PhanQuyenNguCanh authorizationContext,
                                                     KiemToanTacNhanNguCanh auditActorContext) {
        return new RecordChamCongCommand(maLichLamViec, ChamCongLoai.BOTH, null, checkInTime, checkOutTime,
                authorizationContext, auditActorContext);
    }
}
