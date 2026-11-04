package vn.clinicmanagement.domain.common.events;

public class DonThuocDispensedEvent extends AbstractDomainEvent {
    private final int prescriptionId;
    private final int dispenserId;

    public DonThuocDispensedEvent(int prescriptionId, int dispenserId) {
        super();
        this.prescriptionId = prescriptionId;
        this.dispenserId = dispenserId;
    }

    public int getPrescriptionId() { return prescriptionId; }
    public int getDispenserId() { return dispenserId; }
}
