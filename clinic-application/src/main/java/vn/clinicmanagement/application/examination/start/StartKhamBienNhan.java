package vn.clinicmanagement.application.examination.start;

import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;

import java.time.LocalDateTime;

public record StartKhamBienNhan(int visitId, TrangThaiLuotKham status, LocalDateTime startedAt) {
}
