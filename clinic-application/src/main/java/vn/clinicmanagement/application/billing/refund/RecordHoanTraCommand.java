package vn.clinicmanagement.application.billing.refund;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;

import java.math.BigDecimal;
import java.util.UUID;

public record RecordHoanTraCommand(
        int invoiceId,
        BigDecimal amount,
        PhuongThucThanhToan paymentMethod,
        String reason,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext,
        UUID requestId
) implements Command<RecordHoanTraBienNhan> {
    public RecordHoanTraCommand(
            int invoiceId,
            BigDecimal amount,
            PhuongThucThanhToan paymentMethod,
            String reason,
            PhanQuyenNguCanh authorizationContext,
            KiemToanTacNhanNguCanh auditActorContext
    ) {
        this(invoiceId, amount, paymentMethod, reason, authorizationContext, auditActorContext, UUID.randomUUID());
    }
}
