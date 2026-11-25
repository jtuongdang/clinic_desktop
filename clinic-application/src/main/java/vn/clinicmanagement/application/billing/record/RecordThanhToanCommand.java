package vn.clinicmanagement.application.billing.record;

import vn.clinicmanagement.application.audit.KiemToanTacNhanNguCanh;
import vn.clinicmanagement.application.authorization.PhanQuyenNguCanh;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordThanhToanCommand(
        int invoiceId,
        BigDecimal amount,
        PhuongThucThanhToan paymentMethod,
        LocalDate debtDueDate,
        String notes,
        PhanQuyenNguCanh authorizationContext,
        KiemToanTacNhanNguCanh auditActorContext,
        java.util.UUID requestId
) implements Command<RecordThanhToanBienNhan> {
    public RecordThanhToanCommand(int invoiceId, BigDecimal amount, PhuongThucThanhToan paymentMethod,
            LocalDate debtDueDate, String notes, PhanQuyenNguCanh authorizationContext,
            KiemToanTacNhanNguCanh auditActorContext) {
        this(invoiceId, amount, paymentMethod, debtDueDate, notes, authorizationContext,
                auditActorContext, java.util.UUID.randomUUID());
    }
}
