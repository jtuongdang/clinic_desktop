package vn.clinicmanagement.domain.common.events;

public class DonThuocDaHuySuKien extends TruuTuongMienNghiepVuSuKien {
    /** Trường `prescriptionId`: đơn thuốc mã. */
    private final int prescriptionId;
    /** Trường `reason`: lý do. */
    private final String reason;

    public DonThuocDaHuySuKien(int prescriptionId, String reason) {
        super();
        this.prescriptionId = prescriptionId;
        this.reason = reason;
    }

    /** Hàm `getPrescriptionId`: lấy đơn thuốc mã. */
    public int getPrescriptionId() { return prescriptionId; }
    /** Hàm `getReason`: lấy lý do. */
    public String getReason() { return reason; }
}
