package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;
import vn.clinicmanagement.domain.common.enums.PhuongThucThanhToan;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordPaymentCommand(
        int invoiceId,
        BigDecimal amount,
        PhuongThucThanhToan paymentMethod,
        LocalDate debtDueDate,
        String notes,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext
) implements Command<RecordPaymentReceipt> {}
