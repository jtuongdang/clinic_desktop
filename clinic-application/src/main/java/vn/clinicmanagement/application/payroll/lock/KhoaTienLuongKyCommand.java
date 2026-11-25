package vn.clinicmanagement.application.payroll.lock;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;

public record KhoaTienLuongKyCommand(
        int periodId,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext
) implements Command<KhoaTienLuongKyBienNhan> {}
