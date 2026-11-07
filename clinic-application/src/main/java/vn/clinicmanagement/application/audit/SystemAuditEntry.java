package vn.clinicmanagement.application.audit;

import java.time.Instant;
import java.util.UUID;

public record SystemAuditEntry(
        Integer actorEmployeeId,
        Integer roleId,
        String roleCode,
        Integer facilityId,
        Integer departmentId,
        AuditActionCode actionCode,
        AuditResourceType resourceType,
        Integer resourceId,
        AuditOutcome outcome,
        AuditReasonCode reasonCode,
        Instant occurredAtUtc,
        String workstationId,
        UUID correlationId) {

    public SystemAuditEntry {
        AuditMetadataValidator.validate(actorEmployeeId, roleId, roleCode, facilityId, departmentId, actionCode,
                resourceType, resourceId, outcome, occurredAtUtc, workstationId, correlationId);
    }
}
