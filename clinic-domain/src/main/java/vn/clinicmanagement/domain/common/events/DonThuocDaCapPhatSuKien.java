package vn.clinicmanagement.domain.common.events;

public class DonThuocDaCapPhatSuKien extends TruuTuongMienNghiepVuSuKien {
    /** Trường `prescriptionId`: đơn thuốc mã. */
    private final int prescriptionId;
    /** Trường `dispenserId`: `dispenser` mã. */
    private final int dispenserId;

    public DonThuocDaCapPhatSuKien(int prescriptionId, int dispenserId) {
        super();
        this.prescriptionId = prescriptionId;
        this.dispenserId = dispenserId;
    }

    /** Hàm `getPrescriptionId`: lấy đơn thuốc mã. */
    public int getPrescriptionId() { return prescriptionId; }
    /** Hàm `getDispenserId`: lấy `Dispenser` mã. */
    public int getDispenserId() { return dispenserId; }
}
