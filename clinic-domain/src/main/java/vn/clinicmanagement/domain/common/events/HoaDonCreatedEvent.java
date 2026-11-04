package vn.clinicmanagement.domain.common.events;

public class HoaDonCreatedEvent extends AbstractDomainEvent {
    private final int invoiceId;
    private final int visitId;
    private final java.math.BigDecimal totalAmount;

    public HoaDonCreatedEvent(int invoiceId, int visitId, java.math.BigDecimal totalAmount) {
        super();
        this.invoiceId = invoiceId;
        this.visitId = visitId;
        this.totalAmount = totalAmount;
    }

    public int getInvoiceId() { return invoiceId; }
    public int getVisitId() { return visitId; }
    public java.math.BigDecimal getTotalAmount() { return totalAmount; }
}
