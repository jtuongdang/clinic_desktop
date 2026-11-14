package vn.clinicmanagement.application.prescription;

import vn.clinicmanagement.domain.common.enums.TrangThaiDonThuoc;

import java.time.LocalDateTime;

public record CancelDonThuocBienNhan(
        /** Trường `prescriptionId`: đơn thuốc mã. */
        int prescriptionId,
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        String lyDoHuy,
        TrangThaiDonThuoc trangThai,
        /** Trường `cancelledAt`: dahuy luc. */
        LocalDateTime cancelledAt) {
}
