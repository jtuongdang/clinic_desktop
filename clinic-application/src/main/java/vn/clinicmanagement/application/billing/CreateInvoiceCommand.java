package vn.clinicmanagement.application.billing;

import vn.clinicmanagement.application.audit.AuditActorContext;
import vn.clinicmanagement.application.authorization.AuthorizationContext;
import vn.clinicmanagement.application.common.Command;

import java.math.BigDecimal;
import java.util.List;

public record CreateInvoiceCommand(
        int visitId,
        int patientId,
        int facilityId,
        BigDecimal insurancePaidAmount,
        BigDecimal advanceAmount,
        BigDecimal discountAmount,
        BigDecimal discountPercentage,
        String discountReason,
        String notes,
        List<InvoiceItemInput> items,
        AuthorizationContext authorizationContext,
        AuditActorContext auditActorContext
) implements Command<CreateInvoiceReceipt> {}
