package vn.clinicmanagement.application.audit;

import java.time.Instant;
import java.util.UUID;

public record HeThongKiemToanNhapLieu(
        Integer actorEmployeeId,
        Integer roleId,
        String roleCode,
        Integer facilityId,
        Integer departmentId,
        KiemToanHanhDongMa actionCode,
        KiemToanTaiNguyenLoai resourceType,
        Integer resourceId,
        KiemToanKetQua outcome,
        KiemToanLyDoMa reasonCode,
        Instant occurredAtUtc,
        String workstationId,
        UUID correlationId) {

    public HeThongKiemToanNhapLieu {
        KiemToanSieuDuLieuKiemTra.validate(actorEmployeeId, roleId, roleCode, facilityId, departmentId, actionCode,
                resourceType, resourceId, outcome, occurredAtUtc, workstationId, correlationId);
    }
}
