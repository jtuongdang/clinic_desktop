package vn.clinicmanagement.application.examination;

import java.time.LocalDateTime;

public record AddChanDoanBienNhan(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `chanDoanText`: `chan` doan văn bản. */
        String chanDoanText,
        boolean laChinh,
        String loaiChanDoan,
        /** Trường `addedAt`: `added` luc. */
        LocalDateTime addedAt) {
}
