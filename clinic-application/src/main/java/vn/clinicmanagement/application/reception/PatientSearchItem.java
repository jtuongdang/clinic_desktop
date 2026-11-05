package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;

public record PatientSearchItem(BenhNhan patient, BaoHiem latestInsurance) {
    @Override public String toString() {
        return patient.getHoTen() + " · " + patient.getMaBenhNhanFormat()
                + (patient.getDienThoai() == null || patient.getDienThoai().isBlank() ? "" : " · " + patient.getDienThoai());
    }
}
