package vn.clinicmanagement.application.examination;

import vn.clinicmanagement.domain.common.enums.TrangThaiLuotKham;

import java.time.LocalDateTime;

public record ReopenEMRBienNhan(
        int visitId,
        boolean emrLocked,
        TrangThaiLuotKham trangThai,
        int approverId,
        LocalDateTime reopenedAt
) {}
