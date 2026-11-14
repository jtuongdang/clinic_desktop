package vn.clinicmanagement.application.attendance;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lệnh điều chỉnh thông tin chấm công (check-in, check-out hoặc số giờ công kèm lý do).
 * Chỉ dành cho người có thẩm quyền ADMIN hoặc HR_ADMIN.
 */
public record AdjustChamCongCommand(
        /** Trường `attendanceId`: chamcong mã. */
        int attendanceId,
        /** Trường `newCheckInTime`: `new` kiểm tra trong thời gian. */
        LocalDateTime newCheckInTime,
        /** Trường `newCheckOutTime`: `new` kiểm tra `Out` thời gian. */
        LocalDateTime newCheckOutTime,
        /** Trường `adjustedWorkHours`: `adjusted` làm việc giờ. */
        BigDecimal adjustedWorkHours,
        /** Trường `reason`: lý do. */
        String reason,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext
) implements Command<AdjustChamCongBienNhan> {

    public AdjustChamCongCommand(int attendanceId,
                                  LocalDateTime newCheckInTime,
                                  LocalDateTime newCheckOutTime,
                                  BigDecimal adjustedWorkHours,
                                  String reason,
                                  PhanQuyenNguCanh authorizationContext) {
        this(attendanceId, newCheckInTime, newCheckOutTime, adjustedWorkHours, reason, authorizationContext, null);
    }
}
