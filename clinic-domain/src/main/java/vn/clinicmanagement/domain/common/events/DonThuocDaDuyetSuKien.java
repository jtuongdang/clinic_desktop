package vn.clinicmanagement.domain.common.events;

public class DonThuocDaDuyetSuKien extends TruuTuongMienNghiepVuSuKien {
    /** Trường `prescriptionId`: đơn thuốc mã. */
    private final int prescriptionId;
    /** Trường `visitId`: luotkham mã. */
    private final int visitId;
    /** Trường `doctorId`: bacsi mã. */
    private final int doctorId;

    public DonThuocDaDuyetSuKien(int prescriptionId, int visitId, int doctorId) {
        super();
        this.prescriptionId = prescriptionId;
        this.visitId = visitId;
        this.doctorId = doctorId;
    }

    /** Hàm `getPrescriptionId`: lấy đơn thuốc mã. */
    public int getPrescriptionId() { return prescriptionId; }
    /** Hàm `getVisitId`: lấy luotkham mã. */
    public int getVisitId() { return visitId; }
    /** Hàm `getDoctorId`: lấy bacsi mã. */
    public int getDoctorId() { return doctorId; }
}
