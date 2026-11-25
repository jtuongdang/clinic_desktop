package vn.clinicmanagement.application.examination.add;

import java.time.LocalDateTime;

public record AddChanDoanBienNhan(
        int visitId,
        String chanDoanText,
        boolean laChinh,
        String loaiChanDoan,
        LocalDateTime addedAt) {
}
