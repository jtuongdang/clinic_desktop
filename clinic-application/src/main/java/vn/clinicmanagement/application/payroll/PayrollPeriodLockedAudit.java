package vn.clinicmanagement.application.payroll;

import java.time.Instant;

public record PayrollPeriodLockedAudit(
        int periodId,
        int actorEmployeeId,
        int actorRoleId,
        String actorRoleCode,
        Instant occurredAt,
        String actionCode) {}
