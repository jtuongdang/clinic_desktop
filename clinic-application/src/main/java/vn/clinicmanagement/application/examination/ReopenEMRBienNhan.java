package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;

import java.time.LocalDateTime;

public record ReopenEMRBienNhan(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `emrLocked`: emr dakhoa. */
        boolean emrLocked,
        TrangThaiLuotKham trangThai,
        /** Trường `approverId`: `approver` mã. */
        int approverId,
        /** Trường `reopenedAt`: damolai luc. */
        LocalDateTime reopenedAt
) {}
