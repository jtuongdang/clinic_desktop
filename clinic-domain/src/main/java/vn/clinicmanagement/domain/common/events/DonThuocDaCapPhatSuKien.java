package vn.clinicmanagement.domain.common.events;

public class DonThuocDaCapPhatSuKien extends TruuTuongMienNghiepVuSuKien {
    private final int prescriptionId;
    private final int dispenserId;

    public DonThuocDaCapPhatSuKien(int prescriptionId, int dispenserId) {
        super();
        this.prescriptionId = prescriptionId;
        this.dispenserId = dispenserId;
    }

    public int getPrescriptionId() { return prescriptionId; }
    public int getDispenserId() { return dispenserId; }
}
