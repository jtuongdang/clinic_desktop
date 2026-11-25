package vn.clinicmanagement.application.examination.record;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecordSinhHieuDauHieuBienNhan(int visitId, BigDecimal bmi, LocalDateTime recordedAt) {}
