package vn.clinicmanagement.domain.common.events;

public class DonThuocDaHuySuKien extends TruuTuongMienNghiepVuSuKien {
    private final int prescriptionId;
    private final String reason;

    public DonThuocDaHuySuKien(int prescriptionId, String reason) {
        super();
        this.prescriptionId = prescriptionId;
        this.reason = reason;
    }

    public int getPrescriptionId() { return prescriptionId; }
    public String getReason() { return reason; }
}
