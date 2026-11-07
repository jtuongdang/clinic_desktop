package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;

import java.time.LocalDateTime;

public record CancelPrescriptionReceipt(
        int prescriptionId,
        int visitId,
        String lyDoHuy,
        TrangThaiDonThuoc trangThai,
        LocalDateTime cancelledAt) {
}
