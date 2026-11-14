package vn.clinicmanagement.application.audit;

import java.time.Instant;
import java.util.UUID;

public record HeThongKiemToanNhapLieu(
        /** Trường `actorEmployeeId`: tacnhan nhân viên mã. */
        Integer actorEmployeeId,
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
        /** Trường `resourceType`: tainguyen loai. */
        KiemToanTaiNguyenLoai resourceType,
        /** Trường `resourceId`: tainguyen mã. */
        Integer resourceId,
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

    public HeThongKiemToanNhapLieu {
        KiemToanSieuDuLieuKiemTra.validate(actorEmployeeId, roleId, roleCode, facilityId, departmentId, actionCode,
                resourceType, resourceId, outcome, occurredAtUtc, workstationId, correlationId);
    }
}
