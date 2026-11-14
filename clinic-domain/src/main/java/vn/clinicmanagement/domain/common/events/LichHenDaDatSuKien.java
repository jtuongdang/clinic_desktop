package vn.clinicmanagement.domain.common.events;

public class LichHenDaDatSuKien extends TruuTuongMienNghiepVuSuKien {
    /** Trường `appointmentId`: lịch hẹn mã. */
    private final int appointmentId;
    /** Trường `patientId`: bệnh nhân mã. */
    private final int patientId;
    /** Trường `doctorId`: bacsi mã. */
    private final int doctorId;
    /** Trường `startTime`: bắt đầu thời gian. */
    private final java.time.LocalDateTime startTime;

    public LichHenDaDatSuKien(int appointmentId, int patientId, int doctorId, java.time.LocalDateTime startTime) {
        super();
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.startTime = startTime;
    }

    /** Hàm `getAppointmentId`: lấy lịch hẹn mã. */
    public int getAppointmentId() { return appointmentId; }
    /** Hàm `getPatientId`: lấy bệnh nhân mã. */
    public int getPatientId() { return patientId; }
    /** Hàm `getDoctorId`: lấy bacsi mã. */
    public int getDoctorId() { return doctorId; }
    /** Hàm `getStartTime`: lấy bắt đầu thời gian. */
    public java.time.LocalDateTime getStartTime() { return startTime; }
}
