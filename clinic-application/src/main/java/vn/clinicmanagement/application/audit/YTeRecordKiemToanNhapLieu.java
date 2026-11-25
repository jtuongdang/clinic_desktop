package vn.clinicmanagement.application.audit;

import java.time.Instant;
import java.util.UUID;

public record YTeRecordKiemToanNhapLieu(
        int visitId,
        int actorEmployeeId,
        Integer roleId,
        String roleCode,
        Integer facilityId,
        Integer departmentId,
        KiemToanHanhDongMa actionCode,
        KiemToanKetQua outcome,
        KiemToanLyDoMa reasonCode,
        Instant occurredAtUtc,
        String workstationId,
        UUID correlationId) {

    public YTeRecordKiemToanNhapLieu {
        if (visitId <= 0) throw new IllegalArgumentException("visitId must be positive.");
        if (actorEmployeeId <= 0) throw new IllegalArgumentException("actorEmployeeId must be positive.");
        KiemToanSieuDuLieuKiemTra.validate(actorEmployeeId, roleId, roleCode, facilityId, departmentId, actionCode,
                KiemToanTaiNguyenLoai.VISIT, visitId, outcome, occurredAtUtc, workstationId, correlationId);
    }

    public KiemToanTaiNguyenLoai resourceType() {
        return KiemToanTaiNguyenLoai.VISIT;
    }

    public int resourceId() {
        return visitId;
    }
}
