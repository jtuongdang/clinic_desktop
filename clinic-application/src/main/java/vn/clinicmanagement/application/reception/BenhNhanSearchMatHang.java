package vn.clinicmanagement.application.reception;

import vn.clinicmanagement.domain.patient.BaoHiem;
import vn.clinicmanagement.domain.patient.BenhNhan;

/** Trường `patient`: bệnh nhân. */
public record BenhNhanSearchMatHang(BenhNhan patient, BaoHiem latestInsurance) {
    /** Hàm `toString`: den `String`. Gốc khai báo tại `Object#toString`. */
    @Override public String toString() {
        return patient.getHoTen() + " · " + patient.getMaBenhNhanFormat()
                + (patient.getDienThoai() == null || patient.getDienThoai().isBlank() ? "" : " · " + patient.getDienThoai());
    }
}
