package vn.clinicmanagement.application.audit;

import java.time.Instant;
import java.util.UUID;

public record MedicalRecordAuditEntry(
        int visitId,
        int actorEmployeeId,
        Integer roleId,
        String roleCode,
        Integer facilityId,
        Integer departmentId,
        AuditActionCode actionCode,
        AuditOutcome outcome,
        AuditReasonCode reasonCode,
        Instant occurredAtUtc,
        String workstationId,
        UUID correlationId) {

    public MedicalRecordAuditEntry {
        if (visitId <= 0) throw new IllegalArgumentException("visitId must be positive.");
        if (actorEmployeeId <= 0) throw new IllegalArgumentException("actorEmployeeId must be positive.");
        AuditMetadataValidator.validate(actorEmployeeId, roleId, roleCode, facilityId, departmentId, actionCode,
                AuditResourceType.VISIT, visitId, outcome, occurredAtUtc, workstationId, correlationId);
    }

    public AuditResourceType resourceType() {
        return AuditResourceType.VISIT;
    }

    public int resourceId() {
        return visitId;
    }
}
