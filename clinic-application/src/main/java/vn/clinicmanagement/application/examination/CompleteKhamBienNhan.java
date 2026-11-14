package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;

import java.time.LocalDateTime;

/** Trường `visitId`: luotkham mã. */
/** Trường `status`: trạng thái. */
/** Trường `completedAt`: dahoantat luc. */
public record CompleteKhamBienNhan(int visitId, TrangThaiLuotKham status, LocalDateTime completedAt) {
}
