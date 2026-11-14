package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PrescribeThuocBienNhan(
        /** Trường `prescriptionId`: đơn thuốc mã. */
        int prescriptionId,
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `itemCount`: mathang dem. */
        int itemCount,
        BigDecimal totalAmount,
        TrangThaiDonThuoc trangThai,
        /** Trường `prescribedAt`: `prescribed` luc. */
        LocalDateTime prescribedAt) {
}
