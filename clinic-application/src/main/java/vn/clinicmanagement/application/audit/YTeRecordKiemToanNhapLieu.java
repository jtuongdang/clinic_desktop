package vn.clinicmanagement.application.audit;

import java.time.Instant;
import java.util.UUID;

public record YTeRecordKiemToanNhapLieu(
        /** Trường `visitId`: luotkham mã. */
        int visitId,
        /** Trường `actorEmployeeId`: tacnhan nhân viên mã. */
        int actorEmployeeId,
        /** Trường `roleId`: vai trò mã. */
        Integer roleId,
        /** Trường `roleCode`: vai trò mã. */
        String roleCode,
        /** Trường `facilityId`: cơ sở mã. */
        Integer facilityId,
        /** Trường `departmentId`: khoa mã. */
        Integer departmentId,
        /** Trường `actionCode`: hành động mã. */
        KiemToanHanhDongMa actionCode,
        /** Trường `outcome`: kết quả. */
        KiemToanKetQua outcome,
        /** Trường `reasonCode`: lý do mã. */
        KiemToanLyDoMa reasonCode,
        /** Trường `occurredAtUtc`: `occurred` luc `Utc`. */
        Instant occurredAtUtc,
        /** Trường `workstationId`: `workstation` mã. */
        String workstationId,
        /** Trường `correlationId`: `correlation` mã. */
        UUID correlationId) {

    public YTeRecordKiemToanNhapLieu {
        if (visitId <= 0) throw new IllegalArgumentException("visitId must be positive.");
        if (actorEmployeeId <= 0) throw new IllegalArgumentException("actorEmployeeId must be positive.");
        KiemToanSieuDuLieuKiemTra.validate(actorEmployeeId, roleId, roleCode, facilityId, departmentId, actionCode,
                KiemToanTaiNguyenLoai.VISIT, visitId, outcome, occurredAtUtc, workstationId, correlationId);
    }

    /** Hàm `resourceType`: tainguyen loai. */
    public KiemToanTaiNguyenLoai resourceType() {
        return KiemToanTaiNguyenLoai.VISIT;
    }

    /** Hàm `resourceId`: tainguyen mã. */
    public int resourceId() {
        return visitId;
    }
}
