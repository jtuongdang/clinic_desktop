package vn.clinicmanagement.application.prescription.approve;

import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;

import java.time.LocalDateTime;

public record ApproveDonThuocBienNhan(
        int prescriptionId,
        int visitId,
        TrangThaiDonThuoc trangThai,
        LocalDateTime approvedAt) {
}
