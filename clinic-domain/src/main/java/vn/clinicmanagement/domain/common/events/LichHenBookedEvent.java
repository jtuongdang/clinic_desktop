package vn.clinicmanagement.domain.common.events;

public class LichHenBookedEvent extends AbstractDomainEvent {
    private final int appointmentId;
    private final int patientId;
    private final int doctorId;
    private final java.time.LocalDateTime startTime;

    public LichHenBookedEvent(int appointmentId, int patientId, int doctorId, java.time.LocalDateTime startTime) {
        super();
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.startTime = startTime;
    }

    public int getAppointmentId() { return appointmentId; }
    public int getPatientId() { return patientId; }
    public int getDoctorId() { return doctorId; }
    public java.time.LocalDateTime getStartTime() { return startTime; }
}
