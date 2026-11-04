package vn.clinicmanagement.domain.common.events;

public class DonThuocApprovedEvent extends AbstractDomainEvent {
    private final int prescriptionId;
    private final int visitId;
    private final int doctorId;

    public DonThuocApprovedEvent(int prescriptionId, int visitId, int doctorId) {
        super();
        this.prescriptionId = prescriptionId;
        this.visitId = visitId;
        this.doctorId = doctorId;
    }

    public int getPrescriptionId() { return prescriptionId; }
    public int getVisitId() { return visitId; }
    public int getDoctorId() { return doctorId; }
}
