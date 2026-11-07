package vn.clinicmanagement.application.examination;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RecordVitalSignsReceipt(int visitId, BigDecimal bmi, LocalDateTime recordedAt) {}
