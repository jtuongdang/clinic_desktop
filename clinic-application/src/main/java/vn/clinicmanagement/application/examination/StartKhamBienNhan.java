package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;

import java.time.LocalDateTime;

/** Trường `visitId`: luotkham mã. */
/** Trường `status`: trạng thái. */
/** Trường `startedAt`: dabatdau luc. */
public record StartKhamBienNhan(int visitId, TrangThaiLuotKham status, LocalDateTime startedAt) {
}
