package vn.clinicmanagement.domain.common.events;

public class BenhNhanDaDangKySuKien extends TruuTuongMienNghiepVuSuKien {
    /** Trường `patientId`: bệnh nhân mã. */
    private final int patientId;
    /** Trường `cccd`: cccd. */
    private final String cccd;
    /** Trường `fullName`: `full` tên. */
    private final String fullName;

    public BenhNhanDaDangKySuKien(int patientId, String cccd, String fullName) {
        super();
        this.patientId = patientId;
        this.cccd = cccd;
        this.fullName = fullName;
    }

    /** Hàm `getPatientId`: lấy bệnh nhân mã. */
    public int getPatientId() { return patientId; }
    /** Hàm `getCccd`: lấy cccd. */
    public String getCccd() { return cccd; }
    /** Hàm `getFullName`: lấy `Full` tên. */
    public String getFullName() { return fullName; }
}
