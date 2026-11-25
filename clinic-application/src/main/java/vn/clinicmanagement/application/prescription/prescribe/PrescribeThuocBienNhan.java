package vn.clinicmanagement.application.prescription.prescribe;

import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PrescribeThuocBienNhan(
        int prescriptionId,
        int visitId,
        int itemCount,
        BigDecimal totalAmount,
        TrangThaiDonThuoc trangThai,
        LocalDateTime prescribedAt) {
}
