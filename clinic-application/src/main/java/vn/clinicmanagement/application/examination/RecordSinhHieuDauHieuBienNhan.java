package vn.clinicmanagement.application.examination;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Trường `visitId`: luotkham mã. */
/** Trường `recordedAt`: daghinhan luc. */
public record RecordSinhHieuDauHieuBienNhan(int visitId, BigDecimal bmi, LocalDateTime recordedAt) {}
