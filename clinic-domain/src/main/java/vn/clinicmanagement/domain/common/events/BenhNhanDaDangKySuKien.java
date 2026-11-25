package vn.clinicmanagement.domain.common.events;

public class BenhNhanDaDangKySuKien extends TruuTuongMienNghiepVuSuKien {
    private final int patientId;
    private final String cccd;
    private final String fullName;

    public BenhNhanDaDangKySuKien(int patientId, String cccd, String fullName) {
        super();
        this.patientId = patientId;
        this.cccd = cccd;
        this.fullName = fullName;
    }

    public int getPatientId() { return patientId; }
    public String getCccd() { return cccd; }
    public String getFullName() { return fullName; }
}
