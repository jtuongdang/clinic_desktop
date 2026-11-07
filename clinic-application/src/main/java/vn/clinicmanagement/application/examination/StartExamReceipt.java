package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;

import java.time.LocalDateTime;

public record StartExamReceipt(int visitId, TrangThaiLuotKham status, LocalDateTime startedAt) {
}
