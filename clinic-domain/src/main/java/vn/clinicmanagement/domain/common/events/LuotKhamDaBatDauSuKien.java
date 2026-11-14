package vn.clinicmanagement.domain.common.events;

public class LuotKhamDaBatDauSuKien extends TruuTuongMienNghiepVuSuKien {
    /** Trường `visitId`: luotkham mã. */
    private final int visitId;
    /** Trường `patientId`: bệnh nhân mã. */
    private final int patientId;
    /** Trường `doctorId`: bacsi mã. */
    private final int doctorId;

    public LuotKhamDaBatDauSuKien(int visitId, int patientId, int doctorId) {
        super();
        this.visitId = visitId;
        this.patientId = patientId;
        this.doctorId = doctorId;
    }

    /** Hàm `getVisitId`: lấy luotkham mã. */
    public int getVisitId() { return visitId; }
    /** Hàm `getPatientId`: lấy bệnh nhân mã. */
    public int getPatientId() { return patientId; }
    /** Hàm `getDoctorId`: lấy bacsi mã. */
    public int getDoctorId() { return doctorId; }
}
