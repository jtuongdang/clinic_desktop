package vn.clinicmanagement.application.payroll;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

public record KhoaTienLuongKyCommand(
        /** Trường `periodId`: ky mã. */
        int periodId,
        /** Trường `authorizationContext`: phanquyen ngữ cảnh. */
        PhanQuyenNguCanh authorizationContext,
        /** Trường `auditActorContext`: kiểm toán tacnhan ngữ cảnh. */
        KiemToanTacNhanNguCanh auditActorContext
) implements Command<KhoaTienLuongKyBienNhan> {}
