package vn.clinicmanagement.domain.common.events;

public class EMRDaKhoaSuKien extends TruuTuongMienNghiepVuSuKien {
    /** Trường `visitId`: luotkham mã. */
    private final int visitId;
    /** Trường `doctorId`: bacsi mã. */
    private final int doctorId;
    private final String signature;

    public EMRDaKhoaSuKien(int visitId, int doctorId, String signature) {
        super();
        this.visitId = visitId;
        this.doctorId = doctorId;
        this.signature = signature;
    }

    /** Hàm `getVisitId`: lấy luotkham mã. */
    public int getVisitId() { return visitId; }
    /** Hàm `getDoctorId`: lấy bacsi mã. */
    public int getDoctorId() { return doctorId; }
    /** Hàm `getSignature`: lấy `Signature`. */
    public String getSignature() { return signature; }
}
