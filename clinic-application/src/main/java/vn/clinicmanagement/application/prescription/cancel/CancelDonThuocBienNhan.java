package vn.clinicmanagement.application.prescription.cancel;

import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;

import java.time.LocalDateTime;

public record CancelDonThuocBienNhan(
        int prescriptionId,
        int visitId,
        String lyDoHuy,
        TrangThaiDonThuoc trangThai,
        LocalDateTime cancelledAt) {
}
