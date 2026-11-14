package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;

import java.time.LocalDateTime;

public record ApproveDonThuocBienNhan(
        /** Trường `prescriptionId`: đơn thuốc mã. */
        int prescriptionId,
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        TrangThaiDonThuoc trangThai,
        /** Trường `approvedAt`: daduyet luc. */
        LocalDateTime approvedAt) {
}
