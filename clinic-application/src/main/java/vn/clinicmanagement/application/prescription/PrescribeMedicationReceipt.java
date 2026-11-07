package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PrescribeMedicationReceipt(
        int prescriptionId,
        int visitId,
        int itemCount,
        BigDecimal totalAmount,
        TrangThaiDonThuoc trangThai,
        LocalDateTime prescribedAt) {
}
