package vn.clinicmanagement.domain.common.events;

public class LuotKhamCompletedEvent extends AbstractDomainEvent {
    private final int visitId;
    private final int patientId;
    private final int doctorId;

    public LuotKhamCompletedEvent(int visitId, int patientId, int doctorId) {
        super();
        this.visitId = visitId;
        this.patientId = patientId;
        this.doctorId = doctorId;
    }

    public int getVisitId() { return visitId; }
    public int getPatientId() { return patientId; }
    public int getDoctorId() { return doctorId; }
}
