package vn.clinicmanagement.domain.common.events;

public class DonThuocCancelledEvent extends AbstractDomainEvent {
    private final int prescriptionId;
    private final String reason;

    public DonThuocCancelledEvent(int prescriptionId, String reason) {
        super();
        this.prescriptionId = prescriptionId;
        this.reason = reason;
    }

    public int getPrescriptionId() { return prescriptionId; }
    public String getReason() { return reason; }
}
