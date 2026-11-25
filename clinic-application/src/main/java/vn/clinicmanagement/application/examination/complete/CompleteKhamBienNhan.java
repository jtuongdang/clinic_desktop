package vn.clinicmanagement.application.examination.complete;

import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;

import java.time.LocalDateTime;

public record CompleteKhamBienNhan(int visitId, TrangThaiLuotKham status, LocalDateTime completedAt) {
}
