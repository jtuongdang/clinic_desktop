package vn.clinicmanagement.domain.common.events;

public class PaymentRecordedEvent extends AbstractDomainEvent {
    private final int invoiceId;
    private final java.math.BigDecimal amountPaid;
    private final String paymentMethod;

    public PaymentRecordedEvent(int invoiceId, java.math.BigDecimal amountPaid, String paymentMethod) {
        super();
        this.invoiceId = invoiceId;
        this.amountPaid = amountPaid;
        this.paymentMethod = paymentMethod;
    }

    public int getInvoiceId() { return invoiceId; }
    public java.math.BigDecimal getAmountPaid() { return amountPaid; }
    public String getPaymentMethod() { return paymentMethod; }
}
