package vn.clinicmanagement.application.payroll;

import java.time.Instant;

public record TienLuongKyDaKhoaKiemToan(
        int periodId,
        int actorEmployeeId,
        int actorRoleId,
        String actorRoleCode,
        Instant occurredAt,
        String actionCode) {}
