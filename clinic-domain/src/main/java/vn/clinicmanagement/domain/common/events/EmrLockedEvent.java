package vn.clinicmanagement.domain.common.events;

public class EmrLockedEvent extends AbstractDomainEvent {
    private final int visitId;
    private final int doctorId;
    private final String signature;

    public EmrLockedEvent(int visitId, int doctorId, String signature) {
        super();
        this.visitId = visitId;
        this.doctorId = doctorId;
        this.signature = signature;
    }

    public int getVisitId() { return visitId; }
    public int getDoctorId() { return doctorId; }
    public String getSignature() { return signature; }
}
