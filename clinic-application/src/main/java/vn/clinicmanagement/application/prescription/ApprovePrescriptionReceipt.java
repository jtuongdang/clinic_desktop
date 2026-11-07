package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;

import java.time.LocalDateTime;

public record ApprovePrescriptionReceipt(
        int prescriptionId,
        int visitId,
        TrangThaiDonThuoc trangThai,
        LocalDateTime approvedAt) {
}
