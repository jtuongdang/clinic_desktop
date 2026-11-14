package vn.clinicmanagement.application.payroll;

import java.time.Instant;

public record TienLuongKyDaKhoaKiemToan(
        /** Trường `periodId`: ky mã. */
        int periodId,
        /** Trường `actorEmployeeId`: tacnhan nhân viên mã. */
        int actorEmployeeId,
        /** Trường `actorRoleId`: tacnhan vai trò mã. */
        int actorRoleId,
        /** Trường `actorRoleCode`: tacnhan vai trò mã. */
        String actorRoleCode,
        /** Trường `occurredAt`: `occurred` luc. */
        Instant occurredAt,
        /** Trường `actionCode`: hành động mã. */
        String actionCode) {}
